package com.dibya.knowledgehub.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class KnowledgeHubMetrics {

    private final Counter chatRequestsTotal;
    private final Counter documentsUploaded;
    private final Timer ragLatency;
    private final Timer embeddingLatency;

    public KnowledgeHubMetrics(MeterRegistry registry) {
        this.chatRequestsTotal = Counter.builder("knowledgehub.chat.requests.total")
                .description("Total chat/stream requests")
                .register(registry);

        this.documentsUploaded = Counter.builder("knowledgehub.documents.uploaded.total")
                .description("Total documents successfully processed")
                .register(registry);

        this.ragLatency = Timer.builder("knowledgehub.rag.latency")
                .description("RAG retrieval latency")
                .register(registry);

        this.embeddingLatency = Timer.builder("knowledgehub.embedding.latency")
                .description("Vector similarity search latency")
                .register(registry);
    }

    public void incrementChatRequests() {
        chatRequestsTotal.increment();
    }

    public void incrementDocumentsUploaded() {
        documentsUploaded.increment();
    }

    public void recordRagLatency(Duration duration) {
        ragLatency.record(duration);
    }

    public void recordEmbeddingLatency(Duration duration) {
        embeddingLatency.record(duration);
    }
}
