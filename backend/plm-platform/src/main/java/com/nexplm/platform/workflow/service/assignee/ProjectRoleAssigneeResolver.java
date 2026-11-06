package com.nexplm.platform.workflow.service.assignee;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 项目角色（如项目经理、设计负责人）：依赖项目管理（P7）的数据模型。
 * <p>当前先注册类型占位，解析时明确报「未接入」，避免被误当成"解析为空"而静默跳过审批。</p>
 */
@Component
public class ProjectRoleAssigneeResolver implements AssigneeResolver {

    @Override
    public String type() {
        return "PROJECT_ROLE";
    }

    @Override
    public String label() {
        return "项目角色（待 P7 接入）";
    }

    @Override
    public List<Long> resolve(AssigneeSpec spec, AssigneeContext context) {
        throw BizException.of(ErrorCode.WF_VARIABLE_INVALID,
                "项目角色解析依赖项目管理模块（P7），当前版本未接入");
    }
}
