package com.nexplm.platform.workflow.service.plugin;

import com.nexplm.common.exception.BizException;
import com.nexplm.platform.lifecycle.service.LifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ChangeState 插件：流程事件驱动对象生命周期状态流转。
 *
 * <p>与默认生命周期（{@code DEFAULT_LIFECYCLE}）对应：</p>
 * <ul>
 *   <li>流程发起 → {@code Draft --submit--> InWorkflow}</li>
 *   <li>流程完成 → {@code InWorkflow --approve--> Released}</li>
 *   <li>流程撤回 → {@code InWorkflow --reject--> Draft}</li>
 * </ul>
 *
 * <p>状态不允许流转时只记日志，不让流程失败——生命周期是业务副作用，不是流程前置条件。</p>
 */
@Component
public class LifecycleAdvancePlugin implements WfBusinessPlugin {

    private static final Logger log = LoggerFactory.getLogger(LifecycleAdvancePlugin.class);

    private final LifecycleService lifecycleService;
    private final JdbcTemplate jdbcTemplate;

    public LifecycleAdvancePlugin(LifecycleService lifecycleService, JdbcTemplate jdbcTemplate) {
        this.lifecycleService = lifecycleService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String name() {
        return "ChangeState";
    }

    @Override
    public List<String> supports() {
        return List.of();
    }

    @Override
    public void onProcessEvent(WfPluginContext context) {
        String action = switch (context.action()) {
            case WfPluginContext.ACTION_STARTED -> "submit";
            case WfPluginContext.ACTION_COMPLETED -> "approve";
            case WfPluginContext.ACTION_TERMINATED -> "reject";
            default -> null;
        };
        if (action == null) {
            return;
        }
        String state = currentState(context);
        if (state == null) {
            return;
        }
        // 已在审批中，无需重复提交
        if ("submit".equals(action) && "InWorkflow".equals(state)) {
            return;
        }
        try {
            lifecycleService.transition(context.objectId(), action, reason(context), context.operatorId());
        } catch (BizException ex) {
            log.info("流程事件 {} 未推进对象 {} 生命周期（当前 {}，动作 {}）：{}",
                    context.action(), context.objectId(), state, action, ex.getMessage());
        }
    }

    /**
     * 取对象当前生命周期状态。
     * <p>带 {@code object_type} 限定：变更申请（ChgEcr）等独立业务对象不在 {@code plm_object}，
     * 若只按 id 查，极端情况下会命中别的对象，把状态改错人。</p>
     */
    private String currentState(WfPluginContext context) {
        try {
            List<String> states = jdbcTemplate.queryForList(
                    "SELECT lifecycle_state FROM plm_object WHERE id = ? AND object_type = ? AND deleted = false",
                    String.class, context.objectId(), context.objectType());
            return states.isEmpty() ? null : states.get(0);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String reason(WfPluginContext context) {
        return switch (context.action()) {
            case WfPluginContext.ACTION_STARTED -> "发起审批流程";
            case WfPluginContext.ACTION_COMPLETED -> "审批通过（流程归档）";
            case WfPluginContext.ACTION_TERMINATED -> "流程撤回";
            default -> null;
        };
    }
}
