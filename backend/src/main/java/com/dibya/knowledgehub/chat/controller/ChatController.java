package com.dibya.knowledgehub.chat.controller;

import com.dibya.knowledgehub.chat.dto.*;
import com.dibya.knowledgehub.chat.service.ChatService;
import com.dibya.knowledgehub.common.response.ApiResponse;
import com.dibya.knowledgehub.common.response.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ChatResponse response = chatService.chat(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ConversationSummary>>> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<ConversationSummary> result = PagedResponse.from(
                chatService.listConversations(userDetails.getUsername(), pageable));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageDTO>>> getMessages(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<MessageDTO> messages = chatService.getMessages(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatService.deleteConversation(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
