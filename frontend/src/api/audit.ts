import { http } from './http'

/** P8-3-2 IATF16949 体系审核：计划 + 检查表 + 不符合项（转质量问题闭环）。 */

export const AUDIT_TYPE_LABEL: Record<string, string> = {
  INTERNAL: '内审',
  EXTERNAL: '外审',
  SUPPLIER: '供方审核',
  PROCESS: '过程审核'
}

export const AUDIT_STATUS_LABEL: Record<string, string> = {
  PLANNED: '计划中',
  IN_PROGRESS: '进行中',
  CLOSED: '已关闭'
}

export const AUDIT_RESULT_LABEL: Record<string, string> = {
  PENDING: '待检',
  OK: '符合',
  NG: '不符合',
  NA: '不适用'
}

export const FINDING_SEVERITY_LABEL: Record<string, string> = {
  MINOR: '轻微',
  MAJOR: '一般',
  CRITICAL: '严重'
}

export interface PlanRow {
  id: string
  code: string
  title: string
  auditType: string
  standard?: string | null
  plannedDate?: string | null
  actualDate?: string | null
  auditorId?: string | null
  auditorName?: string | null
  auditee?: string | null
  status: string
  itemCount: number
  findingCount: number
  openFindingCount: number
  updatedAt?: string | null
}

export interface AuditItemRow {
  id: string
  itemNo: string
  clause?: string | null
  question: string
  result: string
  evidence?: string | null
  sortNo: number
}

export interface AuditFindingRow {
  id: string
  code: string
  planId: string
  itemId?: string | null
  itemNo?: string | null
  title: string
  description?: string | null
  severity: string
  dueDate?: string | null
  status: string
  /** 转成质量问题后回填，整改在 /quality/issues 里做。 */
  issueId?: string | null
  issueCode?: string | null
  closedAt?: string | null
  createdAt?: string | null
}

export interface AuditStats {
  itemTotal: number
  okCount: number
  ngCount: number
  naCount: number
  pendingCount: number
  findingTotal: number
  openFindingCount: number
}

export interface PlanDetail {
  id: string
  code: string
  title: string
  auditType: string
  standard?: string | null
  scope?: string | null
  plannedDate?: string | null
  actualDate?: string | null
  auditorId?: string | null
  auditorName?: string | null
  auditee?: string | null
  status: string
  summary?: string | null
  remark?: string | null
  stats: AuditStats
  items: AuditItemRow[]
  findings: AuditFindingRow[]
}

export interface PlanSaveReq {
  title: string
  auditType?: string
  standard?: string | null
  scope?: string | null
  plannedDate?: string | null
  auditorId?: string | null
  auditee?: string | null
  summary?: string | null
  remark?: string | null
}

export interface ItemSaveReq {
  itemNo: string
  clause?: string | null
  question: string
  result?: string
  evidence?: string | null
  sortNo?: number
}

export interface FindingSaveReq {
  title: string
  description?: string | null
  severity?: string
  dueDate?: string | null
  itemId?: string | null
}

export const auditApi = {
  page(params: {
    keyword?: string
    status?: string
    auditType?: string
    page?: number
    size?: number
  }) {
    return http.get<{ items: PlanRow[]; total: number; page: number; size: number }>(
      '/qlt/audit-plans',
      params
    )
  },
  detail(id: string): Promise<PlanDetail> {
    return http.get<PlanDetail>(`/qlt/audit-plans/${id}`)
  },
  create(req: PlanSaveReq): Promise<PlanDetail> {
    return http.post<PlanDetail>('/qlt/audit-plans', req)
  },
  update(id: string, req: PlanSaveReq): Promise<PlanDetail> {
    return http.put<PlanDetail>(`/qlt/audit-plans/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/qlt/audit-plans/${id}`)
  },
  /** 整表替换检查表。 */
  replaceItems(id: string, items: ItemSaveReq[]): Promise<PlanDetail> {
    return http.put<PlanDetail>(`/qlt/audit-plans/${id}/items`, { items })
  },
  start(id: string): Promise<PlanDetail> {
    return http.post<PlanDetail>(`/qlt/audit-plans/${id}/start`)
  },
  /** 关闭审核：存在未关闭不符合项会被拒。 */
  close(id: string): Promise<PlanDetail> {
    return http.post<PlanDetail>(`/qlt/audit-plans/${id}/close`)
  },
  addFinding(id: string, req: FindingSaveReq): Promise<PlanDetail> {
    return http.post<PlanDetail>(`/qlt/audit-plans/${id}/findings`, req)
  },
  updateFinding(id: string, req: FindingSaveReq): Promise<PlanDetail> {
    return http.put<PlanDetail>(`/qlt/audit-findings/${id}`, req)
  },
  removeFinding(id: string): Promise<void> {
    return http.delete<void>(`/qlt/audit-findings/${id}`)
  },
  /** 转质量问题：整改与验证走 P8-1。 */
  findingToIssue(id: string): Promise<PlanDetail> {
    return http.post<PlanDetail>(`/qlt/audit-findings/${id}/to-issue`)
  },
  /** 关闭不符合项：关联质量问题必须已关闭。 */
  closeFinding(id: string): Promise<PlanDetail> {
    return http.post<PlanDetail>(`/qlt/audit-findings/${id}/close`)
  }
}
