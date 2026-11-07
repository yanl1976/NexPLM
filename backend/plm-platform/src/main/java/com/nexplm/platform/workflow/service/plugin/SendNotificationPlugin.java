package com.nexplm.platform.workflow.service.plugin;

import com.nexplm.platform.notify.service.NotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * SendNotification 插件：流程事件驱动通知通道（P9-6）发消息。
 *
 * <p>与 {@code ChangeState} 插件同构——只订阅流程事件，不反向影响流程：</p>
 * <ul>
 *   <li>单个任务办理完成 → {@code wf.task.completed}（会签每票各一条）</li>
 *   <li>流程走完全部分支 → {@code wf.process.completed}</li>
 * </ul>
 *
 * <p><b>为什么不在插件里拼模板引擎</b>：{@code sys_notify_message} 存的是渲染后文案快照，
 * 重发时不回查业务数据；模板变量在这里一次性成型，后续业务数据变化不会篡改已发出的通知。</p>
 *
 * <p><b>通道未订阅即静默</b>：{@code dispatch} 按 {@code events @> [eventType]} 匹配通道，
 * 没有通道订阅时返回 0，不产生任何记录，因此本插件可以常开。</p>
 */
@Component
@Order(100)
public class SendNotificationPlugin implements WfBusinessPlugin {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationPlugin.class);

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotifyService notifyService;
    private final JdbcTemplate jdbcTemplate;

    public SendNotificationPlugin(NotifyService notifyService, JdbcTemplate jdbcTemplate) {
        this.notifyService = notifyService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String name() {
        return "SendNotification";
    }

    @Override
    public List<String> supports() {
        return List.of();
    }

    @Override
    public void onProcessEvent(WfPluginContext context) {
        String eventType = switch (context.action()) {
            case WfPluginContext.ACTION_TASK_COMPLETED -> NotifyService.EVENT_WF_TASK_COMPLETED;
            case WfPluginContext.ACTION_COMPLETED -> NotifyService.EVENT_WF_PROCESS_COMPLETED;
            default -> null;
        };
        if (eventType == null) {
            return;
        }
        ObjectInfo info = loadObject(context);
        if (info == null || info.companyId() == null) {
            // ChgEcr 之外的独立业务对象（无 plm_object 行）不打扰通知通道
            log.debug("流程 {} 的对象 {}/{} 无公司归属，跳过通知",
                    context.processInstanceId(), context.objectType(), context.objectId());
            return;
        }
        boolean finished = WfPluginContext.ACTION_COMPLETED.equals(context.action());
        String operator = userName(context.operatorId());
        String taskName = finished ? "流程结束" : taskName(context);
        String title = (finished ? "【NexPLM】审批通过：" : "【NexPLM】审批任务已办理：")
                + info.code() + " " + info.name();
        String content = "对象：" + context.objectType() + " " + info.code() + "《" + info.name() + "》"
                + "\n节点：" + taskName
                + (context.taskKey() == null ? "" : "（" + context.taskKey() + "）")
                + "\n结果：" + (finished ? "审批通过，流程归档" : "本节点已办理")
                + "\n办理人：" + operator
                + "\n时间：" + TIME.format(OffsetDateTime.now())
                + "\n流程：" + context.processKey() + " / 实例 " + context.processInstanceId();
        String eventKey = eventType + ":" + context.processInstanceId() + ":"
                + (context.taskKey() == null ? "END" : context.taskKey()) + ":"
                + System.currentTimeMillis();
        int created = notifyService.dispatch(eventType, eventKey, title, content, info.companyId());
        if (created > 0) {
            log.info("流程通知分发：{} → {} 个通道（对象 {}）", eventType, created, info.code());
        }
    }

    /** 取业务对象编码/名称/公司：{@code plm_object} 优先，变更单在 {@code chg_ecr}。 */
    private ObjectInfo loadObject(WfPluginContext context) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT code, name, company_id FROM plm_object
                     WHERE id = ? AND object_type = ? AND deleted = false
                    """, context.objectId(), context.objectType());
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                return new ObjectInfo(str(row.get("code")), str(row.get("name")), longOf(row.get("company_id")));
            }
            rows = jdbcTemplate.queryForList(
                    "SELECT code, title AS name, company_id FROM chg_ecr WHERE id = ? AND deleted = false",
                    context.objectId());
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                return new ObjectInfo(str(row.get("code")), str(row.get("name")), longOf(row.get("company_id")));
            }
            return null;
        } catch (RuntimeException ex) {
            log.debug("流程 {} 查询业务对象失败：{}", context.processInstanceId(), ex.getMessage());
            return null;
        }
    }

    /** 节点中文名取历史任务名（已完成的运行时任务已删除，只能查历史）。 */
    private String taskName(WfPluginContext context) {
        try {
            List<String> names = jdbcTemplate.queryForList("""
                    SELECT NAME_ FROM ACT_HI_TASKINST
                     WHERE PROC_INST_ID_ = ? AND TASK_DEF_KEY_ = ?
                     ORDER BY END_TIME_ DESC NULLS LAST
                     LIMIT 1
                    """, String.class, context.processInstanceId(), context.taskKey());
            if (!names.isEmpty() && names.get(0) != null) {
                return names.get(0);
            }
        } catch (RuntimeException ex) {
            log.debug("流程 {} 查询节点名失败：{}", context.processInstanceId(), ex.getMessage());
        }
        return context.taskKey() == null ? "-" : context.taskKey();
    }

    private String userName(Long userId) {
        if (userId == null) {
            return "-";
        }
        try {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT display_name FROM sys_user WHERE id = ?", String.class, userId);
            if (!names.isEmpty() && names.get(0) != null) {
                return names.get(0);
            }
        } catch (RuntimeException ex) {
            log.debug("查询用户 {} 姓名失败：{}", userId, ex.getMessage());
        }
        return "用户" + userId;
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Long longOf(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record ObjectInfo(String code, String name, Long companyId) {
    }
}
