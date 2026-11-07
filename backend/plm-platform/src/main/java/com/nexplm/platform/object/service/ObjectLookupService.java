package com.nexplm.platform.object.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.object.api.ObjectLookupDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 扫码查对象（P10-3）：按编码<b>精确</b>匹配，先查通用对象表、再兜底变更单。
 *
 * <p>为什么需要这个接口：现有列表接口都是模糊搜索（{@code LIKE %kw%}），扫码得到的是一串
 * 精确编码，用模糊接口会出现"扫 A 返回一堆 A1/A2"，前端还得本地二次过滤；而唯一支持跨类型的
 * {@code /open/objects} 走的是 API Key 作用域，普通登录用户没有。</p>
 *
 * <p>为什么查不到返回空数组而不是 404：扫到别的公司的、或者一张随手拍的二维码，
 * 都是正常业务结果，不是"资源不存在"的错误。</p>
 */
@Service
public class ObjectLookupService {

    /** 同一编码可能同时存在物料与文档（编码规则各自独立），最多回 5 条让前端选。 */
    private static final int MAX_RESULTS = 5;

    private final JdbcTemplate jdbcTemplate;

    public ObjectLookupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 通用对象模糊检索（对象选择器用）：按编码/名称模糊匹配，可按对象类型过滤。
     *
     * <p>与 {@link #lookup} 的分工：扫码要"精确且唯一"，选择器要"边输边缩小范围"，
     * 所以这里走 {@code ILIKE} 并限制返回条数，编码/名称命中优先、更新时间次之排序。</p>
     */
    public List<ObjectLookupDtos.LookupResult> search(Long companyId, String keyword, String objectType,
                                                      Integer limit) {
        int size = limit == null ? 20 : Math.min(Math.max(limit, 1), 50);
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if ("ChgEcr".equalsIgnoreCase(objectType)) {
            return searchChgEcr(companyId, kw, size);
        }
        String type = objectType == null || objectType.isBlank() ? null : objectType.trim();
        List<ObjectLookupDtos.LookupResult> results = jdbcTemplate.query("""
                        SELECT id, object_type, code, name, revision, iteration, lifecycle_state, updated_at
                          FROM plm_object
                         WHERE company_id = ? AND deleted = false AND is_latest = true
                           AND (?::varchar IS NULL OR object_type = ?)
                           AND (?::varchar IS NULL OR code ILIKE ? OR name ILIKE ?)
                         ORDER BY (code = ?) DESC, (code ILIKE ?) DESC, updated_at DESC
                         LIMIT ?
                        """,
                (rs, rowNum) -> new ObjectLookupDtos.LookupResult(
                        rs.getLong("id"), rs.getString("object_type"), rs.getString("code"),
                        rs.getString("name"), rs.getInt("revision"), rs.getInt("iteration"),
                        rs.getString("lifecycle_state"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                companyId, type, type, kw, like(kw), like(kw), kw == null ? "" : kw,
                kw == null ? "" : kw + "%", size);
        if (!results.isEmpty() || type != null) {
            return results;
        }
        // 不限类型时的兜底与扫码保持一致：变更单不在 plm_object
        return searchChgEcr(companyId, kw, size);
    }

    private List<ObjectLookupDtos.LookupResult> searchChgEcr(Long companyId, String keyword, int limit) {
        return jdbcTemplate.query("""
                        SELECT id, code, title, status, updated_at
                          FROM chg_ecr
                         WHERE company_id = ? AND deleted = false
                           AND (?::varchar IS NULL OR code ILIKE ? OR title ILIKE ?)
                         ORDER BY updated_at DESC
                         LIMIT ?
                        """,
                (rs, rowNum) -> new ObjectLookupDtos.LookupResult(
                        rs.getLong("id"), "ChgEcr", rs.getString("code"), rs.getString("title"),
                        null, null, rs.getString("status"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                companyId, keyword, like(keyword), like(keyword), limit);
    }

    private String like(String keyword) {
        return keyword == null ? null : "%" + keyword + "%";
    }

    public List<ObjectLookupDtos.LookupResult> lookup(Long companyId, String code) {
        if (code == null || code.isBlank()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "请提供要查询的编码");
        }
        String keyword = code.trim();
        // 大小写不敏感：标签上印的编码常被人手抄成全大写，库里存的是原始大小写
        List<ObjectLookupDtos.LookupResult> results = jdbcTemplate.query("""
                        SELECT id, object_type, code, name, revision, iteration, lifecycle_state,
                               updated_at
                          FROM plm_object
                         WHERE company_id = ? AND deleted = false AND is_latest = true
                           AND UPPER(code) = UPPER(?)
                         ORDER BY updated_at DESC
                         LIMIT ?
                        """,
                (rs, rowNum) -> new ObjectLookupDtos.LookupResult(
                        rs.getLong("id"), rs.getString("object_type"), rs.getString("code"),
                        rs.getString("name"), rs.getInt("revision"), rs.getInt("iteration"),
                        rs.getString("lifecycle_state"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                companyId, keyword, MAX_RESULTS);
        if (!results.isEmpty()) {
            return results;
        }
        // 变更单不在 plm_object（独立表 + 独立状态机），扫到 ECR 编码时兜底查一次
        return jdbcTemplate.query("""
                        SELECT id, code, title, status, updated_at
                          FROM chg_ecr
                         WHERE company_id = ? AND deleted = false
                           AND UPPER(code) = UPPER(?)
                         ORDER BY updated_at DESC
                         LIMIT ?
                        """,
                (rs, rowNum) -> new ObjectLookupDtos.LookupResult(
                        rs.getLong("id"), "ChgEcr", rs.getString("code"), rs.getString("title"),
                        null, null, rs.getString("status"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                companyId, keyword, MAX_RESULTS);
    }
}
