package com.enterprise.ca.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Function;

/*
 * PURPOSE: Small generic helper so every "export to Excel" feature (ledger,
 * invoices, compliance calendar) shares one implementation instead of each
 * service hand-rolling POI boilerplate.
 */
public class ExcelExportUtil {

    public static <T> byte[] toXlsx(String sheetName, String[] headers, List<T> rows, Function<T, Object[]> rowMapper) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (T item : rows) {
                Row row = sheet.createRow(rowIdx++);
                Object[] values = rowMapper.apply(item);
                for (int i = 0; i < values.length; i++) {
                    Cell cell = row.createCell(i);
                    Object v = values[i];
                    if (v == null) {
                        cell.setBlank();
                    } else if (v instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else if (v instanceof Boolean b) {
                        cell.setCellValue(b);
                    } else {
                        cell.setCellValue(v.toString());
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
