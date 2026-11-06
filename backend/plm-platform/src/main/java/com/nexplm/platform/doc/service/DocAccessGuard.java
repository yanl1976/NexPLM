package com.nexplm.platform.doc.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.object.domain.PlmObject;
import com.nexplm.platform.object.mapper.PlmObjectMapper;
import com.nexplm.platform.security.scope.ScopeApplier;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 文档访问闸门：按主键直取对象时的统一校验（对象类型 + 数据范围）。
 *
 * <p>列表查询由 {@link ScopeApplier} 注入过滤条件，但 {@code GET /docs/{id}/...}
 * 这类按 id 直取的接口不经过 wrapper，必须显式校验，否则列表里看不到的文档
 * 能被拼 id 拿到。此前只有 {@link DocService} 自己做校验，关联对象、日志、
 * 预览、批量打包等旁路接口都缺这层判断。</p>
 *
 * <p>越权与不存在统一按"不存在"返回，不泄露对象是否存在。</p>
 */
@Service
public class DocAccessGuard {

    private final PlmObjectMapper objectMapper;
    private final ScopeApplier scopeApplier;

    public DocAccessGuard(PlmObjectMapper objectMapper, ScopeApplier scopeApplier) {
        this.objectMapper = objectMapper;
        this.scopeApplier = scopeApplier;
    }

    /** 可见文档；不存在、类型不符或越权统一返回 {@code empty}。 */
    public Optional<PlmObject> find(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        PlmObject object = objectMapper.selectById(id);
        if (object == null || !DocService.OBJECT_TYPE.equals(object.getObjectType())) {
            return Optional.empty();
        }
        if (!scopeApplier.isVisible(object.getCompanyId(), object.getOrgId(), object.getOwnerId())) {
            return Optional.empty();
        }
        return Optional.of(object);
    }

    /** 必须可见：标识为空报参数错误，不存在或越权统一报"文档不存在"。 */
    public PlmObject require(Long id) {
        if (id == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "文档标识不能为空");
        }
        return find(id).orElseThrow(() -> BizException.of(ErrorCode.OBJ_NOT_FOUND, "文档不存在：" + id));
    }
}
