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

export const taskApi = {
  board(projectId: string): Promise<TaskBoard> {
    return http.get<TaskBoard>(`/prj/projects/${projectId}/tasks`)
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
