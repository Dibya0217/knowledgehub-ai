package com.dibya.knowledgehub.chat.dto;

import java.util.UUID;

public record SourceReference(UUID documentId, String filename, int chunkIndex) {}
