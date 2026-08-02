package com.dibya.knowledgehub.rag;

import com.dibya.knowledgehub.monitoring.KnowledgeHubMetrics;
import com.dibya.knowledgehub.vector.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final VectorStoreService vectorStoreService;
    private final KnowledgeHubMetrics metrics;

    public RagService(VectorStoreService vectorStoreService, KnowledgeHubMetrics metrics) {
        this.vectorStoreService = vectorStoreService;
        this.metrics = metrics;
    }

    public List<Document> retrieve(String question, UUID userId, int topK) {
        log.debug("RAG retrieve: userId={}, topK={}, question length={}", userId, topK, question.length());
        Instant start = Instant.now();
        List<Document> results = vectorStoreService.search(question, userId, topK);
        Duration elapsed = Duration.between(start, Instant.now());
        metrics.recordRagLatency(elapsed);
        log.debug("RAG retrieved {} chunks in {}ms for userId={}", results.size(), elapsed.toMillis(), userId);
        return results;
    }

    public String buildContext(List<Document> docs) {
        if (docs.isEmpty()) {
            log.debug("No relevant chunks found — returning empty context");
            return "No relevant documents found.";
        }
        StringBuilder sb = new StringBuilder();
        for (Document doc : docs) {
            String filename = (String) doc.getMetadata().getOrDefault("filename", "unknown");
            String index = (String) doc.getMetadata().getOrDefault("index", "0");
            sb.append("[Source: ").append(filename).append(", chunk ").append(index).append("]\n");
            sb.append(doc.getText()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
