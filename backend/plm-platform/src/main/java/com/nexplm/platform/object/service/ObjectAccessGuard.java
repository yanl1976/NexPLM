package com.nexplm.platform.object.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.service.CheckoutService;
import com.nexplm.platform.object.domain.PlmObject;
import com.nexplm.platform.object.mapper.PlmObjectMapper;
import com.nexplm.platform.security.scope.ScopeApplier;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * 对象访问闸门：按主键直取对象时的统一校验（数据范围 + 生命周期锁定 + 检出锁）。
 *
 * <p>列表查询由 {@link ScopeApplier} 注入过滤条件，但 {@code GET /docs/{id}/...}、
 * {@code GET /files/{fileId}/content} 这类按 id 直取的接口不经过 wrapper，必须显式校验，
 * 否则列表里看不到的对象能被拼 id 越权读取或写入。</p>
 *
 * <p>越权与不存在统一按"不存在"返回，不泄露对象是否存在。</p>
 *
 * <p>检出锁（{@link CheckoutService}）的实现位于 doc 包，但它操作的是通用的
 * {@code plm_checkout} 表，图文档与零部件共用同一套软锁，故此处复用。</p>
 */
@Service
public class ObjectAccessGuard {

    private static final Set<String> LOCKED_STATES = Set.of("Released", "Obsolete");

    private final PlmObjectMapper objectMapper;
    private final ScopeApplier scopeApplier;
    private final CheckoutService checkoutService;

    public ObjectAccessGuard(PlmObjectMapper objectMapper, ScopeApplier scopeApplier,
                             CheckoutService checkoutService) {
        this.objectMapper = objectMapper;
        this.scopeApplier = scopeApplier;
        this.checkoutService = checkoutService;
    }

    /** 可见对象；不存在或越权返回 {@code empty}。 */
    public Optional<PlmObject> find(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        PlmObject object = objectMapper.selectById(id);
        if (object == null) {
            return Optional.empty();
        }
        if (!scopeApplier.isVisible(object.getCompanyId(), object.getOrgId(), object.getOwnerId())) {
            return Optional.empty();
        }
        return Optional.of(object);
    }

    /** 可见且对象类型匹配；否则 {@code empty}（用于批量接口跳过不可见项）。 */
    public Optional<PlmObject> findType(Long id, String objectType) {
        return find(id).filter(object -> objectType.equals(object.getObjectType()));
    }

    /** 必须可见：标识为空报参数错误，不存在或越权统一报"对象不存在"。 */
    public PlmObject require(Long id) {
        return requireInternal(id, null, "对象");
    }

    /** 必须可见且类型匹配：类型不符也按"不存在"返回，不泄露对象真实类型。 */
    public PlmObject requireType(Long id, String objectType, String label) {
        return requireInternal(id, objectType, label);
    }

    /**
     * 必须可写：可见 + 未处于已发布/已作废状态 + 未被他人检出。
     * <p>与各领域服务里的 {@code ensureWritable} 同语义，供文件绑定等旁路入口复用，
     * 避免绕过领域服务的写校验。</p>
     */
    public PlmObject requireWritable(Long id, Long userId, String label) {
        PlmObject object = require(id);
        if (LOCKED_STATES.contains(object.getLifecycleState())) {
            throw BizException.of(ErrorCode.OBJ_STATE_TRANSITION_DENIED,
                    label + "处于「" + object.getLifecycleState() + "」状态，不可修改");
        }
        checkoutService.active(id).ifPresent(lock -> {
            if (!lock.userId().equals(userId)) {
                throw BizException.of(ErrorCode.OBJ_CHECKOUT_CONFLICT,
                        label + "已被他人检出（用户 " + lock.userId() + "），当前为只读");
            }
        });
        return object;
    }

    private PlmObject requireInternal(Long id, String objectType, String label) {
        if (id == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, label + "标识不能为空");
        }
        Optional<PlmObject> found = objectType == null ? find(id) : findType(id, objectType);
        return found.orElseThrow(() -> BizException.of(ErrorCode.OBJ_NOT_FOUND, label + "不存在：" + id));
    }
}
