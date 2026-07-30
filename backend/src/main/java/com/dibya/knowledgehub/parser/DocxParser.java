package com.dibya.knowledgehub.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.StringJoiner;

@Component
public class DocxParser implements DocumentParser {

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            StringJoiner joiner = new StringJoiner("\n");

            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    joiner.add(text);
                }
            }

            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    StringJoiner rowJoiner = new StringJoiner("\t");
                    row.getTableCells().forEach(cell -> rowJoiner.add(cell.getText()));
                    joiner.add(rowJoiner.toString());
                }
            }

            return ParsedDocument.of(joiner.toString(), 1);
        }
    }

    @Override
    public boolean supports(String mimeType) {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType);
    }
}
