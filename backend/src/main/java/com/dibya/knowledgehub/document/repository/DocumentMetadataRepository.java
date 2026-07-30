package com.dibya.knowledgehub.document.repository;

import com.dibya.knowledgehub.document.entity.Document;
import com.dibya.knowledgehub.document.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, UUID> {
    Optional<DocumentMetadata> findByDocument(Document document);
}
