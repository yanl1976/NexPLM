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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图纸关联（DRW-01）。
 *
 * <p>图纸本身是文档域对象（`PdmDoc`，`doc_master.file_ext='dwg'`），由
 * `DocController` 负责创建、上传、版本与检出；本控制器只维护
 * 蓝本 `PSEITEMRELATION(Specification)` 的等价关系 `plm_relation`。</p>
 */
@Tag(name = "图纸关联")
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

    /**
     * 新建图纸并关联物料。
     *
     * <p>字段取自蓝本 `DOCDATASET + DOCBASEFORM`：
     * 编号/名称/详细描述/分类/密级；
     * 标准号、年代号、旧编号、规格、重要度等级、精度等级、计量单位、
     * 附图序号、附图分类、项目代号、文件类型（`DOCDWG-CREATE.dic` 与
     * `DOCBASEFORM$.dic`）。</p>
     */
    @Operation(summary = "新建图纸并关联物料")
    @PostMapping
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    public Result<Map<String, Object>> create(@jakarta.validation.Valid @RequestBody DrawingCreateReq req) {
        requireObject(req.itemId(), "PdmItem", "物料不存在：");
        DocDtos.DocDetail doc = docService.create(new DocDtos.DocSaveReq(
                req.code().trim(),
                req.name().trim(),
                "DESIGN", "CAD", null, req.description(), null, req.securityLevel(), req.classId(), null),
                CurrentUser.userId());

        Map<String, Object> drawing = new HashMap<>();
        drawing.put("standardCode", req.standardCode());
        drawing.put("yearNo", req.yearNo());
        drawing.put("oldCode", req.oldCode());
        drawing.put("spec", req.spec());
        drawing.put("importance", req.importance());
        drawing.put("precisionGrade", req.precisionGrade());
        drawing.put("measureUnit", req.measureUnit());
        drawing.put("attachNo", req.attachNo());
        drawing.put("attachType", req.attachType());
        drawing.put("projectCode", req.projectCode());
        drawing.put("fileType", req.fileType());
        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of("drawing", drawing));
        } catch (Exception ex) {
            throw new IllegalStateException("图纸属性序列化失败", ex);
        }
        jdbcTemplate.update("""
                UPDATE plm_object SET ext_json = ext_json || ?::jsonb WHERE id = ?
                """, json, doc.id());

        linkRelation(req.itemId(), doc.id());
        Map<String, Object> result = new HashMap<>();
        result.put("docId", doc.id());
        result.put("code", doc.code());
        result.put("name", doc.name());
        return Result.ok(result);
    }

    /** 某个物料的图纸（等价蓝本 GetUpdateDWGDocInfo）。 */
    @Operation(summary = "物料的图纸")
    @GetMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<Map<String, Object>>> byItem(@PathVariable Long itemId) {
        return Result.ok(jdbcTemplate.queryForList("""
                SELECT r.id AS link_id, r.src_id AS item_id, r.dst_id AS doc_id,
                       d.code AS dwg_no, d.name AS doc_name, o.lifecycle_state,
                       o.revision, o.iteration, dm.file_ext, dm.file_count
                  FROM plm_relation r
                  JOIN plm_object d ON d.id = r.dst_id AND d.deleted = false
                  LEFT JOIN plm_object o ON o.id = r.dst_id
                  LEFT JOIN doc_master dm ON dm.object_id = r.dst_id
                 WHERE r.relation_type = ? AND r.deleted = false AND r.src_id = ?
                 ORDER BY r.created_at DESC
                """, RELATION, itemId));
    }

    /** 某张图纸关联了哪些物料。 */
    @Operation(summary = "图纸关联的物料")
    @GetMapping("/docs/{docId}/items")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<Map<String, Object>>> byDoc(@PathVariable Long docId) {
        return Result.ok(jdbcTemplate.queryForList("""
                SELECT r.id AS link_id, r.src_id AS item_id, i.code AS item_code,
                       i.name AS item_name, m.item_type
                  FROM plm_relation r
                  JOIN plm_object i ON i.id = r.src_id AND i.deleted = false
                  LEFT JOIN item_master m ON m.object_id = r.src_id
                 WHERE r.relation_type = ? AND r.deleted = false AND r.dst_id = ?
                 ORDER BY r.created_at DESC
                """, RELATION, docId));
    }

    /** 把已有图纸文档关联到物料；图纸创建与文件上传走 {@code /docs}。 */
    @Operation(summary = "关联图纸文档到物料")
    @PostMapping("/{docId}/link")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    public Result<Void> link(@PathVariable Long docId, @RequestParam Long itemId) {
        requireObject(itemId, "PdmItem", "物料不存在：");
        requireObject(docId, "PdmDoc", "图纸文档不存在：");
        linkRelation(itemId, docId);
        return Result.ok();
    }

    private void linkRelation(Long itemId, Long docId) {
        Long companyId = CurrentUser.companyId() == null ? 0L : CurrentUser.companyId();
        jdbcTemplate.update("""
                INSERT INTO plm_relation (id, relation_type, src_id, dst_id, seq, attrs_json,
                                          company_id, deleted, created_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, 0, '{}'::jsonb, ?, false, ?, now(), now())
                ON CONFLICT DO NOTHING
                """, IdWorker.getId(), RELATION, itemId, docId, companyId, CurrentUser.userId());
    }

    /** 解除关联：只删关系，不删图纸文档与文件。 */
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

    /** 图纸新建请求：对应蓝本 `DOCDATASET + DOCBASEFORM` 的图档属性。 */
    public record DrawingCreateReq(
            @jakarta.validation.constraints.NotNull(message = "物料不能为空") Long itemId,
            @jakarta.validation.constraints.NotBlank(message = "图号不能为空") String code,
            @jakarta.validation.constraints.NotBlank(message = "图纸名称不能为空") String name,
            String description,
            Long classId,
            Integer securityLevel,
            String standardCode,
            String yearNo,
            String oldCode,
            String spec,
            String importance,
            String precisionGrade,
            String measureUnit,
            Integer attachNo,
            String attachType,
            String projectCode,
            String fileType) {
    }

    private void requireObject(Long id, String objectType, String prefix) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM plm_object
                 WHERE id = ? AND object_type = ? AND deleted = false
                """, Long.class, id, objectType);
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, prefix + id);
        }
    }
}
