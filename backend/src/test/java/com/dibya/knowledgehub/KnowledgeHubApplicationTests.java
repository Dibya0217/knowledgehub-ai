package com.dibya.knowledgehub;

import com.dibya.knowledgehub.base.PostgresIntegrationTest;
import com.dibya.knowledgehub.email.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class KnowledgeHubApplicationTests extends PostgresIntegrationTest {

    @MockitoBean
    VectorStore vectorStore;

    @MockitoBean
    ChatModel chatModel;

    @MockitoBean
    EmbeddingModel embeddingModel;

    @MockitoBean
    EmailService emailService;

    @Test
    void contextLoads() {
    }
}
