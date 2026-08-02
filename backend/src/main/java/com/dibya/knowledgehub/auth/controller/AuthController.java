package com.dibya.knowledgehub.auth.controller;

import com.dibya.knowledgehub.auth.dto.AuthResponse;
import com.dibya.knowledgehub.auth.dto.ForgotPasswordRequest;
// AuthResponse still used by login, refresh
import com.dibya.knowledgehub.auth.dto.LoginRequest;
import com.dibya.knowledgehub.auth.dto.RefreshRequest;
import com.dibya.knowledgehub.auth.dto.RegisterRequest;
import com.dibya.knowledgehub.auth.dto.ResetPasswordRequest;
import com.dibya.knowledgehub.auth.dto.SendVerificationRequest;
import com.dibya.knowledgehub.auth.dto.VerifyEmailRequest;
import com.dibya.knowledgehub.auth.service.AuthService;
import com.dibya.knowledgehub.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, refresh, logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user — sends verification email")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created. Check your email for a verification code."));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse response = authService.login(req);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest req) {
        AuthResponse response = authService.refresh(req.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — blacklists current access token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal UserDetails userDetails) {
        String token = authHeader.substring(7);
        authService.logout(token, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset OTP")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("If that email exists, a reset code was sent"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully"));
    }

    @PostMapping("/send-verification")
    @Operation(summary = "Send email verification OTP")
    public ResponseEntity<ApiResponse<Void>> sendVerification(@Valid @RequestBody SendVerificationRequest req) {
        authService.sendVerification(req);
        return ResponseEntity.ok(ApiResponse.ok("Verification code sent if account exists and is unverified"));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email using OTP")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req);
        return ResponseEntity.ok(ApiResponse.ok("Email verified successfully"));
    }
}
