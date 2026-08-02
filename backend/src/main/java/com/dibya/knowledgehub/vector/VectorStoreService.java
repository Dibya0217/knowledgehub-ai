package com.dibya.knowledgehub.vector;

import com.dibya.knowledgehub.chunk.TextChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final VectorStore vectorStore;

    public VectorStoreService(@Lazy VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void upsert(List<TextChunk> chunks) {
        log.debug("Upserting {} vectors to Qdrant", chunks.size());
        List<Document> docs = chunks.stream()
                .map(chunk -> new Document(
                        chunk.chunkId(),
                        chunk.text(),
                        Map.of(
                                "documentId", chunk.documentId().toString(),
                                "userId",     chunk.userId().toString(),
                                "filename",   chunk.filename(),
                                "index",      String.valueOf(chunk.index())
                        )
                ))
                .toList();
        vectorStore.add(docs);
        log.info("Upserted {} vectors to Qdrant", docs.size());
    }

    public List<Document> search(String query, UUID userId, int topK) {
        log.debug("Vector search: userId={}, topK={}", userId, topK);
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.5)
                        .filterExpression("userId == '" + userId + "'")
                        .build()
        );
        log.debug("Vector search returned {} results for userId={}", results.size(), userId);
        return results;
    }

    public void deleteByDocumentId(UUID documentId) {
        log.debug("Deleting vectors for document: {}", documentId);
        vectorStore.delete("documentId == '" + documentId + "'");
        log.info("Deleted vectors for document: {}", documentId);
    }
}
