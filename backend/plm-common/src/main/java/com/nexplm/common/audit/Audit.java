package com.nexplm.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解。加在 Controller 方法上，由平台审计切面记录操作日志。
 * <p>注意：审计只记录"谁在什么时候对什么做了什么"，不记录敏感字段（密码/令牌）。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /** 模块，如 IAM / PDM / PSE */
    String module();

    /** 动作码，如 USER_LOGIN / DOC_CHECKIN */
    String action();

    /** 对象类型（元数据 entity code），可留空 */
    String objectType() default "";

    /** 是否记录请求参数（敏感接口应关闭） */
    boolean recordArgs() default true;
}
