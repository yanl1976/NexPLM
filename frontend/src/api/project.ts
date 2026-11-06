import { http } from './http'

/** 项目状态：草稿 / 执行中 / 暂停 / 已结项（终态）。 */
export type ProjectStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'CLOSED'

export const PROJECT_STATUS_LABEL: Record<ProjectStatus, string> = {
  DRAFT: '草稿',
  ACTIVE: '执行中',
  PAUSED: '暂停',
  CLOSED: '已结项'
}

/** 项目类型：研发 / 订单 / 其它。 */
export const PROJECT_TYPE_LABEL: Record<string, string> = {
  RD: '研发',
  ORDER: '订单',
  OTHER: '其它'
}

/** 成员角色。 */
export const MEMBER_ROLE_LABEL: Record<string, string> = {
  MANAGER: '负责人',
  MEMBER: '成员',
  VIEWER: '只读'
}

export interface EpsNode {
  id: string
  parentId?: string | null
  code: string
  name: string
  /** 物化路径 /父id/本id/，用于「查整棵子树」。 */
  path: string
  sortNo: number
  remark?: string | null
  /** 只统计直挂本节点的项目数（不含子树）。 */
  projectCount: number
  children?: EpsNode[]
}

export interface EpsSaveReq {
  code: string
  name: string
  parentId?: string | null
  sortNo?: number
  remark?: string | null
}

export interface ProjectMember {
  userId: string
  username?: string | null
  displayName?: string | null
  role: string
}

export interface MemberSaveReq {
  userId: string
  role?: string
}

export interface ProjectRow {
  id: string
  code: string
  name: string
  epsId?: string | null
  epsName?: string | null
  managerId?: string | null
  managerName?: string | null
  type: string
  status: ProjectStatus
  priority: number
  planStart?: string | null
  planEnd?: string | null
  actualStart?: string | null
  actualEnd?: string | null
  progress: number
  memberCount: number
  createdAt?: string | null
  updatedAt?: string | null
}

export interface ProjectDetail extends Omit<ProjectRow, 'memberCount'> {
  remark?: string | null
  members: ProjectMember[]
}

/** 立项/修改入参：没有 status —— 状态只能走 activate/pause/close 动作。 */
export interface ProjectSaveReq {
  name: string
  epsId?: string | null
  managerId?: string | null
  type?: string
  priority?: number
  planStart?: string | null
  planEnd?: string | null
  progress?: number
  remark?: string | null
  /** 不传表示「本次不动成员」。 */
  members?: MemberSaveReq[] | null
}

// ---------------------------------------------------------------- WBS 任务（P7-2）

export type TaskType = 'TASK' | 'MILESTONE'
export type TaskStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'SUBMITTED' | 'DONE' | 'BLOCKED'

export const TASK_TYPE_LABEL: Record<string, string> = {
  TASK: '任务',
  MILESTONE: '里程碑'
}

export const TASK_STATUS_LABEL: Record<string, string> = {
  NOT_STARTED: '未开始',
  IN_PROGRESS: '进行中',
  SUBMITTED: '待审核',
  DONE: '已完成',
  BLOCKED: '受阻'
}

export const DEP_TYPE_LABEL: Record<string, string> = {
  FS: '完成-开始',
  SS: '开始-开始',
  FF: '完成-完成',
  SF: '开始-完成'
}

export interface ProjectTask {
  id: string
  projectId: string
  parentId?: string | null
  /** WBS 编码：1 / 1.2 / 1.2.3，由服务端生成。 */
  wbsCode: string
  name: string
  type: TaskType
  status: TaskStatus
  sortNo: number
  planStart?: string | null
  planEnd?: string | null
  actualStart?: string | null
  actualEnd?: string | null
  durationDays?: number | null
  progress: number
  ownerId?: string | null
  ownerName?: string | null
  remark?: string | null
  childCount: number
  /** 执行闭环时间戳（P7-3）：派工 / 接收 / 提交 / 通过。 */
  assignedAt?: string | null
  acceptedAt?: string | null
  submittedAt?: string | null
  approvedAt?: string | null
  submitNote?: string | null
  rejectReason?: string | null
  children?: ProjectTask[]
}

/** 我的任务行（跨项目平铺）。 */
export interface MyTaskRow {
  id: string
  projectId: string
  projectCode: string
  projectName: string
  wbsCode: string
  name: string
  status: TaskStatus
  planStart?: string | null
  planEnd?: string | null
  progress: number
  ownerName?: string | null
}

export interface TaskDependency {
  id: string
  predTaskId: string
  succTaskId: string
  depType: string
  lagDays: number
  predName?: string | null
  succName?: string | null
}

export interface TaskBoard {
  projectId: string
  projectCode: string
  projectName: string
  /** 项目进度＝叶子任务按工期加权（服务端汇总）。 */
  projectProgress: number
  rangeStart?: string | null
  rangeEnd?: string | null
  tasks: ProjectTask[]
  dependencies: TaskDependency[]
}

/** 任务保存入参：没有 wbsCode —— 编码由服务端生成，顺序调整走 resequence。 */
export interface TaskSaveReq {
  name: string
  parentId?: string | null
  type?: string
  status?: string
  sortNo?: number
  planStart?: string | null
  planEnd?: string | null
  actualStart?: string | null
  actualEnd?: string | null
  durationDays?: number | null
  progress?: number
  ownerId?: string | null
  remark?: string | null
}

export interface DependencySaveReq {
  predTaskId: string
  succTaskId: string
  depType?: string
  lagDays?: number
}

/** 日历事件（扁平，前端按月网格展开到每一天）。 */
export interface TaskCalendarRow {
  id: string
  parentId?: string | null
  wbsCode: string
  name: string
  type: string
  status: string
  planStart: string
  planEnd: string
  durationDays: number
  progress: number
  ownerName?: string | null
}

export interface CalendarBoard {
  projectId: string
  projectCode?: string | null
  projectName?: string | null
  from: string
  to: string
  tasks: TaskCalendarRow[]
}

export const taskApi = {
  board(projectId: string): Promise<TaskBoard> {
    return http.get<TaskBoard>(`/prj/projects/${projectId}/tasks`)
  },
  /** 日历：只返回与区间相交且已排期的任务。 */
  calendar(projectId: string, from: string, to: string): Promise<CalendarBoard> {
    return http.get<CalendarBoard>(`/prj/projects/${projectId}/calendar`, { from, to })
  },
  /** 拖拽改期：级联平移子树，不动祖先与依赖。 */
  reschedule(taskId: string, planStart: string, planEnd: string): Promise<TaskBoard> {
    return http.post<TaskBoard>(`/prj/tasks/${taskId}/reschedule`, { planStart, planEnd })
  },
  create(projectId: string, req: TaskSaveReq): Promise<TaskBoard> {
    return http.post<TaskBoard>(`/prj/projects/${projectId}/tasks`, req)
  },
  update(taskId: string, req: TaskSaveReq): Promise<TaskBoard> {
    return http.put<TaskBoard>(`/prj/tasks/${taskId}`, req)
  },
  remove(taskId: string): Promise<TaskBoard> {
    return http.delete<TaskBoard>(`/prj/tasks/${taskId}`)
  },
  /** 按排序号重排 WBS 编码（1 / 1.2 / 1.2.3）。 */
  resequence(projectId: string): Promise<TaskBoard> {
    return http.post<TaskBoard>(`/prj/projects/${projectId}/tasks/resequence`)
  },
  createDependency(projectId: string, req: DependencySaveReq): Promise<TaskBoard> {
    return http.post<TaskBoard>(`/prj/projects/${projectId}/task-dependencies`, req)
  },
  removeDependency(dependencyId: string): Promise<TaskBoard> {
    return http.delete<TaskBoard>(`/prj/task-dependencies/${dependencyId}`)
  },
  /** 派工：把任务交给执行人（项目经理）。 */
  assign(taskId: string, ownerId: string): Promise<TaskBoard> {
    return http.post<TaskBoard>(`/prj/tasks/${taskId}/assign`, { ownerId })
  },
  /** 接收任务（执行人）。 */
  accept(taskId: string): Promise<TaskBoard> {
    return http.post<TaskBoard>(`/prj/tasks/${taskId}/accept`)
  },
  /** 开始执行（未开始 → 进行中）。 */
  start(taskId: string): Promise<TaskBoard> {
    return http.post<TaskBoard>(`/prj/tasks/${taskId}/start`)
  },
  /** 提交审核（进行中 → 已提交）。 */
  submit(taskId: string, note?: string): Promise<TaskBoard> {
    return http.post<TaskBoard>(`/prj/tasks/${taskId}/submit`, { note: note ?? null })
  },
  /** 审核通过（已提交 → 已完成，进度 100%）。 */
  approve(taskId: string): Promise<TaskBoard> {
    return http.post<TaskBoard>(`/prj/tasks/${taskId}/approve`)
  },
  /** 打回（已提交 → 进行中，必须写原因）。 */
  reject(taskId: string, reason: string): Promise<TaskBoard> {
    return http.post<TaskBoard>(`/prj/tasks/${taskId}/reject`, { reason })
  },
  /** 我的任务：默认只查未完成。 */
  myTasks(status?: string): Promise<MyTaskRow[]> {
    return http.get<MyTaskRow[]>('/prj/my-tasks', status ? { status } : undefined)
  }
}

export const epsApi = {
  tree(): Promise<EpsNode[]> {
    return http.get<EpsNode[]>('/prj/eps')
  },
  create(req: EpsSaveReq): Promise<EpsNode> {
    return http.post<EpsNode>('/prj/eps', req)
  },
  update(id: string, req: EpsSaveReq): Promise<EpsNode> {
    return http.put<EpsNode>(`/prj/eps/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/prj/eps/${id}`)
  }
}

// ---------------------------------------------------------------- 交付物与齐套（P7-4）

export type DeliverableObjectType = 'DOC' | 'ITEM' | 'CAPP_CARD'
export type DeliverableOutputType = 'INPUT' | 'OUTPUT'

export const DELIVERABLE_OBJECT_LABEL: Record<string, string> = {
  DOC: '图文档',
  ITEM: '零部件',
  CAPP_CARD: '工艺卡片'
}

export const DELIVERABLE_OUTPUT_LABEL: Record<string, string> = {
  INPUT: '输入',
  OUTPUT: '输出'
}

/** 对象状态：未发布只提醒，缺失/废弃才拦提交。 */
export const DELIVERABLE_STATE_LABEL: Record<string, string> = {
  OK: '可用',
  UNRELEASED: '未发布',
  MISSING: '缺失',
  OBSOLETE: '已废弃'
}

export interface DeliverableRow {
  id: string
  taskId: string
  taskWbsCode?: string | null
  taskName?: string | null
  outputType: DeliverableOutputType
  objectType: DeliverableObjectType
  objectId: string
  code?: string | null
  name?: string | null
  lifecycleState?: string | null
  state: 'OK' | 'UNRELEASED' | 'MISSING' | 'OBSOLETE'
  required: boolean
  ready: boolean
  remark?: string | null
}

/** 任务齐套行：items 含输入与输出全部交付物。 */
export interface TaskCompleteness {
  taskId: string
  wbsCode: string
  taskName: string
  status: TaskStatus
  requiredCount: number
  readyCount: number
  missingCount: number
  unreleasedCount: number
  ready: boolean
  items: DeliverableRow[]
}

export interface DeliverableBoard {
  projectId: string
  projectCode: string
  projectName: string
  requiredTotal: number
  readyTotal: number
  missingTotal: number
  unreleasedTotal: number
  /** 只把「必需输出缺失/废弃」当不齐套，未发布不算失败。 */
  ready: boolean
  tasks: TaskCompleteness[]
}

export interface DeliverableCandidate {
  objectType: DeliverableObjectType
  objectId: string
  code: string
  name?: string | null
  lifecycleState?: string | null
}

export interface DeliverableSaveReq {
  objectType: DeliverableObjectType
  objectId: string
  outputType?: DeliverableOutputType
  required?: boolean
  remark?: string | null
}

export interface DeliverableUpdateReq {
  outputType?: DeliverableOutputType
  required?: boolean
  remark?: string | null
}

export const deliverableApi = {
  board(projectId: string): Promise<DeliverableBoard> {
    return http.get<DeliverableBoard>(`/prj/projects/${projectId}/deliverables`)
  },
  add(taskId: string, req: DeliverableSaveReq): Promise<DeliverableBoard> {
    return http.post<DeliverableBoard>(`/prj/tasks/${taskId}/deliverables`, req)
  },
  update(outputId: string, req: DeliverableUpdateReq): Promise<DeliverableBoard> {
    return http.put<DeliverableBoard>(`/prj/deliverables/${outputId}`, req)
  },
  remove(outputId: string): Promise<DeliverableBoard> {
    return http.delete<DeliverableBoard>(`/prj/deliverables/${outputId}`)
  },
  /** 可选对象：只列文档/零部件最新版本与工艺卡片。 */
  candidates(objectType: DeliverableObjectType, keyword?: string, limit?: number): Promise<DeliverableCandidate[]> {
    return http.get<DeliverableCandidate[]>('/prj/deliverable-candidates', {
      objectType,
      keyword: keyword || undefined,
      limit
    })
  }
}

// ---------------------------------------------------------------- 工时填报与审批（P7-5）

export type TimesheetStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'
export type TimesheetWorkType = 'NORMAL' | 'OVERTIME'

export const TIMESHEET_STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '待审批',
  APPROVED: '已通过',
  REJECTED: '已打回'
}

export const TIMESHEET_WORK_TYPE_LABEL: Record<string, string> = {
  NORMAL: '正常',
  OVERTIME: '加班'
}

export interface TimesheetRow {
  id: string
  projectId: string
  projectCode?: string | null
  projectName?: string | null
  taskId: string
  taskWbsCode?: string | null
  taskName?: string | null
  userId: string
  userName?: string | null
  workDate: string
  hours: number
  workType: TimesheetWorkType
  status: TimesheetStatus
  remark?: string | null
  submittedAt?: string | null
  approverName?: string | null
  approvedAt?: string | null
  rejectReason?: string | null
  createdAt?: string | null
}

export interface TimesheetPage {
  items: TimesheetRow[]
  total: number
  page: number
  size: number
  totalHours: number
}

/** 填报入参：projectId 由服务端从任务反查，前端不用传。 */
export interface TimesheetSaveReq {
  taskId: string
  workDate: string
  hours: number
  workType?: TimesheetWorkType
  remark?: string | null
}

export interface UserHoursRow {
  userId: string
  userName?: string | null
  approvedHours: number
  pendingHours: number
}

export interface TaskHoursRow {
  taskId: string
  wbsCode?: string | null
  taskName?: string | null
  approvedHours: number
  pendingHours: number
}

export interface WeekHoursRow {
  weekStart: string
  approvedHours: number
}

/** 汇总口径：实际工时只认 APPROVED，SUBMITTED 单列待审批。 */
export interface TimesheetSummary {
  projectId: string
  projectCode?: string | null
  projectName?: string | null
  from: string
  to: string
  approvedHours: number
  pendingHours: number
  rejectedHours: number
  approvedCount: number
  pendingCount: number
  byUser: UserHoursRow[]
  byTask: TaskHoursRow[]
  byWeek: WeekHoursRow[]
}

export interface ResourceLoadRow {
  userId: string
  userName?: string | null
  approvedHours: number
  weeks: WeekHoursRow[]
}

export interface ResourceLoad {
  projectId: string
  from: string
  to: string
  weekCount: number
  /** 周容量 = 周数 × 40h（不接节假日日历，P7-7 再做真日历）。 */
  capacityHours: number
  rows: ResourceLoadRow[]
}

export const timesheetApi = {
  /** 工时列表：没有 prj.project.write 的人只会拿到自己的。 */
  page(params: {
    projectId?: string
    userId?: string
    taskId?: string
    status?: string
    from?: string
    to?: string
    page?: number
    size?: number
  }): Promise<TimesheetPage> {
    return http.get<TimesheetPage>('/prj/timesheets', params)
  },
  create(req: TimesheetSaveReq): Promise<TimesheetRow> {
    return http.post<TimesheetRow>('/prj/timesheets', req)
  },
  update(id: string, req: TimesheetSaveReq): Promise<TimesheetRow> {
    return http.put<TimesheetRow>(`/prj/timesheets/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/prj/timesheets/${id}`)
  },
  submit(id: string): Promise<TimesheetRow> {
    return http.post<TimesheetRow>(`/prj/timesheets/${id}/submit`)
  },
  approve(id: string): Promise<TimesheetRow> {
    return http.post<TimesheetRow>(`/prj/timesheets/${id}/approve`)
  },
  batchApprove(ids: string[]): Promise<{ approved: number }> {
    return http.post<{ approved: number }>('/prj/timesheets/batch-approve', { ids })
  },
  reject(id: string, reason: string): Promise<TimesheetRow> {
    return http.post<TimesheetRow>(`/prj/timesheets/${id}/reject`, { reason })
  },
  summary(projectId: string, from?: string, to?: string): Promise<TimesheetSummary> {
    return http.get<TimesheetSummary>(`/prj/projects/${projectId}/timesheet-summary`, { from, to })
  },
  resourceLoad(projectId: string, from?: string, to?: string): Promise<ResourceLoad> {
    return http.get<ResourceLoad>(`/prj/projects/${projectId}/resource-load`, { from, to })
  }
}

// ---------------------------------------------------------------- 项目模板（P7-6）

export interface ProjectTemplateRow {
  id: string
  code: string
  name: string
  type: string
  enabled: boolean
  remark?: string | null
  taskCount: number
  folderCount: number
  updatedAt?: string | null
}

/** 模板任务：编码由服务端按数组顺序生成，offset 是「相对项目开工日第几天」。 */
export interface TemplateTask {
  id: string
  parentId?: string | null
  wbsCode: string
  name: string
  type: string
  startOffset: number
  durationDays: number
  sortNo: number
  remark?: string | null
}

export interface TemplateFolder {
  id: string
  parentPath: string
  name: string
  sortNo: number
}

export interface TemplateDetail {
  id: string
  code: string
  name: string
  type: string
  enabled: boolean
  remark?: string | null
  tasks: TemplateTask[]
  folders: TemplateFolder[]
}

export interface TemplateSaveReq {
  name: string
  type?: string
  enabled?: boolean
  remark?: string | null
}

export interface TemplateTaskSaveReq {
  parentWbsCode?: string | null
  name: string
  type?: string
  startOffset?: number
  durationDays?: number
  remark?: string | null
}

export interface TemplateFolderSaveReq {
  parentPath?: string
  name: string
  sortNo?: number
}

export interface FromTemplateReq {
  templateId: string
  name: string
  epsId?: string | null
  managerId?: string | null
  planStart: string
  priority?: number
  remark?: string | null
}

export const templateApi = {
  list(params: { keyword?: string; enabled?: boolean; page?: number; size?: number }) {
    return http.get<{ items: ProjectTemplateRow[]; total: number; page: number; size: number }>(
      '/prj/templates',
      params
    )
  },
  detail(id: string): Promise<TemplateDetail> {
    return http.get<TemplateDetail>(`/prj/templates/${id}`)
  },
  create(req: TemplateSaveReq): Promise<TemplateDetail> {
    return http.post<TemplateDetail>('/prj/templates', req)
  },
  update(id: string, req: TemplateSaveReq): Promise<TemplateDetail> {
    return http.put<TemplateDetail>(`/prj/templates/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/prj/templates/${id}`)
  },
  /** 任务骨架整表替换（编码按数组顺序生成）。 */
  replaceTasks(id: string, tasks: TemplateTaskSaveReq[]): Promise<TemplateDetail> {
    return http.put<TemplateDetail>(`/prj/templates/${id}/tasks`, { tasks })
  },
  /** 目录骨架整表替换（父路径必须先声明）。 */
  replaceFolders(id: string, folders: TemplateFolderSaveReq[]): Promise<TemplateDetail> {
    return http.put<TemplateDetail>(`/prj/templates/${id}/folders`, { folders })
  },
  /** 从模板立项：生成项目 + WBS 任务 + 文档目录。 */
  fromTemplate(req: FromTemplateReq): Promise<ProjectDetail> {
    return http.post<ProjectDetail>('/prj/projects/from-template', req)
  }
}

// ---------------------------------------------------------------- 项目门户三视图（P7-8）

export interface PortalProjectCard {
  id: string
  code: string
  name: string
  type: string
  status: ProjectStatus
  progress: number
  planStart?: string | null
  planEnd?: string | null
  managerName?: string | null
  /** 落在"我"头上的任务数（门户第一屏回答"我要做什么"）。 */
  myTaskCount: number
  myOpenTaskCount: number
  manager: boolean
  member: boolean
}

export interface ManagedProjectRow {
  id: string
  code: string
  name: string
  status: ProjectStatus
  progress: number
  planStart?: string | null
  planEnd?: string | null
  /** null = 计划起止没填全，不判断快慢。 */
  timeProgress?: number | null
  /** progress − timeProgress，负数代表落后。 */
  deviation?: number | null
  taskCount: number
  doneTaskCount: number
  overdueTaskCount: number
  pendingTimesheetCount: number
  memberCount: number
  managerName?: string | null
}

export interface PortfolioStatusCount {
  status: string
  count: number
}

export interface PortfolioRiskRow {
  id: string
  code: string
  name: string
  status: string
  progress: number
  timeProgress?: number | null
  deviation?: number | null
  planEnd?: string | null
  overdueTaskCount: number
}

export interface PortfolioOverview {
  totalProjects: number
  activeProjects: number
  closedProjects: number
  overdueProjects: number
  avgProgress: number
  byStatus: PortfolioStatusCount[]
  risks: PortfolioRiskRow[]
}

export const portalApi = {
  myProjects(): Promise<PortalProjectCard[]> {
    return http.get<PortalProjectCard[]>('/prj/portal/my-projects')
  },
  managedProjects(): Promise<ManagedProjectRow[]> {
    return http.get<ManagedProjectRow[]>('/prj/portal/managed-projects')
  },
  portfolio(): Promise<PortfolioOverview> {
    return http.get<PortfolioOverview>('/prj/portal/portfolio')
  }
}

export const projectApi = {
  list(params: {
    page?: number
    size?: number
    keyword?: string
    status?: string
    epsId?: string
    /** true = 连同子树一起查。 */
    includeSubtree?: boolean
  }): Promise<{ items: ProjectRow[]; total: number; page: number; size: number }> {
    return http.get('/prj/projects', params)
  },
  detail(id: string): Promise<ProjectDetail> {
    return http.get<ProjectDetail>(`/prj/projects/${id}`)
  },
  create(req: ProjectSaveReq): Promise<ProjectDetail> {
    return http.post<ProjectDetail>('/prj/projects', req)
  },
  update(id: string, req: ProjectSaveReq): Promise<ProjectDetail> {
    return http.put<ProjectDetail>(`/prj/projects/${id}`, req)
  },
  replaceMembers(id: string, members: MemberSaveReq[]): Promise<ProjectDetail> {
    return http.put<ProjectDetail>(`/prj/projects/${id}/members`, members)
  },
  activate(id: string): Promise<ProjectDetail> {
    return http.post<ProjectDetail>(`/prj/projects/${id}/activate`)
  },
  pause(id: string): Promise<ProjectDetail> {
    return http.post<ProjectDetail>(`/prj/projects/${id}/pause`)
  },
  close(id: string): Promise<ProjectDetail> {
    return http.post<ProjectDetail>(`/prj/projects/${id}/close`)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/prj/projects/${id}`)
  }
}
