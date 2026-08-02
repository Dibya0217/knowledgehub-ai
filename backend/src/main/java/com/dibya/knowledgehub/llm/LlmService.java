package com.dibya.knowledgehub.llm;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);
    private static final String FALLBACK_MESSAGE = "I'm currently unavailable. Please try again in a moment.";

    private final ChatClient chatClient;

    public LlmService(@Lazy ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Retry(name = "llm", fallbackMethod = "callFallback")
    @CircuitBreaker(name = "llm", fallbackMethod = "callFallback")
    public String call(String systemPrompt, List<Message> messages) {
        log.debug("LLM call: {} messages in history", messages.size());
        String response = chatClient.prompt()
                .system(systemPrompt)
                .messages(messages)
                .call()
                .content();
        log.debug("LLM call completed: responseLength={}", response != null ? response.length() : 0);
        return response;
    }

    public String callFallback(String systemPrompt, List<Message> messages, Exception ex) {
        log.warn("LLM call failed after retries — returning fallback. Cause: {}", ex.getMessage());
        return FALLBACK_MESSAGE;
    }

    @Retry(name = "llm", fallbackMethod = "streamFallback")
    @CircuitBreaker(name = "llm", fallbackMethod = "streamFallback")
    public Flux<String> stream(String systemPrompt, List<Message> messages) {
        log.debug("LLM stream started: {} messages in history", messages.size());
        return chatClient.prompt()
                .system(systemPrompt)
                .messages(messages)
                .stream()
                .content();
    }

    public Flux<String> streamFallback(String systemPrompt, List<Message> messages, Exception ex) {
        log.warn("LLM stream failed after retries — returning fallback. Cause: {}", ex.getMessage());
        return Flux.just(FALLBACK_MESSAGE);
    }
}
