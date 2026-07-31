package com.dibya.knowledgehub.prompt;

import com.dibya.knowledgehub.conversation.entity.Message;
import com.dibya.knowledgehub.conversation.entity.MessageRole;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PromptBuilder {

    private static final String SYSTEM_TEMPLATE = """
            You are KnowledgeHub AI, a helpful assistant that answers questions \
            based on the user's uploaded documents.
            Use ONLY the context below. If the answer is not in the context, say \
            "I don't have enough information in your documents to answer this."

            Context:
            {context}
            """;

    public String buildSystem(String context) {
        return SYSTEM_TEMPLATE.replace("{context}", context);
    }

    public List<org.springframework.ai.chat.messages.Message> buildMessages(
            String context, List<Message> history, String userQuestion) {

        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        messages.add(new SystemMessage(buildSystem(context)));

        List<Message> recentHistory = history.size() > 10
                ? history.subList(history.size() - 10, history.size())
                : history;

        for (Message msg : recentHistory) {
            if (msg.getRole() == MessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        messages.add(new UserMessage(userQuestion));
        return messages;
    }
}
