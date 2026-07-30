package com.dibya.knowledgehub.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

@Component
public class CsvParser implements DocumentParser {

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) throws IOException {
        Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(reader);

        StringJoiner joiner = new StringJoiner("\n");
        int rowCount = 0;

        for (CSVRecord record : records) {
            if (rowCount == 0) {
                Set<String> headers = record.toMap().keySet();
                joiner.add(String.join(",", headers));
            }
            joiner.add(String.join(",", record.toMap().values()));
            rowCount++;
        }

        return ParsedDocument.of(joiner.toString(), 1);
    }

    @Override
    public boolean supports(String mimeType) {
        return "text/csv".equals(mimeType) || "text/comma-separated-values".equals(mimeType);
    }
}
