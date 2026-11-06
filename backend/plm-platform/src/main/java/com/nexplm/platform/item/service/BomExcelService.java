package com.nexplm.platform.item.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.item.api.BomDtos;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * BOM 的 Excel 导出与导入。
 * <p>模板口径沿用旧系统 {@code ImportBom} 的「层级号」方式（{@code LEVEL} 形如 {@code 1.2.3}，
 * 父层级号 = 去掉最后一段），旧系统另一种「父编号 PECODE」方式未实现。
 * 差异在于：新模型导入是**增量合并**（新增/更新），不会删除导入文件中不存在的现有行，
 * 以免误删；旧系统逐行比对后的删除动作需要显式在界面执行。</p>
 */
@Service
public class BomExcelService {

    private static final Pattern LEVEL_PATTERN = Pattern.compile("^\\d+(\\.\\d+)*$");

    private static final String[] EXPORT_HEADERS = {
            "层级号", "编码", "名称", "规格型号", "单位", "数量", "损耗率(%)", "有效用量", "生效日期", "失效日期"
    };

    private final BomService bomService;
    private final ItemService itemService;
    private final JdbcTemplate jdbcTemplate;

    public BomExcelService(BomService bomService, ItemService itemService, JdbcTemplate jdbcTemplate) {
        this.bomService = bomService;
        this.itemService = itemService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------------------ 导出

    /** 导出当前视图的展开结果（层级号由层级路径推导，可直接改数量后回导）。 */
    public byte[] export(Long rootId, String viewType, Integer depth, LocalDate effectAt) {
        List<BomDtos.BomSummaryRow> rows = bomService.summary(rootId, viewType, depth, effectAt);
        Map<String, String> levelCodeByPath = levelCodes(rows);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BOM");
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (BomDtos.BomSummaryRow row : rows) {
                Row line = sheet.createRow(rowIndex++);
                line.createCell(0).setCellValue(levelCodeByPath.getOrDefault(row.path(), ""));
                line.createCell(1).setCellValue(nullToEmpty(row.code()));
                line.createCell(2).setCellValue(nullToEmpty(row.name()));
                line.createCell(3).setCellValue(nullToEmpty(row.specification()));
                line.createCell(4).setCellValue(nullToEmpty(row.unit()));
                line.createCell(5).setCellValue(row.qty() == null ? 0 : row.qty().doubleValue());
                line.createCell(6).setCellValue(row.lossRate() == null ? 0 : row.lossRate().doubleValue());
                line.createCell(7).setCellValue(row.effectiveQty() == null ? 0 : row.effectiveQty().doubleValue());
                line.createCell(8).setCellValue(row.effectFrom() == null ? "" : row.effectFrom().toString());
                line.createCell(9).setCellValue(row.effectTo() == null ? "" : row.effectTo().toString());
            }
            int[] widths = {10, 18, 24, 20, 8, 12, 12, 12, 14, 14};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("导出 BOM 失败：" + ex.getMessage(), ex);
        }
    }

    /** 层级号推导：由层级路径（line_no 串）反推 1 / 1.1 / 1.2.1 形式。 */
    private Map<String, String> levelCodes(List<BomDtos.BomSummaryRow> rows) {
        Map<String, String> codes = new LinkedHashMap<>();
        Map<String, Integer> seqByParent = new HashMap<>();
        for (BomDtos.BomSummaryRow row : rows) {
            String parentPath = parentPathOf(row.path());
            String parentKey = parentPath == null ? "" : parentPath;
            String parentCode = parentPath == null ? "" : codes.getOrDefault(parentPath, "");
            int seq = seqByParent.merge(parentKey, 1, Integer::sum);
            codes.put(row.path(), parentCode.isEmpty() ? String.valueOf(seq) : parentCode + "." + seq);
        }
        return codes;
    }

    private String parentPathOf(String path) {
        if (path == null) {
            return null;
        }
        int index = path.lastIndexOf('.');
        return index < 0 ? null : path.substring(0, index);
    }

    // ------------------------------------------------------------ 导入：预演

    /** 解析并校验导入文件，返回「新增 / 更新 / 不变」分类与错误明细（不落库）。 */
    public BomDtos.BomExcelPreview preview(Long rootId, String viewType, InputStream input) {
        return analyze(rootId, viewType, input).preview();
    }

    // ------------------------------------------------------------ 导入：执行

    /** 增量导入：新增缺失的父子关系、更新已有行的用量/损耗率/单位/有效期。 */
    @Transactional
    public BomDtos.BomExcelImportResult apply(Long rootId, String viewType, InputStream input, Long userId) {
        Analysis analysis = analyze(rootId, viewType, input);
        if (!analysis.preview().errors().isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "导入数据存在 " + analysis.preview().errors().size() + " 个问题，请修正后重试："
                            + analysis.preview().errors().get(0));
        }
        int created = 0;
        int updated = 0;
        int skipped = 0;
        for (ParsedRow row : analysis.rows()) {
            if ("NEW".equals(row.action())) {
                BomDtos.BomLineSaveReq req = new BomDtos.BomLineSaveReq(row.childId(), row.qty(), row.unit(),
                        row.lossRate(), row.effectFrom(), row.effectTo(), null, null);
                bomService.add(analysis.parentIdOf(row), analysis.view(), req, userId);
                created++;
            } else if ("UPDATE".equals(row.action())) {
                BomDtos.BomLineUpdateReq req = new BomDtos.BomLineUpdateReq(row.qty(), row.unit(), row.lossRate(),
                        row.effectFrom(), row.effectTo(), null, null);
                bomService.update(row.existingLineId(), req, userId);
                updated++;
            } else {
                skipped++;
            }
        }
        return new BomDtos.BomExcelImportResult(created, updated, skipped);
    }

    // ------------------------------------------------------------ 内部

    private Analysis analyze(Long rootId, String viewType, InputStream input) {
        itemService.requireItem(rootId);
        String view = BomViews.normalize(viewType);
        List<String> errors = new ArrayList<>();
        Map<String, Integer> headerIndex = new HashMap<>();
        List<ParsedRow> parsed = new ArrayList<>();
        Map<String, Long> objectIdByCode = new HashMap<>();
        int dataRowCount = 0;

        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "Excel 中没有可读取的工作表");
            }
            Row header = sheet.getRow(0);
            if (header == null) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "第一行必须是表头（需包含：层级号、编码、数量）");
            }
            for (Cell cell : header) {
                String name = cellString(cell);
                if (name != null && !name.isBlank()) {
                    headerIndex.put(name.replace(" ", ""), cell.getColumnIndex());
                }
            }
            for (String required : List.of("层级号", "编码", "数量")) {
                if (!headerIndex.containsKey(required)) {
                    throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "表头缺少必需列：" + required);
                }
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row line = sheet.getRow(i);
                if (line == null || isBlankRow(line)) {
                    continue;
                }
                int rowNum = i + 1;
                dataRowCount++;
                String levelCode = value(line, headerIndex, "层级号");
                String code = value(line, headerIndex, "编码");
                String qtyText = value(line, headerIndex, "数量");
                if (levelCode == null || levelCode.isBlank()) {
                    errors.add("第 " + rowNum + " 行：层级号不能为空");
                    continue;
                }
                if (!LEVEL_PATTERN.matcher(levelCode).matches()) {
                    errors.add("第 " + rowNum + " 行：层级号格式应为 1 / 1.1 / 1.1.2，实际为 " + levelCode);
                    continue;
                }
                if (code == null || code.isBlank()) {
                    errors.add("第 " + rowNum + " 行：编码不能为空");
                    continue;
                }
                BigDecimal qty = parseDecimal(qtyText);
                if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("第 " + rowNum + " 行：数量必须为大于 0 的数字");
                    continue;
                }
                BigDecimal lossRate = parseDecimal(value(line, headerIndex, "损耗率(%)"));
                if (lossRate == null) {
                    lossRate = parseDecimal(value(line, headerIndex, "损耗率"));
                }
                LocalDate effectFrom = parseDate(value(line, headerIndex, "生效日期"));
                LocalDate effectTo = parseDate(value(line, headerIndex, "失效日期"));
                if (effectFrom != null && effectTo != null && effectFrom.isAfter(effectTo)) {
                    errors.add("第 " + rowNum + " 行：生效日期不能晚于失效日期");
                    continue;
                }
                String unit = value(line, headerIndex, "单位");
                Long childId = resolveItem(code, objectIdByCode);
                if (childId == null) {
                    errors.add("第 " + rowNum + " 行：零部件编码不存在：" + code);
                    continue;
                }
                if (childId.equals(rootId)) {
                    errors.add("第 " + rowNum + " 行：不能把根件自身导入为子件");
                    continue;
                }
                parsed.add(new ParsedRow(rowNum, levelCode, childId, code, qty,
                        lossRate == null ? BigDecimal.ZERO : lossRate, unit, effectFrom, effectTo));
            }
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "Excel 解析失败：" + ex.getMessage());
        }

        // 层级号重复 / 父层级号不存在 / 同一父下重复子件
        Map<String, ParsedRow> byLevelCode = new LinkedHashMap<>();
        for (ParsedRow row : parsed) {
            if (byLevelCode.putIfAbsent(row.levelCode(), row) != null) {
                errors.add("第 " + row.rowNum() + " 行：层级号重复 " + row.levelCode());
            }
        }
        Map<String, ParsedRow> seenParentChild = new HashMap<>();
        for (ParsedRow row : parsed) {
            String parentLevelCode = parentOf(row.levelCode());
            if (parentLevelCode != null) {
                if (parentLevelCode.isEmpty() || !byLevelCode.containsKey(parentLevelCode)) {
                    errors.add("第 " + row.rowNum() + " 行：父层级号不存在 " + parentLevelCode);
                    continue;
                }
            }
            String key = (parentLevelCode == null ? "" : parentLevelCode) + "#" + row.childId();
            if (seenParentChild.putIfAbsent(key, row) != null) {
                errors.add("第 " + row.rowNum() + " 行：同一父节点下不允许存在相同零部件 " + row.code());
            }
        }

        // 与现有结构比对：按「父对象 + 子件」判定新增/更新/不变
        Map<String, Long> objectIdByLevelCode = new HashMap<>();
        objectIdByLevelCode.put("", rootId);
        List<ParsedRow> resolved = new ArrayList<>();
        List<BomDtos.BomExcelPreviewRow> previewRows = new ArrayList<>();
        int createCount = 0;
        int updateCount = 0;
        int unchangedCount = 0;
        List<ParsedRow> sorted = new ArrayList<>(parsed);
        sorted.sort((a, b) -> Integer.compare(depthOf(a.levelCode()), depthOf(b.levelCode())));
        for (ParsedRow row : sorted) {
            String parentLevelCode = parentOf(row.levelCode());
            Long parentId = parentLevelCode == null ? rootId : objectIdByLevelCode.get(parentLevelCode);
            if (parentId == null) {
                // 父行本身未通过校验，错误已在上面记录
                continue;
            }
            objectIdByLevelCode.put(row.levelCode(), row.childId());
            ExistingLine existing = findExisting(parentId, row.childId(), view);
            // 单位与有效期「留空 = 不改」，避免导入只改数量时误清空现有属性
            String effectiveUnit = hasText(row.unit()) ? row.unit()
                    : (existing == null ? row.unit() : existing.unit());
            LocalDate effectiveFrom = row.effectFrom() != null ? row.effectFrom()
                    : (existing == null ? null : existing.effectFrom());
            LocalDate effectiveTo = row.effectTo() != null ? row.effectTo()
                    : (existing == null ? null : existing.effectTo());
            String action;
            if (existing == null) {
                action = "NEW";
                createCount++;
            } else if (!sameValue(existing.qty(), row.qty())
                    || !sameValue(existing.lossRate(), row.lossRate())
                    || (hasText(row.unit()) && !sameText(existing.unit(), row.unit()))
                    || (row.effectFrom() != null && !java.util.Objects.equals(existing.effectFrom(), row.effectFrom()))
                    || (row.effectTo() != null && !java.util.Objects.equals(existing.effectTo(), row.effectTo()))) {
                action = "UPDATE";
                updateCount++;
            } else {
                action = "SAME";
                unchangedCount++;
            }
            ParsedRow withAction = new ParsedRow(row.rowNum(), row.levelCode(), row.childId(), row.code(),
                    row.qty(), row.lossRate(), effectiveUnit, effectiveFrom, effectiveTo,
                    action, existing == null ? null : existing.lineId(), parentId);
            resolved.add(withAction);
            previewRows.add(new BomDtos.BomExcelPreviewRow(row.rowNum(), row.levelCode(), parentLevelCode,
                    row.childId(), row.code(), existing == null ? null : existing.name(),
                    row.qty(), row.lossRate(), effectiveUnit, effectiveFrom, effectiveTo, action));
        }

        BomDtos.BomExcelPreview preview = new BomDtos.BomExcelPreview(dataRowCount,
                Math.max(0, dataRowCount - errors.size()), errors.size(),
                createCount, updateCount, unchangedCount, previewRows, errors);
        return new Analysis(view, preview, resolved);
    }

    private ExistingLine findExisting(Long parentId, Long childId, String view) {
        return jdbcTemplate.query("""
                        SELECT b.id, b.qty, b.loss_rate, b.unit, b.effect_from, b.effect_to, o.name
                          FROM bom_line b
                          LEFT JOIN plm_object o ON o.id = b.child_object_id
                         WHERE b.parent_object_id = ? AND b.child_object_id = ?
                           AND b.view_type = ? AND b.deleted = false
                        """,
                rs -> rs.next() ? new ExistingLine(rs.getLong("id"), rs.getBigDecimal("qty"),
                        rs.getBigDecimal("loss_rate"), rs.getString("unit"),
                        rs.getObject("effect_from", LocalDate.class),
                        rs.getObject("effect_to", LocalDate.class), rs.getString("name")) : null,
                parentId, childId, view);
    }

    private Long resolveItem(String code, Map<String, Long> cache) {
        if (cache.containsKey(code)) {
            return cache.get(code);
        }
        Long id = jdbcTemplate.query("""
                        SELECT id FROM plm_object
                         WHERE code = ? AND object_type = 'PdmItem' AND is_latest = true AND deleted = false
                         ORDER BY id DESC LIMIT 1
                        """, rs -> rs.next() ? rs.getLong(1) : null, code);
        if (id != null) {
            cache.put(code, id);
        }
        return id;
    }

    private String parentOf(String levelCode) {
        int index = levelCode.lastIndexOf('.');
        return index < 0 ? null : levelCode.substring(0, index);
    }

    private int depthOf(String levelCode) {
        int depth = 1;
        for (int i = 0; i < levelCode.length(); i++) {
            if (levelCode.charAt(i) == '.') {
                depth++;
            }
        }
        return depth;
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            String text = cellString(cell);
            if (text != null && !text.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String value(Row row, Map<String, Integer> headerIndex, String column) {
        Integer index = headerIndex.get(column);
        if (index == null) {
            return null;
        }
        return cellString(row.getCell(index));
    }

    private String cellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> {
                String text = cell.getStringCellValue();
                yield text == null ? null : text.trim();
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                BigDecimal decimal = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
                yield decimal.toPlainString();
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case BLANK -> null;
            default -> null;
        };
    }

    private BigDecimal parseDecimal(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private boolean sameValue(BigDecimal left, BigDecimal right) {
        BigDecimal a = left == null ? BigDecimal.ZERO : left;
        BigDecimal b = right == null ? BigDecimal.ZERO : right;
        return a.compareTo(b) == 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean sameText(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        return a.equalsIgnoreCase(b);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** 解析结果：视图 + 预览 + 带动作的待执行行。 */
    private record Analysis(String view, BomDtos.BomExcelPreview preview, List<ParsedRow> rows) {

        Long parentIdOf(ParsedRow row) {
            return row.parentId();
        }
    }

    private record ParsedRow(int rowNum, String levelCode, Long childId, String code, BigDecimal qty,
                             BigDecimal lossRate, String unit, LocalDate effectFrom, LocalDate effectTo,
                             String action, Long existingLineId, Long parentId) {

        ParsedRow(int rowNum, String levelCode, Long childId, String code, BigDecimal qty,
                  BigDecimal lossRate, String unit, LocalDate effectFrom, LocalDate effectTo) {
            this(rowNum, levelCode, childId, code, qty, lossRate, unit, effectFrom, effectTo, null, null, null);
        }
    }

    private record ExistingLine(Long lineId, BigDecimal qty, BigDecimal lossRate, String unit,
                                LocalDate effectFrom, LocalDate effectTo, String name) {
    }
}
