package com.dibya.knowledgehub.document.controller;

import com.dibya.knowledgehub.common.response.ApiResponse;
import com.dibya.knowledgehub.common.response.PagedResponse;
import com.dibya.knowledgehub.document.dto.DocumentResponse;
import com.dibya.knowledgehub.document.dto.DocumentStatusResponse;
import com.dibya.knowledgehub.document.dto.DocumentUploadResponse;
import com.dibya.knowledgehub.document.service.DocumentService;
import com.dibya.knowledgehub.exception.BadRequestException;
import com.dibya.knowledgehub.parser.ParserFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Upload and manage documents")
public class DocumentController {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/json",
            "text/plain",
            "text/csv",
            "text/comma-separated-values",
            "text/markdown",
            "text/x-markdown"
    );

    private final DocumentService documentService;
    private final ParserFactory parserFactory;

    public DocumentController(DocumentService documentService, ParserFactory parserFactory) {
        this.documentService = documentService;
        this.parserFactory = parserFactory;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document for processing")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> upload(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File exceeds maximum size of 50MB");
        }

        String mimeType = parserFactory.detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BadRequestException("Unsupported file type: " + mimeType);
        }

        DocumentUploadResponse response = documentService.upload(file, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List current user's documents")
    public ResponseEntity<ApiResponse<PagedResponse<DocumentResponse>>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<DocumentResponse> page = PagedResponse.from(
                documentService.listDocuments(userDetails.getUsername(), pageable));
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {

        DocumentResponse response = documentService.getDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get document processing status")
    public ResponseEntity<ApiResponse<DocumentStatusResponse>> getStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {

        DocumentStatusResponse response = documentService.getStatus(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {

        documentService.deleteDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Document deleted successfully"));
    }
}
