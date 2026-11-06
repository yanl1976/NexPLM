import { http } from './http'

/** 变更申请列表行。 */
export interface EcrRow {
  id: string
  code: string
  title: string
  changeType: string
  category?: string | null
  priority: string
  status: string
  requesterId: string
  requesterName?: string | null
  receiverId?: string | null
  projectNo?: string | null
  targetDate?: string | null
  createdAt: string | null
  affectedCount: number
}

/** 关联对象行。 */
export interface ChangeObjectRow {
  id: string
  objectType: string
  objectId: string
  objectCode?: string | null
  objectName?: string | null
  relation: string
  action?: string | null
  revisionFrom?: number | null
  revisionTo?: number | null
  execStatus: string
  remark?: string | null
}

/** 变更通知行（P5-2 接入前通常为空）。 */
export interface EcnRow {
  id: string
  code: string
  ecrId: string
  title: string
  status: string
  executorId?: string | null
  executorName?: string | null
  planDate?: string | null
  issuedAt?: string | null
  closedAt?: string | null
}

export interface EcrDetail {
  id: string
  code: string
  title: string
  changeType: string
  category?: string | null
  reason?: string | null
  feasibility?: string | null
  priority: string
  status: string
  requesterId: string
  requesterName?: string | null
  receiverId?: string | null
  receiverName?: string | null
  projectNo?: string | null
  targetDate?: string | null
  remark?: string | null
  approvedAt?: string | null
  closedAt?: string | null
  createdAt: string | null
  updatedAt: string | null
  objects: ChangeObjectRow[]
  ecns: EcnRow[]
}

export interface EcrSaveReq {
  title: string
  changeType: string
  category?: string | null
  reason?: string | null
  feasibility?: string | null
  priority?: string | null
  receiverId?: string | null
  projectNo?: string | null
  targetDate?: string | null
  remark?: string | null
}

export interface ChangeObjectAddReq {
  objectType: string
  objectId: string
  relation?: string | null
  action?: string | null
  remark?: string | null
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

/** 变更问题行。 */
export interface ProblemRow {
  id: string
  code: string
  ecnId: string
  description: string
  status: string
  ownerId?: string | null
  ownerName?: string | null
  dueDate?: string | null
  closedAt?: string | null
  createdAt: string | null
}

export interface ExecuteResult {
  total: number
  succeeded: number
  skipped: number
  messages: string[]
}

/** 影响分析行。 */
export interface ImpactRow {
  direction: 'UP' | 'DOWN'
  depth: number
  objectType: string
  objectId: string
  code: string
  name: string
  lifecycleState: string
  revision: number
  iteration: number
  linked: boolean
}

/** 变更追踪时间线事件（申请→评审→通知→执行→关闭）。 */
export interface TimelineEvent {
  time: string | null
  stage: 'CREATE' | 'REVIEW' | 'APPROVE' | 'REJECT' | 'WITHDRAW' | 'NOTICE' | 'PROBLEM' | 'EXECUTE' | 'CLOSE'
  title: string
  actorName?: string | null
  detail?: string | null
}

export const changeApi = {
  /** 变更追踪时间线：由 ECR / 流程意见 / 通知 / 问题 / 执行记录派生。 */
  timeline(ecrId: string) {
    return http.get<TimelineEvent[]>(`/chg/ecrs/${ecrId}/timeline`)
  },
  impact(ecrId: string, depth = 3, viewType = 'DESIGN') {
    return http.get<ImpactRow[]>(`/chg/ecrs/${ecrId}/impact`, { depth, viewType })
  },
  createEcn(ecrId: string, body: { title: string; executorId?: string | null; planDate?: string | null; remark?: string | null }) {
    return http.post<EcnRow>(`/chg/ecrs/${ecrId}/ecns`, body)
  },
  issueEcn(ecnId: string) {
    return http.post<EcnRow>(`/chg/ecns/${ecnId}/issue`)
  },
  closeEcn(ecnId: string) {
    return http.post<EcnRow>(`/chg/ecns/${ecnId}/close`)
  },
  problems(ecnId: string) {
    return http.get<ProblemRow[]>(`/chg/ecns/${ecnId}/problems`)
  },
  addProblem(ecnId: string, body: { description: string; ownerId?: string | null; dueDate?: string | null }) {
    return http.post<ProblemRow>(`/chg/ecns/${ecnId}/problems`, body)
  },
  closeProblem(problemId: string) {
    return http.post<void>(`/chg/problems/${problemId}/close`)
  },
  /** 执行变更：对受影响对象按意图升版/废弃/发布。 */
  execute(ecrId: string) {
    return http.post<ExecuteResult>(`/chg/ecrs/${ecrId}/execute`)
  },
  list(params: { keyword?: string; status?: string; page?: number; size?: number }) {
    return http.get<PageResult<EcrRow>>('/chg/ecrs', params)
  },
  detail(id: string) {
    return http.get<EcrDetail>(`/chg/ecrs/${id}`)
  },
  create(body: EcrSaveReq) {
    return http.post<EcrDetail>('/chg/ecrs', body)
  },
  update(id: string, body: EcrSaveReq) {
    return http.put<EcrDetail>(`/chg/ecrs/${id}`, body)
  },
  submit(id: string) {
    return http.post<EcrDetail>(`/chg/ecrs/${id}/submit`)
  },
  close(id: string) {
    return http.post<EcrDetail>(`/chg/ecrs/${id}/close`)
  },
  addObject(ecrId: string, body: ChangeObjectAddReq) {
    return http.post<ChangeObjectRow>(`/chg/ecrs/${ecrId}/objects`, body)
  },
  removeObject(ecrId: string, linkId: string) {
    return http.delete<void>(`/chg/ecrs/${ecrId}/objects/${linkId}`)
  }
}
