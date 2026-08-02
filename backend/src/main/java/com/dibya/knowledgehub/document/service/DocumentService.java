package com.dibya.knowledgehub.document.service;

import com.dibya.knowledgehub.audit.AuditService;
import com.dibya.knowledgehub.chunk.ChunkingService;
import com.dibya.knowledgehub.chunk.TextChunk;
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
import com.dibya.knowledgehub.monitoring.KnowledgeHubMetrics;
import com.dibya.knowledgehub.parser.ParsedDocument;
import com.dibya.knowledgehub.parser.ParserFactory;
import com.dibya.knowledgehub.storage.FileStorageService;
import com.dibya.knowledgehub.user.entity.User;
import com.dibya.knowledgehub.user.repository.UserRepository;
import com.dibya.knowledgehub.vector.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
    private final ChunkingService chunkingService;
    private final VectorStoreService vectorStoreService;
    private final AuditService auditService;
    private final KnowledgeHubMetrics metrics;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentMetadataRepository metadataRepository,
                           UserRepository userRepository,
                           FileStorageService fileStorageService,
                           ParserFactory parserFactory,
                           ChunkingService chunkingService,
                           VectorStoreService vectorStoreService,
                           AuditService auditService,
                           KnowledgeHubMetrics metrics) {
        this.documentRepository = documentRepository;
        this.metadataRepository = metadataRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.parserFactory = parserFactory;
        this.chunkingService = chunkingService;
        this.vectorStoreService = vectorStoreService;
        this.auditService = auditService;
        this.metrics = metrics;
    }

    @Transactional
    @CacheEvict(value = "documents", key = "#email")
    public DocumentUploadResponse upload(MultipartFile file, String email) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        String mimeType = parserFactory.detectMimeType(file);

        Document document = new Document();
        document.setUser(user);
        document.setFilename(sanitizeFilename(file.getOriginalFilename()));
        document.setOriginalName(file.getOriginalFilename());
        document.setFileType(mimeType);
        document.setFileSize(file.getSize());
        document.setStoragePath("");
        document.setStatus(DocumentStatus.PENDING);
        documentRepository.save(document);

        try {
            String storagePath = fileStorageService.store(file, user.getId(), document.getId());
            document.setStoragePath(storagePath);
            documentRepository.save(document);
        } catch (IOException e) {
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            throw new StorageException("Failed to store file: " + file.getOriginalFilename(), e);
        }

        processDocumentAsync(document.getId(), user.getId());

        return new DocumentUploadResponse(
                document.getId(), document.getFilename(), document.getStatus(), document.getCreatedAt());
    }

    @Async
    public void processDocumentAsync(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) return;

        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        try {
            Resource resource = fileStorageService.load(document.getStoragePath());
            ParsedDocument parsed = parserFactory.select(document.getFileType())
                    .parse(resource.getInputStream(), document.getFilename());

            List<TextChunk> chunks = chunkingService.chunk(
                    parsed.text(), document.getId(), document.getUser().getId(), document.getFilename());

            vectorStoreService.upsert(chunks);

            int wordCount = Arrays.stream(parsed.text().split("\\s+"))
                    .filter(w -> !w.isBlank())
                    .mapToInt(w -> 1)
                    .sum();

            DocumentMetadata metadata = new DocumentMetadata();
            metadata.setDocument(document);
            metadata.setPageCount(parsed.pageCount());
            metadata.setWordCount(wordCount);
            metadata.setTitle(parsed.metadata().getOrDefault("title", null));
            metadata.setAuthor(parsed.metadata().getOrDefault("author", null));
            metadataRepository.save(metadata);

            log.info("Processed document {} — {} chunks, {} pages, {} words",
                    documentId, chunks.size(), parsed.pageCount(), wordCount);

            document.setStatus(DocumentStatus.READY);
            documentRepository.save(document);

            metrics.incrementDocumentsUploaded();
            auditService.log(userId, "DOCUMENT_UPLOAD", "document", documentId.toString(), null);

        } catch (Exception e) {
            log.error("Failed to process document {}: {}", documentId, e.getMessage(), e);
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
        }
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return documentRepository.findByUser(user, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID id, String email) {
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        Document document = documentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));

        try {
            if (!document.getStoragePath().isBlank()) {
                fileStorageService.delete(document.getStoragePath());
            }
        } catch (IOException e) {
            log.warn("Could not delete file for document {}: {}", id, e.getMessage());
        }

        vectorStoreService.deleteByDocumentId(id);
        documentRepository.delete(document);

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
