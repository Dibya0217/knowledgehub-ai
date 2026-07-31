package com.dibya.knowledgehub.conversation.repository;

import com.dibya.knowledgehub.conversation.entity.Conversation;
import com.dibya.knowledgehub.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Page<Conversation> findByUserOrderByUpdatedAtDesc(User user, Pageable pageable);
    Optional<Conversation> findByIdAndUser(UUID id, User user);
}
