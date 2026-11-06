package com.nexplm.platform.capp.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappQualityDtos;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
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

/**
 * 控制计划 Excel 导出（P6-8）。
 *
 * <p>版面就是质量部门常见的「控制计划表」：标题行 → 计划号/零件/路线副标题 → 13 列表头 → 行数据。
 * 列顺序与 {@link CappQualityService#EXPORT_COLUMNS} 一一对应，改列只改那一处。</p>
 */
@Service
public class CappQualityExcelService {

    private static final String SHEET_NAME = "控制计划";
    private static final String FONT_NAME = "宋体";
    private static final int[] COLUMN_WIDTHS = {8, 10, 18, 22, 20, 20, 14, 12, 12, 20, 20, 10, 20};

    public byte[] export(CappQualityDtos.PlanDetail plan) {
        List<CappQualityDtos.PlanRow> rows = plan.rows() == null ? List.of() : plan.rows();
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Styles styles = new Styles(workbook);
            int lastCol = CappQualityService.EXPORT_COLUMNS.size() - 1;

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(30);
            cell(titleRow, 0, "控制计划", styles.title);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastCol));

            Row subtitle = sheet.createRow(1);
            subtitle.setHeightInPoints(20);
            String text = "计划编号：" + nullToEmpty(plan.code())
                    + "    零件：" + nullToEmpty(plan.objectCode()) + " " + nullToEmpty(plan.objectName())
                    + "    工艺路线：" + nullToEmpty(plan.routingCode())
                    + "    状态：" + ("RELEASED".equals(plan.status()) ? "已发布" : "草稿");
            cell(subtitle, 0, text, styles.subtitle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, lastCol));

            Row head = sheet.createRow(2);
            head.setHeightInPoints(24);
            for (int i = 0; i < CappQualityService.EXPORT_COLUMNS.size(); i++) {
                cell(head, i, CappQualityService.EXPORT_COLUMNS.get(i), styles.head);
            }

            int rowIndex = 3;
            for (CappQualityDtos.PlanRow row : rows) {
                Row target = sheet.createRow(rowIndex++);
                target.setHeightInPoints(20);
                cell(target, 0, String.valueOf(row.seqNo()), styles.center);
                cell(target, 1, row.stepSeqNo() == null ? "" : String.valueOf(row.stepSeqNo()),
                        styles.center);
                cell(target, 2, nullToEmpty(row.stepName()), styles.left);
                cell(target, 3, nullToEmpty(row.characteristic()), styles.left);
                cell(target, 4, nullToEmpty(row.spec()), styles.left);
                cell(target, 5, nullToEmpty(row.method()), styles.left);
                cell(target, 6, nullToEmpty(row.gauge()), styles.left);
                cell(target, 7, nullToEmpty(row.sampleSize()), styles.center);
                cell(target, 8, nullToEmpty(row.frequency()), styles.center);
                cell(target, 9, nullToEmpty(row.controlMethod()), styles.left);
                cell(target, 10, nullToEmpty(row.reactionPlan()), styles.left);
                cell(target, 11, row.critical() ? "是" : "", styles.center);
                cell(target, 12, nullToEmpty(row.remark()), styles.left);
            }

            for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
                sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
            }
            sheet.createFreezePane(0, 3);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "导出控制计划失败：" + ex.getMessage());
        }
    }

    private static void cell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class Styles {
        private final CellStyle title;
        private final CellStyle subtitle;
        private final CellStyle head;
        private final CellStyle left;
        private final CellStyle center;

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

            subtitle = workbook.createCellStyle();
            subtitle.setFont(base);
            subtitle.setAlignment(HorizontalAlignment.LEFT);
            subtitle.setVerticalAlignment(VerticalAlignment.CENTER);

            head = bordered(workbook, bold, HorizontalAlignment.CENTER);
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            left = bordered(workbook, base, HorizontalAlignment.LEFT);
            center = bordered(workbook, base, HorizontalAlignment.CENTER);
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
