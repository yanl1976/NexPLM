package com.nexplm.platform.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.List;
import java.util.Map;

/**
 * 把领域事件翻译成 Webhook 报文并分发。
 *
 * <p>用 {@code AFTER_COMMIT} 而不是普通 {@code @EventListener}：事务回滚时不该对外通知；
 * 用 {@code fallbackExecution = true} 兜住没有事务的调用方，避免"事件发了但没人收到"。</p>
 *
 * <p>异常一律吞掉并记日志：外部系统是否可达不能反向影响本系统的业务提交，
 * 失败本身已经落成投递记录，交给重试任务与死信重放。</p>
 */
@Component
public class WebhookPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebhookPublisher.class);

    private final WebhookService webhookService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;

    public WebhookPublisher(WebhookService webhookService, JdbcTemplate jdbcTemplate,
                            ObjectMapper jsonMapper) {
        this.webhookService = webhookService;
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    /** 文档发布：只认 {@code PdmDoc} 迁到 {@code Released}，其它对象/迁移不发通知。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onObjectStateChanged(ObjectStateChangedEvent event) {
        if (!"PdmDoc".equals(event.objectType()) || !"Released".equals(event.toState())) {
            return;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT code, name, revision, iteration, owner_id, released_at
                      FROM plm_object WHERE id = ? AND deleted = false
                    """, event.objectId());
            if (rows.isEmpty()) {
                return;
            }
            Map<String, Object> row = rows.get(0);
            ObjectNode data = jsonMapper.createObjectNode();
            data.put("objectId", event.objectId());
            data.put("objectType", event.objectType());
            data.put("code", str(row.get("code")));
            data.put("name", str(row.get("name")));
            data.put("revision", intOf(row.get("revision")));
            data.put("iteration", intOf(row.get("iteration")));
            data.put("lifecycleState", event.toState());
            data.put("fromState", event.fromState());
            data.put("ownerId", longOf(row.get("owner_id")));
            data.put("releasedAt", str(row.get("released_at")));
            publish(WebhookService.EVENT_DOC_RELEASED,
                    WebhookService.EVENT_DOC_RELEASED + ":" + event.objectId() + ":"
                            + epochMilli(event.occurredAt()), data, event.companyId(),
                    event.occurredAt());
        } catch (RuntimeException ex) {
            log.warn("文档发布事件分发失败：object={} 原因={}", event.objectId(), ex.getMessage());
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
            ObjectNode data = jsonMapper.createObjectNode();
            data.put("ecrId", event.ecrId());
            data.put("ecrCode", str(row.get("code")));
            data.put("title", str(row.get("title")));
            data.put("status", str(row.get("status")));
            data.put("succeeded", event.succeeded());
            data.put("skipped", event.skipped());
            data.put("operatorId", event.operatorId());
            publish(WebhookService.EVENT_CHANGE_EFFECTIVE,
                    WebhookService.EVENT_CHANGE_EFFECTIVE + ":" + event.ecrId() + ":"
                            + epochMilli(event.occurredAt()), data, event.companyId(),
                    event.occurredAt());
        } catch (RuntimeException ex) {
            log.warn("变更生效事件分发失败：ecr={} 原因={}", event.ecrId(), ex.getMessage());
        }
    }

    /** 任务完成：{@code prj_task.status → DONE}。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTaskCompleted(TaskCompletedEvent event) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT t.wbs_code, t.name, t.progress, t.actual_end, t.owner_id, t.project_id,
                           p.code AS project_code
                      FROM prj_task t LEFT JOIN prj_project p ON p.id = t.project_id
                     WHERE t.id = ?
                    """, event.taskId());
            if (rows.isEmpty()) {
                return;
            }
            Map<String, Object> row = rows.get(0);
            ObjectNode data = jsonMapper.createObjectNode();
            data.put("taskId", event.taskId());
            data.put("projectId", longOf(row.get("project_id")));
            data.put("projectCode", str(row.get("project_code")));
            data.put("wbsCode", str(row.get("wbs_code")));
            data.put("name", str(row.get("name")));
            data.put("status", "DONE");
            data.put("progress", intOf(row.get("progress")));
            data.put("ownerId", longOf(row.get("owner_id")));
            data.put("actualEnd", str(row.get("actual_end")));
            publish(WebhookService.EVENT_TASK_COMPLETED,
                    WebhookService.EVENT_TASK_COMPLETED + ":" + event.taskId() + ":"
                            + epochMilli(event.occurredAt()), data, event.companyId(),
                    event.occurredAt());
        } catch (RuntimeException ex) {
            log.warn("任务完成事件分发失败：task={} 原因={}", event.taskId(), ex.getMessage());
        }
    }

    private void publish(String eventType, String eventKey, ObjectNode data, Long companyId,
                         OffsetDateTime occurredAt) {
        ObjectNode root = jsonMapper.createObjectNode();
        root.put("event", eventType);
        root.put("eventId", eventKey);
        root.put("occurredAt", occurredAt == null ? OffsetDateTime.now().toString()
                : occurredAt.toString());
        root.put("companyId", companyId);
        root.set("data", data);
        int created = webhookService.dispatch(eventType, eventKey, root, companyId);
        if (created > 0) {
            log.info("Webhook 事件分发：{} → {} 个订阅", eventType, created);
        }
    }

    private static long epochMilli(OffsetDateTime time) {
        return time == null ? System.currentTimeMillis() : time.toInstant().toEpochMilli();
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intOf(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static Long longOf(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
