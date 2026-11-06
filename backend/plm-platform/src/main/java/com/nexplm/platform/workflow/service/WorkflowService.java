package com.nexplm.platform.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.workflow.api.WfDtos;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流服务：BPMN 流程的发起、待办、办理、驳回、撤回与详情。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>Flowable 只管「流程流转」，业务对象信息全部落在 {@code wf_instance_link}，
 *       通过 {@code process_instance_id} 关联，避免把业务字段塞进流程变量；</li>
 *   <li>办理人用 PLM 用户 id 的字符串形式作为 assignee，不使用 Flowable IDM 用户体系；</li>
 *   <li>状态以运行时引擎为准，{@code wf_instance_link.status} 只做列表快筛，每次流转后回写。</li>
 * </ul>
 */
@Service
public class WorkflowService {

    private static final int MAX_PAGE_SIZE = 200;

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final JdbcTemplate jdbcTemplate;

    public WorkflowService(RuntimeService runtimeService, TaskService taskService,
                           RepositoryService repositoryService, JdbcTemplate jdbcTemplate) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.repositoryService = repositoryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 业务对象与流程实例的关联信息（用于把任务行补全为可展示的业务上下文）。 */
    private record LinkInfo(String objectType, Long objectId, String code, String name) {
    }

    // ------------------------------------------------------------------ 发起

    @Transactional
    public WfDtos.StartResp start(WfDtos.StartReq req, Long userId) {
        requireObject(req.objectId());
        String processKey = resolveProcessKey(req.objectType(), req.processKey());
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey).latestVersion().singleResult();
        if (definition == null) {
            throw BizException.of(ErrorCode.WF_DEFINITION_NOT_FOUND, "流程定义未部署：" + processKey);
        }
        List<String> approvers = req.approverIds().stream().distinct().map(String::valueOf).toList();
        if (approvers.isEmpty()) {
            throw BizException.of(ErrorCode.WF_VARIABLE_INVALID, "会签人不能为空");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approver", String.valueOf(req.approverId()));
        variables.put("approvers", approvers);
        variables.put("initiator", String.valueOf(userId));
        variables.put("objectType", req.objectType());
        variables.put("objectId", req.objectId());

        String businessKey = req.objectType() + ":" + req.objectId();
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(processKey, businessKey, variables);

        jdbcTemplate.update("""
                        INSERT INTO wf_instance_link (id, object_type, object_id, process_instance_id, process_key,
                                                      business_key, status, started_by, started_at)
                        VALUES (?, ?, ?, ?, ?, ?, 'RUNNING', ?, ?)
                        """,
                IdWorker.getId(), req.objectType(), req.objectId(), instance.getId(), processKey,
                businessKey, userId, OffsetDateTime.now());
        saveComment(instance.getId(), null, null, "START", req.remark(), userId);

        return new WfDtos.StartResp(instance.getId(), processKey, businessKey, activeTasks(instance.getId()));
    }

    // ------------------------------------------------------------------ 待办

    public WfDtos.PageResult<WfDtos.TaskRow> myTasks(Long userId, int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String assignee = String.valueOf(userId);
        long total = taskService.createTaskQuery().taskAssignee(assignee).count();
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .listPage((pageNo - 1) * pageSize, pageSize);
        return new WfDtos.PageResult<>(total, toRows(tasks));
    }

    // ------------------------------------------------------------------ 办理

    @Transactional
    public void complete(String taskId, WfDtos.CompleteReq req, Long userId) {
        Task task = requireOwnTask(taskId, userId);
        saveComment(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(), "APPROVE",
                req == null ? null : req.comment(), userId);
        if (req != null && req.variables() != null && !req.variables().isEmpty()) {
            taskService.complete(taskId, req.variables());
        } else {
            taskService.complete(taskId);
        }
        refreshLinkStatus(task.getProcessInstanceId());
    }

    /**
     * 驳回：把当前活动回退到指定节点（如会签退回初审、初审退回发起）。
     * <p>目标节点必须在流程模型中存在；跨流程/跨视图的跳转一律拒绝。</p>
     */
    @Transactional
    public void reject(String taskId, WfDtos.RejectReq req, Long userId) {
        Task task = requireOwnTask(taskId, userId);
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        if (model == null || model.getFlowElement(req.targetActivityId()) == null) {
            throw BizException.of(ErrorCode.WF_VARIABLE_INVALID, "驳回目标节点不存在：" + req.targetActivityId());
        }
        if (req.targetActivityId().equals(task.getTaskDefinitionKey())) {
            throw BizException.of(ErrorCode.WF_VARIABLE_INVALID, "不能驳回当前节点自身");
        }
        saveComment(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(), "REJECT",
                req.comment(), userId);
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveActivityIdTo(task.getTaskDefinitionKey(), req.targetActivityId())
                .changeState();
        refreshLinkStatus(task.getProcessInstanceId());
    }

    /** 撤回：仅发起人可撤回未结束的流程，撤回即删除实例并留痕。 */
    @Transactional
    public void withdraw(String processInstanceId, WfDtos.WithdrawReq req, Long userId) {
        List<Long> starters = jdbcTemplate.queryForList(
                "SELECT started_by FROM wf_instance_link WHERE process_instance_id = ?", Long.class, processInstanceId);
        if (starters.isEmpty()) {
            throw BizException.of(ErrorCode.WF_INSTANCE_NOT_FOUND, "流程实例不存在：" + processInstanceId);
        }
        if (!starters.get(0).equals(userId)) {
            throw BizException.of(ErrorCode.WF_OPERATION_DENIED, "只有发起人可以撤回该流程");
        }
        boolean running = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).count() > 0;
        if (!running) {
            throw BizException.of(ErrorCode.WF_INSTANCE_NOT_FOUND, "流程已结束，无法撤回");
        }
        saveComment(processInstanceId, null, null, "WITHDRAW", req == null ? null : req.reason(), userId);
        runtimeService.deleteProcessInstance(processInstanceId, "发起人撤回");
        markLink(processInstanceId, "TERMINATED");
    }

    // ------------------------------------------------------------------ 查询

    public WfDtos.InstanceDetail instance(String processInstanceId) {
        List<WfDtos.InstanceDetail> rows = jdbcTemplate.query("""
                        SELECT l.process_instance_id, l.process_key, l.business_key, l.object_type, l.object_id,
                               l.status, l.started_by, l.started_at, l.ended_at, o.code, o.name
                          FROM wf_instance_link l
                          LEFT JOIN plm_object o ON o.id = l.object_id
                         WHERE l.process_instance_id = ?
                        """,
                (rs, rowNum) -> new WfDtos.InstanceDetail(
                        rs.getString("process_instance_id"), rs.getString("process_key"),
                        rs.getString("business_key"), rs.getString("object_type"), rs.getLong("object_id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("status"),
                        rs.getLong("started_by"),
                        rs.getObject("started_at", OffsetDateTime.class),
                        rs.getObject("ended_at", OffsetDateTime.class),
                        List.of(), List.of()),
                processInstanceId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.WF_INSTANCE_NOT_FOUND, "流程实例不存在：" + processInstanceId);
        }
        WfDtos.InstanceDetail base = rows.get(0);
        boolean running = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).count() > 0;
        return new WfDtos.InstanceDetail(base.processInstanceId(), base.processKey(), base.businessKey(),
                base.objectType(), base.objectId(), base.objectCode(), base.objectName(),
                running ? "RUNNING" : finalStatus(processInstanceId), base.startedBy(),
                base.startedAt(), base.endedAt(), activeTasks(processInstanceId), comments(processInstanceId));
    }

    public List<WfDtos.BindingRow> bindings(String objectType) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, object_type, category, process_key, name, priority, enabled
                  FROM wf_binding
                """);
        List<Object> args = new ArrayList<>();
        if (objectType != null && !objectType.isBlank()) {
            sql.append(" WHERE object_type = ?");
            args.add(objectType);
        }
        sql.append(" ORDER BY object_type, priority, id");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new WfDtos.BindingRow(
                rs.getLong("id"), rs.getString("object_type"), rs.getString("category"),
                rs.getString("process_key"), rs.getString("name"), rs.getInt("priority"),
                rs.getBoolean("enabled")), args.toArray());
    }

    // ------------------------------------------------------------------ 内部

    private Task requireOwnTask(String taskId, Long userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw BizException.of(ErrorCode.WF_TASK_NOT_FOUND, "任务不存在或已被处理：" + taskId);
        }
        if (!String.valueOf(userId).equals(task.getAssignee())) {
            throw BizException.of(ErrorCode.WF_TASK_NOT_ASSIGNEE, "任务不属于当前用户：" + taskId);
        }
        return task;
    }

    private List<WfDtos.TaskRow> activeTasks(String processInstanceId) {
        return toRows(taskService.createTaskQuery().processInstanceId(processInstanceId)
                .orderByTaskCreateTime().asc().list());
    }

    private List<WfDtos.TaskRow> toRows(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        List<String> instanceIds = tasks.stream().map(Task::getProcessInstanceId).distinct().toList();
        Map<String, LinkInfo> links = loadLinks(instanceIds);
        List<WfDtos.TaskRow> rows = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            LinkInfo info = links.get(task.getProcessInstanceId());
            rows.add(new WfDtos.TaskRow(task.getId(), task.getTaskDefinitionKey(), task.getName(),
                    task.getAssignee(), task.getProcessInstanceId(),
                    info == null ? null : info.objectType(),
                    info == null ? null : info.objectId(),
                    info == null ? null : info.code(),
                    info == null ? null : info.name(),
                    toOffsetDateTime(task.getCreateTime())));
        }
        return rows;
    }

    private Map<String, LinkInfo> loadLinks(List<String> processInstanceIds) {
        if (processInstanceIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", Collections.nCopies(processInstanceIds.size(), "?"));
        String sql = """
                SELECT l.process_instance_id, l.object_type, l.object_id, o.code, o.name
                  FROM wf_instance_link l
                  LEFT JOIN plm_object o ON o.id = l.object_id
                 WHERE l.process_instance_id IN (%s)
                """.formatted(placeholders);
        Map<String, LinkInfo> result = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            result.put(rs.getString("process_instance_id"),
                    new LinkInfo(rs.getString("object_type"), rs.getLong("object_id"),
                            rs.getString("code"), rs.getString("name")));
        }, processInstanceIds.toArray());
        return result;
    }

    private List<WfDtos.CommentRow> comments(String processInstanceId) {
        return jdbcTemplate.query("""
                        SELECT task_id, task_key, action, comment_text, user_id, created_at
                          FROM wf_comment
                         WHERE process_instance_id = ?
                         ORDER BY created_at, id
                        """,
                (rs, rowNum) -> new WfDtos.CommentRow(rs.getString("task_id"), rs.getString("task_key"),
                        rs.getString("action"), rs.getString("comment_text"), rs.getLong("user_id"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                processInstanceId);
    }

    private void saveComment(String processInstanceId, String taskId, String taskKey,
                             String action, String comment, Long userId) {
        jdbcTemplate.update("""
                        INSERT INTO wf_comment (id, process_instance_id, task_id, task_key, action,
                                                comment_text, user_id, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                IdWorker.getId(), processInstanceId, taskId, taskKey, action,
                comment == null ? null : comment.substring(0, Math.min(comment.length(), 2000)),
                userId, OffsetDateTime.now());
    }

    private void refreshLinkStatus(String processInstanceId) {
        boolean running = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).count() > 0;
        if (running) {
            markLink(processInstanceId, "RUNNING");
        } else {
            markLink(processInstanceId, finalStatus(processInstanceId));
        }
    }

    /** 已结束实例的终态：被删除的记为 TERMINATED，正常走到终点的记为 COMPLETED。 */
    private String finalStatus(String processInstanceId) {
        List<String> status = jdbcTemplate.queryForList(
                "SELECT status FROM wf_instance_link WHERE process_instance_id = ?", String.class, processInstanceId);
        if (!status.isEmpty() && "TERMINATED".equals(status.get(0))) {
            return "TERMINATED";
        }
        return "COMPLETED";
    }

    private void markLink(String processInstanceId, String status) {
        if ("RUNNING".equals(status)) {
            jdbcTemplate.update("UPDATE wf_instance_link SET status = ? WHERE process_instance_id = ?",
                    status, processInstanceId);
        } else {
            jdbcTemplate.update("""
                    UPDATE wf_instance_link SET status = ?, ended_at = COALESCE(ended_at, now())
                     WHERE process_instance_id = ?
                    """, status, processInstanceId);
        }
    }

    private void requireObject(Long objectId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM plm_object WHERE id = ? AND deleted = false", Long.class, objectId);
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "业务对象不存在：" + objectId);
        }
    }

    private String resolveProcessKey(String objectType, String processKey) {
        if (processKey != null && !processKey.isBlank()) {
            return processKey;
        }
        List<String> keys = jdbcTemplate.queryForList("""
                SELECT process_key FROM wf_binding
                 WHERE object_type = ? AND enabled = true
                 ORDER BY priority, id
                 LIMIT 1
                """, String.class, objectType);
        if (keys.isEmpty()) {
            throw BizException.of(ErrorCode.WF_BINDING_NOT_FOUND, "对象类型未配置流程：" + objectType);
        }
        return keys.get(0);
    }

    private OffsetDateTime toOffsetDateTime(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    /** 供测试/白盒断言：确认流程实例仍在运行。 */
    public boolean isRunning(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count() > 0;
    }

    /** 供上层复用：按任务 id 查所属流程实例（不存在返回 null）。 */
    public String processInstanceOf(String taskId) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            return task == null ? null : task.getProcessInstanceId();
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }
}
