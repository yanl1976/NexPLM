package com.nexplm.platform.notify.service;

import com.nexplm.platform.object.event.ChangeExecutedEvent;
import com.nexplm.platform.object.event.ObjectStateChangedEvent;
import com.nexplm.platform.object.event.TaskCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 把领域事件渲染成"人读的通知文案"并分发到各通道。
 *
 * <p>与 {@code WebhookPublisher} 同一批事件源，但目标不同：webhook 给系统（原样报文 + 签名），
 * 通知给人（标题 + 正文）。渲染放在这里而不是业务层，业务层只负责声明"发生了什么"。</p>
 */
@Component
public class NotifyPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotifyPublisher.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotifyService notifyService;
    private final JdbcTemplate jdbcTemplate;

    public NotifyPublisher(NotifyService notifyService, JdbcTemplate jdbcTemplate) {
        this.notifyService = notifyService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 文档发布：PdmDoc → Released。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onObjectStateChanged(ObjectStateChangedEvent event) {
        if (!"PdmDoc".equals(event.objectType()) || !"Released".equals(event.toState())) {
            return;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT code, name, revision, iteration, released_at
                      FROM plm_object WHERE id = ? AND deleted = false
                    """, event.objectId());
            if (rows.isEmpty()) {
                return;
            }
            Map<String, Object> row = rows.get(0);
            String code = str(row.get("code"));
            String name = str(row.get("name"));
            String title = "【NexPLM】文档发布：" + code + " " + name;
            String content = "文档编码：" + code
                    + "\n文档名称：" + name
                    + "\n版本：R" + intOf(row.get("revision")) + "（迭代 " + intOf(row.get("iteration")) + "）"
                    + "\n状态：" + event.fromState() + " → " + event.toState()
                    + "\n发布时间：" + now(event.occurredAt())
                    + "\n操作人ID：" + event.operatorId();
            dispatch(NotifyService.EVENT_DOC_RELEASED, event.objectId(), event.occurredAt(), title,
                    content, event.companyId());
        } catch (RuntimeException ex) {
            log.warn("文档发布通知分发失败：object={} 原因={}", event.objectId(), ex.getMessage());
        }
    }

    /** 变更生效：ECR 执行有对象成功。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChangeExecuted(ChangeExecutedEvent event) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT code, title, status FROM chg_ecr WHERE id = ?
                    """, event.ecrId());
            Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
            String code = str(row.get("code"));
            String title = "【NexPLM】变更生效：" + code;
            String content = "变更单：" + code
                    + "\n标题：" + str(row.get("title"))
                    + "\n执行结果：成功 " + event.succeeded() + " 项，跳过 " + event.skipped() + " 项"
                    + "\n执行时间：" + now(event.occurredAt())
                    + "\n操作人ID：" + event.operatorId();
            dispatch(NotifyService.EVENT_CHANGE_EFFECTIVE, event.ecrId(), event.occurredAt(), title,
                    content, event.companyId());
        } catch (RuntimeException ex) {
            log.warn("变更生效通知分发失败：ecr={} 原因={}", event.ecrId(), ex.getMessage());
        }
    }

    /** 任务完成：prj_task → DONE。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTaskCompleted(TaskCompletedEvent event) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT t.wbs_code, t.name, t.actual_end, t.owner_id, p.code AS project_code,
                           p.name AS project_name
                      FROM prj_task t LEFT JOIN prj_project p ON p.id = t.project_id
                     WHERE t.id = ?
                    """, event.taskId());
            if (rows.isEmpty()) {
                return;
            }
            Map<String, Object> row = rows.get(0);
            String wbs = str(row.get("wbs_code"));
            String name = str(row.get("name"));
            String title = "【NexPLM】任务完成：" + wbs + " " + name;
            String content = "项目：" + str(row.get("project_code")) + " " + str(row.get("project_name"))
                    + "\n任务：" + wbs + " " + name
                    + "\n完成日期：" + str(row.get("actual_end"))
                    + "\n完成时间：" + now(event.occurredAt())
                    + "\n负责人ID：" + row.get("owner_id");
            dispatch(NotifyService.EVENT_TASK_COMPLETED, event.taskId(), event.occurredAt(), title,
                    content, event.companyId());
        } catch (RuntimeException ex) {
            log.warn("任务完成通知分发失败：task={} 原因={}", event.taskId(), ex.getMessage());
        }
    }

    private void dispatch(String eventType, Long bizId, OffsetDateTime occurredAt, String title,
                          String content, Long companyId) {
        String eventKey = eventType + ":" + bizId + ":"
                + (occurredAt == null ? System.currentTimeMillis()
                : occurredAt.toInstant().toEpochMilli());
        int created = notifyService.dispatch(eventType, eventKey, title, content, companyId);
        if (created > 0) {
            log.info("通知分发：{} → {} 个通道", eventType, created);
        }
    }

    private static String now(OffsetDateTime time) {
        return (time == null ? OffsetDateTime.now() : time).format(TIME);
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intOf(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }
}
