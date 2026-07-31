package com.dibya.knowledgehub.chat.dto;

import java.util.List;
import java.util.UUID;

public record ChatResponse(UUID conversationId, String answer, List<SourceReference> sources) {}
