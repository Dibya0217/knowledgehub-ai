package com.dibya.knowledgehub.rag;

import com.dibya.knowledgehub.monitoring.KnowledgeHubMetrics;
import com.dibya.knowledgehub.vector.VectorStoreService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RagService {

    private final VectorStoreService vectorStoreService;
    private final KnowledgeHubMetrics metrics;

    public RagService(VectorStoreService vectorStoreService, KnowledgeHubMetrics metrics) {
        this.vectorStoreService = vectorStoreService;
        this.metrics = metrics;
    }

    public List<Document> retrieve(String question, UUID userId, int topK) {
        Instant start = Instant.now();
        List<Document> results = vectorStoreService.search(question, userId, topK);
        metrics.recordRagLatency(Duration.between(start, Instant.now()));
        return results;
    }

    public String buildContext(List<Document> docs) {
        if (docs.isEmpty()) {
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
