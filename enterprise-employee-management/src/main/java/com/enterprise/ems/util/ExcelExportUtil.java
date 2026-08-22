package com.enterprise.ems.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/*
 * PURPOSE: One shared, reusable .xlsx builder for every "export to Excel"
 * endpoint in the app (leaves today; attendance/employees can follow the
 * same call shape). Callers just supply a sheet name, column headers, the
 * rows, and a function that turns one row-object into a list of cell values -
 * no module needs to touch Apache POI directly.
 *
 * Deliberately uses XSSFWorkbook (in-memory), which is fine for this app's
 * scale. If a single export could realistically exceed tens of thousands of
 * rows, swap to SXSSFWorkbook (streaming) instead to avoid holding the whole
 * sheet in memory.
 */
public final class ExcelExportUtil {

    private ExcelExportUtil() {
        // Utility class - prevent instantiation
    }

    public static <T> byte[] toXlsx(String sheetName, List<String> headers,
                                     List<T> rows, Function<T, List<Object>> rowMapper) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers.get(col));
                cell.setCellStyle(headerStyle);
                // Fixed, generous column width rather than sheet.autoSizeColumn():
                // autoSizeColumn needs AWT font metrics, which is unreliable on
                // headless servers without fontconfig installed. Not worth the risk.
                sheet.setColumnWidth(col, 22 * 256);
            }

            int rowIndex = 1;
            for (T item : rows) {
                Row row = sheet.createRow(rowIndex++);
                List<Object> values = rowMapper.apply(item);
                for (int col = 0; col < values.size(); col++) {
                    writeCell(row.createCell(col), values.get(col), dateStyle);
                }
            }

            sheet.createFreezePane(0, 1); // keep header row visible while scrolling

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate Excel export", e);
        }
    }

    private static void writeCell(Cell cell, Object value, CellStyle dateStyle) {
        switch (value) {
            case null -> cell.setBlank();
            case Number n -> cell.setCellValue(n.doubleValue());
            case LocalDate d -> {
                cell.setCellValue(d);
                cell.setCellStyle(dateStyle);
            }
            case LocalDateTime d -> {
                cell.setCellValue(d);
                cell.setCellStyle(dateStyle);
            }
            default -> cell.setCellValue(value.toString());
        }
    }
}
