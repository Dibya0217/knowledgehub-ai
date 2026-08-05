package com.dibya.knowledgehub.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Primary
@ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "nomic")
public class NomicEmbeddingModel implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(NomicEmbeddingModel.class);
    private static final String JINA_URL = "https://api.jina.ai/v1/embeddings";

    private final RestClient restClient;

    @Value("${app.ai.nomic.api-key:}")
    private String apiKey;

    @Value("${app.ai.nomic.model:jina-embeddings-v3}")
    private String model;

    public NomicEmbeddingModel(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        log.debug("Jina embedding: {} texts", texts.size());

        var body = Map.of("model", model, "input", texts);

        JinaResponse response = restClient.post()
                .uri(JINA_URL)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JinaResponse.class);

        List<Embedding> embeddings = new ArrayList<>();
        if (response != null && response.data() != null) {
            for (JinaEmbeddingData item : response.data()) {
                float[] floats = toFloatArray(item.embedding());
                embeddings.add(new Embedding(floats, item.index()));
            }
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    private float[] toFloatArray(List<Double> doubles) {
        float[] result = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            result[i] = doubles.get(i).floatValue();
        }
        return result;
    }

    record JinaResponse(List<JinaEmbeddingData> data) {}
    record JinaEmbeddingData(List<Double> embedding, Integer index) {}
}
