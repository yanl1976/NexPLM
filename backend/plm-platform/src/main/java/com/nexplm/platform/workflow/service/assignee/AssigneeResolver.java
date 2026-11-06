package com.nexplm.platform.workflow.service.assignee;

import java.util.List;

/**
 * 执行人解析 SPI：把「用户/角色/组织/组/组+角色/发起人/表单字段/项目角色」解析成具体用户 id。
 *
 * <p>扩展方式：实现本接口并标注 {@code @Component}，{@link AssigneeResolverRegistry} 会自动注册；
 * 新增类型无需改动流程服务。</p>
 */
public interface AssigneeResolver {

    /** 类型码（大写），如 USER / ROLE / ORG / GROUP / ROLE_GROUP / STARTER / FORM_FIELD / PROJECT_ROLE。 */
    String type();

    /** 类型中文名，用于前端下拉与错误提示。 */
    String label();

    /**
     * 解析执行人。
     *
     * @return 用户 id 列表（去重由注册表统一处理）；解析不到时应返回空列表，由注册表统一报错
     */
    List<Long> resolve(AssigneeSpec spec, AssigneeContext context);
}
