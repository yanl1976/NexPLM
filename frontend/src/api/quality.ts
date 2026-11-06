import { http } from './http'

/** P8-1 质量问题闭环。 */

export type IssueSource = 'INTERNAL' | 'CUSTOMER' | 'SUPPLIER' | 'AUDIT'
export type IssueSeverity = 'MINOR' | 'MAJOR' | 'CRITICAL'
export type IssueStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'VERIFYING' | 'CLOSED'
export type ActionType = 'CONTAINMENT' | 'ROOT_CAUSE' | 'CORRECTIVE' | 'PREVENTIVE' | 'VERIFY'

export const ISSUE_SOURCE_LABEL: Record<string, string> = {
  INTERNAL: '内部',
  CUSTOMER: '客户',
  SUPPLIER: '供应商',
  AUDIT: '审核'
}

export const ISSUE_SEVERITY_LABEL: Record<string, string> = {
  MINOR: '轻微',
  MAJOR: '一般',
  CRITICAL: '严重'
}

export const ISSUE_STATUS_LABEL: Record<string, string> = {
  OPEN: '待分派',
  ASSIGNED: '已分派',
  IN_PROGRESS: '整改中',
  VERIFYING: '待验证',
  CLOSED: '已关闭'
}

export const ACTION_TYPE_LABEL: Record<string, string> = {
  CONTAINMENT: '临时遏制',
  ROOT_CAUSE: '根因分析',
  CORRECTIVE: '纠正措施',
  PREVENTIVE: '预防措施',
  VERIFY: '验证记录'
}

export interface IssueRow {
  id: string
  code: string
  title: string
  source: IssueSource
  severity: IssueSeverity
  status: IssueStatus
  /** 当前整改轮次（验证不通过会 +1）。 */
  roundNo: number
  objectType?: string | null
  objectId?: string | null
  objectCode?: string | null
  projectId?: string | null
  supplierId?: string | null
  ownerId?: string | null
  ownerName?: string | null
  reportedByName?: string | null
  reportedAt?: string | null
  dueDate?: string | null
  closedAt?: string | null
  verifyNote?: string | null
  actionCount: number
  openActionCount: number
  overdue: boolean
  remark?: string | null
  updatedAt?: string | null
}

export interface IssueActionRow {
  id: string
  roundNo: number
  actionType: ActionType
  content: string
  ownerId?: string | null
  ownerName?: string | null
  dueDate?: string | null
  status: 'PENDING' | 'DONE'
  doneAt?: string | null
  createdAt?: string | null
}

export interface IssueDetail {
  issue: IssueRow
  actions: IssueActionRow[]
}

export interface IssueSaveReq {
  title: string
  description?: string | null
  source?: string
  severity?: string
  ownerId?: string | null
  dueDate?: string | null
  objectType?: string | null
  objectId?: string | null
  objectCode?: string | null
  projectId?: string | null
  supplierId?: string | null
  remark?: string | null
}

export interface ActionSaveReq {
  content: string
  actionType?: string
  ownerId?: string | null
  dueDate?: string | null
}

export const qualityIssueApi = {
  page(params: {
    keyword?: string
    status?: string
    severity?: string
    source?: string
    ownerId?: string
    overdue?: boolean
    page?: number
    size?: number
  }) {
    return http.get<{ items: IssueRow[]; total: number; page: number; size: number }>(
      '/qlt/issues',
      params
    )
  },
  detail(id: string): Promise<IssueDetail> {
    return http.get<IssueDetail>(`/qlt/issues/${id}`)
  },
  create(req: IssueSaveReq): Promise<IssueDetail> {
    return http.post<IssueDetail>('/qlt/issues', req)
  },
  update(id: string, req: IssueSaveReq): Promise<IssueDetail> {
    return http.put<IssueDetail>(`/qlt/issues/${id}`, req)
  },
  assign(id: string, ownerId: string, dueDate?: string | null): Promise<IssueDetail> {
    return http.post<IssueDetail>(`/qlt/issues/${id}/assign`, { ownerId, dueDate })
  },
  addAction(id: string, req: ActionSaveReq): Promise<IssueDetail> {
    return http.post<IssueDetail>(`/qlt/issues/${id}/actions`, req)
  },
  markAction(id: string, actionId: string, done: boolean): Promise<IssueDetail> {
    return http.post<IssueDetail>(`/qlt/issues/${id}/actions/${actionId}/done?done=${done}`)
  },
  removeAction(id: string, actionId: string): Promise<void> {
    return http.delete<void>(`/qlt/issues/${id}/actions/${actionId}`)
  },
  /** 提交验证：当前轮措施必须全部完成。 */
  submit(id: string): Promise<IssueDetail> {
    return http.post<IssueDetail>(`/qlt/issues/${id}/submit`)
  },
  /** 验证：不通过会退回整改并开启下一轮。 */
  verify(id: string, passed: boolean, note?: string | null): Promise<IssueDetail> {
    return http.post<IssueDetail>(`/qlt/issues/${id}/verify`, { passed, note })
  }
}
