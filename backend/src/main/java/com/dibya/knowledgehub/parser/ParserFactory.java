package com.dibya.knowledgehub.parser;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
public class ParserFactory {

    private final List<DocumentParser> parsers;
    private final TikaFallbackParser fallback;
    private final Tika tika = new Tika();

    public ParserFactory(List<DocumentParser> parsers, TikaFallbackParser fallback) {
        this.parsers = parsers;
        this.fallback = fallback;
    }

    public DocumentParser select(String mimeType) {
        return parsers.stream()
                .filter(p -> !(p instanceof TikaFallbackParser))
                .filter(p -> p.supports(mimeType))
                .findFirst()
                .orElse(fallback);
    }

    public ParsedDocument parse(MultipartFile file) throws IOException {
        String mimeType = tika.detect(file.getInputStream(), file.getOriginalFilename());
        DocumentParser parser = select(mimeType);
        return parser.parse(file.getInputStream(), file.getOriginalFilename());
    }

    public String detectMimeType(MultipartFile file) throws IOException {
        return tika.detect(file.getInputStream(), file.getOriginalFilename());
    }
}
