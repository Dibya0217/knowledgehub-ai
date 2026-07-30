package com.dibya.knowledgehub.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
public class PdfParser implements DocumentParser {

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) throws IOException {
        try (PDDocument doc = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            int pageCount = doc.getNumberOfPages();
            String title = doc.getDocumentInformation().getTitle();
            String author = doc.getDocumentInformation().getAuthor();
            Map<String, String> metadata = Map.of(
                    "title", title != null ? title : "",
                    "author", author != null ? author : ""
            );
            return new ParsedDocument(text.strip(), pageCount, metadata);
        }
    }

    @Override
    public boolean supports(String mimeType) {
        return "application/pdf".equals(mimeType);
    }
}
