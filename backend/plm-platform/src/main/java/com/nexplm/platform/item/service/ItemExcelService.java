package com.nexplm.platform.item.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.item.api.ItemDtos;
import com.nexplm.platform.object.domain.PlmObject;
import com.nexplm.platform.object.mapper.PlmObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 零部件 Excel 导入 / 导出（旧系统 ImportItem 的「模板 + 数据检查 + 增量合并」口径）。
 * <p>导入是**增量合并**：编码已存在则更新，不存在则新建；不会删除文件中未列出的零部件。</p>
 */
@Service
public class ItemExcelService {

    private static final String[] EXPORT_HEADERS = {
            "编号", "名称", "类型", "规格型号", "材料", "单位", "供应方式", "版本",
            "生命周期", "使用状态", "流程状态", "发布时间", "创建人", "修改人", "创建时间",
            "供应商", "图号", "产品类别", "产品型号", "产品规格", "毛坯种类", "表面处理",
            "是否工装", "重量单位", "所属装配", "是否颜色件"
    };

    private static final int MAX_IMPORT_ROWS = 5000;
    private static final int EXPORT_PAGE_SIZE = 200;

    private final ItemService itemService;
    private final PlmObjectMapper objectMapper;

    public ItemExcelService(ItemService itemService, PlmObjectMapper objectMapper) {
        this.itemService = itemService;
        this.objectMapper = objectMapper;
    }

    /** 导入模板：表头 + 一行示例，列顺序与导入解析一致。 */
    public byte[] template() {
        String[] headers = {"编号", "名称", "类型", "规格型号", "材料", "单位", "供应方式", "供应商",
                "图号", "产品类别", "产品型号", "产品规格", "毛坯种类", "表面处理", "是否工装",
                "重量单位", "所属装配", "是否颜色件", "备注"};
        String[] sample = {"（留空自动取号）", "示例：传动轴", "PART", "Φ50×320", "45#", "EA", "自制",
                "示例供应商", "TD-0001", "传动部件", "TD-200", "Φ50", "圆钢", "发黑", "否",
                "kg", "", "否", "示例行，可删除"};
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("零部件导入");
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);
            Row header = sheet.createRow(0);
            Row example = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                example.createCell(i).setCellValue(sample[i]);
                sheet.setColumnWidth(i, 18 * 256);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("生成零部件导入模板失败：" + ex.getMessage(), ex);
        }
    }

    // ------------------------------------------------------------ 导出

    /** 导出当前筛选条件下的全部零部件（不止当前页）。 */
    public byte[] export(String keyword, String itemType, String sourceType, String usingState,
                         String phaseState, String workflowState, Long createdBy) {
        List<ItemDtos.ItemSummary> rows = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            PageResult<ItemDtos.ItemSummary> page = itemService.page(pageNo, EXPORT_PAGE_SIZE, keyword,
                    itemType, sourceType, usingState, phaseState, workflowState, createdBy,
                    null, null, null, null, null, null);
            rows.addAll(page.items());
            if (rows.size() >= page.total() || page.items().isEmpty()) {
                break;
            }
            pageNo++;
        }
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("零部件");
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
            int index = 1;
            for (ItemDtos.ItemSummary row : rows) {
                ItemDtos.ItemDesignAttr attr = itemService.designAttr(row.id());
                Row line = sheet.createRow(index++);
                line.createCell(0).setCellValue(nullToEmpty(row.code()));
                line.createCell(1).setCellValue(nullToEmpty(row.name()));
                line.createCell(2).setCellValue(nullToEmpty(row.itemType()));
                line.createCell(3).setCellValue(nullToEmpty(row.specification()));
                line.createCell(4).setCellValue(nullToEmpty(row.material()));
                line.createCell(5).setCellValue(nullToEmpty(row.unit()));
                line.createCell(6).setCellValue(sourceTypeLabel(row.sourceType()));
                line.createCell(7).setCellValue(nullToEmpty(row.revisionLabel()));
                line.createCell(8).setCellValue(lifecycleLabel(row.lifecycleState()));
                line.createCell(9).setCellValue(usingStateLabel(row.usingState()));
                line.createCell(10).setCellValue(workflowLabel(row.workflowState()));
                line.createCell(11).setCellValue(row.releasedAt() == null ? "" : row.releasedAt().toString());
                line.createCell(12).setCellValue(nullToEmpty(row.createdByName()));
                line.createCell(13).setCellValue(nullToEmpty(row.updatedByName()));
                line.createCell(14).setCellValue(row.createdAt() == null ? "" : row.createdAt().toString());
                line.createCell(15).setCellValue(nullToEmpty(attr.provider()));
                line.createCell(16).setCellValue(nullToEmpty(attr.dwgNo()));
                line.createCell(17).setCellValue(nullToEmpty(attr.productCategory()));
                line.createCell(18).setCellValue(nullToEmpty(attr.productModel()));
                line.createCell(19).setCellValue(nullToEmpty(attr.productSpec()));
                line.createCell(20).setCellValue(nullToEmpty(attr.materialType()));
                line.createCell(21).setCellValue(nullToEmpty(attr.surfaceTreatment()));
                line.createCell(22).setCellValue(Boolean.TRUE.equals(attr.isTooling()) ? "是" : "否");
                line.createCell(23).setCellValue(nullToEmpty(attr.weightUnit()));
                line.createCell(24).setCellValue(nullToEmpty(attr.montage()));
                line.createCell(25).setCellValue(Boolean.TRUE.equals(attr.colorItem()) ? "是" : "否");
            }
            int[] widths = {18, 26, 14, 22, 14, 8, 10, 10, 12, 10, 10, 20, 14, 14, 20,
                    18, 18, 16, 16, 16, 14, 16, 10, 10, 16, 10};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("导出零部件失败：" + ex.getMessage(), ex);
        }
    }

    // ------------------------------------------------------------ 导入

    /** 预演：只做数据检查与新建/更新判定，不落库。 */
    public Map<String, Object> preview(InputStream input) {
        List<ParsedRow> rows = parse(input);
        Map<String, Long> existing = existingByCode();
        List<String> errors = new ArrayList<>();
        int create = 0;
        int update = 0;
        for (ParsedRow row : rows) {
            if (row.code() == null || row.code().isBlank()) {
                errors.add("第 " + row.rowNo() + " 行：编号为空（新建可留空自动取号）");
                create++;
            } else if (existing.containsKey(row.code().trim())) {
                update++;
            } else {
                create++;
            }
            if (row.name() == null || row.name().isBlank()) {
                errors.add("第 " + row.rowNo() + " 行：名称为空");
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("createCount", create);
        result.put("updateCount", update);
        result.put("errors", errors);
        result.put("valid", errors.isEmpty());
        return result;
    }

    /** 应用：逐行新建或更新（部分失败不阻断其余行，返回逐行信息）。 */
    public Map<String, Object> apply(InputStream input, Long userId) {
        List<ParsedRow> rows = parse(input);
        Map<String, Long> existing = existingByCode();
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> messages = new ArrayList<>();
        for (ParsedRow row : rows) {
            if (row.name() == null || row.name().isBlank()) {
                skipped++;
                messages.add("第 " + row.rowNo() + " 行：名称不能为空，已跳过");
                continue;
            }
            try {
                ItemDtos.ItemSaveReq req = row.toSaveReq();
                Long id = row.code() == null ? null : existing.get(row.code().trim());
                if (id == null) {
                    itemService.create(req, userId);
                    created++;
                } else {
                    itemService.update(id, req, userId);
                    updated++;
                }
            } catch (BizException ex) {
                skipped++;
                messages.add("第 " + row.rowNo() + " 行：" + ex.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("created", created);
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("messages", messages);
        return result;
    }

    // ------------------------------------------------------------ 解析

    private List<ParsedRow> parse(InputStream input) {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "Excel 中没有可读取的工作表");
            }
            Row header = sheet.getRow(0);
            if (header == null) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "第一行必须是表头（至少包含：名称）");
            }
            Map<String, Integer> index = new HashMap<>();
            for (Cell cell : header) {
                String name = cellString(cell);
                if (name != null && !name.isBlank()) {
                    index.put(name.trim(), cell.getColumnIndex());
                }
            }
            if (!index.containsKey("名称")) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "表头缺少「名称」列");
            }
            List<ParsedRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row line = sheet.getRow(i);
                if (line == null) {
                    continue;
                }
                ParsedRow row = ParsedRow.of(line, index, i + 1);
                if (row.isEmpty()) {
                    continue;
                }
                rows.add(row);
                if (rows.size() > MAX_IMPORT_ROWS) {
                    throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                            "单次导入不能超过 " + MAX_IMPORT_ROWS + " 行，请拆分文件");
                }
            }
            return rows;
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "无法读取 Excel：" + ex.getMessage());
        }
    }

    private Map<String, Long> existingByCode() {
        Map<String, Long> map = new HashMap<>();
        for (PlmObject object : objectMapper.selectList(new LambdaQueryWrapper<PlmObject>()
                .eq(PlmObject::getObjectType, ItemService.OBJECT_TYPE)
                .eq(PlmObject::getIsLatest, Boolean.TRUE)
                .select(PlmObject::getId, PlmObject::getCode))) {
            map.put(object.getCode(), object.getId());
        }
        return map;
    }

    private record ParsedRow(int rowNo, String code, String name, String itemType, String specification,
                             String material, String unit, String sourceType, String remark,
                             String provider, String dwgNo, String productCategory, String productModel,
                             String productSpec, String materialType, String surfaceTreatment,
                             boolean tooling, String weightUnit, String montage, boolean colorItem) {

        static ParsedRow of(Row line, Map<String, Integer> index, int rowNo) {
            return new ParsedRow(rowNo,
                    cellValue(line, index, "编号"),
                    cellValue(line, index, "名称"),
                    cellValue(line, index, "类型"),
                    cellValue(line, index, "规格型号"),
                    cellValue(line, index, "材料"),
                    cellValue(line, index, "单位"),
                    cellValue(line, index, "供应方式"),
                    cellValue(line, index, "备注"),
                    cellValue(line, index, "供应商"),
                    cellValue(line, index, "图号"),
                    cellValue(line, index, "产品类别"),
                    cellValue(line, index, "产品型号"),
                    cellValue(line, index, "产品规格"),
                    cellValue(line, index, "毛坯种类"),
                    cellValue(line, index, "表面处理"),
                    yes(cellValue(line, index, "是否工装")),
                    cellValue(line, index, "重量单位"),
                    cellValue(line, index, "所属装配"),
                    yes(cellValue(line, index, "是否颜色件")));
        }

        boolean isEmpty() {
            return (code == null || code.isBlank()) && (name == null || name.isBlank());
        }

        ItemDtos.ItemSaveReq toSaveReq() {
            return new ItemDtos.ItemSaveReq(code, name, itemType == null || itemType.isBlank() ? "PART" : itemType,
                    specification, material, unit, null,
                    sourceType == null ? null : ("外购".equals(sourceType) || "外购件".equals(sourceType)
                            || "BUY".equalsIgnoreCase(sourceType) ? "BUY" : "MAKE"),
                    null, remark, null, null,
                    new ItemDtos.ItemDesignAttr(provider, dwgNo, productCategory, productModel, productSpec,
                            materialType, null, surfaceTreatment, tooling, weightUnit, montage, colorItem),
                    // Excel 导入不含客制字段（模板列固定），保留既有 ext_json
                    null);
        }

        private static boolean yes(String value) {
            return value != null && ("是".equals(value.trim()) || "true".equalsIgnoreCase(value.trim())
                    || "Y".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()));
        }
    }

    private static String cellValue(Row line, Map<String, Integer> index, String header) {
        Integer col = index.get(header);
        if (col == null) {
            return null;
        }
        return cellString(line.getCell(col));
    }

    private static String cellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double value = cell.getNumericCellValue();
                yield value == Math.floor(value) && !Double.isInfinite(value)
                        ? String.valueOf((long) value) : String.valueOf(value);
            }
            case BOOLEAN -> cell.getBooleanCellValue() ? "是" : "否";
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String sourceTypeLabel(String sourceType) {
        return "BUY".equalsIgnoreCase(sourceType) ? "外购" : "自制";
    }

    private static String lifecycleLabel(String state) {
        return switch (state == null ? "" : state) {
            case "Draft", "INIT" -> "草稿";
            case "InWorkflow" -> "审批中";
            case "Released" -> "已发布";
            case "Obsolete" -> "已废弃";
            default -> nullToEmpty(state);
        };
    }

    private static String usingStateLabel(String state) {
        return switch (state == null ? "" : state) {
            case "Using" -> "在用";
            case "StopUsing" -> "停用";
            case "GiveUp" -> "废弃";
            default -> "在用";
        };
    }

    private static String workflowLabel(String state) {
        return switch (state == null ? "" : state) {
            case "New", "NONE" -> "新建";
            case "Running" -> "审批中";
            case "Approved" -> "已通过";
            case "Rejected" -> "已驳回";
            default -> nullToEmpty(state);
        };
    }

    /** 未使用但保留：导出文件名的时间戳由 Controller 生成。 */
    static String nowSuffix() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }
}
