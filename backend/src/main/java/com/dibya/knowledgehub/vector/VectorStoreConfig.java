package com.dibya.knowledgehub.vector;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Value("${spring.ai.vectorstore.qdrant.host:localhost}")
    private String host;

    @Value("${spring.ai.vectorstore.qdrant.port:6334}")
    private int port;

    @Value("${spring.ai.vectorstore.qdrant.use-tls:false}")
    private boolean useTls;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:knowledge_vectors}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.qdrant.initialize-schema:true}")
    private boolean initializeSchema;

    @Value("${spring.ai.vectorstore.qdrant.api-key:}")
    private String apiKey;

    @Bean(destroyMethod = "close")
    @Lazy
    public QdrantClient qdrantClient() {
        var builder = QdrantGrpcClient.newBuilder(host, port, useTls);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.withApiKey(apiKey);
        }
        return new QdrantClient(builder.build());
    }

    @Bean
    @Lazy
    public VectorStore vectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        var store = QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(collectionName)
                .initializeSchema(initializeSchema)
                .build();
        try {
            qdrantClient.createPayloadIndexAsync(collectionName, "userId", PayloadSchemaType.Keyword, null, null, null, null).get();
            log.info("Qdrant payload index created for 'userId'");
        } catch (Exception e) {
            log.debug("Qdrant userId index already exists or skipped: {}", e.getMessage());
        }
        return store;
    }
}
