package com.nexplm.platform.doc.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 检出/检入的软锁管理。
 * <p>锁表 {@code plm_checkout} 对 {@code object_id} 唯一；带到期时间（默认 8 小时），
 * 到期后在下次访问时惰性释放，无需后台任务即可保证不会永久锁死。</p>
 */
@Service
public class CheckoutService {

    public record Lock(Long objectId, Long userId, OffsetDateTime checkedOutAt, OffsetDateTime expireAt) {

        public boolean expired() {
            return expireAt != null && expireAt.isBefore(OffsetDateTime.now());
        }
    }

    private final JdbcTemplate jdbcTemplate;
    private final int expireHours;

    public CheckoutService(JdbcTemplate jdbcTemplate,
                           @Value("${nexplm.doc.checkout-hours:8}") int expireHours) {
        this.jdbcTemplate = jdbcTemplate;
        this.expireHours = expireHours <= 0 ? 8 : expireHours;
    }

    /** 当前有效锁；若已过期则就地清理并返回空。 */
    public Optional<Lock> active(Long objectId) {
        List<Lock> locks = jdbcTemplate.query("""
                        SELECT object_id, user_id, checked_out_at, expire_at FROM plm_checkout
                         WHERE object_id = ?
                        """,
                (rs, rowNum) -> new Lock(rs.getLong("object_id"), rs.getLong("user_id"),
                        rs.getObject("checked_out_at", OffsetDateTime.class),
                        rs.getObject("expire_at", OffsetDateTime.class)),
                objectId);
        if (locks.isEmpty()) {
            return Optional.empty();
        }
        Lock lock = locks.get(0);
        if (lock.expired()) {
            jdbcTemplate.update("DELETE FROM plm_checkout WHERE object_id = ? AND user_id = ?",
                    objectId, lock.userId());
            return Optional.empty();
        }
        return Optional.of(lock);
    }

    /**
     * 获取锁。同一用户重复检出视为续期；他人持有则冲突。
     */
    @Transactional
    public Lock acquire(Long objectId, Long userId, String clientIp, String machine, String remark) {
        Optional<Lock> current = active(objectId);
        OffsetDateTime now = OffsetDateTime.now();
        if (current.isPresent()) {
            Lock lock = current.get();
            if (!lock.userId().equals(userId)) {
                throw BizException.of(ErrorCode.OBJ_CHECKOUT_CONFLICT,
                        "该对象已被他人检出（用户 " + lock.userId() + "，到期时间 " + lock.expireAt() + "）");
            }
            OffsetDateTime expire = now.plusHours(expireHours);
            jdbcTemplate.update("UPDATE plm_checkout SET expire_at = ? WHERE object_id = ?", expire, objectId);
            return new Lock(objectId, userId, lock.checkedOutAt(), expire);
        }
        OffsetDateTime expire = now.plusHours(expireHours);
        try {
            jdbcTemplate.update("""
                            INSERT INTO plm_checkout
                              (id, object_id, user_id, checked_out_at, expire_at, client_ip, machine, remark)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), objectId, userId, now, expire,
                    truncate(clientIp, 64), truncate(machine, 200), truncate(remark, 500));
        } catch (DuplicateKeyException ex) {
            // 并发抢占：唯一索引兜底
            throw BizException.of(ErrorCode.OBJ_CHECKOUT_CONFLICT);
        }
        return new Lock(objectId, userId, now, expire);
    }

    /** 释放自己持有的锁；未持有时幂等成功。 */
    @Transactional
    public void release(Long objectId, Long userId) {
        int rows = jdbcTemplate.update(
                "DELETE FROM plm_checkout WHERE object_id = ? AND user_id = ?", objectId, userId);
        if (rows == 0) {
            Optional<Lock> holder = active(objectId);
            if (holder.isPresent()) {
                throw BizException.of(ErrorCode.COMMON_FORBIDDEN,
                        "该对象的检出锁不属于当前用户（持有者：" + holder.get().userId() + "）");
            }
        }
    }

    /** 管理员强制解锁（调用方负责权限与审计）。 */
    public Long forceRelease(Long objectId) {
        List<Long> holders = jdbcTemplate.queryForList(
                "SELECT user_id FROM plm_checkout WHERE object_id = ?", Long.class, objectId);
        jdbcTemplate.update("DELETE FROM plm_checkout WHERE object_id = ?", objectId);
        return holders.isEmpty() ? null : holders.get(0);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
