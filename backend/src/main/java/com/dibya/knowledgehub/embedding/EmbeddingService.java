package com.dibya.knowledgehub.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] embed(String text) {
        log.debug("Embedding single text: length={}", text.length());
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        return response.getResults().get(0).getOutput();
    }

    public List<float[]> embedBatch(List<String> texts) {
        log.debug("Batch embedding {} texts", texts.size());
        EmbeddingResponse response = embeddingModel.embedForResponse(texts);
        List<float[]> results = response.getResults().stream()
                .map(r -> r.getOutput())
                .toList();
        log.debug("Batch embedding complete: {} vectors returned", results.size());
        return results;
    }
}
