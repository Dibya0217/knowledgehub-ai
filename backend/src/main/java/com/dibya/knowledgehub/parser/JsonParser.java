package com.dibya.knowledgehub.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class JsonParser implements DocumentParser {

    private final ObjectMapper objectMapper;

    public JsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) throws IOException {
        JsonNode root = objectMapper.readTree(inputStream);
        String text = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        return ParsedDocument.of(text, 1);
    }

    @Override
    public boolean supports(String mimeType) {
        return "application/json".equals(mimeType);
    }
}
