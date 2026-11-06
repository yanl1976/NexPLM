package com.nexplm.platform.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.workflow.api.WfDtos;
import com.nexplm.platform.workflow.service.assignee.AssigneeContext;
import com.nexplm.platform.workflow.service.assignee.AssigneeResolverRegistry;
import com.nexplm.platform.workflow.service.plugin.WfPluginContext;
import com.nexplm.platform.workflow.service.plugin.WfPluginRegistry;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final HistoryService historyService;
    private final JdbcTemplate jdbcTemplate;
    private final AssigneeResolverRegistry assigneeResolvers;
    private final WfPluginRegistry pluginRegistry;

    public WorkflowService(RuntimeService runtimeService, TaskService taskService,
                           RepositoryService repositoryService, HistoryService historyService,
                           JdbcTemplate jdbcTemplate, AssigneeResolverRegistry assigneeResolvers,
                           WfPluginRegistry pluginRegistry) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
        this.jdbcTemplate = jdbcTemplate;
        this.assigneeResolvers = assigneeResolvers;
        this.pluginRegistry = pluginRegistry;
    }

    /** 业务对象与流程实例的关联信息（用于把任务行补全为可展示的业务上下文）。 */
    private record LinkInfo(String objectType, Long objectId, String code, String name, String processKey) {
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
        Map<String, Object> variables = req.variables() == null ? new HashMap<>() : new HashMap<>(req.variables());
        AssigneeContext context = new AssigneeContext(req.objectType(), req.objectId(), userId, variables);

        Long initialApprover = resolveInitialApprover(req, context);
        List<String> approvers = resolveCountersignApprovers(req, context).stream()
                .map(String::valueOf).toList();

        variables.put("approver", String.valueOf(initialApprover));
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
        firePlugin(instance.getId(), WfPluginContext.ACTION_STARTED, userId, null);

        return new WfDtos.StartResp(instance.getId(), processKey, businessKey, activeTasks(instance.getId()));
    }

    // ------------------------------------------------------------------ 待办

    public WfDtos.PageResult<WfDtos.TaskRow> myTasks(Long userId, int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<String> assignees = taskAssignees(userId);
        long total = assigneeQuery(assignees).count();
        List<Task> tasks = assigneeQuery(assignees)
                .orderByTaskCreateTime().desc()
                .listPage((pageNo - 1) * pageSize, pageSize);
        return new WfDtos.PageResult<>(total, toRows(tasks));
    }

    /** 待办范围：本人 + 生效委托给我的 owner（代办理，assignee 不变）。 */
    private List<String> taskAssignees(Long userId) {
        List<Long> owners = jdbcTemplate.queryForList("""
                SELECT owner_id FROM wf_delegate
                 WHERE delegate_id = ? AND enabled = true AND start_at <= now() AND end_at >= now()
                """, Long.class, userId);
        LinkedHashSet<String> assignees = new LinkedHashSet<>();
        assignees.add(String.valueOf(userId));
        owners.forEach(owner -> assignees.add(String.valueOf(owner)));
        return List.copyOf(assignees);
    }

    private TaskQuery assigneeQuery(List<String> assignees) {
        if (assignees.size() == 1) {
            return taskService.createTaskQuery().taskAssignee(assignees.get(0));
        }
        TaskQuery query = taskService.createTaskQuery().or();
        for (String assignee : assignees) {
            query = query.taskAssignee(assignee);
        }
        return query.endOr();
    }

    // ------------------------------------------------------------------ 办理

    @Transactional
    public void complete(String taskId, WfDtos.CompleteReq req, Long userId) {
        Task task = requireOperableTask(taskId, userId);
        saveComment(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(), "APPROVE",
                delegateNote(task, userId, req == null ? null : req.comment()), userId);
        signTask(task, userId, "APPROVE", req == null ? null : req.comment());
        if (req != null && req.variables() != null && !req.variables().isEmpty()) {
            taskService.complete(taskId, req.variables());
        } else {
            taskService.complete(taskId);
        }
        String processInstanceId = task.getProcessInstanceId();
        boolean running = refreshLinkStatus(processInstanceId);
        if (!running) {
            firePlugin(processInstanceId, WfPluginContext.ACTION_COMPLETED, userId, task.getTaskDefinitionKey());
        }
    }

    /**
     * 驳回：把当前活动回退到指定节点（如会签退回初审、初审退回发起）。
     * <p>目标节点必须在流程模型中存在；跨流程/跨视图的跳转一律拒绝。</p>
     */
    @Transactional
    public void reject(String taskId, WfDtos.RejectReq req, Long userId) {
        Task task = requireOperableTask(taskId, userId);
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        if (model == null || model.getFlowElement(req.targetActivityId()) == null) {
            throw BizException.of(ErrorCode.WF_VARIABLE_INVALID, "驳回目标节点不存在：" + req.targetActivityId());
        }
        if (req.targetActivityId().equals(task.getTaskDefinitionKey())) {
            throw BizException.of(ErrorCode.WF_VARIABLE_INVALID, "不能驳回当前节点自身");
        }
        saveComment(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(), "REJECT",
                delegateNote(task, userId, req.comment()), userId);
        signTask(task, userId, "REJECT", req.comment());
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
        firePlugin(processInstanceId, WfPluginContext.ACTION_TERMINATED, userId, null);
    }

    /**
     * 加签：给会签（多实例）节点追加一名审批人。
     * <p>走引擎运行时 API {@code addMultiInstanceExecution}，父执行取当前任务执行的上级（多实例根执行）。</p>
     */
    @Transactional
    public void addSigner(String taskId, WfDtos.AddSignerReq req, Long userId) {
        Task task = requireOperableTask(taskId, userId);
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        FlowElement element = model == null ? null : model.getFlowElement(task.getTaskDefinitionKey());
        if (!(element instanceof UserTask userTask) || userTask.getLoopCharacteristics() == null) {
            throw BizException.of(ErrorCode.WF_OPERATION_DENIED, "只有会签（多实例）节点可以加签");
        }
        requireUser(req.signerId(), "加签人不存在");
        runtimeService.addMultiInstanceExecution(task.getTaskDefinitionKey(), multiInstanceRootParent(task),
                Map.of("countersigner", String.valueOf(req.signerId())));
        saveComment(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(), "ADD_SIGNER",
                req.comment(), userId);
    }

    /**
     * 求「多实例根执行的父执行」id。
     *
     * <p>引擎的 {@code AddMultiInstanceExecutionCmd.searchForMultiInstanceActivity} 是
     * <b>从传入执行的子执行里</b>递归找 {@code isMultiInstanceRoot} 的执行，所以必须传多实例根的
     * <b>父执行</b>（通常是流程实例执行），传实例执行或根本身都会报
     * 「No multi instance execution found for activity id」。</p>
     *
     * <p>用 {@code act_ru_execution} 沿 parent 链上溯定位，兼容子流程内嵌套会签。</p>
     */
    private String multiInstanceRootParent(Task task) {
        String current = task.getExecutionId();
        for (int depth = 0; depth < 10 && current != null; depth++) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT parent_id_, is_mi_root_ FROM act_ru_execution WHERE id_ = ?", current);
            if (rows.isEmpty()) {
                break;
            }
            Object parent = rows.get(0).get("parent_id_");
            Object miRoot = rows.get(0).get("is_mi_root_");
            if (Boolean.TRUE.equals(miRoot) || Boolean.valueOf(String.valueOf(miRoot))) {
                return parent == null ? task.getProcessInstanceId() : String.valueOf(parent);
            }
            current = parent == null ? null : String.valueOf(parent);
        }
        return task.getProcessInstanceId();
    }

    // ------------------------------------------------------------------ 委托

    @Transactional
    public WfDtos.DelegateRow createDelegate(WfDtos.DelegateReq req, Long userId) {
        Long ownerId = req.ownerId() == null ? userId : req.ownerId();
        if (!ownerId.equals(userId)) {
            throw BizException.of(ErrorCode.WF_OPERATION_DENIED, "只能设置自己的委托");
        }
        if (ownerId.equals(req.delegateId())) {
            throw BizException.of(ErrorCode.WF_VARIABLE_INVALID, "不能把待办委托给自己");
        }
        if (!req.endAt().isAfter(req.startAt())) {
            throw BizException.of(ErrorCode.WF_VARIABLE_INVALID, "失效时间必须晚于生效时间");
        }
        requireUser(req.delegateId(), "受托人不存在");
        long id = IdWorker.getId();
        String processKey = req.processKey() == null || req.processKey().isBlank()
                ? null : req.processKey().trim();
        jdbcTemplate.update("""
                        INSERT INTO wf_delegate (id, owner_id, delegate_id, start_at, end_at,
                                                 process_key, enabled, remark, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, true, ?, ?)
                        """,
                id, ownerId, req.delegateId(), req.startAt(), req.endAt(), processKey,
                req.remark(), userId);
        return delegateById(id);
    }

    /** 我设置的 + 我收到的委托。 */
    public List<WfDtos.DelegateRow> delegates(Long userId) {
        return jdbcTemplate.query("""
                        SELECT d.id, d.owner_id, ou.display_name AS owner_name, d.delegate_id,
                               du.display_name AS delegate_name, d.start_at, d.end_at,
                               d.process_key, d.enabled, d.remark
                          FROM wf_delegate d
                          LEFT JOIN sys_user ou ON ou.id = d.owner_id
                          LEFT JOIN sys_user du ON du.id = d.delegate_id
                         WHERE d.owner_id = ? OR d.delegate_id = ?
                         ORDER BY d.start_at DESC
                        """,
                (rs, rowNum) -> delegateRow(rs),
                userId, userId);
    }

    @Transactional
    public void removeDelegate(Long id, Long userId) {
        int rows = jdbcTemplate.update("DELETE FROM wf_delegate WHERE id = ? AND owner_id = ?", id, userId);
        if (rows == 0) {
            throw BizException.of(ErrorCode.WF_OPERATION_DENIED, "委托不存在或无权删除：" + id);
        }
    }

    private WfDtos.DelegateRow delegateById(Long id) {
        List<WfDtos.DelegateRow> rows = jdbcTemplate.query("""
                        SELECT d.id, d.owner_id, ou.display_name AS owner_name, d.delegate_id,
                               du.display_name AS delegate_name, d.start_at, d.end_at,
                               d.process_key, d.enabled, d.remark
                          FROM wf_delegate d
                          LEFT JOIN sys_user ou ON ou.id = d.owner_id
                          LEFT JOIN sys_user du ON du.id = d.delegate_id
                         WHERE d.id = ?
                        """,
                (rs, rowNum) -> delegateRow(rs), id);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "委托不存在：" + id);
        }
        return rows.get(0);
    }

    private WfDtos.DelegateRow delegateRow(ResultSet rs) throws SQLException {
        return new WfDtos.DelegateRow(rs.getLong("id"), rs.getLong("owner_id"),
                rs.getString("owner_name"), rs.getLong("delegate_id"), rs.getString("delegate_name"),
                rs.getObject("start_at", OffsetDateTime.class), rs.getObject("end_at", OffsetDateTime.class),
                rs.getString("process_key"), rs.getBoolean("enabled"), rs.getString("remark"));
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

    /** 已办：历史任务（按结束时间倒序）。 */
    public WfDtos.PageResult<WfDtos.DoneTaskRow> doneTasks(Long userId, int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String assignee = String.valueOf(userId);
        long total = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(assignee).finished().count();
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(assignee).finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .listPage((pageNo - 1) * pageSize, pageSize);
        Map<String, LinkInfo> links = loadLinks(tasks.stream()
                .map(HistoricTaskInstance::getProcessInstanceId).distinct().toList());
        List<WfDtos.DoneTaskRow> rows = new ArrayList<>(tasks.size());
        for (HistoricTaskInstance task : tasks) {
            LinkInfo info = links.get(task.getProcessInstanceId());
            rows.add(new WfDtos.DoneTaskRow(task.getId(), task.getTaskDefinitionKey(), task.getName(),
                    task.getAssignee(), task.getProcessInstanceId(),
                    info == null ? null : info.objectType(),
                    info == null ? null : info.objectId(),
                    info == null ? null : info.code(),
                    info == null ? null : info.name(),
                    toOffsetDateTime(task.getCreateTime()), toOffsetDateTime(task.getEndTime()),
                    task.getDurationInMillis(), task.getDeleteReason()));
        }
        return new WfDtos.PageResult<>(total, rows);
    }

    /** 我发起的流程：附带当前活动节点名称，便于判断卡在哪一步。 */
    public WfDtos.PageResult<WfDtos.InstanceRow> startedInstances(Long userId, int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM wf_instance_link WHERE started_by = ?", Long.class, userId);
        List<WfDtos.InstanceRow> rows = jdbcTemplate.query("""
                        SELECT l.process_instance_id, l.process_key, l.object_type, l.object_id, l.status,
                               l.started_by, l.started_at, l.ended_at, o.code, o.name
                          FROM wf_instance_link l
                          LEFT JOIN plm_object o ON o.id = l.object_id
                         WHERE l.started_by = ?
                         ORDER BY l.started_at DESC
                         LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> new WfDtos.InstanceRow(rs.getString("process_instance_id"),
                        rs.getString("process_key"), rs.getString("object_type"), rs.getLong("object_id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("status"),
                        rs.getLong("started_by"),
                        rs.getObject("started_at", OffsetDateTime.class),
                        rs.getObject("ended_at", OffsetDateTime.class), List.of()),
                userId, pageSize, (pageNo - 1) * pageSize);
        if (rows.isEmpty()) {
            return new WfDtos.PageResult<>(total == null ? 0 : total, rows);
        }
        List<String> instanceIds = rows.stream().map(WfDtos.InstanceRow::processInstanceId).toList();
        Map<String, List<String>> activities = loadActiveActivities(instanceIds);
        List<WfDtos.InstanceRow> enriched = rows.stream()
                .map(row -> new WfDtos.InstanceRow(row.processInstanceId(), row.processKey(),
                        row.objectType(), row.objectId(), row.objectCode(), row.objectName(), row.status(),
                        row.startedBy(), row.startedAt(), row.endedAt(),
                        activities.getOrDefault(row.processInstanceId(), List.of())))
                .toList();
        return new WfDtos.PageResult<>(total == null ? 0 : total, enriched);
    }

    /**
     * 批量办理：逐条独立处理，一条失败不影响其它，失败项回带原因。
     * <p>刻意不加类级事务：单条办理各自提交，避免某条异常把整批标记 rollback-only。</p>
     */
    public WfDtos.BatchResult batchComplete(List<String> taskIds, String comment, Long userId) {
        int succeeded = 0;
        List<String> messages = new ArrayList<>();
        for (String taskId : taskIds) {
            try {
                complete(taskId, new WfDtos.CompleteReq(comment, null), userId);
                succeeded++;
            } catch (BizException ex) {
                messages.add(taskId + "：" + ex.getMessage());
            }
        }
        return new WfDtos.BatchResult(taskIds.size(), succeeded, taskIds.size() - succeeded, messages);
    }

    /**
     * 超时催办：扫描超过 {@code hours} 小时未办理的任务并写 {@code REMIND} 留痕。
     * <p>幂等：同一任务在窗口内已有 REMIND 则跳过，可被外部定时任务反复调用。
     * 真正的外部通知（站内信/企微）留待 P8 通知模块消费 wf_comment。</p>
     */
    @Transactional
    public WfDtos.RemindResult remindOverdueTasks(int hours, Long operatorId) {
        int window = Math.min(Math.max(hours, 1), 24 * 30);
        Date before = Date.from(Instant.now().minus(window, ChronoUnit.HOURS));
        List<Task> overdueTasks = taskService.createTaskQuery()
                .taskCreatedBefore(before)
                .orderByTaskCreateTime().asc()
                .list();
        int reminded = 0;
        int skipped = 0;
        for (Task task : overdueTasks) {
            Long existing = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM wf_comment
                     WHERE task_id = ? AND action = 'REMIND'
                       AND created_at >= now() - make_interval(hours => ?)
                    """, Long.class, task.getId(), window);
            if (existing != null && existing > 0) {
                skipped++;
                continue;
            }
            saveComment(task.getProcessInstanceId(), task.getId(), task.getTaskDefinitionKey(), "REMIND",
                    "任务已超过 " + window + " 小时未办理，请及时处理", operatorId);
            reminded++;
        }
        return new WfDtos.RemindResult(OffsetDateTime.now(), overdueTasks.size(), reminded, skipped,
                toRows(overdueTasks));
    }

    private Map<String, List<String>> loadActiveActivities(List<String> processInstanceIds) {
        String placeholders = String.join(",", Collections.nCopies(processInstanceIds.size(), "?"));
        Map<String, List<String>> result = new HashMap<>();
        jdbcTemplate.query("""
                SELECT proc_inst_id_, name_ FROM act_ru_task
                 WHERE proc_inst_id_ IN (%s)
                 ORDER BY create_time_
                """.formatted(placeholders), rs -> {
            String pid = rs.getString("proc_inst_id_");
            result.computeIfAbsent(pid, key -> new ArrayList<>()).add(rs.getString("name_"));
        }, processInstanceIds.toArray());
        return result;
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

    // ------------------------------------------------------------------ 执行人

    /** 已注册的执行人解析类型（前端选择器用）。 */
    public List<WfDtos.AssigneeTypeRow> assigneeTypes() {
        return assigneeResolvers.supported().stream()
                .map(info -> new WfDtos.AssigneeTypeRow(info.type(), info.label()))
                .toList();
    }

    /** 初审人：优先执行人声明（必须解析为唯一用户），否则用显式 approverId。 */
    private Long resolveInitialApprover(WfDtos.StartReq req, AssigneeContext context) {
        if (req.initialAssignees() != null && !req.initialAssignees().isEmpty()) {
            List<Long> users = assigneeResolvers.resolve(req.initialAssignees(), context);
            if (users.size() != 1) {
                throw BizException.of(ErrorCode.WF_VARIABLE_INVALID,
                        "初审人必须解析为唯一用户，当前解析出 " + users.size() + " 人");
            }
            return users.get(0);
        }
        if (req.approverId() == null) {
            throw BizException.of(ErrorCode.WF_VARIABLE_INVALID,
                    "请指定初审人（approverId 或 initialAssignees）");
        }
        return req.approverId();
    }

    /** 会签人：优先执行人声明（可解析出多人），否则用显式 approverIds。 */
    private List<Long> resolveCountersignApprovers(WfDtos.StartReq req, AssigneeContext context) {
        if (req.countersignAssignees() != null && !req.countersignAssignees().isEmpty()) {
            return assigneeResolvers.resolve(req.countersignAssignees(), context);
        }
        if (req.approverIds() == null || req.approverIds().isEmpty()) {
            throw BizException.of(ErrorCode.WF_VARIABLE_INVALID,
                    "请指定会签人（approverIds 或 countersignAssignees）");
        }
        return req.approverIds().stream().distinct().toList();
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 校验任务可办理：本人，或在本人的生效委托区间内代 owner 办理。
     * <p>委托不改 assignee，只在权限层放行，避免引擎数据被改写后无法回退。</p>
     */
    private Task requireOperableTask(String taskId, Long userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw BizException.of(ErrorCode.WF_TASK_NOT_FOUND, "任务不存在或已被处理：" + taskId);
        }
        if (String.valueOf(userId).equals(task.getAssignee())) {
            return task;
        }
        if (task.getAssignee() != null && hasActiveDelegate(task, userId)) {
            return task;
        }
        throw BizException.of(ErrorCode.WF_TASK_NOT_ASSIGNEE, "任务不属于当前用户：" + taskId);
    }

    private boolean hasActiveDelegate(Task task, Long delegateId) {
        long ownerId;
        try {
            ownerId = Long.parseLong(task.getAssignee());
        } catch (NumberFormatException ex) {
            return false;
        }
        List<Long> hit = jdbcTemplate.queryForList("""
                SELECT COUNT(1) FROM wf_delegate
                 WHERE owner_id = ? AND delegate_id = ? AND enabled = true
                   AND start_at <= now() AND end_at >= now()
                   AND (process_key IS NULL OR process_key = ?)
                """, Long.class, ownerId, delegateId, processKeyOf(task));
        return !hit.isEmpty() && hit.get(0) > 0;
    }

    private String processKeyOf(Task task) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId()).singleResult();
        return definition == null ? null : definition.getKey();
    }

    /** 代办时在意见前加「代 XXX 办理」标记，便于审计区分。 */
    private String delegateNote(Task task, Long userId, String comment) {
        if (String.valueOf(userId).equals(task.getAssignee())) {
            return comment;
        }
        String prefix = "[代 " + displayName(task.getAssignee()) + " 办理]";
        return comment == null || comment.isBlank() ? prefix : prefix + " " + comment;
    }

    private String displayName(String assignee) {
        try {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT display_name FROM sys_user WHERE id = ?", String.class, Long.parseLong(assignee));
            return names.isEmpty() ? assignee : names.get(0);
        } catch (NumberFormatException ex) {
            return assignee;
        }
    }

    private void requireUser(Long userId, String message) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_user WHERE id = ? AND deleted = false", Long.class, userId);
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.WF_VARIABLE_INVALID, message + "：" + userId);
        }
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
                SELECT l.process_instance_id, l.object_type, l.object_id, l.process_key, o.code, o.name
                  FROM wf_instance_link l
                  LEFT JOIN plm_object o ON o.id = l.object_id
                 WHERE l.process_instance_id IN (%s)
                """.formatted(placeholders);
        Map<String, LinkInfo> result = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            result.put(rs.getString("process_instance_id"),
                    new LinkInfo(rs.getString("object_type"), rs.getLong("object_id"),
                            rs.getString("code"), rs.getString("name"), rs.getString("process_key")));
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

    /**
     * 电子签名留痕：办理时记录 who/when/what/meaning 与当时主文件摘要。
     * <p>只增不改；文件摘要用于事后核验「签名后文件是否被替换」。</p>
     */
    private void signTask(Task task, Long userId, String meaning, String remark) {
        LinkInfo info = loadLinks(List.of(task.getProcessInstanceId())).get(task.getProcessInstanceId());
        if (info == null) {
            return;
        }
        Long fileId = null;
        String fileSha = null;
        try {
            List<Map<String, Object>> files = jdbcTemplate.queryForList("""
                    SELECT d.main_file_id AS file_id, f.sha256 AS sha256
                      FROM doc_master d
                      LEFT JOIN file_object f ON f.id = d.main_file_id
                     WHERE d.object_id = ?
                    """, info.objectId());
            if (!files.isEmpty() && files.get(0).get("file_id") != null) {
                fileId = ((Number) files.get(0).get("file_id")).longValue();
                Object sha = files.get(0).get("sha256");
                fileSha = sha == null ? null : String.valueOf(sha).trim();
            }
        } catch (RuntimeException ex) {
            // 非文档对象、无主文件或表结构差异时不做文件绑定，不影响办理
            fileId = null;
            fileSha = null;
        }
        jdbcTemplate.update("""
                        INSERT INTO wf_signature (id, object_type, object_id, revision_label, task_id, task_key,
                                                  process_instance_id, signer_id, signer_name, sign_meaning,
                                                  file_id, file_sha256, sign_time, remark)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                IdWorker.getId(), info.objectType(), info.objectId(),
                revisionLabel(info.objectId()), task.getId(), task.getTaskDefinitionKey(),
                task.getProcessInstanceId(), userId, displayName(String.valueOf(userId)), meaning,
                fileId, fileSha, OffsetDateTime.now(),
                remark == null ? null : remark.substring(0, Math.min(remark.length(), 500)));
    }

    private String revisionLabel(Long objectId) {
        try {
            List<String> labels = jdbcTemplate.queryForList(
                    "SELECT revision || '.' || iteration FROM plm_object WHERE id = ?", String.class, objectId);
            return labels.isEmpty() ? null : labels.get(0);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** 查询对象签名记录，并实时比对主文件摘要给出核验结果（VALID/CHANGED/UNKNOWN）。 */
    public List<WfDtos.SignatureRow> signatures(String objectType, Long objectId) {
        List<WfDtos.SignatureRow> rows = jdbcTemplate.query("""
                        SELECT id, object_type, object_id, revision_label, task_id, task_key,
                               process_instance_id, signer_id, signer_name, sign_meaning,
                               file_id, file_sha256, sign_time
                          FROM wf_signature
                         WHERE object_type = ? AND object_id = ?
                         ORDER BY sign_time DESC, id DESC
                        """,
                (rs, rowNum) -> new WfDtos.SignatureRow(rs.getLong("id"), rs.getString("object_type"),
                        rs.getLong("object_id"), rs.getString("revision_label"), rs.getString("task_id"),
                        rs.getString("task_key"), rs.getString("process_instance_id"),
                        rs.getLong("signer_id"), rs.getString("signer_name"), rs.getString("sign_meaning"),
                        rs.getObject("file_id", Long.class),
                        rs.getString("file_sha256") == null ? null : rs.getString("file_sha256").trim(),
                        rs.getObject("sign_time", OffsetDateTime.class), "UNKNOWN"),
                objectType, objectId);
        if (rows.isEmpty()) {
            return rows;
        }
        Map<Long, String> currentShas = currentFileShas(rows);
        return rows.stream().map(row -> {
            String current = row.fileId() == null ? null : currentShas.get(row.fileId());
            return new WfDtos.SignatureRow(row.id(), row.objectType(), row.objectId(),
                    row.revisionLabel(), row.taskId(), row.taskKey(), row.processInstanceId(),
                    row.signerId(), row.signerName(), row.meaning(), row.fileId(), row.fileSha256(),
                    row.signTime(), verifySignature(row.fileSha256(), current));
        }).toList();
    }

    /**
     * 一致性巡检：核对「引擎运行实例 ↔ wf_instance_link ↔ 对象生命周期」三方是否自洽。
     * <p>{@code fix=true} 只修正可判定的漂移（孤儿 link 置终态）；生命周期不符仅报告，交人工判断。</p>
     */
    @Transactional
    public WfDtos.ConsistencyReport checkConsistency(boolean fix) {
        List<WfDtos.ConsistencyIssue> issues = new ArrayList<>();
        List<String> autoFixed = new ArrayList<>();

        List<Map<String, Object>> orphanLinks = jdbcTemplate.queryForList("""
                SELECT l.process_instance_id, l.object_type, l.object_id
                  FROM wf_instance_link l
                 WHERE l.status = 'RUNNING'
                   AND NOT EXISTS (SELECT 1 FROM act_ru_execution e
                                    WHERE e.proc_inst_id_ = l.process_instance_id)
                """);
        for (Map<String, Object> row : orphanLinks) {
            String pid = String.valueOf(row.get("process_instance_id"));
            issues.add(new WfDtos.ConsistencyIssue("ORPHAN_RUNNING_LINK", "ERROR",
                    String.valueOf(row.get("object_type")), toLong(row.get("object_id")), pid,
                    "流程实例已结束但业务关联仍标记为运行中"));
            if (fix) {
                HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(pid).singleResult();
                boolean terminated = historic == null || historic.getDeleteReason() != null;
                markLink(pid, terminated ? "TERMINATED" : "COMPLETED");
                autoFixed.add(pid + " → " + (terminated ? "TERMINATED" : "COMPLETED"));
            }
        }

        List<Map<String, Object>> unlinked = jdbcTemplate.queryForList("""
                SELECT DISTINCT e.proc_inst_id_ AS pid
                  FROM act_ru_execution e
                 WHERE e.parent_id_ IS NULL
                   AND NOT EXISTS (SELECT 1 FROM wf_instance_link l
                                    WHERE l.process_instance_id = e.proc_inst_id_)
                """);
        for (Map<String, Object> row : unlinked) {
            issues.add(new WfDtos.ConsistencyIssue("UNLINKED_INSTANCE", "ERROR", null, null,
                    String.valueOf(row.get("pid")), "存在运行中的流程实例但未关联业务对象"));
        }

        List<Map<String, Object>> stateMismatch = jdbcTemplate.queryForList("""
                SELECT l.process_instance_id, l.object_type, l.object_id, o.lifecycle_state
                  FROM wf_instance_link l
                  JOIN plm_object o ON o.id = l.object_id
                 WHERE l.status = 'RUNNING' AND o.lifecycle_state <> 'InWorkflow'
                """);
        for (Map<String, Object> row : stateMismatch) {
            issues.add(new WfDtos.ConsistencyIssue("LIFECYCLE_NOT_IN_WORKFLOW", "WARN",
                    String.valueOf(row.get("object_type")), toLong(row.get("object_id")),
                    String.valueOf(row.get("process_instance_id")),
                    "流程在运行但对象状态为 " + row.get("lifecycle_state")));
        }

        List<Map<String, Object>> stranded = jdbcTemplate.queryForList("""
                SELECT o.id, o.object_type
                  FROM plm_object o
                 WHERE o.lifecycle_state = 'InWorkflow' AND o.deleted = false
                   AND NOT EXISTS (SELECT 1 FROM wf_instance_link l
                                    WHERE l.object_id = o.id AND l.status = 'RUNNING')
                """);
        for (Map<String, Object> row : stranded) {
            issues.add(new WfDtos.ConsistencyIssue("STRANDED_IN_WORKFLOW", "WARN",
                    String.valueOf(row.get("object_type")), toLong(row.get("id")), null,
                    "对象处于审批中但没有运行中的流程"));
        }

        return new WfDtos.ConsistencyReport(OffsetDateTime.now(), issues.size(), autoFixed, issues);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private Map<Long, String> currentFileShas(List<WfDtos.SignatureRow> rows) {
        List<Long> fileIds = rows.stream().map(WfDtos.SignatureRow::fileId)
                .filter(Objects::nonNull).distinct().toList();
        if (fileIds.isEmpty()) {
            return new HashMap<>();
        }
        String placeholders = String.join(",", Collections.nCopies(fileIds.size(), "?"));
        Map<Long, String> result = new HashMap<>();
        jdbcTemplate.query("SELECT id, sha256 FROM file_object WHERE id IN (" + placeholders + ")", rs -> {
            String sha = rs.getString("sha256");
            result.put(rs.getLong("id"), sha == null ? null : sha.trim());
        }, fileIds.toArray());
        return result;
    }

    private String verifySignature(String recorded, String current) {
        if (recorded == null || recorded.isBlank()) {
            return "UNKNOWN";
        }
        if (current == null) {
            return "CHANGED";
        }
        return recorded.equalsIgnoreCase(current) ? "VALID" : "CHANGED";
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

    /** 回写实例状态，返回是否仍在运行（供判断是否触发「流程完成」插件）。 */
    private boolean refreshLinkStatus(String processInstanceId) {
        boolean running = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).count() > 0;
        if (running) {
            markLink(processInstanceId, "RUNNING");
        } else {
            markLink(processInstanceId, finalStatus(processInstanceId));
        }
        return running;
    }

    /** 触发业务插件：事务内注册为提交后回调，异常与流程隔离。 */
    private void firePlugin(String processInstanceId, String action, Long operatorId, String taskKey) {
        LinkInfo info = loadLinks(List.of(processInstanceId)).get(processInstanceId);
        if (info == null) {
            return;
        }
        pluginRegistry.fire(new WfPluginContext(info.objectType(), info.objectId(), processInstanceId,
                info.processKey(), action, operatorId, taskKey, Map.of()));
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
