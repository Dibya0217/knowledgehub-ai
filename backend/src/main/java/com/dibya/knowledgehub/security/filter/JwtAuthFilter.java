package com.dibya.knowledgehub.security.filter;

import com.dibya.knowledgehub.security.jwt.JwtService;
import com.dibya.knowledgehub.security.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final StringRedisTemplate redisTemplate;

    public JwtAuthFilter(JwtService jwtService,
                         UserDetailsServiceImpl userDetailsService,
                         StringRedisTemplate redisTemplate) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (isBlacklisted(token)) {
            log.debug("Blacklisted JWT detected — skipping authentication for: {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("JWT authenticated: user={}, uri={}", email, request.getRequestURI());
                } else {
                    log.debug("JWT invalid for user={} on uri={}", email, request.getRequestURI());
                }
            }
        } catch (Exception ex) {
            log.debug("JWT processing failed for uri={}: {}", request.getRequestURI(), ex.getMessage());
        }

        chain.doFilter(request, response);
    }

    private boolean isBlacklisted(String token) {
        try {
            String key = "jwt:blacklist:" + token.hashCode();
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }
}
