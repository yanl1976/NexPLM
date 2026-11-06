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
  /** 最近一次应用的 8D 模板（仅追溯用，删除模板不影响已生成措施）。 */
  templateId?: string | null
  templateCode?: string | null
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

/* ------------------------------------------------------------ P8-2 8D 模板 */

export type TemplateType = '8D' | 'CUSTOM'

export const TEMPLATE_TYPE_LABEL: Record<string, string> = {
  '8D': '8D 八步法',
  CUSTOM: '企业自定义'
}

export interface TemplateItemRow {
  id: string
  itemCode: string
  name: string
  actionType: ActionType
  prompt?: string | null
  required: boolean
  sortNo: number
}

export interface TemplateRow {
  id: string
  code: string
  name: string
  templateType: string
  enabled: boolean
  remark?: string | null
  /** 步骤数量，列表直接看模板厚薄。 */
  itemCount: number
  updatedAt?: string | null
}

export interface TemplateDetail {
  id: string
  code: string
  name: string
  templateType: string
  enabled: boolean
  remark?: string | null
  items: TemplateItemRow[]
}

export interface TemplateSaveReq {
  name: string
  templateType?: string
  enabled?: boolean
  remark?: string | null
}

export interface TemplateItemSaveReq {
  itemCode: string
  name: string
  actionType?: string
  prompt?: string | null
  required?: boolean
  sortNo?: number
}

export const qualityTemplateApi = {
  page(params: {
    keyword?: string
    enabled?: boolean
    page?: number
    size?: number
  }) {
    return http.get<{ items: TemplateRow[]; total: number; page: number; size: number }>(
      '/qlt/templates',
      params
    )
  },
  detail(id: string): Promise<TemplateDetail> {
    return http.get<TemplateDetail>(`/qlt/templates/${id}`)
  },
  create(req: TemplateSaveReq): Promise<TemplateDetail> {
    return http.post<TemplateDetail>('/qlt/templates', req)
  },
  update(id: string, req: TemplateSaveReq): Promise<TemplateDetail> {
    return http.put<TemplateDetail>(`/qlt/templates/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/qlt/templates/${id}`)
  },
  /** 整表替换步骤（先删后插），顺序即执行顺序。 */
  replaceItems(id: string, items: TemplateItemSaveReq[]): Promise<TemplateDetail> {
    return http.put<TemplateDetail>(`/qlt/templates/${id}/items`, { items })
  }
}

/* ------------------------------------------------------------ P8-2 质量看板 */

export interface CountRow {
  key: string
  count: number
}

export interface TrendRow {
  /** yyyy-MM */
  month: string
  created: number
  closed: number
}

export interface OwnerLoadRow {
  ownerId: string
  ownerName?: string | null
  openCount: number
  overdueCount: number
}

export interface QualityDashboard {
  total: number
  open: number
  closed: number
  /** 关闭率（%），全量快照口径，不随期间变化。 */
  closedRate: number
  overdue: number
  avgCloseDays: number
  byStatus: CountRow[]
  bySource: CountRow[]
  bySeverity: CountRow[]
  trend: TrendRow[]
  topOwners: OwnerLoadRow[]
}

export const qualityDashboardApi = {
  get(params?: { from?: string; to?: string }): Promise<QualityDashboard> {
    return http.get<QualityDashboard>('/qlt/dashboard', params)
  }
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
  },
  /** 应用 8D 模板：按步骤批量生成当前轮措施（同一轮同一模板只允许一次）。 */
  applyTemplate(id: string, templateId: string): Promise<IssueDetail> {
    return http.post<IssueDetail>(`/qlt/issues/${id}/apply-template`, { templateId })
  }
}
