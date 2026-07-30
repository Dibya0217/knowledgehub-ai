package com.dibya.knowledgehub.parser;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class TikaFallbackParser implements DocumentParser {

    private final Tika tika = new Tika();

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) throws IOException {
        try {
            String text = tika.parseToString(inputStream);
            return ParsedDocument.of(text != null ? text.strip() : "", 1);
        } catch (TikaException e) {
            throw new IOException("Tika failed to parse: " + filename, e);
        }
    }

    @Override
    public boolean supports(String mimeType) {
        return true;
    }
}
