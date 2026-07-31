package com.dibya.knowledgehub.chat.dto;

import java.util.UUID;

public record ChatRequest(String question, UUID conversationId) {}
