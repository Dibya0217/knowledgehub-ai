package com.dibya.knowledgehub.chat.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationSummary(UUID id, String title, OffsetDateTime updatedAt) {}
