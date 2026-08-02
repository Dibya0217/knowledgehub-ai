package com.dibya.knowledgehub.chat.service;

import com.dibya.knowledgehub.chat.dto.*;
import com.dibya.knowledgehub.citation.CitationExtractor;
import com.dibya.knowledgehub.citation.entity.MessageCitation;
import com.dibya.knowledgehub.citation.repository.MessageCitationRepository;
import com.dibya.knowledgehub.conversation.entity.Conversation;
import com.dibya.knowledgehub.conversation.entity.Message;
import com.dibya.knowledgehub.conversation.entity.MessageRole;
import com.dibya.knowledgehub.conversation.repository.ConversationRepository;
import com.dibya.knowledgehub.conversation.repository.MessageRepository;
import com.dibya.knowledgehub.exception.ResourceNotFoundException;
import com.dibya.knowledgehub.llm.LlmService;
import com.dibya.knowledgehub.llm.TokenEstimator;
import com.dibya.knowledgehub.memory.ConversationMemoryService;
import com.dibya.knowledgehub.prompt.PromptBuilder;
import com.dibya.knowledgehub.rag.RagService;
import com.dibya.knowledgehub.user.entity.User;
import com.dibya.knowledgehub.user.repository.UserRepository;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Transactional
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageCitationRepository messageCitationRepository;
    private final UserRepository userRepository;
    private final RagService ragService;
    private final LlmService llmService;
    private final PromptBuilder promptBuilder;
    private final CitationExtractor citationExtractor;
    private final ConversationMemoryService memoryService;
    private final TokenEstimator tokenEstimator;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       MessageCitationRepository messageCitationRepository,
                       UserRepository userRepository,
                       RagService ragService,
                       LlmService llmService,
                       PromptBuilder promptBuilder,
                       CitationExtractor citationExtractor,
                       ConversationMemoryService memoryService,
                       TokenEstimator tokenEstimator) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageCitationRepository = messageCitationRepository;
        this.userRepository = userRepository;
        this.ragService = ragService;
        this.llmService = llmService;
        this.promptBuilder = promptBuilder;
        this.citationExtractor = citationExtractor;
        this.memoryService = memoryService;
        this.tokenEstimator = tokenEstimator;
    }

    public ChatResponse chat(ChatRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Conversation conversation = loadOrCreate(request.conversationId(), request.question(), user);

        List<Message> recentHistory = messageRepository.findTop20ByConversationOrderByCreatedAtDesc(conversation);
        List<Message> history = new ArrayList<>(recentHistory);
        Collections.reverse(history);

        List<Document> chunks = ragService.retrieve(request.question(), user.getId(), 5);
        String context = ragService.buildContext(chunks);
        String systemPrompt = promptBuilder.buildSystem(context);
        List<Message> trimmedHistory = tokenEstimator.trimHistory(history, context, request.question());

        List<org.springframework.ai.chat.messages.Message> nonSystemMessages = promptBuilder
                .buildMessages(context, trimmedHistory, request.question())
                .stream()
                .filter(m -> !(m instanceof org.springframework.ai.chat.messages.SystemMessage))
                .toList();

        String answer = llmService.call(systemPrompt, nonSystemMessages);

        Message userMessage = saveMessage(conversation, MessageRole.USER, request.question());
        Message assistantMessage = saveMessage(conversation, MessageRole.ASSISTANT, answer);

        List<MessageCitation> citations = citationExtractor.extract(assistantMessage, chunks);
        if (!citations.isEmpty()) {
            messageCitationRepository.saveAll(citations);
        }

        memoryService.push(conversation.getId(), MessageRole.USER, request.question());
        memoryService.push(conversation.getId(), MessageRole.ASSISTANT, answer);

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

    public Flux<String> stream(String question, UUID conversationId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Conversation conversation = loadOrCreate(conversationId, question, user);

        List<Message> recentHistory = messageRepository.findTop20ByConversationOrderByCreatedAtDesc(conversation);
        List<Message> history = new ArrayList<>(recentHistory);
        Collections.reverse(history);

        List<Document> chunks = ragService.retrieve(question, user.getId(), 5);
        String context = ragService.buildContext(chunks);
        String systemPrompt = promptBuilder.buildSystem(context);
        List<Message> trimmedHistory = tokenEstimator.trimHistory(history, context, question);

        List<org.springframework.ai.chat.messages.Message> nonSystemMessages = promptBuilder
                .buildMessages(context, trimmedHistory, question)
                .stream()
                .filter(m -> !(m instanceof org.springframework.ai.chat.messages.SystemMessage))
                .toList();

        saveMessage(conversation, MessageRole.USER, question);
        memoryService.push(conversation.getId(), MessageRole.USER, question);

        AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());
        UUID conversationId2 = conversation.getId();

        return llmService.stream(systemPrompt, nonSystemMessages)
                .doOnNext(token -> buffer.get().append(token))
                .doOnComplete(() -> {
                    String fullAnswer = buffer.get().toString();
                    Message assistantMessage = saveMessage(
                            conversationRepository.findById(conversationId2).orElseThrow(),
                            MessageRole.ASSISTANT,
                            fullAnswer
                    );
                    List<MessageCitation> citations = citationExtractor.extract(assistantMessage, chunks);
                    if (!citations.isEmpty()) {
                        messageCitationRepository.saveAll(citations);
                    }
                    memoryService.push(conversationId2, MessageRole.ASSISTANT, fullAnswer);
                    conversationRepository.findById(conversationId2).ifPresent(c -> {
                        c.setUpdatedAt(OffsetDateTime.now());
                        conversationRepository.save(c);
                    });
                })
                .concatWith(reactor.core.publisher.Mono.fromCallable(
                        () -> "[CONV:" + conversationId2 + "]"
                ));
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
                .map(m -> {
                    List<CitationDTO> citationDTOs = messageCitationRepository
                            .findByMessageOrderByChunkIndexAsc(m)
                            .stream()
                            .map(c -> new CitationDTO(c.getDocumentId(), c.getFilename(), c.getChunkIndex(), c.getExcerpt()))
                            .toList();
                    return new MessageDTO(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt(), citationDTOs);
                })
                .toList();
    }

    public void deleteConversation(UUID conversationId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Conversation conversation = conversationRepository.findByIdAndUser(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        memoryService.clear(conversation.getId());
        conversationRepository.delete(conversation);
    }

    private Conversation loadOrCreate(UUID conversationId, String question, User user) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUser(conversationId, user)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        }
        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(question.length() > 60 ? question.substring(0, 60) : question);
        return conversationRepository.save(conversation);
    }

    private Message saveMessage(Conversation conversation, MessageRole role, String content) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);
        return messageRepository.save(message);
    }
}
