package com.nexplm.platform.change.service;

import com.nexplm.platform.change.api.ChgDtos;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 变更管理 Excel 导出（P5 任务 4「影响面可导出」）。
 *
 * <p>直接复用 {@link ChgExecService#impact} 的结果，不另写查询——保证「导出与页面看到的是同一份数据」，
 * 深度与视图口径也由同一处限幅（1~10 层、默认 DESIGN 视图）。</p>
 */
@Service
public class ChgExcelService {

    private static final String[] IMPACT_HEADERS = {
            "方向", "层级", "对象类型", "编码", "名称", "生命周期", "版本", "已在本变更"
    };

    private final ChgExecService execService;

    public ChgExcelService(ChgExecService execService) {
        this.execService = execService;
    }

    /** 导出某个 ECR 的影响分析（xlsx）。 */
    public byte[] exportImpact(Long ecrId, Long companyId, Integer depth, String viewType) {
        List<ChgDtos.ImpactRow> rows = execService.impact(ecrId, companyId, depth == null ? 3 : depth, viewType);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("影响分析");
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < IMPACT_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(IMPACT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (ChgDtos.ImpactRow row : rows) {
                Row line = sheet.createRow(rowIndex++);
                line.createCell(0).setCellValue("DOWN".equals(row.direction()) ? "向下（组成件）" : "向上（被使用）");
                line.createCell(1).setCellValue(row.depth());
                line.createCell(2).setCellValue(nullToEmpty(row.objectType()));
                line.createCell(3).setCellValue(nullToEmpty(row.code()));
                line.createCell(4).setCellValue(nullToEmpty(row.name()));
                line.createCell(5).setCellValue(nullToEmpty(row.lifecycleState()));
                line.createCell(6).setCellValue("v" + row.revision() + "." + row.iteration());
                line.createCell(7).setCellValue(row.linked() ? "是" : "否");
            }
            int[] widths = {16, 8, 14, 20, 28, 14, 10, 12};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("导出影响分析失败：" + ex.getMessage(), ex);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
