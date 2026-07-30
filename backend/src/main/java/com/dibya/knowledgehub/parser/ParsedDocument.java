package com.dibya.knowledgehub.parser;

import java.util.Map;

public record ParsedDocument(String text, int pageCount, Map<String, String> metadata) {
    public static ParsedDocument of(String text, int pageCount) {
        return new ParsedDocument(text, pageCount, Map.of());
    }
}
