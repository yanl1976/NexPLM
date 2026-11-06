package com.nexplm.platform.object.event;

import java.time.OffsetDateTime;

/**
 * 变更执行完成事件（{@code ChgExecService.execute} 有对象执行成功时发布）。
 *
 * <p>只在 {@code succeeded > 0} 时发布：一次全是跳过的执行没有"变更生效"的事实可通知。</p>
 */
public record ChangeExecutedEvent(Long ecrId, Long companyId, Long operatorId, int succeeded,
                                  int skipped, OffsetDateTime occurredAt) {
}
