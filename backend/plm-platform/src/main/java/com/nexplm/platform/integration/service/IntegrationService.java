package com.nexplm.platform.integration.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.integration.api.IntegrationDtos;
import com.nexplm.platform.integration.support.ItgHttpSender;
import com.nexplm.platform.integration.support.MappingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 集成框架（P9-1）：端点与映射配置 + 任务提交（幂等）+ 发送（重试/死信）+ 重放。
 *
 * <p>几条刻意定下的口径：</p>
 * <ul>
 *   <li><b>报文快照不可变</b>：提交时就把映射结果写进 {@code request_payload}，重放用快照。
 *       若重放时重新取源数据，"当年发了什么"这份证据就没了，而且源数据可能已经变了；</li>
 *   <li><b>幂等交给数据库</b>：(endpoint_id, biz_type, biz_key) 唯一，重复提交返回已存在的任务
 *       并标记 {@code duplicated}，不重复发送；</li>
 *   <li><b>抢占式执行</b>：发送前用 {@code UPDATE ... WHERE status IN ('PENDING','FAILED')}
 *       抢占，影响行数为 0 就放弃，避免定时重试与人工触发把同一条发两次；</li>
 *   <li><b>重试用退避，死信要人工</b>：attempt 用尽置 DEAD，不自动无限重试——
 *       对端长期不可用时，无限重试只会把日志刷满；</li>
 *   <li><b>失败不抛异常</b>：一次发送失败要落任务状态与日志后正常返回，
 *       否则批量下发会在第一条失败处中断。</li>
 * </ul>
 */
@Service
public class IntegrationService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationService.class);

    private static final Set<String> AUTH_TYPES = Set.of("NONE", "BASIC", "BEARER", "HEADER");
    private static final Set<String> JOB_STATUSES =
            Set.of("PREVIEW", "PENDING", "RUNNING", "SUCCESS", "FAILED", "DEAD");
    private static final Set<String> SYSTEM_CODES = Set.of("KINGDEE", "WMS", "MES", "OTHER");
    private static final int MAX_BODY = 4000;
    private static final int MAX_SNIPPET = 2000;
    private static final int MAX_ERROR = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ItgHttpSender sender;
    private final int retryBaseSeconds;
    private final CodeRuleService codeRuleService;

    public IntegrationService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                              ItgHttpSender sender,
                              @Value("${nexplm.integration.retry-base-seconds:30}") int retryBaseSeconds,
                              CodeRuleService codeRuleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.sender = sender;
        this.retryBaseSeconds = Math.max(1, retryBaseSeconds);
        this.codeRuleService = codeRuleService;
    }

    /* ================================================================ 端点 */

    public PageResult<IntegrationDtos.EndpointRow> endpointPage(Long companyId, String keyword,
                                                                Boolean enabled, Integer page,
                                                                Integer size) {
        StringBuilder where = new StringBuilder(" WHERE a.company_id = ? AND a.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (a.code ILIKE ? OR a.name ILIKE ? OR a.base_url ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (enabled != null) {
            where.append(" AND a.enabled = ?");
            args.add(enabled);
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM itg_endpoint a" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<IntegrationDtos.EndpointRow> items = jdbcTemplate.query("""
                        SELECT a.id, a.code, a.name, a.system_code, a.base_url, a.auth_type,
                               (a.auth_config <> '{}'::jsonb) AS auth_configured, a.timeout_ms,
                               a.enabled, a.remark, a.updated_at
                          FROM itg_endpoint a
                        """ + where + " ORDER BY a.code LIMIT ? OFFSET ?",
                (rs, rowNum) -> new IntegrationDtos.EndpointRow(rs.getLong("id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("system_code"),
                        rs.getString("base_url"), rs.getString("auth_type"),
                        rs.getBoolean("auth_configured"), rs.getInt("timeout_ms"),
                        rs.getBoolean("enabled"), rs.getString("remark"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public IntegrationDtos.EndpointDetail endpointDetail(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT *, (auth_config <> '{}'::jsonb) AS auth_configured
                  FROM itg_endpoint WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_ENDPOINT_NOT_FOUND, "集成端点不存在：" + id);
        }
        Map<String, Object> row = rows.get(0);
        return new IntegrationDtos.EndpointDetail(id, str(row.get("code")), str(row.get("name")),
                str(row.get("system_code")), str(row.get("base_url")), str(row.get("auth_type")),
                Boolean.TRUE.equals(row.get("auth_configured")),
                readMap(str(row.get("headers"))), intOf(row.get("timeout_ms")),
                Boolean.TRUE.equals(row.get("enabled")), str(row.get("remark")));
    }

    @Transactional
    public IntegrationDtos.EndpointDetail createEndpoint(IntegrationDtos.EndpointSaveReq req,
                                                         Long companyId, Long userId) {
        String code = req.code().trim();
        String authType = authType(req.authType());
        String baseUrl = normalizeUrl(req.baseUrl());
        try {
            jdbcTemplate.update("""
                            INSERT INTO itg_endpoint (id, company_id, code, name, system_code,
                                                      base_url, auth_type, auth_config, headers,
                                                      timeout_ms, enabled, remark, created_by,
                                                      updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, code, req.name().trim(),
                    systemCode(req.systemCode()), baseUrl, authType,
                    writeJson(req.authConfig() == null ? Map.of() : req.authConfig()),
                    writeJson(req.headers() == null ? Map.of() : req.headers()),
                    timeout(req.timeoutMs()), req.enabled() == null || req.enabled(),
                    blankToNull(req.remark()), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.ITG_CODE_DUPLICATED, "端点编码已存在：" + code);
        }
        return endpointDetail(byCode(companyId, code), companyId);
    }

    @Transactional
    public IntegrationDtos.EndpointDetail updateEndpoint(Long id, IntegrationDtos.EndpointSaveReq req,
                                                         Long companyId, Long userId) {
        requireEndpoint(id, companyId);
        String authType = authType(req.authType());
        // authConfig 传 null 表示"不修改"，避免前端拿不到密钥却在保存时把认证信息清空
        String authSql = req.authConfig() == null ? "" : ", auth_config = ?::jsonb";
        List<Object> args = new ArrayList<>();
        args.add(req.name().trim());
        args.add(systemCode(req.systemCode()));
        args.add(normalizeUrl(req.baseUrl()));
        args.add(authType);
        if (req.authConfig() != null) {
            args.add(writeJson(req.authConfig()));
        }
        args.add(writeJson(req.headers() == null ? Map.of() : req.headers()));
        args.add(timeout(req.timeoutMs()));
        args.add(req.enabled() == null || req.enabled());
        args.add(blankToNull(req.remark()));
        args.add(userId);
        args.add(id);
        args.add(companyId);
        jdbcTemplate.update("""
                        UPDATE itg_endpoint
                           SET name = ?, system_code = ?, base_url = ?, auth_type = ?
                        """ + authSql + """
                           , headers = ?::jsonb, timeout_ms = ?, enabled = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ? AND deleted = false
                        """, args.toArray());
        return endpointDetail(id, companyId);
    }

    /** 端点软删并连带停用其映射；历史任务保留（任务表不随端点删除消失）。 */
    @Transactional
    public void deleteEndpoint(Long id, Long companyId) {
        requireEndpoint(id, companyId);
        jdbcTemplate.update("""
                UPDATE itg_endpoint SET deleted = true, enabled = false, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        jdbcTemplate.update("""
                UPDATE itg_mapping SET deleted = true, enabled = false, updated_at = now()
                 WHERE endpoint_id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
    }

    /* ================================================================ 映射 */

    public PageResult<IntegrationDtos.MappingRow> mappingPage(Long companyId, Long endpointId,
                                                              String objectCode, Integer page,
                                                              Integer size) {
        StringBuilder where = new StringBuilder(" WHERE m.company_id = ? AND m.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (endpointId != null) {
            where.append(" AND m.endpoint_id = ?");
            args.add(endpointId);
        }
        if (objectCode != null && !objectCode.isBlank()) {
            where.append(" AND m.object_code = ?");
            args.add(objectCode.trim());
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM itg_mapping m" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<IntegrationDtos.MappingRow> items = jdbcTemplate.query("""
                        SELECT m.*, e.code AS endpoint_code,
                               jsonb_array_length(m.rules) AS rule_count
                          FROM itg_mapping m
                          JOIN itg_endpoint e ON e.id = m.endpoint_id
                        """ + where + " ORDER BY e.code, m.object_code LIMIT ? OFFSET ?",
                (rs, rowNum) -> new IntegrationDtos.MappingRow(rs.getLong("id"),
                        rs.getLong("endpoint_id"), rs.getString("endpoint_code"),
                        rs.getString("object_code"), rs.getString("direction"),
                        rs.getString("payload_version"), rs.getInt("rule_count"),
                        rs.getBoolean("enabled"), rs.getString("remark"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public IntegrationDtos.MappingDetail mappingDetail(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT m.*, e.code AS endpoint_code
                  FROM itg_mapping m JOIN itg_endpoint e ON e.id = m.endpoint_id
                 WHERE m.id = ? AND m.company_id = ? AND m.deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_MAPPING_NOT_FOUND, "字段映射不存在：" + id);
        }
        Map<String, Object> row = rows.get(0);
        return new IntegrationDtos.MappingDetail(id, longOf(row.get("endpoint_id")),
                str(row.get("endpoint_code")), str(row.get("object_code")),
                str(row.get("direction")), str(row.get("payload_version")),
                rulesOf(str(row.get("rules"))), Boolean.TRUE.equals(row.get("enabled")),
                str(row.get("remark")));
    }

    @Transactional
    public IntegrationDtos.MappingDetail createMapping(IntegrationDtos.MappingSaveReq req,
                                                       Long companyId, Long userId) {
        requireEndpoint(req.endpointId(), companyId);
        String direction = direction(req.direction());
        String rules = rulesJson(req.rules());
        try {
            jdbcTemplate.update("""
                            INSERT INTO itg_mapping (id, company_id, endpoint_id, object_code,
                                                     direction, payload_version, rules, enabled,
                                                     remark, created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, req.endpointId(), req.objectCode().trim(),
                    direction, payloadVersion(req.payloadVersion()), rules,
                    req.enabled() == null || req.enabled(), blankToNull(req.remark()),
                    userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.ITG_MAPPING_DUPLICATED,
                    "该端点下 " + req.objectCode() + "/" + direction + " 的映射已存在");
        }
        return mappingDetail(byMappingKey(companyId, req.endpointId(), req.objectCode().trim(),
                direction), companyId);
    }

    @Transactional
    public IntegrationDtos.MappingDetail updateMapping(Long id, IntegrationDtos.MappingSaveReq req,
                                                       Long companyId, Long userId) {
        Map<String, Object> exist = requireMapping(id, companyId);
        requireEndpoint(req.endpointId(), companyId);
        String objectCode = req.objectCode().trim();
        String direction = direction(req.direction());
        if (!objectCode.equals(str(exist.get("object_code")))
                || !direction.equals(str(exist.get("direction")))
                || !req.endpointId().equals(longOf(exist.get("endpoint_id")))) {
            Integer conflict = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM itg_mapping
                     WHERE company_id = ? AND deleted = false AND id <> ?
                       AND endpoint_id = ? AND object_code = ? AND direction = ?
                    """, Integer.class, companyId, id, req.endpointId(), objectCode, direction);
            if (conflict != null && conflict > 0) {
                throw BizException.of(ErrorCode.ITG_MAPPING_DUPLICATED,
                        "该端点下 " + objectCode + "/" + direction + " 的映射已存在");
            }
        }
        jdbcTemplate.update("""
                        UPDATE itg_mapping
                           SET endpoint_id = ?, object_code = ?, direction = ?, payload_version = ?,
                               rules = ?::jsonb, enabled = ?, remark = ?, updated_by = ?,
                               updated_at = now()
                         WHERE id = ? AND company_id = ? AND deleted = false
                        """,
                req.endpointId(), objectCode, direction, payloadVersion(req.payloadVersion()),
                rulesJson(req.rules()), req.enabled() == null || req.enabled(),
                blankToNull(req.remark()), userId, id, companyId);
        return mappingDetail(id, companyId);
    }

    @Transactional
    public void deleteMapping(Long id, Long companyId) {
        requireMapping(id, companyId);
        jdbcTemplate.update("""
                UPDATE itg_mapping SET deleted = true, enabled = false, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    /* ================================================================ 任务 */

    public PageResult<IntegrationDtos.JobRow> jobPage(Long companyId, Long endpointId,
                                                      String bizType, String status,
                                                      Integer page, Integer size) {
        StringBuilder where = new StringBuilder(" WHERE j.company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (endpointId != null) {
            where.append(" AND j.endpoint_id = ?");
            args.add(endpointId);
        }
        if (bizType != null && !bizType.isBlank()) {
            where.append(" AND j.biz_type = ?");
            args.add(bizType.trim());
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND j.status = ?");
            args.add(normalize(status, JOB_STATUSES, "任务状态"));
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM itg_job j" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<IntegrationDtos.JobRow> items = jdbcTemplate.query("""
                        SELECT j.*, e.code AS endpoint_code FROM itg_job j
                          LEFT JOIN itg_endpoint e ON e.id = j.endpoint_id
                        """ + where + " ORDER BY j.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new IntegrationDtos.JobRow(rs.getLong("id"),
                        rs.getString("job_no"), rs.getLong("endpoint_id"),
                        rs.getString("endpoint_code"), rs.getString("biz_type"),
                        rs.getString("biz_key"), rs.getString("direction"),
                        rs.getString("status"), rs.getInt("attempt"), rs.getInt("max_attempt"),
                        (Integer) rs.getObject("http_status"), rs.getInt("duration_ms"),
                        rs.getObject("next_retry_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("finished_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public IntegrationDtos.JobDetail jobDetail(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT j.*, e.code AS endpoint_code FROM itg_job j
                  LEFT JOIN itg_endpoint e ON e.id = j.endpoint_id
                 WHERE j.id = ? AND j.company_id = ?
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_JOB_NOT_FOUND, "集成任务不存在：" + id);
        }
        Map<String, Object> row = rows.get(0);
        List<IntegrationDtos.JobLogRow> logs = jdbcTemplate.query("""
                        SELECT * FROM itg_log WHERE job_id = ? ORDER BY attempt, id
                        """,
                (rs, rowNum) -> new IntegrationDtos.JobLogRow(rs.getLong("id"),
                        rs.getInt("attempt"), rs.getString("status"),
                        (Integer) rs.getObject("http_status"), (Integer) rs.getObject("duration_ms"),
                        rs.getString("error_msg"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                id);
        return new IntegrationDtos.JobDetail(id, str(row.get("job_no")),
                longOf(row.get("endpoint_id")), str(row.get("endpoint_code")),
                row.get("mapping_id") == null ? null : longOf(row.get("mapping_id")),
                str(row.get("biz_type")), str(row.get("biz_key")), str(row.get("direction")),
                str(row.get("status")), intOf(row.get("attempt")), intOf(row.get("max_attempt")),
                (Integer) row.get("http_status"), intOf(row.get("duration_ms")),
                str(row.get("error_msg")), readJson(str(row.get("request_payload"))),
                str(row.get("response_body")),
                row.get("next_retry_at") instanceof OffsetDateTime retryAt ? retryAt : null,
                row.get("started_at") instanceof OffsetDateTime startedAt ? startedAt : null,
                row.get("finished_at") instanceof OffsetDateTime finishedAt ? finishedAt : null,
                logs);
    }

    /**
     * 提交任务：映射 → 快照 → 幂等插入 → （非 dryRun）立即发送一次。
     *
     * <p>幂等命中时直接返回已有任务（{@code duplicated=true}），不改状态也不重发。</p>
     */
    @Transactional
    public IntegrationDtos.JobSubmitResult submitJob(IntegrationDtos.JobSubmitReq req,
                                                     Long companyId, Long userId) {
        Map<String, Object> endpoint = requireEndpoint(req.endpointId(), companyId);
        if (!Boolean.TRUE.equals(endpoint.get("enabled"))) {
            throw BizException.of(ErrorCode.ITG_ENDPOINT_INVALID, "端点已停用，不能提交任务");
        }
        String bizType = req.bizType().trim();
        String bizKey = req.bizKey().trim();
        Map<String, Object> mapping = findMapping(companyId, req.endpointId(), bizType);
        JsonNode payload = req.payload();
        if (mapping != null) {
            payload = MappingEngine.apply(
                    MappingEngine.parseRules(readJson(str(mapping.get("rules")))), req.payload());
        }
        String payloadJson = writeJson(payload);

        List<Long> exist = jdbcTemplate.queryForList(
                "SELECT id FROM itg_job WHERE endpoint_id = ? AND biz_type = ? AND biz_key = ?",
                Long.class, req.endpointId(), bizType, bizKey);
        if (!exist.isEmpty()) {
            return new IntegrationDtos.JobSubmitResult(
                    jobDetail(exist.get(0), companyId), true);
        }

        long id = IdWorker.getId();
        boolean dryRun = Boolean.TRUE.equals(req.dryRun());
        int maxAttempt = req.maxAttempt() == null ? 3 : Math.min(10, Math.max(1, req.maxAttempt()));
        try {
            jdbcTemplate.update("""
                            INSERT INTO itg_job (id, company_id, job_no, endpoint_id, mapping_id,
                                                 biz_type, biz_key, direction, status, attempt,
                                                 max_attempt, request_payload, created_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, 'OUT', ?, 0, ?, ?::jsonb, ?)
                            """,
                    id, companyId, nextJobNo(), req.endpointId(),
                    mapping == null ? null : longOf(mapping.get("id")), bizType, bizKey,
                    dryRun ? "PREVIEW" : "PENDING", maxAttempt, payloadJson, userId);
        } catch (DuplicateKeyException ex) {
            // 并发下别人刚插入：按幂等语义返回已存在的那条
            Long existId = jdbcTemplate.queryForObject("""
                    SELECT id FROM itg_job WHERE endpoint_id = ? AND biz_type = ? AND biz_key = ?
                    """, Long.class, req.endpointId(), bizType, bizKey);
            return new IntegrationDtos.JobSubmitResult(jobDetail(existId, companyId), true);
        }
        if (!dryRun) {
            execute(id);
        }
        return new IntegrationDtos.JobSubmitResult(jobDetail(id, companyId), false);
    }

    /** 立即发送：只允许 PENDING / FAILED（成功或死信要走 replay）。 */
    public IntegrationDtos.JobDetail dispatchJob(Long id, Long companyId) {
        Map<String, Object> job = requireJob(id, companyId);
        String status = str(job.get("status"));
        if (!"PENDING".equals(status) && !"FAILED".equals(status)) {
            throw BizException.of(ErrorCode.ITG_JOB_STATE_INVALID,
                    "只有待发送或失败待重试的任务可以发送（当前 " + status + "）");
        }
        execute(id);
        return jobDetail(id, companyId);
    }

    /** 重放：FAILED / DEAD → 归零重试次数并重发（使用原报文快照，不重新取源数据）。 */
    @Transactional
    public IntegrationDtos.JobDetail replayJob(Long id, Long companyId) {
        Map<String, Object> job = requireJob(id, companyId);
        String status = str(job.get("status"));
        if (!"FAILED".equals(status) && !"DEAD".equals(status)) {
            throw BizException.of(ErrorCode.ITG_JOB_STATE_INVALID,
                    "只有失败或死信任务可以重放（当前 " + status + "）");
        }
        jdbcTemplate.update("""
                UPDATE itg_job SET status = 'PENDING', attempt = 0, next_retry_at = null,
                                   error_msg = null, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        execute(id);
        return jobDetail(id, companyId);
    }

    /** 定时重试：取到期的 PENDING / FAILED 任务逐条发送，返回处理条数。 */
    public int retryDue(int limit) {
        List<Long> due = jdbcTemplate.queryForList("""
                SELECT id FROM itg_job
                 WHERE status = 'PENDING'
                    OR (status = 'FAILED' AND next_retry_at IS NOT NULL AND next_retry_at <= now())
                 ORDER BY created_at
                 LIMIT ?
                """, Long.class, limit);
        int done = 0;
        for (Long id : due) {
            try {
                execute(id);
                done++;
            } catch (RuntimeException ex) {
                log.warn("集成任务重试失败：id={} 原因={}", id, ex.getMessage());
            }
        }
        return done;
    }

    /**
     * 回收卡在 RUNNING 的任务：进程重启或线程被中断时任务会停在 RUNNING，
     * 不回收就永远等不到重试（attempt 已计数，所以按剩余次数决定回 FAILED 还是 DEAD）。
     */
    public int reclaimStaleRunning(int minutes) {
        return jdbcTemplate.update("""
                UPDATE itg_job
                   SET status = CASE WHEN attempt >= max_attempt THEN 'DEAD' ELSE 'FAILED' END,
                       error_msg = '发送中断（进程重启或超时）',
                       next_retry_at = now(), updated_at = now()
                 WHERE status = 'RUNNING' AND started_at < now() - make_interval(mins => ?)
                """, Math.max(1, minutes));
    }

    /** 一次发送：抢占 → 发送 → 写日志 → 落状态（失败按退避排重试或进死信）。 */
    private void execute(Long id) {
        int claimed = jdbcTemplate.update("""
                UPDATE itg_job
                   SET status = 'RUNNING', attempt = attempt + 1, started_at = now(),
                       next_retry_at = null, updated_at = now()
                 WHERE id = ? AND status IN ('PENDING', 'FAILED')
                """, id);
        if (claimed == 0) {
            return;
        }
        Map<String, Object> job = jdbcTemplate.queryForMap("""
                SELECT j.*, e.code AS endpoint_code, e.base_url, e.auth_type, e.auth_config,
                       e.headers, e.timeout_ms, e.enabled AS endpoint_enabled, e.deleted AS ep_deleted
                  FROM itg_job j LEFT JOIN itg_endpoint e ON e.id = j.endpoint_id
                 WHERE j.id = ?
                """, id);
        String payload = str(job.get("request_payload"));
        int attempt = intOf(job.get("attempt"));
        int maxAttempt = Math.max(1, intOf(job.get("max_attempt")));

        ItgHttpSender.SendResult result;
        if (job.get("base_url") == null || Boolean.TRUE.equals(job.get("ep_deleted"))) {
            result = new ItgHttpSender.SendResult(0, null, "端点不存在或已删除", 0);
        } else {
            result = sender.send(new ItgHttpSender.EndpointRef(
                    str(job.get("endpoint_code")), str(job.get("base_url")),
                    str(job.get("auth_type")), readMap(str(job.get("auth_config"))),
                    readMap(str(job.get("headers"))), intOf(job.get("timeout_ms"))), payload);
        }

        writeLog(longOf(job.get("company_id")), id, attempt, result, payload);
        if (result.success()) {
            jdbcTemplate.update("""
                    UPDATE itg_job
                       SET status = 'SUCCESS', http_status = ?, response_body = ?, error_msg = null,
                           duration_ms = ?, finished_at = now(), updated_at = now()
                     WHERE id = ?
                    """, result.httpStatus(), truncate(result.body(), MAX_BODY),
                    (int) result.durationMs(), id);
            return;
        }
        boolean dead = attempt >= maxAttempt;
        String error = result.error() == null
                ? "对端返回 HTTP " + result.httpStatus() : result.error();
        jdbcTemplate.update("""
                        UPDATE itg_job
                           SET status = ?, http_status = ?, response_body = ?, error_msg = ?,
                               duration_ms = ?, finished_at = now(), updated_at = now(),
                               next_retry_at = CASE WHEN ? THEN NULL ELSE
                                   now() + make_interval(secs => ?) END
                         WHERE id = ?
                        """,
                dead ? "DEAD" : "FAILED", result.httpStatus(),
                truncate(result.body(), MAX_BODY), truncate(error, MAX_ERROR),
                (int) result.durationMs(), dead, backoffSeconds(attempt), id);
        log.info("集成任务发送失败：id={} attempt={}/{} status={} 原因={}", id, attempt, maxAttempt,
                dead ? "DEAD" : "FAILED", error);
    }

    private void writeLog(Long companyId, Long jobId, int attempt,
                          ItgHttpSender.SendResult result, String payload) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO itg_log (id, company_id, job_id, attempt, status,
                                                 http_status, duration_ms, request_snippet,
                                                 response_snippet, error_msg)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, jobId, attempt,
                    result.success() ? "SUCCESS" : "FAILED",
                    result.httpStatus() == 0 ? null : result.httpStatus(),
                    (int) result.durationMs(), truncate(payload, MAX_SNIPPET),
                    truncate(result.body(), MAX_SNIPPET), truncate(result.error(), MAX_ERROR));
        } catch (RuntimeException ex) {
            log.warn("集成日志写入失败：job={} 原因={}", jobId, ex.getMessage());
        }
    }

    private int backoffSeconds(int attempt) {
        long seconds = (long) retryBaseSeconds * (1L << Math.min(6, Math.max(0, attempt - 1)));
        return (int) Math.min(3600L, seconds);
    }

    /* ================================================================ 内部 */

    private Map<String, Object> requireEndpoint(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM itg_endpoint WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_ENDPOINT_NOT_FOUND, "集成端点不存在：" + id);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireMapping(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM itg_mapping WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_MAPPING_NOT_FOUND, "字段映射不存在：" + id);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireJob(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM itg_job WHERE id = ? AND company_id = ?
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_JOB_NOT_FOUND, "集成任务不存在：" + id);
        }
        return rows.get(0);
    }

    /** 找该端点下启用的映射；没有映射时按原样报文发送（有些接口不需要字段转换）。 */
    private Map<String, Object> findMapping(Long companyId, Long endpointId, String objectCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM itg_mapping
                 WHERE company_id = ? AND endpoint_id = ? AND object_code = ?
                   AND direction = 'OUT' AND enabled = true AND deleted = false
                """, companyId, endpointId, objectCode);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Long byCode(Long companyId, String code) {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM itg_endpoint WHERE company_id = ? AND code = ? AND deleted = false
                """, Long.class, companyId, code);
    }

    private Long byMappingKey(Long companyId, Long endpointId, String objectCode,
                              String direction) {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM itg_mapping
                 WHERE company_id = ? AND endpoint_id = ? AND object_code = ? AND direction = ?
                   AND deleted = false
                """, Long.class, companyId, endpointId, objectCode, direction);
    }

    /** 取号统一走 code_rule（台账 B19）。 */
    private String nextJobNo() {
        return codeRuleService.next("ItgJob");
    }

    private String rulesJson(List<IntegrationDtos.MappingRuleReq> rules) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (IntegrationDtos.MappingRuleReq rule : rules) {
            normalized.add(Map.of(
                    "source", rule.source().trim(),
                    "target", rule.target().trim(),
                    "transform", rule.transform() == null ? "" : rule.transform().trim(),
                    "required", Boolean.TRUE.equals(rule.required())));
        }
        // 交给引擎校验一遍，避免存进库的规则在发送时才发现不合法
        MappingEngine.parseRules(objectMapper.valueToTree(normalized));
        return writeJson(normalized);
    }

    private List<IntegrationDtos.MappingRuleReq> rulesOf(String rulesJson) {
        JsonNode node = readJson(rulesJson);
        List<IntegrationDtos.MappingRuleReq> rules = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                rules.add(new IntegrationDtos.MappingRuleReq(
                        item.path("source").asText(""), item.path("target").asText(""),
                        item.path("transform").asText(""), item.path("required").asBoolean(false)));
            }
        }
        return rules;
    }

    private String normalizeUrl(String baseUrl) {
        ItgHttpSender.parseUri(baseUrl);
        return baseUrl.trim();
    }

    private static String authType(String value) {
        String type = value == null || value.isBlank() ? "NONE"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!AUTH_TYPES.contains(type)) {
            throw BizException.of(ErrorCode.ITG_ENDPOINT_INVALID,
                    "未知认证方式：" + value + "（可选 NONE/BASIC/BEARER/HEADER）");
        }
        return type;
    }

    private static String systemCode(String value) {
        String code = value == null || value.isBlank() ? "OTHER"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!SYSTEM_CODES.contains(code)) {
            throw BizException.of(ErrorCode.ITG_ENDPOINT_INVALID,
                    "未知目标系统：" + value + "（可选 KINGDEE/WMS/MES/OTHER）");
        }
        return code;
    }

    private static String direction(String value) {
        String dir = value == null || value.isBlank() ? "OUT"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!"OUT".equals(dir) && !"IN".equals(dir)) {
            throw BizException.of(ErrorCode.ITG_MAPPING_INVALID, "未知方向：" + value);
        }
        return dir;
    }

    private static int timeout(Integer value) {
        int timeout = value == null ? 10000 : value;
        if (timeout < 1000 || timeout > 120000) {
            throw BizException.of(ErrorCode.ITG_ENDPOINT_INVALID, "超时需在 1000~120000 毫秒之间");
        }
        return timeout;
    }

    private static String payloadVersion(String value) {
        return value == null || value.isBlank() ? "1.0" : value.trim();
    }

    private static String normalize(String value, Set<String> allowed, String label) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "未知" + label + "：" + value + "（可选 " + String.join("/", allowed) + "）");
        }
        return normalized;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw BizException.of(ErrorCode.ITG_ENDPOINT_INVALID, "JSON 序列化失败：" + ex.getMessage());
        }
    }

    private JsonNode readJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException ex) {
            return objectMapper.createObjectNode();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, Map.class);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /** 报文可能很大，库里只留截断快照（完整报文应当在对端日志里查）。 */
    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max) + "...(truncated)";
    }

    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 供 P9-2 适配器复用：把对象映射成目标报文。 */
    public ObjectNode applyMapping(Long mappingId, Long companyId, JsonNode source) {
        Map<String, Object> mapping = requireMapping(mappingId, companyId);
        return MappingEngine.apply(
                MappingEngine.parseRules(readJson(str(mapping.get("rules")))), source);
    }
}
