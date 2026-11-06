import { http, request } from './http'

/** 分页结果（与后端 PageResult 对齐）。 */
export interface PageResult<T> {
  total: number
  items: T[]
}

/**
 * 执行人声明（与后端 AssigneeSpec 对齐）。
 * type 取值见 {@link workflowApi.assigneeTypes}：
 * USER / ROLE / ORG / GROUP / ROLE_GROUP / INITIATOR / FORM_FIELD / PROJECT_ROLE。
 */
export interface AssigneeSpec {
  type: string
  /** 主键集合：USER=用户 id，ROLE=角色 id，ORG=组织 id，GROUP=组 id，ROLE_GROUP=[角色 id, 组 id]。 */
  ids?: string[]
  /** FORM_FIELD 时为流程变量名。 */
  expression?: string | null
}

/** 发起流程请求：approverId/approverIds 与 *Assignees 两种写法二选一，同时传以 Assignees 为准。 */
export interface StartReq {
  objectType: string
  objectId: string
  processKey?: string
  /** 初审人。 */
  approverId?: string
  /** 会签人。 */
  approverIds?: string[]
  /** 初审人执行人声明，解析后必须恰好 1 人。 */
  initialAssignees?: AssigneeSpec[]
  /** 会签人执行人声明，可解析出多人（全票通过）。 */
  countersignAssignees?: AssigneeSpec[]
  variables?: Record<string, unknown>
  remark?: string
}

export interface StartResp {
  processInstanceId: string
  processKey: string
  businessKey: string | null
  activeTasks: TaskRow[]
}

/** 待办行：附带业务对象上下文，可直接跳转详情。 */
export interface TaskRow {
  taskId: string
  taskKey: string
  name: string
  assignee: string | null
  processInstanceId: string
  objectType: string | null
  objectId: string | null
  objectCode: string | null
  objectName: string | null
  createTime: string | null
}

/** 已办行（历史任务）。 */
export interface DoneTaskRow {
  taskId: string
  taskKey: string
  name: string
  assignee: string | null
  processInstanceId: string
  objectType: string | null
  objectId: string | null
  objectCode: string | null
  objectName: string | null
  startTime: string | null
  endTime: string | null
  durationInMillis: number | null
  deleteReason: string | null
}

/** 我发起的流程行。 */
export interface InstanceRow {
  processInstanceId: string
  processKey: string
  objectType: string
  objectId: string
  objectCode: string | null
  objectName: string | null
  status: string
  startedBy: string
  startedAt: string | null
  endedAt: string | null
  currentActivities: string[]
}

export interface CommentRow {
  taskId: string | null
  taskKey: string | null
  action: string
  comment: string | null
  userId: string | null
  userName: string | null
  createdAt: string | null
}

export interface InstanceDetail {
  processInstanceId: string
  processKey: string
  businessKey: string | null
  objectType: string
  objectId: string
  objectCode: string | null
  objectName: string | null
  status: string
  startedBy: string
  startedByName: string | null
  startedAt: string | null
  endedAt: string | null
  activeTasks: TaskRow[]
  comments: CommentRow[]
}

/** 已走过的用户任务节点（驳回目标候选）。 */
export interface ActivityRow {
  key: string
  name: string | null
  firstEnteredAt: string | null
}

/** 电子签名：verifyResult = VALID / CHANGED / UNKNOWN。 */
export interface SignatureRow {
  id: string
  objectType: string
  objectId: string
  revisionLabel: string | null
  taskId: string | null
  taskKey: string | null
  processInstanceId: string | null
  signerId: string
  signerName: string | null
  meaning: string
  fileId: string | null
  fileSha256: string | null
  signTime: string | null
  verifyResult: string
}

export interface BatchResult {
  total: number
  succeeded: number
  failed: number
  messages: string[]
}

export interface AssigneeTypeRow {
  type: string
  label: string
}

/** 选人项（仅三列，普通审批人可检索）。 */
export interface ActorUserRow {
  id: string
  username: string
  displayName: string
}

export interface DelegateRow {
  id: string
  ownerId: string
  ownerName: string | null
  delegateId: string
  delegateName: string | null
  startAt: string
  endAt: string
  processKey: string | null
  enabled: boolean
  remark: string | null
}

export interface DelegateReq {
  delegateId: string
  startAt: string
  endAt: string
  processKey?: string | null
  remark?: string | null
}

/** 流程绑定（对象类型 → 流程定义）。 */
export interface BindingRow {
  id: string
  objectType: string
  category: string
  processKey: string
  name: string
  priority: number
  enabled: boolean
}

/** 一致性巡检问题项：level = ERROR（必须处理）/ WARN（需人工确认）。 */
export interface ConsistencyIssue {
  code: string
  level: string
  objectType: string
  objectId: string
  processInstanceId: string | null
  message: string
}

export interface ConsistencyReport {
  checkedAt: string | null
  issueCount: number
  autoFixed: string[]
  issues: ConsistencyIssue[]
}

/** 超时催办结果：overdue=超期任务数，reminded=本次新提醒，skipped=窗口内已提醒过。 */
export interface RemindResult {
  checkedAt: string | null
  overdue: number
  reminded: number
  skipped: number
  tasks: TaskRow[]
}

export const workflowApi = {
  start(body: StartReq) {
    return http.post<StartResp>('/workflow/instances', body)
  },
  todo(page = 1, size = 20) {
    return http.get<PageResult<TaskRow>>('/workflow/tasks', { page, size })
  },
  done(page = 1, size = 20) {
    return http.get<PageResult<DoneTaskRow>>('/workflow/tasks/done', { page, size })
  },
  started(page = 1, size = 20) {
    return http.get<PageResult<InstanceRow>>('/workflow/instances/started', { page, size })
  },
  /** 某个业务对象的流程实例（对象详情「审批」页签）。 */
  objectInstances(objectType: string, objectId: string) {
    return http.get<InstanceRow[]>('/workflow/instances', { objectType, objectId })
  },
  instance(processInstanceId: string) {
    return http.get<InstanceDetail>(`/workflow/instances/${processInstanceId}`)
  },
  activities(processInstanceId: string) {
    return http.get<ActivityRow[]>(`/workflow/instances/${processInstanceId}/activities`)
  },
  complete(taskId: string, comment?: string) {
    return http.post<void>(`/workflow/tasks/${taskId}/complete`, { comment })
  },
  reject(taskId: string, targetActivityId: string, comment?: string) {
    return http.post<void>(`/workflow/tasks/${taskId}/reject`, { targetActivityId, comment })
  },
  batchComplete(taskIds: string[], comment?: string) {
    return http.post<BatchResult>('/workflow/tasks/batch-complete', { taskIds, comment })
  },
  withdraw(processInstanceId: string, reason?: string) {
    return http.post<void>(`/workflow/instances/${processInstanceId}/withdraw`, { reason })
  },
  addSigner(taskId: string, signerId: string, comment?: string) {
    return http.post<void>(`/workflow/tasks/${taskId}/add-signer`, { signerId, comment })
  },
  /** 超时催办（POST + query，无请求体）。 */
  remind(hours = 24) {
    return request<RemindResult>('/workflow/tasks/remind', { method: 'POST', query: { hours } })
  },
  signatures(objectType: string, objectId: string) {
    return http.get<SignatureRow[]>('/workflow/signatures', { objectType, objectId })
  },
  bindings(objectType?: string) {
    return http.get<BindingRow[]>('/workflow/bindings', { objectType })
  },
  /** 一致性巡检（POST + query，fix=true 时自动修正可判定漂移）。 */
  checkConsistency(fix = false) {
    return request<ConsistencyReport>('/workflow/consistency/check', { method: 'POST', query: { fix } })
  },
  assigneeTypes() {
    return http.get<AssigneeTypeRow[]>('/workflow/assignee-types')
  },
  delegates() {
    return http.get<DelegateRow[]>('/workflow/delegates')
  },
  createDelegate(body: DelegateReq) {
    return http.post<DelegateRow>('/workflow/delegates', body)
  },
  removeDelegate(id: string) {
    return http.delete<void>(`/workflow/delegates/${id}`)
  },
  /**
   * 选人（加签 / 委托 / 发起审批人）。
   * 走工作流专用检索接口，权限 `wf.task.read`，普通审批人可用；
   * 不复用 `/system/users`（类级要求 `system.user.manage`，会 403）。
   */
  actorUsers(keyword?: string, limit = 50) {
    return http.get<ActorUserRow[]>('/workflow/actors/users', { keyword, limit })
  }
}
