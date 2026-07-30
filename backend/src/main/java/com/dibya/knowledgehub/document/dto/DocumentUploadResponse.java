package com.dibya.knowledgehub.document.dto;

import com.dibya.knowledgehub.document.entity.DocumentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentUploadResponse(UUID id, String filename, DocumentStatus status, OffsetDateTime createdAt) {}
