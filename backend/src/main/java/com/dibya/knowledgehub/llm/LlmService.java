package com.dibya.knowledgehub.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class LlmService {

    private final ChatClient chatClient;

    public LlmService(@Lazy ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public String call(String systemPrompt, List<Message> messages) {
        return chatClient.prompt()
                .system(systemPrompt)
                .messages(messages)
                .call()
                .content();
    }

    public Flux<String> stream(String systemPrompt, List<Message> messages) {
        return chatClient.prompt()
                .system(systemPrompt)
                .messages(messages)
                .stream()
                .content();
    }
}
