package com.dibya.knowledgehub.rag;

import com.dibya.knowledgehub.vector.VectorStoreService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RagService {

    private final VectorStoreService vectorStoreService;

    public RagService(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    public List<Document> retrieve(String question, UUID userId, int topK) {
        return vectorStoreService.search(question, userId, topK);
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
