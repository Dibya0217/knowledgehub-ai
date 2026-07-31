package com.dibya.knowledgehub.chat.dto;

import java.util.UUID;

public record CitationDTO(UUID documentId, String filename, int chunkIndex, String excerpt) {}
