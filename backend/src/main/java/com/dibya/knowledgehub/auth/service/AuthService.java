package com.dibya.knowledgehub.auth.service;

import com.dibya.knowledgehub.auth.dto.AuthResponse;
import com.dibya.knowledgehub.auth.dto.ForgotPasswordRequest;
import com.dibya.knowledgehub.auth.dto.LoginRequest;
import com.dibya.knowledgehub.auth.dto.RegisterRequest;
import com.dibya.knowledgehub.auth.dto.ResetPasswordRequest;
import com.dibya.knowledgehub.auth.dto.SendVerificationRequest;
import com.dibya.knowledgehub.auth.dto.VerifyEmailRequest;
import com.dibya.knowledgehub.auth.entity.RefreshToken;
import com.dibya.knowledgehub.auth.repository.RefreshTokenRepository;
import com.dibya.knowledgehub.exception.ConflictException;
import com.dibya.knowledgehub.exception.UnauthorizedException;
import com.dibya.knowledgehub.role.RoleRepository;
import com.dibya.knowledgehub.security.jwt.JwtService;
import com.dibya.knowledgehub.security.service.UserDetailsServiceImpl;
import com.dibya.knowledgehub.user.entity.User;
import com.dibya.knowledgehub.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserDetailsServiceImpl userDetailsService,
                       AuthenticationManager authenticationManager,
                       StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationManager = authenticationManager;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered: " + req.email());
        }

        var role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found — run V1 migration"));

        User user = new User();
        user.setEmail(req.email());
        user.setName(req.name());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setProvider("LOCAL");
        user.setRoles(Set.of(role));
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        return buildAuthResponse(user);
    }

    public void forgotPassword(ForgotPasswordRequest req) {
        if (!userRepository.existsByEmail(req.email())) {
            return;
        }
        String otp = String.format("%06d", new Random().nextInt(999999));
        String key = "otp:" + req.email();
        redisTemplate.opsForValue().set(key, otp, 5, TimeUnit.MINUTES);
        log.info("Password reset OTP for {}: {}", req.email(), otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String key = "otp:" + req.email();
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null || !storedOtp.equals(req.otp())) {
            throw new UnauthorizedException("Invalid or expired OTP");
        }
        redisTemplate.delete(key);

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUser(user);
    }

    public void sendVerification(SendVerificationRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (user.isEmailVerified()) {
            return;
        }
        String otp = String.format("%06d", new Random().nextInt(999999));
        String key = "email:verify:" + req.email();
        redisTemplate.opsForValue().set(key, otp, 15, TimeUnit.MINUTES);
        log.info("Email verification OTP for {}: {}", req.email(), otp);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest req) {
        String key = "email:verify:" + req.email();
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null || !storedOtp.equals(req.otp())) {
            throw new UnauthorizedException("Invalid or expired OTP");
        }
        redisTemplate.delete(key);

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void logout(String accessToken, String email) {
        long ttl = jwtService.getExpiryMillis(accessToken);
        if (ttl > 0) {
            String key = "jwt:blacklist:" + accessToken.hashCode();
            redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.MILLISECONDS);
        }

        userRepository.findByEmail(email).ifPresent(user ->
                refreshTokenRepository.revokeAllByUser(user)
        );
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshTokenValue = jwtService.generateRefreshToken(user.getEmail());

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenValue,
                user,
                OffsetDateTime.now().plusSeconds(604800)
        );
        refreshTokenRepository.save(refreshToken);

        long expiresIn = jwtService.getExpiryMillis(accessToken) / 1000;
        var userInfo = new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getName());
        return AuthResponse.of(accessToken, refreshTokenValue, expiresIn, userInfo);
    }
}
