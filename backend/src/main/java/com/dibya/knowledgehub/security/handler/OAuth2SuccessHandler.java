package com.dibya.knowledgehub.security.handler;

import com.dibya.knowledgehub.auth.entity.RefreshToken;
import com.dibya.knowledgehub.auth.repository.RefreshTokenRepository;
import com.dibya.knowledgehub.role.RoleRepository;
import com.dibya.knowledgehub.security.jwt.JwtService;
import com.dibya.knowledgehub.security.service.UserDetailsServiceImpl;
import com.dibya.knowledgehub.user.entity.User;
import com.dibya.knowledgehub.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Set;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public OAuth2SuccessHandler(UserRepository userRepository,
                                RoleRepository roleRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                JwtService jwtService,
                                UserDetailsServiceImpl userDetailsService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            var role = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name != null ? name : email);
            newUser.setProvider("GOOGLE");
            newUser.setEmailVerified(true);
            newUser.setRoles(Set.of(role));
            return userRepository.save(newUser);
        });

        var userDetails = userDetailsService.loadUserByUsername(email);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshTokenValue = jwtService.generateRefreshToken(email);

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenValue,
                user,
                OffsetDateTime.now().plusSeconds(604800)
        );
        refreshTokenRepository.save(refreshToken);

        String redirectUri = allowedOrigins.split(",")[0];
        getRedirectStrategy().sendRedirect(request, response,
                redirectUri + "/oauth2/callback?token=" + accessToken);
    }
}
