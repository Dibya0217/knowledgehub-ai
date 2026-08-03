package com.dibya.knowledgehub.auth;

import com.dibya.knowledgehub.audit.AuditService;
import com.dibya.knowledgehub.auth.dto.LoginRequest;
import com.dibya.knowledgehub.auth.dto.RegisterRequest;
import com.dibya.knowledgehub.auth.dto.ResetPasswordRequest;
import com.dibya.knowledgehub.auth.entity.RefreshToken;
import com.dibya.knowledgehub.auth.repository.RefreshTokenRepository;
import com.dibya.knowledgehub.auth.service.AuthService;
import com.dibya.knowledgehub.email.EmailService;
import com.dibya.knowledgehub.exception.ConflictException;
import com.dibya.knowledgehub.exception.UnauthorizedException;
import com.dibya.knowledgehub.role.Role;
import com.dibya.knowledgehub.role.RoleRepository;
import com.dibya.knowledgehub.security.jwt.JwtService;
import com.dibya.knowledgehub.security.service.UserDetailsServiceImpl;
import com.dibya.knowledgehub.user.entity.User;
import com.dibya.knowledgehub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock UserDetailsServiceImpl userDetailsService;
    @Mock AuthenticationManager authenticationManager;
    @Mock StringRedisTemplate redisTemplate;
    @Mock EmailService emailService;
    @Mock AuditService auditService;
    @Mock @SuppressWarnings("unchecked") ValueOperations<String, String> valueOps;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, roleRepository, refreshTokenRepository,
                passwordEncoder, jwtService, userDetailsService,
                authenticationManager, redisTemplate, emailService, auditService
        );
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_success_savesUserAndSendsEmail() {
        var req = new RegisterRequest("test@example.com", "Test User", "password123");
        var role = new Role("ROLE_USER");

        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });

        authService.register(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo(req.email());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(userCaptor.getValue().isEmailVerified()).isFalse();

        verify(emailService).sendVerificationEmail(eq(req.email()), eq(req.name()), anyString());
        verify(valueOps).set(startsWith("email:verify:"), anyString(), anyLong(), any());
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        var req = new RegisterRequest("taken@example.com", "User", "pass");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_badCredentials_propagatesException() {
        var req = new LoginRequest("user@example.com", "wrongpass");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_emailNotVerified_throwsUnauthorized() {
        var req = new LoginRequest("unverified@example.com", "pass");
        var user = buildUser("unverified@example.com", false);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not verified");
    }

    @Test
    void login_success_returnsAuthResponse() {
        var req = new LoginRequest("verified@example.com", "pass");
        var user = buildUser("verified@example.com", true);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername(req.email())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.getExpiryMillis(any())).thenReturn(900_000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(auditService).log(any(), eq("LOGIN"), eq("auth"), isNull(), isNull());
    }

    // ── resetPassword ──────────────────────────────────────────────────────────

    @Test
    void resetPassword_invalidOtp_throwsUnauthorized() {
        var req = new ResetPasswordRequest("user@example.com", "000000", "newpass");
        when(valueOps.get("otp:" + req.email())).thenReturn("999999");

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or expired OTP");
    }

    @Test
    void resetPassword_expiredOtp_throwsUnauthorized() {
        var req = new ResetPasswordRequest("user@example.com", "123456", "newpass");
        when(valueOps.get("otp:" + req.email())).thenReturn(null);

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void resetPassword_success_updatesPasswordAndRevokesTokens() {
        var req = new ResetPasswordRequest("user@example.com", "123456", "newpass");
        var user = buildUser("user@example.com", true);

        when(valueOps.get("otp:" + req.email())).thenReturn("123456");
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("hashed-newpass");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.resetPassword(req);

        assertThat(user.getPasswordHash()).isEqualTo("hashed-newpass");
        verify(refreshTokenRepository).revokeAllByUser(user);
        verify(auditService).log(any(), eq("PASSWORD_RESET"), eq("auth"), isNull(), isNull());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User buildUser(String email, boolean verified) {
        User user = new User();
        user.setEmail(email);
        user.setName("Test");
        user.setEmailVerified(verified);
        user.setRoles(Set.of(new Role("ROLE_USER")));
        return user;
    }
}
