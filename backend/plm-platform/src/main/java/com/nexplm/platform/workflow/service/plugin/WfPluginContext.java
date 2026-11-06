package com.nexplm.platform.workflow.service.plugin;

import java.util.Map;

/**
 * 流程事件上下文：业务插件据此决定要做什么。
 *
 * @param objectType        业务对象类型（元数据 entity code）
 * @param objectId          业务对象 id
 * @param processInstanceId 流程实例 id
 * @param processKey        流程定义 key
 * @param action            事件动作，见 {@code ACTION_*} 常量
 * @param operatorId        触发人（发起/办理/撤回的用户）
 * @param taskKey           任务节点 id（仅任务级事件有值）
 * @param variables         流程变量（只读）
 */
public record WfPluginContext(String objectType, Long objectId, String processInstanceId, String processKey,
                              String action, Long operatorId, String taskKey, Map<String, Object> variables) {

    /** 流程发起成功。 */
    public static final String ACTION_STARTED = "STARTED";
    /** 流程走完全部分支（归档）。 */
    public static final String ACTION_COMPLETED = "COMPLETED";
    /** 流程被发起人撤回或外部终止。 */
    public static final String ACTION_TERMINATED = "TERMINATED";
    /** 单个任务办理完成（会签每票各触发一次）。 */
    public static final String ACTION_TASK_COMPLETED = "TASK_COMPLETED";
}
