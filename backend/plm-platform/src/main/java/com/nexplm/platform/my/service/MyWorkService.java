package com.nexplm.platform.my.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.my.api.MyDtos;
import com.nexplm.platform.doc.service.CheckoutService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 个人工作区服务：我的检出 / 收藏夹 / 最近访问 / 站内消息 / 高级查询。
 *
 * <p>数据范围一律以 {@code user_id} 自身限定（不看 ACL）——这些是「我的」数据，
 * 旧系统同样如此（{@code MyCheckOut} / {@code Favorite} / {@code AccessRecord} 都按当前用户过滤）。</p>
 */
@Service
public class MyWorkService {

    /** 对象类型中文名：只覆盖会出现在个人工作区的类型，其余原样返回。 */
    private static final Map<String, String> TYPE_LABELS = Map.of(
            "PdmDoc", "图文档",
            "PdmItem", "零部件",
            "ChgEcr", "变更申请",
            "ChgEcn", "变更通知",
            "PrjProject", "项目",
            "CappRouting", "工艺路线",
            "CappCard", "工艺卡片");

    private final JdbcTemplate jdbcTemplate;
    private final CheckoutService checkoutService;

    public MyWorkService(JdbcTemplate jdbcTemplate, CheckoutService checkoutService) {
        this.jdbcTemplate = jdbcTemplate;
        this.checkoutService = checkoutService;
    }

    // ------------------------------------------------------------ 我的检出

    public List<MyDtos.CheckoutRow> listCheckouts(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        return jdbcTemplate.query("""
                SELECT c.object_id, c.checked_out_at, c.expire_at,
                       o.object_type, o.code, o.name, o.revision
                  FROM plm_checkout c
                  LEFT JOIN plm_object o ON o.id = c.object_id AND o.deleted = false
                 WHERE c.user_id = ?
                 ORDER BY c.checked_out_at DESC
                """, (rs, i) -> {
            OffsetDateTime expire = time(rs, "expire_at");
            String type = rs.getString("object_type");
            return new MyDtos.CheckoutRow(type == null ? "-" : type, label(type),
                    rs.getLong("object_id"), rs.getString("code"), rs.getString("name"),
                    rs.getString("revision"), time(rs, "checked_out_at"), expire,
                    expire != null && expire.isBefore(now));
        }, userId);
    }

    /** 撤销检出：只能撤自己的锁（他人锁需走对象的强制解锁，留审计）。 */
    public void cancelCheckout(Long objectId, Long userId) {
        checkoutService.active(objectId).ifPresent(lock -> {
            if (!lock.userId().equals(userId)) {
                throw BizException.of(ErrorCode.COMMON_FORBIDDEN, "该对象由他人检出，不能由你撤销");
            }
        });
        checkoutService.release(objectId, userId);
    }

    // ------------------------------------------------------------ 收藏夹

    public List<MyDtos.FavoriteRow> listFavorites(Long userId, String objectType) {
        StringBuilder sql = new StringBuilder("""
                SELECT f.id, f.object_type, f.object_id, f.folder, f.seq_no, f.remark, f.created_at,
                       o.code, o.name, o.lifecycle_state
                  FROM plm_favorite f
                  LEFT JOIN plm_object o ON o.id = f.object_id AND o.deleted = false
                 WHERE f.user_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (objectType != null && !objectType.isBlank()) {
            sql.append(" AND f.object_type = ?");
            args.add(objectType);
        }
        sql.append(" ORDER BY COALESCE(f.folder, ''), f.seq_no, f.created_at DESC");
        return jdbcTemplate.query(sql.toString(), (rs, i) -> new MyDtos.FavoriteRow(
                rs.getLong("id"), rs.getString("object_type"), label(rs.getString("object_type")),
                rs.getLong("object_id"), rs.getString("code"), rs.getString("name"),
                rs.getString("lifecycle_state"), rs.getString("folder"), rs.getInt("seq_no"),
                rs.getString("remark"), time(rs, "created_at")), args.toArray());
    }

    /** 收藏：同一对象重复收藏按幂等成功处理（不报 409，前端不必区分）。 */
    public Long addFavorite(Long userId, Long companyId, MyDtos.FavoriteReq req) {
        requireObject(req.objectType(), req.objectId());
        List<Long> exist = jdbcTemplate.queryForList(
                "SELECT id FROM plm_favorite WHERE user_id = ? AND object_type = ? AND object_id = ?",
                Long.class, userId, req.objectType(), req.objectId());
        if (!exist.isEmpty()) {
            return exist.get(0);
        }
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                INSERT INTO plm_favorite (id, company_id, user_id, object_type, object_id, folder, remark)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, companyId, userId, req.objectType(), req.objectId(),
                blankToNull(req.folder()), blankToNull(req.remark()));
        return id;
    }

    public void removeFavorite(Long userId, Long id) {
        int deleted = jdbcTemplate.update("DELETE FROM plm_favorite WHERE id = ? AND user_id = ?", id, userId);
        if (deleted == 0) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "收藏不存在");
        }
    }

    // ------------------------------------------------------------ 最近访问

    public List<MyDtos.RecentRow> listRecent(Long userId, String objectType, int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        StringBuilder sql = new StringBuilder("""
                SELECT r.object_type, r.object_id, r.access_type, r.accessed_at, o.code, o.name
                  FROM plm_access_record r
                  LEFT JOIN plm_object o ON o.id = r.object_id AND o.deleted = false
                 WHERE r.user_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (objectType != null && !objectType.isBlank()) {
            sql.append(" AND r.object_type = ?");
            args.add(objectType);
        }
        sql.append(" ORDER BY r.accessed_at DESC LIMIT ?");
        args.add(size);
        return jdbcTemplate.query(sql.toString(), (rs, i) -> new MyDtos.RecentRow(
                rs.getString("object_type"), label(rs.getString("object_type")),
                rs.getLong("object_id"), rs.getString("code"), rs.getString("name"),
                rs.getString("access_type"), time(rs, "accessed_at")), args.toArray());
    }

    /**
     * 记一条访问。同一用户对同一对象 30 分钟内重复访问不重复写，
     * 否则列表页滚动一遍就会把"最近访问"刷成流水日志。
     */
    public void recordAccess(Long userId, Long companyId, MyDtos.AccessReq req) {
        String type = req.accessType() == null || req.accessType().isBlank() ? "VIEW" : req.accessType();
        List<Long> dup = jdbcTemplate.queryForList("""
                SELECT id FROM plm_access_record
                 WHERE user_id = ? AND object_type = ? AND object_id = ? AND access_type = ?
                   AND accessed_at > now() - interval '30 minutes'
                 LIMIT 1
                """, Long.class, userId, req.objectType(), req.objectId(), type);
        if (!dup.isEmpty()) {
            jdbcTemplate.update("UPDATE plm_access_record SET accessed_at = now() WHERE id = ?", dup.get(0));
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO plm_access_record (id, company_id, user_id, object_type, object_id, access_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """, IdWorker.getId(), companyId, userId, req.objectType(), req.objectId(), type);
    }

    // ------------------------------------------------------------ 站内消息

    /**
     * 站内收件箱（批次 E-2）：按<b>收件人</b>过滤，已读状态取收件箱行，不再读消息行。
     *
     * <p>修的是旧缺陷：原先只按 company_id 过滤 + 消息行级 read_at，等于全公司共用一个收件箱。</p>
     */
    public List<MyDtos.MessageRow> inbox(Long companyId, Long userId, boolean unreadOnly, int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        String sql = """
                SELECT m.id, m.event_type, m.title, m.content, m.status, m.created_at, b.read_at
                  FROM sys_notify_inbox b
                  JOIN sys_notify_message m ON m.id = b.message_id
                 WHERE b.company_id = ? AND b.user_id = ? AND b.deleted = false
                """ + (unreadOnly ? " AND b.read_at IS NULL" : "")
                + " ORDER BY m.created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, (rs, i) -> new MyDtos.MessageRow(
                rs.getLong("id"), rs.getString("event_type"), rs.getString("title"),
                rs.getString("content"), rs.getString("status"), time(rs, "created_at"),
                time(rs, "read_at")), companyId, userId, size);
    }

    public void markRead(Long id, Long companyId, Long userId) {
        jdbcTemplate.update("""
                UPDATE sys_notify_inbox SET read_at = COALESCE(read_at, now())
                 WHERE message_id = ? AND company_id = ? AND user_id = ?
                """, id, companyId, userId);
    }

    /** 全部已读：只影响本人收件箱。 */
    public int markAllRead(Long companyId, Long userId) {
        return jdbcTemplate.update("""
                UPDATE sys_notify_inbox SET read_at = now()
                 WHERE company_id = ? AND user_id = ? AND read_at IS NULL AND deleted = false
                """, companyId, userId);
    }

    public int unreadCount(Long companyId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sys_notify_inbox
                 WHERE company_id = ? AND user_id = ? AND read_at IS NULL AND deleted = false
                """, Integer.class, companyId, userId);
        return count == null ? 0 : count;
    }

    // ------------------------------------------------------------ 高级查询

    /**
     * 跨对象高级查询：{@code plm_object}（图文档/零部件等）∪ {@code chg_ecr}（变更单不在 plm_object）。
     * 旧系统对应 {@code QueryMachine}；这里先做"关键字 + 类型 + 分页"三条件，
     * 保存查询方案沿用文档模块已有的 {@code doc_saved_query} 形态，后续批次再抽通用表。
     */
    public List<MyDtos.SearchRow> search(Long companyId, String keyword, String objectType, int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        String kw = keyword == null ? "" : keyword.trim();
        String like = "%" + kw.toLowerCase() + "%";
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT object_type, id, code, name, lifecycle_state, revision, updated_at
                  FROM plm_object
                 WHERE deleted = false AND company_id = ?
                """);
        args.add(companyId);
        if (!kw.isEmpty()) {
            sql.append(" AND (lower(code) LIKE ? OR lower(name) LIKE ?)");
            args.add(like);
            args.add(like);
        }
        if (objectType != null && !objectType.isBlank()) {
            sql.append(" AND object_type = ?");
            args.add(objectType);
        }
        sql.append(" ORDER BY updated_at DESC LIMIT ?");
        args.add(size);
        List<MyDtos.SearchRow> rows = new ArrayList<>(jdbcTemplate.query(sql.toString(),
                (rs, i) -> new MyDtos.SearchRow(rs.getString("object_type"),
                        label(rs.getString("object_type")), rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("lifecycle_state"), rs.getString("revision"),
                        time(rs, "updated_at")), args.toArray()));

        boolean includeEcr = kw.isEmpty() || objectType == null || objectType.isBlank()
                || "ChgEcr".equals(objectType);
        if (includeEcr && rows.size() < size) {
            int remain = size - rows.size();
            List<Object> ecrArgs = new ArrayList<>();
            ecrArgs.add(companyId);
            StringBuilder ecrSql = new StringBuilder("""
                    SELECT id, code, title AS name, status, NULL::varchar AS revision, updated_at
                      FROM chg_ecr WHERE deleted = false AND company_id = ?
                    """);
            if (!kw.isEmpty()) {
                ecrSql.append(" AND (lower(code) LIKE ? OR lower(title) LIKE ?)");
                ecrArgs.add(like);
                ecrArgs.add(like);
            }
            ecrSql.append(" ORDER BY updated_at DESC LIMIT ?");
            ecrArgs.add(remain);
            rows.addAll(jdbcTemplate.query(ecrSql.toString(), (rs, i) -> new MyDtos.SearchRow(
                    "ChgEcr", label("ChgEcr"), rs.getLong("id"), rs.getString("code"),
                    rs.getString("name"), rs.getString("status"), rs.getString("revision"),
                    time(rs, "updated_at")), ecrArgs.toArray()));
        }
        return rows;
    }

    /** 类型下拉：取公司内实际存在的对象类型（含 ChgEcr），避免前端写死枚举。 */
    public List<String> objectTypes(Long companyId) {
        Set<String> types = new LinkedHashSet<>(jdbcTemplate.queryForList("""
                SELECT DISTINCT object_type FROM plm_object
                 WHERE deleted = false AND company_id = ? ORDER BY object_type
                """, String.class, companyId));
        types.add("ChgEcr");
        return List.copyOf(types);
    }

    public Map<String, String> typeLabels() {
        return TYPE_LABELS;
    }

    // ------------------------------------------------------------ 内部工具

    private void requireObject(String objectType, Long objectId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM plm_object
                 WHERE id = ? AND object_type = ? AND deleted = false
                """, Long.class, objectId, objectType);
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "对象不存在：" + objectType + "/" + objectId);
        }
    }

    private static String label(String objectType) {
        return objectType == null ? "-" : TYPE_LABELS.getOrDefault(objectType, objectType);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static OffsetDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
    }

}
