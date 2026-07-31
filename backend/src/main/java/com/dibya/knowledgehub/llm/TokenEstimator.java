package com.dibya.knowledgehub.llm;

import com.dibya.knowledgehub.conversation.entity.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TokenEstimator {

    private static final int CHARS_PER_TOKEN = 4;
    private static final int MAX_CONTEXT_TOKENS = 3000;

    public int estimate(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.length() / CHARS_PER_TOKEN;
    }

    public List<Message> trimHistory(List<Message> history, String context, String question) {
        int usedTokens = estimate(context) + estimate(question);
        int budget = MAX_CONTEXT_TOKENS - usedTokens;

        if (budget <= 0) {
            return history.size() >= 2 ? history.subList(history.size() - 2, history.size()) : history;
        }

        List<Message> result = new ArrayList<>();
        int accumulated = 0;

        for (int i = history.size() - 1; i >= 0; i--) {
            int tokens = estimate(history.get(i).getContent());
            if (accumulated + tokens > budget && result.size() >= 2) {
                break;
            }
            result.add(0, history.get(i));
            accumulated += tokens;
        }

        return result;
    }
}
