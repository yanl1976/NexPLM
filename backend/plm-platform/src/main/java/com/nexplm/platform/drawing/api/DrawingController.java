package com.nexplm.platform.drawing.api;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.Result;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.api.DocDtos;
import com.nexplm.platform.doc.service.DocService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图纸管理（DRW-01/02/03）。
 *
 * <p>蓝本依据：`PseItem.cmdsql:GetUpdateDWGDocInfo` —— `PSEITEMRELATION`
 * （PEID=物料、CEID=文档、RELATIONTYPE='Specification'）关联 `DOCDATASET`
 * （FILEEXTENT='dwg'）。新系统复用通用关系表 `plm_relation`。</p>
 */
@Tag(name = "图纸管理")
@RestController
@RequestMapping("/api/v1/drawings")
public class DrawingController {

    private static final String RELATION = "Specification";

    private final JdbcTemplate jdbcTemplate;
    private final DocService docService;
    private final ObjectMapper objectMapper;

    public DrawingController(JdbcTemplate jdbcTemplate, DocService docService, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.docService = docService;
        this.objectMapper = objectMapper;
    }

    /** 图纸清单（含关联物料、图号、图幅）。 */
    @Operation(summary = "图纸清单（可按图号/物料/类型过滤）")
    @GetMapping
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long itemId,
                                                  @RequestParam(required = false) String itemType) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.id AS link_id, r.src_id AS item_id, i.code AS item_code, i.name AS item_name,
                       m.item_type, r.dst_id AS doc_id, d.code AS dwg_no, d.name AS doc_name,
                       dm.file_ext, dm.file_count,
                       r.attrs_json->>'sheetSize' AS sheet_size,
                       r.created_at
                  FROM plm_relation r
                  JOIN plm_object i ON i.id = r.src_id AND i.deleted = false
                  JOIN plm_object d ON d.id = r.dst_id AND d.deleted = false
                  LEFT JOIN item_master m ON m.object_id = r.src_id
                  LEFT JOIN doc_master dm ON dm.object_id = r.dst_id
                 WHERE r.relation_type = ? AND r.deleted = false
                """);
        List<Object> args = new ArrayList<>();
        args.add(RELATION);
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (i.code ILIKE ? OR i.name ILIKE ? OR d.code ILIKE ? OR d.name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (itemId != null) {
            sql.append(" AND r.src_id = ?");
            args.add(itemId);
        }
        if (itemType != null && !itemType.isBlank()) {
            sql.append(" AND m.item_type = ?");
            args.add(itemType.trim());
        }
        sql.append(" ORDER BY r.created_at DESC, r.id DESC");
        return Result.ok(jdbcTemplate.queryForList(sql.toString(), args.toArray()));
    }

    /** 某个物料挂了哪些图纸（等价于蓝本 GetUpdateDWGDocInfo）。 */
    @Operation(summary = "物料的图纸")
    @GetMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<Map<String, Object>>> byItem(@PathVariable Long itemId) {
        return list(null, itemId, null);
    }

    /** 关联图纸：可指定已有文档，或按图号新建一张图纸文档。 */
    @Operation(summary = "给物料关联图纸（没有文档时按图号新建）")
    @PostMapping
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    public Result<Map<String, Object>> create(@Valid @RequestBody DrawingCreateReq req) {
        Long itemId = req.itemId();
        Long exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM plm_object
                 WHERE id = ? AND object_type = 'PdmItem' AND deleted = false
                """, Long.class, itemId);
        if (exists == null || exists == 0) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "物料不存在：" + itemId);
        }

        Long docId = req.docId();
        if (docId == null) {
            DocDtos.DocDetail doc = docService.create(new DocDtos.DocSaveReq(
                    req.dwgNo().trim(),
                    req.name() == null || req.name().isBlank() ? req.dwgNo().trim() : req.name().trim(),
                    "DESIGN", "CAD", null, null, null, null, null, null),
                    CurrentUser.userId());
            docId = doc.id();
            jdbcTemplate.update("UPDATE doc_master SET file_ext = 'dwg' WHERE object_id = ?", docId);
        } else {
            Long ok = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM plm_object
                     WHERE id = ? AND object_type = 'PdmDoc' AND deleted = false
                    """, Long.class, docId);
            if (ok == null || ok == 0) {
                throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "图纸文档不存在：" + docId);
            }
        }

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("dwgNo", req.dwgNo().trim());
        attrs.put("sheetSize", req.sheetSize());
        String json;
        try {
            json = objectMapper.writeValueAsString(attrs);
        } catch (Exception ex) {
            throw new IllegalStateException("图纸属性序列化失败", ex);
        }
        Long companyId = CurrentUser.companyId() == null ? 0L : CurrentUser.companyId();
        Long linkId = IdWorker.getId();
        jdbcTemplate.update("""
                INSERT INTO plm_relation (id, relation_type, src_id, dst_id, seq, attrs_json,
                                          company_id, deleted, created_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, 0, ?::jsonb, ?, false, ?, now(), now())
                ON CONFLICT DO NOTHING
                """, linkId, RELATION, itemId, docId, json, companyId, CurrentUser.userId());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id AS link_id, src_id AS item_id, dst_id AS doc_id, attrs_json
                  FROM plm_relation
                 WHERE relation_type = ? AND src_id = ? AND dst_id = ? AND deleted = false
                 ORDER BY id DESC LIMIT 1
                """, RELATION, itemId, docId);
        return Result.ok(rows.isEmpty() ? Map.of("itemId", itemId, "docId", docId) : rows.get(0));
    }

    /** 解除关联：只删关系，不删图纸文档。 */
    @Operation(summary = "解除图纸关联（不删文档）")
    @DeleteMapping("/{linkId}")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    public Result<Void> remove(@PathVariable Long linkId) {
        jdbcTemplate.update("""
                UPDATE plm_relation SET deleted = true, updated_at = now()
                 WHERE id = ? AND relation_type = ? AND deleted = false
                """, linkId, RELATION);
        return Result.ok();
    }

    /** 关联请求：{@code docId} 为空时按图号新建图纸文档。 */
    public record DrawingCreateReq(@NotNull(message = "物料不能为空") Long itemId,
                                   @NotBlank(message = "图号不能为空") String dwgNo,
                                   String name,
                                   String sheetSize,
                                   Long docId) {
    }
}
