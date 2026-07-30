package com.dibya.knowledgehub.document.dto;

import com.dibya.knowledgehub.document.entity.DocumentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentStatusResponse(UUID id, DocumentStatus status, OffsetDateTime updatedAt) {}
