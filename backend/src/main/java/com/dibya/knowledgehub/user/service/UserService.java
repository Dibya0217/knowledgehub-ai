package com.dibya.knowledgehub.user.service;

import com.dibya.knowledgehub.exception.ResourceNotFoundException;
import com.dibya.knowledgehub.user.dto.UpdateProfileRequest;
import com.dibya.knowledgehub.user.dto.UserProfileResponse;
import com.dibya.knowledgehub.user.entity.User;
import com.dibya.knowledgehub.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(value = "users", key = "#email")
    public UserProfileResponse getProfile(String email) {
        log.debug("Fetching profile for: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#email")
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest req) {
        log.debug("Updating profile for: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setName(req.name());
        userRepository.save(user);
        log.info("Profile updated: id={}, email={}", user.getId(), email);
        return toResponse(user);
    }

    private UserProfileResponse toResponse(User user) {
        var roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProvider(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                roles
        );
    }
}
