package com.dibya.knowledgehub.parser;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentParser {
    ParsedDocument parse(InputStream inputStream, String filename) throws IOException;
    boolean supports(String mimeType);
}
