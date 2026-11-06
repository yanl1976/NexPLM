package com.nexplm.platform.change.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.change.api.ChgDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 变更执行侧（P5）：变更通知 ECN、变更问题 Problem、受影响对象批量执行。
 *
 * <p>执行语义：</p>
 * <ul>
 *   <li>{@code REVISE}：复制一行新版本对象（{@code revision+1, iteration=1, is_latest=true}），
 *       旧行 {@code is_latest=false}，并同步复制 {@code item_master}/{@code doc_master} 子表行；</li>
 *   <li>{@code OBSOLETE}：对象置 {@code Obsolete} 并写 {@code obsolete_at}；</li>
 *   <li>{@code RELEASE}：对象置 {@code Released} 并写 {@code released_at}。</li>
 * </ul>
 * <p>执行过程不吞异常：任一对象执行失败整批回滚，避免出现「一半升级、一半没升」的脏状态。</p>
 */
@Service
public class ChgExecService {

    private final JdbcTemplate jdbcTemplate;
    private final ChgStateMachine stateMachine;

    public ChgExecService(JdbcTemplate jdbcTemplate, ChgStateMachine stateMachine) {
        this.jdbcTemplate = jdbcTemplate;
        this.stateMachine = stateMachine;
    }

    // ------------------------------------------------------------------ ECN

    @Transactional
    public ChgDtos.EcnRow createEcn(Long ecrId, ChgDtos.EcnSaveReq req, Long companyId, Long userId) {
        String ecrStatus = ecrStatus(ecrId, companyId);
        // 状态机允许 notice 迁移的状态才可下发（APPROVED→NOTICED；已通知/执行中为自迁移）
        String notifiedStatus = stateMachine.require(ChgStateMachine.ECR, ecrStatus, "notice",
                "只有已批准的变更才能下发通知");
        long id = IdWorker.getId();
        String code = nextCode("ChgEcn", "ECN");
        jdbcTemplate.update("""
                        INSERT INTO chg_ecn (id, company_id, code, ecr_id, title, status, executor_id,
                                             plan_date, remark, created_by)
                        VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?)
                        """,
                id, companyId, code, ecrId, req.title().trim(), req.executorId(), req.planDate(),
                req.remark() == null || req.remark().isBlank() ? null : req.remark().trim(), userId);
        if (!notifiedStatus.equals(ecrStatus)) {
            jdbcTemplate.update("""
                    UPDATE chg_ecr SET status = ?, updated_by = ?, updated_at = now()
                     WHERE id = ? AND company_id = ?
                    """, notifiedStatus, userId, ecrId, companyId);
        }
        return ecnRow(id, companyId);
    }

    @Transactional
    public ChgDtos.EcnRow issueEcn(Long ecnId, Long companyId, Long userId) {
        ChgDtos.EcnRow ecn = ecnRow(ecnId, companyId);
        String target = stateMachine.require(ChgStateMachine.ECN, ecn.status(), "issue",
                "只有草稿通知可以下发");
        jdbcTemplate.update("""
                UPDATE chg_ecn SET status = ?, issued_at = now(), updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, target, ecnId, companyId);
        return ecnRow(ecnId, companyId);
    }

    @Transactional
    public ChgDtos.EcnRow closeEcn(Long ecnId, Long companyId, Long userId) {
        ChgDtos.EcnRow ecn = ecnRow(ecnId, companyId);
        String target = stateMachine.require(ChgStateMachine.ECN, ecn.status(), "close",
                "当前通知状态不允许关闭");
        Integer open = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM chg_problem WHERE ecn_id = ? AND deleted = false AND status <> 'CLOSED'",
                Integer.class, ecnId);
        if (open != null && open > 0) {
            throw BizException.of(ErrorCode.CHG_STATE_INVALID, "还有 " + open + " 个未关闭的变更问题");
        }
        jdbcTemplate.update("""
                UPDATE chg_ecn SET status = ?, closed_at = now(), updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, target, ecnId, companyId);
        return ecnRow(ecnId, companyId);
    }

    private ChgDtos.EcnRow ecnRow(Long ecnId, Long companyId) {
        List<ChgDtos.EcnRow> rows = jdbcTemplate.query("""
                        SELECT n.id, n.code, n.ecr_id, n.title, n.status, n.executor_id,
                               u.display_name AS executor_name, n.plan_date, n.issued_at, n.closed_at
                          FROM chg_ecn n
                          LEFT JOIN sys_user u ON u.id = n.executor_id
                         WHERE n.id = ? AND n.deleted = false AND n.company_id = ?
                        """,
                (rs, rowNum) -> new ChgDtos.EcnRow(rs.getLong("id"), rs.getString("code"),
                        rs.getLong("ecr_id"), rs.getString("title"), rs.getString("status"),
                        (Long) rs.getObject("executor_id"), rs.getString("executor_name"),
                        rs.getObject("plan_date", LocalDate.class),
                        rs.getObject("issued_at", OffsetDateTime.class),
                        rs.getObject("closed_at", OffsetDateTime.class)),
                ecnId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "变更通知不存在：" + ecnId);
        }
        return rows.get(0);
    }

    // ------------------------------------------------------------------ Problem

    public List<ChgDtos.ProblemRow> problems(Long ecnId, Long companyId) {
        return jdbcTemplate.query("""
                        SELECT p.id, p.code, p.ecn_id, p.description, p.status, p.owner_id,
                               u.display_name AS owner_name, p.due_date, p.closed_at, p.created_at
                          FROM chg_problem p
                          LEFT JOIN sys_user u ON u.id = p.owner_id
                         WHERE p.ecn_id = ? AND p.deleted = false AND p.company_id = ?
                         ORDER BY p.created_at
                        """,
                (rs, rowNum) -> new ChgDtos.ProblemRow(rs.getLong("id"), rs.getString("code"),
                        rs.getLong("ecn_id"), rs.getString("description"), rs.getString("status"),
                        (Long) rs.getObject("owner_id"), rs.getString("owner_name"),
                        rs.getObject("due_date", LocalDate.class),
                        rs.getObject("closed_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class)),
                ecnId, companyId);
    }

    @Transactional
    public ChgDtos.ProblemRow addProblem(Long ecnId, ChgDtos.ProblemReq req, Long companyId, Long userId) {
        ChgDtos.EcnRow ecn = ecnRow(ecnId, companyId);
        if ("CLOSED".equals(ecn.status())) {
            throw BizException.of(ErrorCode.CHG_STATE_INVALID, "已关闭的变更通知不能再登记问题");
        }
        long id = IdWorker.getId();
        String code = nextCode("ChgProblem", "PBM");
        jdbcTemplate.update("""
                        INSERT INTO chg_problem (id, company_id, code, ecn_id, description, status,
                                                 owner_id, due_date, created_by)
                        VALUES (?, ?, ?, ?, ?, 'OPEN', ?, ?, ?)
                        """,
                id, companyId, code, ecnId, req.description().trim(), req.ownerId(), req.dueDate(), userId);
        return problems(ecnId, companyId).stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.COMMON_INTERNAL, "变更问题写入失败"));
    }

    @Transactional
    public void closeProblem(Long problemId, Long companyId, Long userId) {
        List<String> states = jdbcTemplate.query("""
                        SELECT status FROM chg_problem
                         WHERE id = ? AND company_id = ? AND deleted = false
                        """,
                (rs, rowNum) -> rs.getString(1), problemId, companyId);
        // 不存在与「已关闭」对外保持同一提示（与改造前行为一致）
        if (states.isEmpty() || !stateMachine.can(ChgStateMachine.PROBLEM, states.get(0), "close")) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "变更问题不存在或已关闭：" + problemId);
        }
        String target = stateMachine.target(ChgStateMachine.PROBLEM, states.get(0), "close");
        jdbcTemplate.update("""
                UPDATE chg_problem SET status = ?, closed_at = now(), updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, target, problemId, companyId);
    }

    // ------------------------------------------------------------------ 执行（升版 / 废弃 / 发布）

    @Transactional
    public ChgDtos.ExecuteResult execute(Long ecrId, Long companyId, Long userId) {
        String ecrStatus = ecrStatus(ecrId, companyId);
        String target = stateMachine.require(ChgStateMachine.ECR, ecrStatus, "execute",
                "只有已批准的变更才能执行");
        List<Map<String, Object>> pending = jdbcTemplate.queryForList("""
                SELECT id, object_type, object_id, action
                  FROM chg_object
                 WHERE ecr_id = ? AND company_id = ? AND relation = 'AFFECTED' AND exec_status = 'PENDING'
                 ORDER BY id
                """, ecrId, companyId);
        int succeeded = 0;
        int skipped = 0;
        List<String> messages = new ArrayList<>();
        for (Map<String, Object> row : pending) {
            long linkId = ((Number) row.get("id")).longValue();
            String objectType = (String) row.get("object_type");
            long objectId = ((Number) row.get("object_id")).longValue();
            String action = (String) row.get("action");
            if (action == null) {
                skipped++;
                messages.add("跳过对象 " + objectId + "：未指定操作意图");
                continue;
            }
            Integer newRevision = null;
            switch (action) {
                case "REVISE" -> newRevision = revise(objectId, objectType, userId);
                case "OBSOLETE" -> markState(objectId, "Obsolete", "obsolete_at", userId);
                case "RELEASE" -> markState(objectId, "Released", "released_at", userId);
                default -> {
                    skipped++;
                    messages.add("跳过对象 " + objectId + "：未知操作意图 " + action);
                    continue;
                }
            }
            jdbcTemplate.update("""
                    UPDATE chg_object SET exec_status = 'DONE', revision_to = ?, updated_at = now()
                     WHERE id = ?
                    """, newRevision, linkId);
            succeeded++;
        }
        if (succeeded > 0 && !target.equals(ecrStatus)) {
            jdbcTemplate.update("""
                    UPDATE chg_ecr SET status = ?, updated_by = ?, updated_at = now()
                     WHERE id = ? AND company_id = ? AND status = ?
                    """, target, userId, ecrId, companyId, ecrStatus);
        }
        return new ChgDtos.ExecuteResult(pending.size(), succeeded, skipped, messages);
    }

    /** 升版：新增 revision+1 的对象行并同步子表，旧行降为历史版本。 */
    private Integer revise(long objectId, String objectType, Long userId) {
        Map<String, Object> source = jdbcTemplate.queryForMap("""
                SELECT object_type, master_id, code, name, company_id, org_id, owner_id, owner_group_id,
                       security_level, phase_state, revision, ext_json::text AS ext_text
                  FROM plm_object
                 WHERE id = ? AND deleted = false
                """, objectId);
        long newId = IdWorker.getId();
        int newRevision = ((Number) source.get("revision")).intValue() + 1;
        jdbcTemplate.update("""
                        INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, org_id,
                                                owner_id, owner_group_id, security_level, lifecycle_state,
                                                phase_state, workflow_state, revision, iteration, is_latest,
                                                ext_json, version, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Draft', ?, 'NONE', ?, 1, true,
                                CAST(? AS jsonb), 0, ?, ?)
                        """,
                newId, source.get("object_type"), source.get("master_id"), source.get("code"),
                source.get("name"), source.get("company_id"), source.get("org_id"), source.get("owner_id"),
                source.get("owner_group_id"), source.get("security_level"), source.get("phase_state"),
                newRevision, source.get("ext_text"), userId, userId);
        jdbcTemplate.update("""
                UPDATE plm_object SET is_latest = false, updated_by = ?, updated_at = now() WHERE id = ?
                """, userId, objectId);
        copyChild(objectType, objectId, newId);
        return newRevision;
    }

    private void copyChild(String objectType, long fromId, long toId) {
        switch (objectType) {
            case "PdmItem" -> jdbcTemplate.update("""
                    INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                                             source_type, std_code, remark)
                    SELECT ?, item_type, specification, material, unit, weight, source_type, std_code, remark
                      FROM item_master WHERE object_id = ?
                    """, toId, fromId);
            case "PdmDoc" -> jdbcTemplate.update("""
                    INSERT INTO doc_master (object_id, doc_type, vault_id, file_ext, file_count, page_count,
                                            source, is_derived, main_file_id)
                    SELECT ?, doc_type, vault_id, file_ext, file_count, page_count, source, is_derived, main_file_id
                      FROM doc_master WHERE object_id = ?
                    """, toId, fromId);
            default -> {
                // 其它对象类型目前只有通用层，不用复制子表
            }
        }
    }

    private void markState(long objectId, String state, String timeColumn, Long userId) {
        jdbcTemplate.update("""
                UPDATE plm_object
                   SET lifecycle_state = ?, %s = now(), updated_by = ?, updated_at = now(),
                       version = version + 1
                 WHERE id = ? AND deleted = false
                """.formatted(timeColumn), state, userId, objectId);
    }

    // ------------------------------------------------------------------ 影响分析

    /** 向下展开（父 → 子件）：从根对象查到其组成件。 */
    private static final String DOWN_SQL = """
            WITH RECURSIVE walk AS (
                SELECT b.child_object_id AS id, 1 AS depth,
                       ARRAY[b.parent_object_id, b.child_object_id] AS path
                  FROM bom_line b
                 WHERE b.parent_object_id = ? AND b.deleted = false AND b.view_type = ?
                UNION ALL
                SELECT b.child_object_id, w.depth + 1, w.path || b.child_object_id
                  FROM bom_line b
                  JOIN walk w ON b.parent_object_id = w.id
                 WHERE b.deleted = false AND b.view_type = ? AND w.depth < ?
                   AND NOT b.child_object_id = ANY (w.path)
            )
            SELECT id, MIN(depth) AS depth FROM walk GROUP BY id
            """;

    /** 向上展开（子件 → 父件）：反查被哪些上层装配使用（where-used）。 */
    private static final String UP_SQL = """
            WITH RECURSIVE walk AS (
                SELECT b.parent_object_id AS id, 1 AS depth,
                       ARRAY[b.child_object_id, b.parent_object_id] AS path
                  FROM bom_line b
                 WHERE b.child_object_id = ? AND b.deleted = false AND b.view_type = ?
                UNION ALL
                SELECT b.parent_object_id, w.depth + 1, w.path || b.parent_object_id
                  FROM bom_line b
                  JOIN walk w ON b.child_object_id = w.id
                 WHERE b.deleted = false AND b.view_type = ? AND w.depth < ?
                   AND NOT b.parent_object_id = ANY (w.path)
            )
            SELECT id, MIN(depth) AS depth FROM walk GROUP BY id
            """;

    /**
     * 影响分析：从 ECR 的「受影响」对象出发，按 BOM 向上/向下展开指定层数。
     *
     * <p>用 PG 递归 CTE 实现，path 数组负责防环（自引用或环形 BOM 不会死循环）。
     * 同一对象若既被向下也被向上命中，保留首次写入的方向（先 DOWN 后 UP）。</p>
     */
    public List<ChgDtos.ImpactRow> impact(Long ecrId, Long companyId, int depth, String viewType) {
        String view = viewType == null || viewType.isBlank() ? "DESIGN" : viewType.trim();
        int maxDepth = Math.min(Math.max(depth, 1), 10);
        List<Long> roots = jdbcTemplate.query("""
                        SELECT object_id FROM chg_object
                         WHERE ecr_id = ? AND company_id = ? AND relation = 'AFFECTED'
                        """,
                (rs, rowNum) -> rs.getLong(1), ecrId, companyId);
        if (roots.isEmpty()) {
            return List.of();
        }
        List<Long> linkedIds = jdbcTemplate.query(
                "SELECT object_id FROM chg_object WHERE ecr_id = ?",
                (rs, rowNum) -> rs.getLong(1), ecrId);
        Map<String, Integer> depths = new java.util.LinkedHashMap<>();
        Map<Long, String> directions = new java.util.HashMap<>();
        for (Long root : roots) {
            collect(DOWN_SQL, root, view, maxDepth, "DOWN", depths, directions);
            collect(UP_SQL, root, view, maxDepth, "UP", depths, directions);
        }
        if (depths.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>(directions.keySet());
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        List<ChgDtos.ImpactRow> rows = new ArrayList<>();
        jdbcTemplate.query("SELECT id, object_type, code, name, lifecycle_state, revision, iteration "
                        + "FROM plm_object WHERE id IN (" + placeholders + ") AND deleted = false",
                rs -> {
                    long id = rs.getLong("id");
                    String direction = directions.get(id);
                    Integer level = depths.get(direction + "|" + id);
                    if (direction == null || level == null) {
                        return;
                    }
                    rows.add(new ChgDtos.ImpactRow(direction, level, rs.getString("object_type"), id,
                            rs.getString("code"), rs.getString("name"), rs.getString("lifecycle_state"),
                            rs.getInt("revision"), rs.getInt("iteration"), linkedIds.contains(id)));
                }, ids.toArray());
        rows.sort(java.util.Comparator
                .comparing(ChgDtos.ImpactRow::direction)
                .thenComparingInt(ChgDtos.ImpactRow::depth)
                .thenComparing(ChgDtos.ImpactRow::code));
        return rows;
    }

    private void collect(String sql, Long root, String view, int maxDepth, String direction,
                         Map<String, Integer> depths, Map<Long, String> directions) {
        jdbcTemplate.query(sql, rs -> {
            long id = rs.getLong("id");
            if (id == root) {
                return;
            }
            int level = rs.getInt("depth");
            String key = direction + "|" + id;
            Integer existing = depths.get(key);
            if (existing == null || level < existing) {
                depths.put(key, level);
                directions.putIfAbsent(id, direction);
            }
        }, root, view, view, maxDepth);
    }

    private String ecrStatus(Long ecrId, Long companyId) {
        List<String> status = jdbcTemplate.query(
                "SELECT status FROM chg_ecr WHERE id = ? AND company_id = ? AND deleted = false",
                (rs, rowNum) -> rs.getString(1), ecrId, companyId);
        if (status.isEmpty()) {
            throw BizException.of(ErrorCode.CHG_ECR_NOT_FOUND, "变更申请不存在：" + ecrId);
        }
        return status.get(0);
    }

    private String nextCode(String ruleCode, String prefix) {
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, ?, 'DEFAULT', 1)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = code_sequence.current_no + 1, updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId(), ruleCode);
        return String.format("%s-%06d", prefix, no == null ? 1L : no);
    }
}
