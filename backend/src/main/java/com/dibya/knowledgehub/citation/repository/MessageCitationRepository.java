package com.dibya.knowledgehub.citation.repository;

import com.dibya.knowledgehub.citation.entity.MessageCitation;
import com.dibya.knowledgehub.conversation.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageCitationRepository extends JpaRepository<MessageCitation, UUID> {
    List<MessageCitation> findByMessageOrderByChunkIndexAsc(Message message);
}
