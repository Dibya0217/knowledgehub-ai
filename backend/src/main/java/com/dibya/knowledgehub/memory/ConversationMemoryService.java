package com.dibya.knowledgehub.memory;

import com.dibya.knowledgehub.conversation.entity.MessageRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);
    private static final String KEY_PREFIX = "chat:memory:";
    private static final Duration TTL = Duration.ofHours(1);
    private static final int MAX_MESSAGES = 20;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ConversationMemoryService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void push(UUID conversationId, MessageRole role, String content) {
        String key = KEY_PREFIX + conversationId;
        try {
            String json = objectMapper.writeValueAsString(new SimpleMessage(role, content));
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
            redisTemplate.expire(key, TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to push message to Redis memory for conversation {}", conversationId, e);
        }
    }

    public List<SimpleMessage> getHistory(UUID conversationId) {
        String key = KEY_PREFIX + conversationId;
        List<String> entries = redisTemplate.opsForList().range(key, 0, -1);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        return entries.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, SimpleMessage.class);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize Redis message: {}", json, e);
                        return null;
                    }
                })
                .filter(m -> m != null)
                .toList();
    }

    public void clear(UUID conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }
}
