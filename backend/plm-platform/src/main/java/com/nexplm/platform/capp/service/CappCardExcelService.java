package com.nexplm.platform.capp.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappCardDtos;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 工艺卡片 Excel 导出（P6-4）。
 *
 * <p>只按卡片**自带的版式快照**渲染：标题 → 表头字段区 → 明细列 → 工序行 → 合计行 → 表尾，
 * 不回查模板表。因此模板后续改版（甚至旧版本被清理）不会改变历史卡片的导出结果。</p>
 *
 * <p>表头字段区按「标签占 1 列 + 取值占 1 列」两列一组排布，每行放 {@code 列数/2} 组，
 * 因此明细列数决定每行能放几个表头字段（6 列时每行 3 组）。</p>
 */
@Service
public class CappCardExcelService {

    private static final String SHEET_NAME = "工艺卡片";
    private static final String FONT_NAME = "宋体";

    /** 按版式渲染为 xlsx 字节。 */
    public byte[] export(CappCardDtos.CardDetail card) {
        CappCardDtos.CardLayout layout = card.layout();
        List<CappCardDtos.CardColumn> columns = layout == null || layout.columns() == null
                ? List.of() : layout.columns();
        if (columns.isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "卡片没有明细列，无法导出");
        }
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Styles styles = new Styles(workbook);
            int lastCol = columns.size() - 1;
            int rowIndex = 0;

            if (layout.title() != null && !layout.title().isBlank()) {
                Row titleRow = sheet.createRow(rowIndex++);
                titleRow.setHeightInPoints(30);
                writeCell(titleRow, 0, layout.title(), styles.title);
                sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(),
                        0, lastCol));
            }

            List<CappCardDtos.HeaderField> headers = layout.headerFields() == null
                    ? List.of() : layout.headerFields();
            if (!headers.isEmpty()) {
                Map<String, String> values = card.fieldValues() == null ? Map.of() : card.fieldValues();
                int perRow = Math.max(1, columns.size() / 2);
                for (int start = 0; start < headers.size(); start += perRow) {
                    Row row = sheet.createRow(rowIndex++);
                    row.setHeightInPoints(20);
                    int col = 0;
                    int end = Math.min(start + perRow, headers.size());
                    for (int i = start; i < end; i++) {
                        CappCardDtos.HeaderField header = headers.get(i);
                        writeCell(row, col, nullToEmpty(header.label()), styles.headerLabel);
                        String value = header.field() == null ? "" : values.getOrDefault(header.field(), "");
                        writeCell(row, col + 1, value, styles.headerValue);
                        col += 2;
                    }
                }
            }

            Row head = sheet.createRow(rowIndex++);
            head.setHeightInPoints(22);
            for (int i = 0; i < columns.size(); i++) {
                writeCell(head, i, nullToEmpty(columns.get(i).title()), styles.columnHead);
            }

            List<Map<String, String>> rows = card.rows() == null ? List.of() : card.rows();
            for (Map<String, String> rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(20);
                boolean summary = "true".equalsIgnoreCase(rowData.get("__summary__"));
                for (int i = 0; i < columns.size(); i++) {
                    CappCardDtos.CardColumn column = columns.get(i);
                    String value = rowData.getOrDefault(CappCardService.columnKey(column), "");
                    writeCell(row, i, nullToEmpty(value),
                            summary ? styles.summaryCell(alignOf(column)) : styles.cell(alignOf(column)));
                }
            }

            if (layout.footerText() != null && !layout.footerText().isBlank()) {
                Row footer = sheet.createRow(rowIndex);
                footer.setHeightInPoints(20);
                writeCell(footer, 0, layout.footerText(), styles.footer);
                if (lastCol > 0) {
                    sheet.addMergedRegion(new CellRangeAddress(footer.getRowNum(), footer.getRowNum(),
                            0, lastCol));
                }
            }

            for (int i = 0; i < columns.size(); i++) {
                Integer width = columns.get(i).width();
                sheet.setColumnWidth(i, (width == null ? 12 : width) * 256);
            }
            sheet.setDefaultRowHeightInPoints(20);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "导出工艺卡片失败：" + ex.getMessage());
        }
    }

    private static HorizontalAlignment alignOf(CappCardDtos.CardColumn column) {
        String align = column.align() == null ? "LEFT" : column.align().toUpperCase();
        return switch (align) {
            case "CENTER" -> HorizontalAlignment.CENTER;
            case "RIGHT" -> HorizontalAlignment.RIGHT;
            default -> HorizontalAlignment.LEFT;
        };
    }

    private static void writeCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** 样式缓存：按对齐方式各建一份，避免每格新建 style（POI 单工作簿样式上限 64000）。 */
    private static final class Styles {

        private final CellStyle title;
        private final CellStyle headerLabel;
        private final CellStyle headerValue;
        private final CellStyle columnHead;
        private final CellStyle footer;
        private final CellStyle left;
        private final CellStyle center;
        private final CellStyle right;
        private final CellStyle summaryLeft;
        private final CellStyle summaryCenter;
        private final CellStyle summaryRight;

        private Styles(Workbook workbook) {
            Font base = workbook.createFont();
            base.setFontName(FONT_NAME);
            base.setFontHeightInPoints((short) 10);
            Font bold = workbook.createFont();
            bold.setFontName(FONT_NAME);
            bold.setFontHeightInPoints((short) 10);
            bold.setBold(true);
            Font titleFont = workbook.createFont();
            titleFont.setFontName(FONT_NAME);
            titleFont.setFontHeightInPoints((short) 15);
            titleFont.setBold(true);

            title = workbook.createCellStyle();
            title.setFont(titleFont);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            headerLabel = bordered(workbook, bold, HorizontalAlignment.CENTER);
            headerValue = bordered(workbook, base, HorizontalAlignment.LEFT);
            columnHead = bordered(workbook, bold, HorizontalAlignment.CENTER);
            columnHead.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            columnHead.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            footer = workbook.createCellStyle();
            footer.setFont(base);
            footer.setAlignment(HorizontalAlignment.LEFT);

            left = bordered(workbook, base, HorizontalAlignment.LEFT);
            center = bordered(workbook, base, HorizontalAlignment.CENTER);
            right = bordered(workbook, base, HorizontalAlignment.RIGHT);
            summaryLeft = bordered(workbook, bold, HorizontalAlignment.LEFT);
            summaryCenter = bordered(workbook, bold, HorizontalAlignment.CENTER);
            summaryRight = bordered(workbook, bold, HorizontalAlignment.RIGHT);
        }

        private CellStyle cell(HorizontalAlignment alignment) {
            return switch (alignment) {
                case CENTER -> center;
                case RIGHT -> right;
                default -> left;
            };
        }

        private CellStyle summaryCell(HorizontalAlignment alignment) {
            return switch (alignment) {
                case CENTER -> summaryCenter;
                case RIGHT -> summaryRight;
                default -> summaryLeft;
            };
        }

        private static CellStyle bordered(Workbook workbook, Font font, HorizontalAlignment alignment) {
            CellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setAlignment(alignment);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setWrapText(true);
            return style;
        }
    }
}
