package com.dibya.knowledgehub.chat.dto;

import com.dibya.knowledgehub.conversation.entity.MessageRole;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MessageDTO(UUID id, MessageRole role, String content, OffsetDateTime createdAt, List<CitationDTO> citations) {}
