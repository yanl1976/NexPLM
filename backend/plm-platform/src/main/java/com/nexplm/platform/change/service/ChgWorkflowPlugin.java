package com.nexplm.platform.change.service;

import com.nexplm.platform.workflow.service.plugin.WfBusinessPlugin;
import com.nexplm.platform.workflow.service.plugin.WfPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 变更申请（ECR）流程插件：把审批流程事件回写成 ECR 业务状态。
 *
 * <p>状态映射（对齐 V18 状态机，流程只推动「审批段」）：</p>
 * <ul>
 *   <li>流程发起 {@code STARTED}：{@code SUBMITTED → REVIEWING}</li>
 *   <li>流程归档 {@code COMPLETED}：{@code REVIEWING → APPROVED}（写 {@code approved_at}）</li>
 *   <li>流程撤回 {@code TERMINATED}：{@code REVIEWING → DRAFT}（回草稿，改完可重新提交）</li>
 * </ul>
 *
 * <p>回写只接受「审批在途」的两个状态（{@code SUBMITTED} / {@code REVIEWING}）。
 * 已在 {@code APPROVED} 之后（如已下发 ECN、已执行）或已被人工关闭的单据，
 * 不会被迟到的流程事件回退——生命周期与流程结论冲突时以业务已推进的状态为准。</p>
 *
 * <p>插件由 {@code WfPluginRegistry} 在流程事务提交后调用，异常只记日志不影响流程。</p>
 */
@Component
public class ChgWorkflowPlugin implements WfBusinessPlugin {

    private static final Logger log = LoggerFactory.getLogger(ChgWorkflowPlugin.class);

    private final JdbcTemplate jdbcTemplate;

    public ChgWorkflowPlugin(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String name() {
        return "ChgEcrApproval";
    }

    @Override
    public List<String> supports() {
        return List.of("ChgEcr");
    }

    @Override
    public void onProcessEvent(WfPluginContext context) {
        switch (context.action()) {
            case WfPluginContext.ACTION_STARTED -> toReviewing(context);
            case WfPluginContext.ACTION_COMPLETED -> toApproved(context);
            case WfPluginContext.ACTION_TERMINATED -> backToDraft(context);
            // 任务级事件（TASK_COMPLETED）不改变 ECR 状态：会签中途还不到结论
            default -> {
            }
        }
    }

    private void toReviewing(WfPluginContext context) {
        int rows = jdbcTemplate.update("""
                        UPDATE chg_ecr
                           SET status = 'REVIEWING', updated_by = COALESCE(?, updated_by), updated_at = now()
                         WHERE id = ? AND deleted = false AND status = 'SUBMITTED'
                        """, context.operatorId(), context.objectId());
        logEvent(context, "REVIEWING", rows);
    }

    private void toApproved(WfPluginContext context) {
        int rows = jdbcTemplate.update("""
                        UPDATE chg_ecr
                           SET status = 'APPROVED', approved_at = COALESCE(approved_at, now()),
                               updated_by = COALESCE(?, updated_by), updated_at = now()
                         WHERE id = ? AND deleted = false AND status IN ('SUBMITTED', 'REVIEWING')
                        """, context.operatorId(), context.objectId());
        logEvent(context, "APPROVED", rows);
    }

    private void backToDraft(WfPluginContext context) {
        int rows = jdbcTemplate.update("""
                        UPDATE chg_ecr
                           SET status = 'DRAFT', approved_at = NULL,
                               updated_by = COALESCE(?, updated_by), updated_at = now()
                         WHERE id = ? AND deleted = false AND status IN ('SUBMITTED', 'REVIEWING')
                        """, context.operatorId(), context.objectId());
        logEvent(context, "DRAFT", rows);
    }

    private void logEvent(WfPluginContext context, String target, int rows) {
        if (rows == 0) {
            log.info("流程事件 {} 未回写 ECR {}（当前状态不在 SUBMITTED/REVIEWING，目标 {}）",
                    context.action(), context.objectId(), target);
            return;
        }
        log.info("流程事件 {} → ECR {} 状态置为 {}（流程实例 {}）",
                context.action(), context.objectId(), target, context.processInstanceId());
    }
}
