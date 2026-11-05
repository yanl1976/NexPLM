package com.nexplm.platform.doc.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.api.DocDtos;
import com.nexplm.platform.doc.domain.DocClass;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 图文档批量导入（CSV）。
 * <p>表头：{@code code,name,docType,source,classCode,keyword,description}（仅 name 必填）。</p>
 * <p>逐行独立处理：单行失败只记录错误，不影响其它行；分类按编码引用，
 * 若目标分类配置了必填属性，则需要在系统中补录（CSV 不携带分类属性列）。</p>
 */
@Service
public class DocImportService {

    private static final int MAX_ROWS = 500;
    private static final List<String> KNOWN_HEADERS = List.of(
            "code", "name", "doctype", "source", "classcode", "keyword", "description");

    public record ImportError(int line, String code, String message) {
    }

    public record ImportResult(int total, int success, int failed, List<ImportError> errors,
                               List<Long> createdIds) {
    }

    private final DocService docService;
    private final DocClassService docClassService;

    public DocImportService(DocService docService, DocClassService docClassService) {
        this.docService = docService;
        this.docClassService = docClassService;
    }

    public ImportResult importCsv(InputStream inputStream, Long userId) {
        List<String> lines = readLines(inputStream);
        if (lines.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "CSV 内容为空");
        }
        Map<String, Integer> header = parseHeader(lines.get(0));
        if (!header.containsKey("name")) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "CSV 表头缺少 name 列，可用列：" + String.join(",", KNOWN_HEADERS));
        }
        int total = 0;
        int success = 0;
        List<ImportError> errors = new ArrayList<>();
        List<Long> createdIds = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String raw = lines.get(index);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            total++;
            if (total > MAX_ROWS) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "单次最多导入 " + MAX_ROWS + " 行");
            }
            int lineNo = index + 1;
            List<String> cells = parseLine(raw);
            String code = cell(cells, header, "code");
            String name = cell(cells, header, "name");
            try {
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException("name 不能为空");
                }
                Long classId = resolveClassId(cell(cells, header, "classcode"));
                DocDtos.DocDetail created = docService.create(new DocDtos.DocSaveReq(
                        blankToNull(code),
                        name.trim(),
                        blankToNull(cell(cells, header, "doctype")) == null
                                ? "OTHER" : cell(cells, header, "doctype").trim().toUpperCase(Locale.ROOT),
                        blankToNull(cell(cells, header, "source")) == null
                                ? "NORMAL" : cell(cells, header, "source").trim().toUpperCase(Locale.ROOT),
                        blankToNull(cell(cells, header, "keyword")),
                        blankToNull(cell(cells, header, "description")),
                        null, 0, classId, Map.of()), userId);
                success++;
                createdIds.add(created.id());
            } catch (Exception ex) {
                errors.add(new ImportError(lineNo, code,
                        ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            }
        }
        return new ImportResult(total, success, errors.size(), errors, createdIds);
    }

    /** 提供模板内容（前端下载用），仅表头 + 一行示例。 */
    public String template() {
        return "code,name,docType,source,classCode,keyword,description\n"
                + ",主传动装配图,DESIGN,NORMAL,MECH-ASSY,传动,示例行可删除\n";
    }

    // ------------------------------------------------------------ 内部

    private Long resolveClassId(String classCode) {
        if (classCode == null || classCode.isBlank()) {
            return null;
        }
        DocClass clazz = docClassService.findByCode(classCode)
                .orElseThrow(() -> new IllegalArgumentException("分类编码不存在：" + classCode));
        return clazz.getId();
    }

    private List<String> readLines(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            if (!lines.isEmpty()) {
                lines.set(0, lines.get(0).replace("\uFEFF", ""));
            }
            return lines;
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "读取 CSV 失败：" + ex.getMessage());
        }
    }

    private Map<String, Integer> parseHeader(String line) {
        Map<String, Integer> header = new HashMap<>();
        List<String> cells = parseLine(line);
        for (int i = 0; i < cells.size(); i++) {
            String key = cells.get(i).trim().toLowerCase(Locale.ROOT);
            if (!key.isEmpty()) {
                header.put(key, i);
            }
        }
        return header;
    }

    /** 解析一行 CSV：支持双引号包裹与 "" 转义，不处理引号内换行。 */
    private List<String> parseLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cells.add(current.toString());
        return cells;
    }

    private String cell(List<String> cells, Map<String, Integer> header, String column) {
        Integer index = header.get(column);
        if (index == null || index >= cells.size()) {
            return null;
        }
        return cells.get(index);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
