package com.dibya.knowledgehub.chat.service;

import com.dibya.knowledgehub.chat.dto.*;
import com.dibya.knowledgehub.conversation.entity.Conversation;
import com.dibya.knowledgehub.conversation.entity.Message;
import com.dibya.knowledgehub.conversation.entity.MessageRole;
import com.dibya.knowledgehub.conversation.repository.ConversationRepository;
import com.dibya.knowledgehub.conversation.repository.MessageRepository;
import com.dibya.knowledgehub.exception.ResourceNotFoundException;
import com.dibya.knowledgehub.llm.LlmService;
import com.dibya.knowledgehub.prompt.PromptBuilder;
import com.dibya.knowledgehub.rag.RagService;
import com.dibya.knowledgehub.user.entity.User;
import com.dibya.knowledgehub.user.repository.UserRepository;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RagService ragService;
    private final LlmService llmService;
    private final PromptBuilder promptBuilder;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       UserRepository userRepository,
                       RagService ragService,
                       LlmService llmService,
                       PromptBuilder promptBuilder) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.ragService = ragService;
        this.llmService = llmService;
        this.promptBuilder = promptBuilder;
    }

    public ChatResponse chat(ChatRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Conversation conversation;
        if (request.conversationId() != null) {
            conversation = conversationRepository.findByIdAndUser(request.conversationId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        } else {
            conversation = new Conversation();
            conversation.setUser(user);
            String title = request.question().length() > 60
                    ? request.question().substring(0, 60)
                    : request.question();
            conversation.setTitle(title);
            conversation = conversationRepository.save(conversation);
        }

        List<Message> recentHistory = messageRepository.findTop20ByConversationOrderByCreatedAtDesc(conversation);
        List<Message> history = new ArrayList<>(recentHistory);
        java.util.Collections.reverse(history);

        List<Document> chunks = ragService.retrieve(request.question(), user.getId(), 5);
        String context = ragService.buildContext(chunks);

        List<org.springframework.ai.chat.messages.Message> messages =
                promptBuilder.buildMessages(context, history, request.question());

        String systemPrompt = promptBuilder.buildSystem(context);
        List<org.springframework.ai.chat.messages.Message> nonSystemMessages = messages.stream()
                .filter(m -> !(m instanceof org.springframework.ai.chat.messages.SystemMessage))
                .toList();

        String answer = llmService.call(systemPrompt, nonSystemMessages);

        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(request.question());
        messageRepository.save(userMessage);

        Message assistantMessage = new Message();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(answer);
        messageRepository.save(assistantMessage);

        conversation.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conversation);

        List<SourceReference> sources = chunks.stream()
                .map(doc -> {
                    String docIdStr = (String) doc.getMetadata().getOrDefault("documentId", "");
                    String filename = (String) doc.getMetadata().getOrDefault("filename", "unknown");
                    int chunkIndex = Integer.parseInt((String) doc.getMetadata().getOrDefault("index", "0"));
                    UUID documentId = docIdStr.isEmpty() ? null : UUID.fromString(docIdStr);
                    return new SourceReference(documentId, filename, chunkIndex);
                })
                .toList();

        return new ChatResponse(conversation.getId(), answer, sources);
    }

    @Transactional(readOnly = true)
    public Page<ConversationSummary> listConversations(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return conversationRepository.findByUserOrderByUpdatedAtDesc(user, pageable)
                .map(c -> new ConversationSummary(c.getId(), c.getTitle(), c.getUpdatedAt()));
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(UUID conversationId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Conversation conversation = conversationRepository.findByIdAndUser(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .map(m -> new MessageDTO(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
    }

    public void deleteConversation(UUID conversationId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Conversation conversation = conversationRepository.findByIdAndUser(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        conversationRepository.delete(conversation);
    }
}
