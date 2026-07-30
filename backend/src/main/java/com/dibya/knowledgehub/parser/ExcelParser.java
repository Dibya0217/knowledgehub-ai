package com.dibya.knowledgehub.parser;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.StringJoiner;

@Component
public class ExcelParser implements DocumentParser {

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            StringJoiner docJoiner = new StringJoiner("\n\n");
            int sheetCount = workbook.getNumberOfSheets();

            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                StringJoiner sheetJoiner = new StringJoiner("\n");
                sheetJoiner.add("Sheet: " + sheet.getSheetName());

                for (Row row : sheet) {
                    StringJoiner rowJoiner = new StringJoiner("\t");
                    for (Cell cell : row) {
                        rowJoiner.add(cellToString(cell));
                    }
                    sheetJoiner.add(rowJoiner.toString());
                }
                docJoiner.add(sheetJoiner.toString());
            }

            return ParsedDocument.of(docJoiner.toString(), sheetCount);
        }
    }

    private String cellToString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    @Override
    public boolean supports(String mimeType) {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(mimeType)
                || "application/vnd.ms-excel".equals(mimeType);
    }
}
