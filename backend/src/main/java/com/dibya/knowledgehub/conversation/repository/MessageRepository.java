package com.dibya.knowledgehub.conversation.repository;

import com.dibya.knowledgehub.conversation.entity.Conversation;
import com.dibya.knowledgehub.conversation.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);
    List<Message> findTop20ByConversationOrderByCreatedAtDesc(Conversation conversation);
}
