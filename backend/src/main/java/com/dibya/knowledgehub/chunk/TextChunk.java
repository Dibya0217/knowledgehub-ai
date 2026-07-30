package com.dibya.knowledgehub.chunk;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TextChunk(
        String chunkId,
        String text,
        int index,
        UUID documentId,
        UUID userId,
        String filename,
        OffsetDateTime createdAt
) {}
