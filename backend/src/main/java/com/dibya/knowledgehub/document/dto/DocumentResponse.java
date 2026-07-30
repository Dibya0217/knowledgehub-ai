package com.dibya.knowledgehub.document.dto;

import com.dibya.knowledgehub.document.entity.DocumentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String filename,
        String originalName,
        String fileType,
        long fileSize,
        DocumentStatus status,
        Integer pageCount,
        Integer wordCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
