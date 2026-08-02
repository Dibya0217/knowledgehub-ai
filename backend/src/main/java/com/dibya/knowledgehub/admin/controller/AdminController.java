package com.dibya.knowledgehub.admin.controller;

import com.dibya.knowledgehub.admin.dto.AdminStatsResponse;
import com.dibya.knowledgehub.audit.AuditService;
import com.dibya.knowledgehub.common.response.ApiResponse;
import com.dibya.knowledgehub.common.response.PagedResponse;
import com.dibya.knowledgehub.conversation.repository.ConversationRepository;
import com.dibya.knowledgehub.conversation.repository.MessageRepository;
import com.dibya.knowledgehub.document.repository.DocumentRepository;
import com.dibya.knowledgehub.user.dto.UserProfileResponse;
import com.dibya.knowledgehub.user.entity.User;
import com.dibya.knowledgehub.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AuditService auditService;

    public AdminController(UserRepository userRepository,
                           DocumentRepository documentRepository,
                           ConversationRepository conversationRepository,
                           MessageRepository messageRepository,
                           AuditService auditService) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.auditService = auditService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get admin stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = new AdminStatsResponse(
                userRepository.count(),
                documentRepository.count(),
                conversationRepository.count(),
                messageRepository.count()
        );
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails principal) {
        userRepository.findByEmail(principal.getUsername()).ifPresent(admin ->
                auditService.log(admin.getId(), "ADMIN_LIST_USERS", "admin", null, null)
        );
        Page<User> users = userRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        Page<UserProfileResponse> mapped = users.map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(mapped)));
    }

    private UserProfileResponse toResponse(User user) {
        var roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProvider(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                roles
        );
    }
}