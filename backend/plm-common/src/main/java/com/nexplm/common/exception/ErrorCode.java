package com.nexplm.common.exception;

/**
 * 业务错误码。命名规范：模块.场景.原因（全大写，点分）。
 * <p>HTTP 状态由 {@link #httpStatus()} 决定，前端据此区分"用户错误/权限/系统错误"。</p>
 */
public enum ErrorCode {

    // 通用
    COMMON_BAD_REQUEST("COMMON.REQUEST.INVALID", "请求参数不合法", 400),
    COMMON_UNAUTHORIZED("COMMON.AUTH.UNAUTHORIZED", "未登录或登录已过期", 401),
    COMMON_FORBIDDEN("COMMON.AUTH.FORBIDDEN", "没有权限执行该操作", 403),
    COMMON_NOT_FOUND("COMMON.RESOURCE.NOT_FOUND", "资源不存在", 404),
    COMMON_CONFLICT("COMMON.DATA.CONFLICT", "数据已被他人修改，请刷新后重试", 409),
    COMMON_INTERNAL("COMMON.SYSTEM.INTERNAL", "系统内部错误", 500),

    // 身份与鉴权
    AUTH_BAD_CREDENTIALS("IAM.AUTH.BAD_CREDENTIALS", "用户名或密码错误", 401),
    AUTH_ACCOUNT_LOCKED("IAM.AUTH.ACCOUNT_LOCKED", "账号已被锁定，请稍后再试", 401),
    AUTH_ACCOUNT_DISABLED("IAM.AUTH.ACCOUNT_DISABLED", "账号已停用", 401),
    AUTH_TOKEN_INVALID("IAM.AUTH.TOKEN_INVALID", "令牌无效", 401),
    AUTH_TOKEN_EXPIRED("IAM.AUTH.TOKEN_EXPIRED", "令牌已过期", 401),
    AUTH_REFRESH_INVALID("IAM.AUTH.REFRESH_INVALID", "刷新令牌无效，请重新登录", 401),

    // 元数据
    META_ENTITY_NOT_FOUND("META.ENTITY.NOT_FOUND", "对象类型不存在", 404),
    META_ENTITY_DUPLICATED("META.ENTITY.DUPLICATED", "对象类型编码已存在", 409),
    META_FIELD_INVALID("META.FIELD.INVALID", "字段定义不合法", 400),
    META_PUBLISH_BLOCKED("META.PUBLISH.BLOCKED", "元数据未通过校验，无法发布", 400),

    // 对象与生命周期
    OBJ_NOT_FOUND("OBJ.OBJECT.NOT_FOUND", "对象不存在", 404),
    OBJ_STATE_TRANSITION_DENIED("OBJ.STATE.TRANSITION_DENIED", "当前状态不允许该操作", 409),
    OBJ_CHECKOUT_CONFLICT("OBJ.CHECKOUT.ALREADY_LOCKED", "对象已被他人检出", 409),
    OBJ_VERSION_CONFLICT("OBJ.VERSION.CONFLICT", "版本冲突，请刷新后重试", 409),
    OBJ_CODE_DUPLICATED("OBJ.CODE.DUPLICATED", "编码已存在", 409),

    // 文件仓
    FILE_UPLOAD_EMPTY("FILE.UPLOAD.EMPTY", "上传内容为空", 400),
    FILE_NAME_INVALID("FILE.NAME.INVALID", "文件名不合法", 400),
    FILE_OBJECT_NOT_FOUND("FILE.OBJECT.NOT_FOUND", "文件不存在", 404),
    FILE_LINK_NOT_FOUND("FILE.LINK.NOT_FOUND", "文件关联不存在", 404),
    FILE_STORAGE_ERROR("FILE.STORAGE.FAILED", "文件存储失败", 500),
    FILE_UPLOAD_SESSION_NOT_FOUND("FILE.UPLOAD.SESSION_NOT_FOUND", "上传会话不存在或已结束", 404),
    FILE_UPLOAD_CHUNK_INVALID("FILE.UPLOAD.CHUNK_INVALID", "分片数据不完整或不合法", 400),
    FILE_SCAN_INFECTED("FILE.SCAN.INFECTED", "文件未通过安全扫描，禁止下载", 403),

    // 工作流
    WF_DEFINITION_NOT_FOUND("WF.DEFINITION.NOT_FOUND", "流程定义不存在或未部署", 404),
    WF_BINDING_NOT_FOUND("WF.BINDING.NOT_FOUND", "该对象类型未配置可用流程", 400),
    WF_INSTANCE_NOT_FOUND("WF.INSTANCE.NOT_FOUND", "流程实例不存在或已结束", 404),
    WF_TASK_NOT_FOUND("WF.TASK.NOT_FOUND", "待办任务不存在或已被处理", 404),
    WF_TASK_NOT_ASSIGNEE("WF.TASK.NOT_ASSIGNEE", "该任务不属于当前用户", 403),
    WF_OPERATION_DENIED("WF.OPERATION.DENIED", "当前流程状态不允许该操作", 409),
    WF_VARIABLE_INVALID("WF.VARIABLE.INVALID", "流程参数不合法", 400),

    // 变更管理
    CHG_ECR_NOT_FOUND("CHG.ECR.NOT_FOUND", "变更申请不存在", 404),
    CHG_STATE_INVALID("CHG.STATE.INVALID", "当前变更状态不允许该操作", 409),
    CHG_OBJECT_INVALID("CHG.OBJECT.INVALID", "变更关联对象不合法", 400),

    // 工艺（CAPP）
    CAPP_ROUTING_NOT_FOUND("CAPP.ROUTING.NOT_FOUND", "工艺路线不存在", 404),
    CAPP_ROUTING_DUPLICATE("CAPP.ROUTING.DUPLICATE", "工艺路线名称已存在", 409),
    CAPP_STATE_INVALID("CAPP.STATE.INVALID", "当前工艺路线状态不允许该操作", 409),
    CAPP_STEP_INVALID("CAPP.STEP.INVALID", "工艺工序不合法", 400),
    CAPP_TEMPLATE_NOT_FOUND("CAPP.TEMPLATE.NOT_FOUND", "工艺卡片模板不存在", 404),
    CAPP_TEMPLATE_STATE_INVALID("CAPP.TEMPLATE.STATE.INVALID", "当前模板状态不允许该操作", 409),
    CAPP_CARD_NOT_FOUND("CAPP.CARD.NOT_FOUND", "工艺卡片不存在", 404),
    CAPP_TIME_RULE_NOT_FOUND("CAPP.TIME_RULE.NOT_FOUND", "工时定额规则不存在", 404),
    CAPP_CARD_INVALID("CAPP.CARD.INVALID", "工艺卡片不合法", 400),

    // 项目与任务（P7）
    PRJ_NOT_FOUND("PRJ.NOT_FOUND", "项目不存在", 404),
    PRJ_STATE_INVALID("PRJ.STATE.INVALID", "当前项目状态不允许该操作", 409),
    PRJ_INVALID("PRJ.INVALID", "项目参数不合法", 400),
    PRJ_EPS_NOT_FOUND("PRJ.EPS.NOT_FOUND", "EPS 节点不存在", 404),
    PRJ_EPS_IN_USE("PRJ.EPS.IN_USE", "EPS 节点下还有子节点或项目，不能删除", 409),
    PRJ_TASK_NOT_FOUND("PRJ.TASK.NOT_FOUND", "任务不存在", 404),
    PRJ_TASK_INVALID("PRJ.TASK.INVALID", "任务参数不合法", 400),
    PRJ_TASK_IN_USE("PRJ.TASK.IN_USE", "任务下还有子任务，不能删除", 409),
    PRJ_TASK_DEP_INVALID("PRJ.TASK_DEP.INVALID", "任务依赖不合法", 400),
    PRJ_DELIVERABLE_NOT_FOUND("PRJ.DELIVERABLE.NOT_FOUND", "任务交付物不存在", 404),
    PRJ_DELIVERABLE_INVALID("PRJ.DELIVERABLE.INVALID", "任务交付物不合法", 400),
    PRJ_DELIVERABLE_INCOMPLETE("PRJ.DELIVERABLE.INCOMPLETE", "任务必需交付物未齐套", 409),
    PRJ_TIMESHEET_NOT_FOUND("PRJ.TIMESHEET.NOT_FOUND", "工时记录不存在", 404),
    PRJ_TIMESHEET_INVALID("PRJ.TIMESHEET.INVALID", "工时记录不合法", 400),
    PRJ_TIMESHEET_STATE_INVALID("PRJ.TIMESHEET.STATE.INVALID", "当前工时状态不允许该操作", 409),
    PRJ_TEMPLATE_NOT_FOUND("PRJ.TEMPLATE.NOT_FOUND", "项目模板不存在", 404),
    PRJ_TEMPLATE_INVALID("PRJ.TEMPLATE.INVALID", "项目模板不合法", 400),
    QLT_ISSUE_NOT_FOUND("QLT.ISSUE.NOT_FOUND", "质量问题不存在", 404),
    QLT_ISSUE_INVALID("QLT.ISSUE.INVALID", "质量问题数据不合法", 400),
    QLT_ISSUE_STATE_INVALID("QLT.ISSUE.STATE.INVALID", "当前质量问题状态不允许该操作", 409),
    QLT_TEMPLATE_NOT_FOUND("QLT.TEMPLATE.NOT_FOUND", "质量模板不存在", 404),
    QLT_TEMPLATE_INVALID("QLT.TEMPLATE.INVALID", "质量模板数据不合法", 400),
    SYSDOC_NOT_FOUND("SYSDOC.NOT_FOUND", "体系文件对象不存在", 404),
    SYSDOC_INVALID("SYSDOC.INVALID", "体系文件数据不合法", 400),
    SYSDOC_STATE_INVALID("SYSDOC.STATE.INVALID", "当前体系文件状态不允许该操作", 409),
    SYSDOC_FOLDER_IN_USE("SYSDOC.FOLDER.IN_USE", "文件夹下还有子文件夹或文件，不能删除", 409),
    AUDIT_NOT_FOUND("AUDIT.NOT_FOUND", "审核对象不存在", 404),
    AUDIT_INVALID("AUDIT.INVALID", "审核数据不合法", 400),
    AUDIT_STATE_INVALID("AUDIT.STATE.INVALID", "当前审核状态不允许该操作", 409),
    SUP_NOT_FOUND("SUP.NOT_FOUND", "供应商对象不存在", 404),
    SUP_INVALID("SUP.INVALID", "供应商数据不合法", 400),
    SUP_IN_USE("SUP.IN_USE", "供应商还有在用的零件关系，不能删除", 409);

    private final String code;
    private final String message;
    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
