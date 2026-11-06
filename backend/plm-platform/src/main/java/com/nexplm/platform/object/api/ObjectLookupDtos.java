package com.nexplm.platform.object.api;

import java.time.OffsetDateTime;

/**
 * 扫码查对象（P10-3）：按编码精确匹配后的最小返回。
 *
 * <p>为什么只回这几列而不是整个对象：移动端扫码后要的是"这是什么、能不能点进去"，
 * 属性区由详情接口按权限再取一次；这里多带字段等于给扫码接口扩了一条越权读的口子。</p>
 */
public final class ObjectLookupDtos {

    private ObjectLookupDtos() {
    }

    /** {@code objectType} 为 {@code PdmItem}/{@code PdmDoc}/{@code ChgEcr} 等，前端据此决定跳哪个详情。 */
    public record LookupResult(Long id,
                               String objectType,
                               String code,
                               String name,
                               Integer revision,
                               Integer iteration,
                               String lifecycleState,
                               OffsetDateTime updatedAt) {
    }
}
