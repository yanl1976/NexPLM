package com.nexplm.platform.analysis.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.analysis.api.ReportDtos;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 报表导出（xlsx / pdf）：结果集由 {@link ReportService} 跑出来后原样渲染，导出侧不认识任何数据集。
 *
 * <p>PDF 必须嵌入中文字体，字体探测顺序与工艺卡片导出一致（{@code nexplm.capp.pdf.font-path}
 * 可覆盖；找不到明确报错，不生成乱码）。</p>
 */
@Service
public class ReportExportService {

    private static final String FONT_NAME = "宋体";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] FONT_CANDIDATES = {
            "C:/Windows/Fonts/simsun.ttc,0",
            "C:/Windows/Fonts/msyh.ttc,0",
            "C:/Windows/Fonts/simhei.ttf",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc,0",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0",
            "/usr/share/fonts/truetype/arphic/uming.ttc,0"
    };

    private final String configuredFontPath;
    private volatile BaseFont cachedFont;

    public ReportExportService(@Value("${nexplm.capp.pdf.font-path:}") String configuredFontPath) {
        this.configuredFontPath = configuredFontPath;
    }

    // ------------------------------------------------------------ xlsx

    public byte[] exportXlsx(ReportDtos.RunResult result) {
        List<ReportDtos.ColumnMeta> columns = result.columns();
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(result.datasetLabel()));
            Styles styles = new Styles(workbook);
            int lastCol = Math.max(columns.size() - 1, 0);

            Row title = sheet.createRow(0);
            title.setHeightInPoints(28);
            cell(title, 0, result.templateName() + "（" + result.datasetLabel() + "）", styles.title);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastCol));

            Row subtitle = sheet.createRow(1);
            subtitle.setHeightInPoints(20);
            cell(subtitle, 0, summary(result), styles.subtitle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, lastCol));

            Row head = sheet.createRow(2);
            head.setHeightInPoints(22);
            for (int i = 0; i < columns.size(); i++) {
                cell(head, i, columns.get(i).label(), styles.head);
                sheet.setColumnWidth(i, widthOf(columns.get(i).label()));
            }

            int rowIndex = 3;
            for (Map<String, Object> row : result.rows()) {
                Row target = sheet.createRow(rowIndex++);
                target.setHeightInPoints(18);
                for (int i = 0; i < columns.size(); i++) {
                    ReportDtos.ColumnMeta column = columns.get(i);
                    Object value = row.get(column.key());
                    if (isNumeric(column) && value instanceof Number number) {
                        Cell targetCell = target.createCell(i);
                        targetCell.setCellValue(number.doubleValue());
                        targetCell.setCellStyle(styles.number);
                    } else {
                        cell(target, i, cellText(column, value),
                                "NUMBER".equals(column.type()) ? styles.right : styles.left);
                    }
                }
            }

            if (result.total() > result.rows().size()) {
                Row footer = sheet.createRow(rowIndex + 1);
                cell(footer, 0, "共 " + result.total() + " 条，本次导出 " + result.rows().size()
                        + " 条（导出上限 " + ReportService.MAX_EXPORT_ROWS + " 条）", styles.subtitle);
                sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, 0, lastCol));
            }

            sheet.createFreezePane(0, 3);
            if (!columns.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(2, 2, 0, lastCol));
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.ANA_REPORT_INVALID, "导出 Excel 失败：" + ex.getMessage());
        }
    }

    // ------------------------------------------------------------ pdf

    public byte[] exportPdf(ReportDtos.RunResult result) {
        BaseFont baseFont = chineseFont();
        List<ReportDtos.ColumnMeta> columns = result.columns();
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 22, 22);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph(result.templateName() + "（" + result.datasetLabel() + "）",
                    pdfFont(baseFont, 15, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6f);
            document.add(title);

            Paragraph subtitle = new Paragraph(summary(result), pdfFont(baseFont, 9, Font.NORMAL));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(10f);
            document.add(subtitle);

            PdfPTable table = new PdfPTable(Math.max(columns.size(), 1));
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (ReportDtos.ColumnMeta column : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(column.label(), pdfFont(baseFont, 9, Font.BOLD)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(0xEB, 0xEE, 0xF5));
                cell.setPadding(4f);
                table.addCell(cell);
            }
            for (Map<String, Object> row : result.rows()) {
                for (ReportDtos.ColumnMeta column : columns) {
                    String text = cellText(column, row.get(column.key()));
                    PdfPCell cell = new PdfPCell(new Phrase(text, pdfFont(baseFont, 8.5f, Font.NORMAL)));
                    cell.setHorizontalAlignment("NUMBER".equals(column.type())
                            ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                    cell.setPadding(3f);
                    table.addCell(cell);
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            if (document.isOpen()) {
                document.close();
            }
            throw BizException.of(ErrorCode.ANA_REPORT_INVALID, "导出 PDF 失败：" + ex.getMessage());
        }
    }

    // ------------------------------------------------------------ 内部

    private static String summary(ReportDtos.RunResult result) {
        StringBuilder text = new StringBuilder();
        text.append("模板：").append(result.templateCode()).append("　数据集：").append(result.datasetLabel())
                .append("　记录数：").append(result.total())
                .append("　导出时间：").append(java.time.LocalDateTime.now().format(DATE_TIME));
        if (result.total() > result.rows().size()) {
            text.append("　本次导出前 ").append(result.rows().size()).append(" 条");
        }
        return text.toString();
    }

    private static boolean isNumeric(ReportDtos.ColumnMeta column) {
        return "NUMBER".equals(column.type());
    }

    private static String cellText(ReportDtos.ColumnMeta column, Object value) {
        if (value == null) {
            return "";
        }
        if ("BOOL".equals(column.type())) {
            return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value)) ? "是" : "否";
        }
        if (value instanceof Timestamp timestamp) {
            String pattern = "DATE".equals(column.type()) ? DATE.format(
                    timestamp.toInstant().atZone(java.time.ZoneId.systemDefault())) : DATE_TIME.format(
                    timestamp.toInstant().atZone(java.time.ZoneId.systemDefault()));
            return pattern;
        }
        if (value instanceof java.time.LocalDate localDate) {
            return DATE.format(localDate);
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private static int widthOf(String label) {
        int width = label == null ? 12 : Math.max(12, label.length() * 2 + 6);
        return Math.min(width * 256, 60 * 256);
    }

    private static String safeSheetName(String name) {
        String text = name == null || name.isBlank() ? "报表" : name.trim();
        text = text.replaceAll("[\\\\/:*?\\[\\]]", "-");
        return text.length() > 28 ? text.substring(0, 28) : text;
    }

    private BaseFont chineseFont() {
        BaseFont font = cachedFont;
        if (font != null) {
            return font;
        }
        synchronized (this) {
            if (cachedFont != null) {
                return cachedFont;
            }
            if (configuredFontPath != null && !configuredFontPath.isBlank()) {
                cachedFont = loadFont(configuredFontPath.trim());
                if (cachedFont != null) {
                    return cachedFont;
                }
                throw BizException.of(ErrorCode.ANA_REPORT_INVALID,
                        "配置的 PDF 字体不可用：nexplm.capp.pdf.font-path=" + configuredFontPath);
            }
            for (String candidate : FONT_CANDIDATES) {
                cachedFont = loadFont(candidate);
                if (cachedFont != null) {
                    return cachedFont;
                }
            }
            throw BizException.of(ErrorCode.ANA_REPORT_INVALID,
                    "未找到可用的中文字体，无法导出 PDF：请配置 nexplm.capp.pdf.font-path（如 "
                            + "C:/Windows/Fonts/simsun.ttc,0）");
        }
    }

    private static BaseFont loadFont(String path) {
        try {
            int comma = path.lastIndexOf(',');
            if (comma > 0 && path.substring(comma + 1).matches("\\d+")) {
                String file = path.substring(0, comma);
                int index = Integer.parseInt(path.substring(comma + 1));
                if (!new File(file).exists()) {
                    return null;
                }
                return BaseFont.createFont(file + "," + index, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
            if (!new File(path).exists()) {
                return null;
            }
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Font pdfFont(BaseFont baseFont, float size, int style) {
        return new Font(baseFont, size, style, Color.BLACK);
    }

    private static void cell(Row row, int index, String text, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(text == null ? "" : text);
        cell.setCellStyle(style);
    }

    /** xlsx 样式集合（与工艺卡片导出口径一致：标题/副标题/表头/正文/数字）。 */
    private static final class Styles {
        private final CellStyle title;
        private final CellStyle subtitle;
        private final CellStyle head;
        private final CellStyle left;
        private final CellStyle right;
        private final CellStyle number;

        private Styles(Workbook workbook) {
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setFontName(FONT_NAME);
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 15);
            title = workbook.createCellStyle();
            title.setFont(titleFont);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            org.apache.poi.ss.usermodel.Font body = workbook.createFont();
            body.setFontName(FONT_NAME);
            body.setFontHeightInPoints((short) 10);

            subtitle = workbook.createCellStyle();
            subtitle.setFont(body);
            subtitle.setAlignment(HorizontalAlignment.LEFT);
            subtitle.setVerticalAlignment(VerticalAlignment.CENTER);

            org.apache.poi.ss.usermodel.Font headFont = workbook.createFont();
            headFont.setFontName(FONT_NAME);
            headFont.setBold(true);
            headFont.setFontHeightInPoints((short) 10);
            headFont.setColor(IndexedColors.WHITE.getIndex());
            head = workbook.createCellStyle();
            head.setFont(headFont);
            head.setAlignment(HorizontalAlignment.CENTER);
            head.setVerticalAlignment(VerticalAlignment.CENTER);
            head.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            head.setBorderBottom(BorderStyle.THIN);
            head.setBorderTop(BorderStyle.THIN);
            head.setBorderLeft(BorderStyle.THIN);
            head.setBorderRight(BorderStyle.THIN);

            left = workbook.createCellStyle();
            left.setFont(body);
            left.setAlignment(HorizontalAlignment.LEFT);
            left.setVerticalAlignment(VerticalAlignment.CENTER);
            border(left);

            right = workbook.createCellStyle();
            right.setFont(body);
            right.setAlignment(HorizontalAlignment.RIGHT);
            right.setVerticalAlignment(VerticalAlignment.CENTER);
            border(right);

            number = workbook.createCellStyle();
            number.setFont(body);
            number.setAlignment(HorizontalAlignment.RIGHT);
            number.setVerticalAlignment(VerticalAlignment.CENTER);
            number.setDataFormat(workbook.createDataFormat().getFormat("#,##0.####"));
            border(number);
        }

        private static void border(CellStyle style) {
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }
}
