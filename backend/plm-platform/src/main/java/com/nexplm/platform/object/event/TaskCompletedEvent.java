package com.nexplm.platform.object.event;

import java.time.OffsetDateTime;

/** 项目任务完成事件（{@code prj_task.status} 变为 {@code DONE} 时发布）。 */
public record TaskCompletedEvent(Long taskId, Long companyId, Long operatorId,
                                 OffsetDateTime occurredAt) {
}
