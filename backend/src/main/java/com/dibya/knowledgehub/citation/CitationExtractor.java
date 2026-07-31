package com.dibya.knowledgehub.citation;

import com.dibya.knowledgehub.citation.entity.MessageCitation;
import com.dibya.knowledgehub.conversation.entity.Message;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CitationExtractor {

    private static final int EXCERPT_MAX_LENGTH = 200;

    public List<MessageCitation> extract(Message message, List<Document> chunks) {
        return chunks.stream()
                .map(doc -> {
                    String docIdStr = (String) doc.getMetadata().getOrDefault("documentId", "");
                    String filename = (String) doc.getMetadata().getOrDefault("filename", "unknown");
                    int chunkIndex = Integer.parseInt((String) doc.getMetadata().getOrDefault("index", "0"));

                    String text = doc.getText();
                    String excerpt = text != null && text.length() > EXCERPT_MAX_LENGTH
                            ? text.substring(0, EXCERPT_MAX_LENGTH) + "..."
                            : text;

                    MessageCitation citation = new MessageCitation();
                    citation.setMessage(message);
                    citation.setDocumentId(docIdStr.isEmpty() ? null : UUID.fromString(docIdStr));
                    citation.setChunkIndex(chunkIndex);
                    citation.setFilename(filename);
                    citation.setExcerpt(excerpt);
                    return citation;
                })
                .toList();
    }
}
