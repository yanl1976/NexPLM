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
 * <p>状态迁移不写死，按元数据状态机 {@code CHG_ECR}（{@code meta_lifecycle}，见 V20）执行：</p>
 * <ul>
 *   <li>流程发起 {@code STARTED} → 动作 {@code review}：{@code SUBMITTED → REVIEWING}</li>
 *   <li>流程归档 {@code COMPLETED} → 动作 {@code approve}：{@code SUBMITTED/REVIEWING → APPROVED}（写 {@code approved_at}）</li>
 *   <li>流程撤回 {@code TERMINATED} → 动作 {@code withdraw}：{@code SUBMITTED/REVIEWING → DRAFT}（回草稿，可改后重新提交）</li>
 * </ul>
 *
 * <p>当前状态在状态机中没有对应迁移时直接跳过——已批准、已下发 ECN、已关闭的单据不会被迟到的流程事件回退；
 * 「以业务已推进的状态为准」这条约束现在由元数据表达，而不是代码里的状态白名单。</p>
 *
 * <p>插件由 {@code WfPluginRegistry} 在流程事务提交后调用，异常只记日志不影响流程。</p>
 */
@Component
public class ChgWorkflowPlugin implements WfBusinessPlugin {

    private static final Logger log = LoggerFactory.getLogger(ChgWorkflowPlugin.class);

    private final JdbcTemplate jdbcTemplate;
    private final ChgStateMachine stateMachine;

    public ChgWorkflowPlugin(JdbcTemplate jdbcTemplate, ChgStateMachine stateMachine) {
        this.jdbcTemplate = jdbcTemplate;
        this.stateMachine = stateMachine;
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
            case WfPluginContext.ACTION_STARTED -> apply(context, "review");
            case WfPluginContext.ACTION_COMPLETED -> apply(context, "approve");
            case WfPluginContext.ACTION_TERMINATED -> apply(context, "withdraw");
            // 任务级事件（TASK_COMPLETED）不改变 ECR 状态：会签中途还不到结论
            default -> {
            }
        }
    }

    private void apply(WfPluginContext context, String action) {
        List<String> states = jdbcTemplate.query("""
                        SELECT status FROM chg_ecr WHERE id = ? AND deleted = false
                        """,
                (rs, rowNum) -> rs.getString(1), context.objectId());
        if (states.isEmpty()) {
            log.warn("流程事件 {} 找不到变更申请 {}（流程 {}）",
                    context.action(), context.objectId(), context.processInstanceId());
            return;
        }
        String current = states.get(0);
        if (!stateMachine.can(ChgStateMachine.ECR, current, action)) {
            log.info("流程事件 {} 未回写 ECR {}：状态 {} 无 {} 迁移",
                    context.action(), context.objectId(), current, action);
            return;
        }
        String target = stateMachine.target(ChgStateMachine.ECR, current, action);
        int rows;
        if ("approve".equals(action)) {
            rows = jdbcTemplate.update("""
                            UPDATE chg_ecr
                               SET status = ?, approved_at = COALESCE(approved_at, now()),
                                   updated_by = COALESCE(?, updated_by), updated_at = now()
                             WHERE id = ? AND status = ?
                            """, target, context.operatorId(), context.objectId(), current);
        } else if ("withdraw".equals(action)) {
            rows = jdbcTemplate.update("""
                            UPDATE chg_ecr
                               SET status = ?, approved_at = NULL,
                                   updated_by = COALESCE(?, updated_by), updated_at = now()
                             WHERE id = ? AND status = ?
                            """, target, context.operatorId(), context.objectId(), current);
        } else {
            rows = jdbcTemplate.update("""
                            UPDATE chg_ecr
                               SET status = ?, updated_by = COALESCE(?, updated_by), updated_at = now()
                             WHERE id = ? AND status = ?
                            """, target, context.operatorId(), context.objectId(), current);
        }
        if (rows == 0) {
            log.info("流程事件 {} 回写 ECR {} 失败：状态已被并发修改（{} → {}）",
                    context.action(), context.objectId(), current, target);
            return;
        }
        log.info("流程事件 {} → ECR {} 状态 {} → {}（流程实例 {}）",
                context.action(), context.objectId(), current, target, context.processInstanceId());
    }
}
