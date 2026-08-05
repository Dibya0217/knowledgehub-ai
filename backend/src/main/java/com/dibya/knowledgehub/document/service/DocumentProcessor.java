package com.dibya.knowledgehub.document.service;

import com.dibya.knowledgehub.audit.AuditService;
import com.dibya.knowledgehub.chunk.ChunkingService;
import com.dibya.knowledgehub.chunk.TextChunk;
import com.dibya.knowledgehub.document.entity.Document;
import com.dibya.knowledgehub.document.entity.DocumentMetadata;
import com.dibya.knowledgehub.document.entity.DocumentStatus;
import com.dibya.knowledgehub.document.repository.DocumentMetadataRepository;
import com.dibya.knowledgehub.document.repository.DocumentRepository;
import com.dibya.knowledgehub.monitoring.KnowledgeHubMetrics;
import com.dibya.knowledgehub.parser.ParsedDocument;
import com.dibya.knowledgehub.parser.ParserFactory;
import com.dibya.knowledgehub.storage.FileStorageService;
import com.dibya.knowledgehub.vector.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class DocumentProcessor {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class);

    private final DocumentRepository documentRepository;
    private final DocumentMetadataRepository metadataRepository;
    private final FileStorageService fileStorageService;
    private final ParserFactory parserFactory;
    private final ChunkingService chunkingService;
    private final VectorStoreService vectorStoreService;
    private final AuditService auditService;
    private final KnowledgeHubMetrics metrics;

    public DocumentProcessor(DocumentRepository documentRepository,
                             DocumentMetadataRepository metadataRepository,
                             FileStorageService fileStorageService,
                             ParserFactory parserFactory,
                             ChunkingService chunkingService,
                             VectorStoreService vectorStoreService,
                             AuditService auditService,
                             KnowledgeHubMetrics metrics) {
        this.documentRepository = documentRepository;
        this.metadataRepository = metadataRepository;
        this.fileStorageService = fileStorageService;
        this.parserFactory = parserFactory;
        this.chunkingService = chunkingService;
        this.vectorStoreService = vectorStoreService;
        this.auditService = auditService;
        this.metrics = metrics;
    }

    @Async
    @Transactional
    public void process(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.error("processDocumentAsync: document not found: {}", documentId);
            return;
        }

        log.info("Processing document: id={}, name='{}'", documentId, document.getOriginalName());
        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        try {
            Resource resource = fileStorageService.load(document.getStoragePath());
            ParsedDocument parsed = parserFactory.select(document.getFileType())
                    .parse(resource.getInputStream(), document.getFilename());
            log.debug("Parsed document {}: pages={}", documentId, parsed.pageCount());

            List<TextChunk> chunks = chunkingService.chunk(
                    parsed.text(), document.getId(), document.getUser().getId(), document.getFilename());
            log.debug("Chunked document {}: {} chunks produced", documentId, chunks.size());

            try {
                vectorStoreService.upsert(chunks);
            } catch (Exception e) {
                log.warn("Vector upsert failed for document {} (Qdrant unavailable?): {}", documentId, e.getMessage());
            }

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

            document.setStatus(DocumentStatus.READY);
            documentRepository.save(document);

            log.info("Document READY: id={}, name='{}', chunks={}, pages={}, words={}",
                    documentId, document.getOriginalName(), chunks.size(), parsed.pageCount(), wordCount);

            metrics.incrementDocumentsUploaded();
            auditService.log(userId, "DOCUMENT_UPLOAD", "document", documentId.toString(), null);

        } catch (Exception e) {
            log.error("Failed to process document {}: {}", documentId, e.getMessage(), e);
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
        }
    }
}
