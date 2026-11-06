package com.nexplm.platform.workflow.service.assignee;

import java.util.Map;

/**
 * 执行人解析上下文：发起时的业务对象、发起人与流程变量。
 *
 * @param objectType 业务对象类型（元数据 entity code）
 * @param objectId   业务对象 id
 * @param starterId  发起人用户 id
 * @param variables  发起时传入的流程变量（FORM_FIELD 解析从其中取值）
 */
public record AssigneeContext(String objectType, Long objectId, Long starterId, Map<String, Object> variables) {
}
