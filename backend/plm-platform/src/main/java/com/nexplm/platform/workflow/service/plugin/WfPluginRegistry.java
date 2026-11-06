package com.nexplm.platform.workflow.service.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 业务插件注册表：把流程事件分发给所有插件。
 *
 * <p>执行时机与失败隔离：</p>
 * <ol>
 *   <li>若当前处于事务中，注册 {@code afterCommit} 回调，**主流程事务提交成功后才执行**，
 *       避免插件动作先落库而流程事务回滚导致数据不一致；</li>
 *   <li>每个插件用独立事务（{@code REQUIRES_NEW}）执行，异常只记日志，不影响流程与其他插件。</li>
 * </ol>
 */
@Component
public class WfPluginRegistry {

    private static final Logger log = LoggerFactory.getLogger(WfPluginRegistry.class);

    private final List<WfBusinessPlugin> plugins;
    private final TransactionTemplate requiresNew;

    public WfPluginRegistry(List<WfBusinessPlugin> plugins, PlatformTransactionManager transactionManager) {
        this.plugins = plugins;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 分发事件（事务内调用会推迟到提交后）。 */
    public void fire(WfPluginContext context) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fireNow(context);
                }
            });
        } else {
            fireNow(context);
        }
    }

    private void fireNow(WfPluginContext context) {
        for (WfBusinessPlugin plugin : plugins) {
            List<String> supports = plugin.supports();
            if (!supports.isEmpty() && !supports.contains(context.objectType())) {
                continue;
            }
            try {
                requiresNew.executeWithoutResult(status -> plugin.onProcessEvent(context));
            } catch (Exception ex) {
                log.warn("流程插件执行失败 plugin={} action={} object={}:{}：{}",
                        plugin.name(), context.action(), context.objectType(), context.objectId(), ex.getMessage());
            }
        }
    }
}
