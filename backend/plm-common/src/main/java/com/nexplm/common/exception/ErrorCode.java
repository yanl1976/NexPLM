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
    OBJ_CODE_DUPLICATED("OBJ.CODE.DUPLICATED", "编码已存在", 409);

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
