package com.dibya.knowledgehub.document;

import com.dibya.knowledgehub.base.PostgresIntegrationTest;
import com.dibya.knowledgehub.email.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.vectorstore.SearchRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    EmailService emailService;

    @MockitoBean
    VectorStore vectorStore;

    @MockitoBean
    ChatModel chatModel;

    @MockitoBean
    EmbeddingModel embeddingModel;

    private String accessToken;

    @BeforeEach
    void setUpUser() throws Exception {
        // Unique email per test run to avoid DB conflicts
        String email = "docuser_" + System.nanoTime() + "@example.com";
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        accessToken = registerVerifyAndLogin(email, "password123");
    }

    @Test
    void list_empty_returns200_with_empty_content() throws Exception {
        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void upload_txtFile_returns202_pending() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE,
                "Hello world this is a test document".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void upload_withoutAuth_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/documents/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_nonExistentDocument_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStatus_nonExistentDocument_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/documents/00000000-0000-0000-0000-000000000000/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerVerifyAndLogin(String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email, "name", "Test", "password", password)));

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(eq(email), any(), otpCaptor.capture());

        mockMvc.perform(post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email, "otp", otpCaptor.getValue())));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", password)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String json(String... keysAndValues) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return objectMapper.writeValueAsString(map);
    }
}
