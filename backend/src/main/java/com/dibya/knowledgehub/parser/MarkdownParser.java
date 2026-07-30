package com.dibya.knowledgehub.parser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class MarkdownParser implements DocumentParser {

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) throws IOException {
        String text = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        return ParsedDocument.of(text, 1);
    }

    @Override
    public boolean supports(String mimeType) {
        return "text/markdown".equals(mimeType) || "text/x-markdown".equals(mimeType);
    }
}
