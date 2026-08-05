package com.dibya.knowledgehub.auth.service;

import com.dibya.knowledgehub.audit.AuditService;
import com.dibya.knowledgehub.auth.dto.AuthResponse;
import com.dibya.knowledgehub.auth.dto.ForgotPasswordRequest;
import com.dibya.knowledgehub.auth.dto.LoginRequest;
import com.dibya.knowledgehub.auth.dto.RegisterRequest;
import com.dibya.knowledgehub.auth.dto.ResetPasswordRequest;
import com.dibya.knowledgehub.auth.dto.SendVerificationRequest;
import com.dibya.knowledgehub.auth.dto.VerifyEmailRequest;
import com.dibya.knowledgehub.auth.entity.RefreshToken;
import com.dibya.knowledgehub.auth.repository.RefreshTokenRepository;
import com.dibya.knowledgehub.email.EmailService;
import com.dibya.knowledgehub.exception.ConflictException;
import com.dibya.knowledgehub.exception.ResourceNotFoundException;
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
import org.springframework.security.authentication.BadCredentialsException;
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
    private final EmailService emailService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserDetailsServiceImpl userDetailsService,
                       AuthenticationManager authenticationManager,
                       StringRedisTemplate redisTemplate,
                       EmailService emailService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationManager = authenticationManager;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    @Transactional
    public void register(RegisterRequest req) {
        log.debug("Registration attempt for email: {}", req.email());
        if (userRepository.existsByEmail(req.email())) {
            log.warn("Registration rejected — email already exists: {}", req.email());
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

        log.info("User registered: id={}, email={}", user.getId(), user.getEmail());
        auditService.log(user.getId(), "REGISTER", "user", user.getId().toString(), null);

        String otp = generateOtp();
        String key = "email:verify:" + req.email();
        redisTemplate.opsForValue().set(key, otp, 15, TimeUnit.MINUTES);
        emailService.sendVerificationEmail(req.email(), req.name(), otp);
        log.debug("Verification email dispatched to: {}", req.email());
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        log.debug("Login attempt for: {}", req.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password())
            );
        } catch (BadCredentialsException ex) {
            log.warn("Login failed — bad credentials for: {}", req.email());
            throw ex;
        }

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.isEmailVerified()) {
            log.warn("Login rejected — email not verified: {}", req.email());
            throw new UnauthorizedException("Email not verified. Please check your inbox.");
        }

        log.info("User logged in: id={}, email={}", user.getId(), user.getEmail());
        auditService.log(user.getId(), "LOGIN", "auth", null, null);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        log.debug("Token refresh requested");
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Refresh token expired or revoked for user: {}", stored.getUser().getEmail());
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        log.debug("Access token refreshed for: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    public void forgotPassword(ForgotPasswordRequest req) {
        log.debug("Forgot-password OTP requested for: {}", req.email());
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with that email address"));
        String otp = generateOtp();
        String key = "otp:" + req.email();
        redisTemplate.opsForValue().set(key, otp, 5, TimeUnit.MINUTES);
        emailService.sendPasswordResetEmail(req.email(), user.getName(), otp);
        log.info("Password reset OTP sent to: {}", req.email());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        log.debug("Password reset attempt for: {}", req.email());
        String key = "otp:" + req.email();
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null || !storedOtp.equals(req.otp())) {
            log.warn("Password reset failed — invalid or expired OTP for: {}", req.email());
            throw new UnauthorizedException("Invalid or expired OTP");
        }
        redisTemplate.delete(key);

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUser(user);

        log.info("Password reset completed for: id={}, email={}", user.getId(), user.getEmail());
        auditService.log(user.getId(), "PASSWORD_RESET", "auth", null, null);
    }

    public void sendVerification(SendVerificationRequest req) {
        log.debug("Resending verification email to: {}", req.email());
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (user.isEmailVerified()) {
            log.debug("Email already verified, skipping resend for: {}", req.email());
            return;
        }
        String otp = generateOtp();
        String key = "email:verify:" + req.email();
        redisTemplate.opsForValue().set(key, otp, 15, TimeUnit.MINUTES);
        emailService.sendVerificationEmail(req.email(), user.getName(), otp);
        log.info("Verification email resent to: {}", req.email());
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest req) {
        log.debug("Email verification attempt for: {}", req.email());
        String key = "email:verify:" + req.email();
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null || !storedOtp.equals(req.otp())) {
            log.warn("Email verification failed — invalid or expired OTP for: {}", req.email());
            throw new UnauthorizedException("Invalid or expired OTP");
        }
        redisTemplate.delete(key);

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email verified for: id={}, email={}", user.getId(), user.getEmail());
    }

    @Transactional
    public void logout(String accessToken, String email) {
        log.debug("Logout requested for: {}", email);
        long ttl = jwtService.getExpiryMillis(accessToken);
        if (ttl > 0) {
            String key = "jwt:blacklist:" + accessToken.hashCode();
            redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.MILLISECONDS);
        }

        userRepository.findByEmail(email).ifPresent(user -> {
            refreshTokenRepository.revokeAllByUser(user);
            log.info("User logged out: id={}, email={}", user.getId(), email);
        });
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
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
