package com.dibya.knowledgehub.document.repository;

import com.dibya.knowledgehub.document.entity.Document;
import com.dibya.knowledgehub.document.entity.DocumentStatus;
import com.dibya.knowledgehub.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByUser(User user, Pageable pageable);
    Optional<Document> findByIdAndUser(UUID id, User user);
    List<Document> findByUserAndStatus(User user, DocumentStatus status);
}
