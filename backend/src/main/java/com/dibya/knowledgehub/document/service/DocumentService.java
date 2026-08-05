package com.dibya.knowledgehub.document.service;

import com.dibya.knowledgehub.audit.AuditService;
import com.dibya.knowledgehub.document.dto.DocumentResponse;
import com.dibya.knowledgehub.document.dto.DocumentStatusResponse;
import com.dibya.knowledgehub.document.dto.DocumentUploadResponse;
import com.dibya.knowledgehub.document.entity.Document;
import com.dibya.knowledgehub.document.entity.DocumentMetadata;
import com.dibya.knowledgehub.document.entity.DocumentStatus;
import com.dibya.knowledgehub.document.repository.DocumentMetadataRepository;
import com.dibya.knowledgehub.document.repository.DocumentRepository;
import com.dibya.knowledgehub.exception.ResourceNotFoundException;
import com.dibya.knowledgehub.exception.StorageException;
import com.dibya.knowledgehub.parser.ParserFactory;
import com.dibya.knowledgehub.storage.FileStorageService;
import com.dibya.knowledgehub.user.entity.User;
import com.dibya.knowledgehub.user.repository.UserRepository;
import com.dibya.knowledgehub.vector.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final DocumentMetadataRepository metadataRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ParserFactory parserFactory;
    private final VectorStoreService vectorStoreService;
    private final AuditService auditService;
    private final DocumentProcessor documentProcessor;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentMetadataRepository metadataRepository,
                           UserRepository userRepository,
                           FileStorageService fileStorageService,
                           ParserFactory parserFactory,
                           VectorStoreService vectorStoreService,
                           AuditService auditService,
                           DocumentProcessor documentProcessor) {
        this.documentRepository = documentRepository;
        this.metadataRepository = metadataRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.parserFactory = parserFactory;
        this.vectorStoreService = vectorStoreService;
        this.auditService = auditService;
        this.documentProcessor = documentProcessor;
    }

    @Transactional
    @CacheEvict(value = "documents", key = "#email")
    public DocumentUploadResponse upload(MultipartFile file, String email) throws IOException {
        log.info("Upload initiated: file='{}', size={} bytes, user={}", file.getOriginalFilename(), file.getSize(), email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        String mimeType = parserFactory.detectMimeType(file);
        log.debug("Detected MIME type '{}' for file '{}'", mimeType, file.getOriginalFilename());

        Document document = new Document();
        document.setUser(user);
        document.setFilename(sanitizeFilename(file.getOriginalFilename()));
        document.setOriginalName(file.getOriginalFilename());
        document.setFileType(mimeType);
        document.setFileSize(file.getSize());
        document.setStoragePath("");
        document.setStatus(DocumentStatus.PENDING);
        documentRepository.save(document);
        log.debug("Document record created: id={}", document.getId());

        try {
            String storagePath = fileStorageService.store(file, user.getId(), document.getId());
            document.setStoragePath(storagePath);
            documentRepository.save(document);
            log.debug("File stored at: {}", storagePath);
        } catch (IOException e) {
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            log.error("Failed to store file '{}' for user {}: {}", file.getOriginalFilename(), email, e.getMessage(), e);
            throw new StorageException("Failed to store file: " + file.getOriginalFilename(), e);
        }

        documentProcessor.process(document.getId(), user.getId());

        return new DocumentUploadResponse(
                document.getId(), document.getFilename(), document.getStatus(), document.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(String email, Pageable pageable) {
        log.debug("Listing documents for: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return documentRepository.findByUser(user, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID id, String email) {
        log.debug("Fetching document: id={}, user={}", id, email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        Document document = documentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public DocumentStatusResponse getStatus(UUID id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        Document document = documentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        return new DocumentStatusResponse(document.getId(), document.getStatus(), document.getUpdatedAt());
    }

    @Transactional
    @CacheEvict(value = "documents", key = "#email")
    public void deleteDocument(UUID id, String email) {
        log.info("Document delete requested: id={}, user={}", id, email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        Document document = documentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));

        try {
            if (!document.getStoragePath().isBlank()) {
                fileStorageService.delete(document.getStoragePath());
                log.debug("File deleted from storage for document: {}", id);
            }
        } catch (IOException e) {
            log.warn("Could not delete file for document {}: {}", id, e.getMessage());
        }

        try {
            vectorStoreService.deleteByDocumentId(id);
        } catch (Exception e) {
            log.warn("Could not delete vectors for document {} (Qdrant unavailable?): {}", id, e.getMessage());
        }
        documentRepository.delete(document);

        log.info("Document deleted: id={}, name='{}', user={}", id, document.getOriginalName(), email);
        auditService.log(user.getId(), "DOCUMENT_DELETE", "document", id.toString(), null);
    }

    private DocumentResponse toResponse(Document doc) {
        DocumentMetadata meta = metadataRepository.findByDocument(doc).orElse(null);
        return new DocumentResponse(
                doc.getId(), doc.getFilename(), doc.getOriginalName(),
                doc.getFileType(), doc.getFileSize(), doc.getStatus(),
                meta != null ? meta.getPageCount() : null,
                meta != null ? meta.getWordCount() : null,
                doc.getCreatedAt(), doc.getUpdatedAt()
        );
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
