package com.nexplm.platform.workflow.service.assignee;

import java.util.List;

/**
 * 执行人声明：由「类型 + 一组主键（或表达式）」描述，交由对应 {@link AssigneeResolver} 解析成具体用户。
 *
 * @param type       解析类型，见 {@link AssigneeResolver#type()}
 * @param ids        主键集合：USER=用户 id，ROLE=角色 id，ORG=组织 id，GROUP=组 id，
 *                   ROLE_GROUP=[角色 id, 组 id]
 * @param expression 表达式：FORM_FIELD 时为流程变量名
 */
public record AssigneeSpec(String type, List<Long> ids, String expression) {

    public List<Long> safeIds() {
        return ids == null ? List.of() : ids;
    }
}
