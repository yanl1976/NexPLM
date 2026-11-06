package com.nexplm.platform.object.event;

import java.time.OffsetDateTime;

/**
 * 对象生命周期状态迁移事件（由 {@code LifecycleService.transition} 在状态日志写完后发布）。
 *
 * <p>只带 id 与状态，不带编码/名称：载荷怎么组织是集成层的事，业务层不替它决定字段。</p>
 *
 * <p>消费方用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 接收——
 * 事务回滚时不该对外发通知，这是"已发生事实"而不是"打算发生"。</p>
 */
public record ObjectStateChangedEvent(Long objectId, String objectType, String fromState,
                                      String toState, String action, Long companyId,
                                      Long operatorId, OffsetDateTime occurredAt) {
}
