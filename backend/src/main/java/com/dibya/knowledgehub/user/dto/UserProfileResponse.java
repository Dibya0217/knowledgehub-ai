package com.dibya.knowledgehub.user.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String name,
        String provider,
        boolean emailVerified,
        OffsetDateTime createdAt,
        Set<String> roles
) {}
