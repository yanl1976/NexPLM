package com.nexplm.platform.workflow.service.plugin;

import java.util.List;

/**
 * 流程业务插件 SPI：在流程关键节点上执行 PLM 侧业务动作（改状态、发通知、发布 ERP 等）。
 *
 * <p>约定：</p>
 * <ul>
 *   <li>插件在**事务提交后**被调用，各自独立事务，异常只记日志不回滚流程；</li>
 *   <li>插件必须自行判断「当前状态是否允许」，不做破坏性操作；</li>
 *   <li>扩展方式：实现本接口并标注 {@code @Component}，注册表自动收集。</li>
 * </ul>
 */
public interface WfBusinessPlugin {

    /** 插件名称（日志与排障用）。 */
    default String name() {
        return getClass().getSimpleName();
    }

    /** 关心的对象类型；返回空表示全部类型。 */
    default List<String> supports() {
        return List.of();
    }

    /** 接收流程事件。 */
    void onProcessEvent(WfPluginContext context);
}
