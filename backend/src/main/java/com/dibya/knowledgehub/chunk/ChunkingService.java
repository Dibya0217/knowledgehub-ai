package com.dibya.knowledgehub.chunk;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkingService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 150;
    private static final String[] SEPARATORS = {"\n\n", "\n", ". ", " "};

    public List<TextChunk> chunk(String text, UUID documentId, UUID userId, String filename) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> rawChunks = splitRecursive(text.strip(), CHUNK_SIZE, CHUNK_OVERLAP);
        List<TextChunk> chunks = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 0; i < rawChunks.size(); i++) {
            chunks.add(new TextChunk(
                    UUID.randomUUID().toString(),
                    rawChunks.get(i),
                    i,
                    documentId,
                    userId,
                    filename,
                    now
            ));
        }
        return chunks;
    }

    private List<String> splitRecursive(String text, int chunkSize, int overlap) {
        if (text.length() <= chunkSize) {
            return text.isBlank() ? List.of() : List.of(text);
        }

        for (String sep : SEPARATORS) {
            List<String> splits = trySplit(text, sep, chunkSize, overlap);
            if (splits != null) {
                return splits;
            }
        }

        // Hard split — no separator worked
        return hardSplit(text, chunkSize, overlap);
    }

    private List<String> trySplit(String text, String sep, int chunkSize, int overlap) {
        String[] parts = text.split(java.util.regex.Pattern.quote(sep), -1);
        if (parts.length <= 1) {
            return null;
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String part : parts) {
            String candidate = current.isEmpty() ? part : current + sep + part;
            if (candidate.length() > chunkSize && !current.isEmpty()) {
                chunks.add(current.toString().strip());
                // Start next chunk with overlap
                String tail = current.toString();
                current = new StringBuilder(tail.length() > overlap
                        ? tail.substring(tail.length() - overlap)
                        : tail);
                current.append(sep).append(part);
            } else {
                current = new StringBuilder(candidate);
            }
        }

        if (!current.isEmpty() && !current.toString().isBlank()) {
            chunks.add(current.toString().strip());
        }

        return chunks.isEmpty() ? null : chunks;
    }

    private List<String> hardSplit(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end).strip());
            start += chunkSize - overlap;
        }
        return chunks;
    }
}
