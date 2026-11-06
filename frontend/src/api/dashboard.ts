import { http } from './http'

/** 看板域的中文名；后端也会回 label，这里作为离线兜底。 */
export const DASHBOARD_DOMAIN_LABEL: Record<string, string> = {
  doc: '文档',
  item: '零部件',
  change: '变更',
  capp: '工艺路线',
  task: '项目任务',
  quality: '质量问题'
}

/**
 * 状态码 → 中文。
 * 文档/零部件用生命周期状态（首字母大写，且可被设计器自定义），其余域用大写枚举，
 * 因此两套写法都收在同一张表里；未命中的状态原样显示（不猜、不吞）。
 */
export const DASHBOARD_STATUS_LABEL: Record<string, string> = {
  Draft: '草稿',
  InWorkflow: '审批中',
  Released: '已发布',
  Obsolete: '已废弃',
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  REVIEWING: '评审中',
  APPROVED: '已批准',
  NOTICED: '已通知',
  EXECUTING: '执行中',
  CLOSED: '已关闭',
  REJECTED: '已驳回',
  CANCELLED: '已取消',
  ARCHIVED: '已归档',
  NOT_STARTED: '未开始',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
  BLOCKED: '阻塞',
  OPEN: '待处理',
  ASSIGNED: '已指派',
  VERIFYING: '验证中'
}

export interface DashboardCountRow {
  key: string
  count: number
}

export interface DashboardDomainCard {
  domain: string
  label: string
  total: number
  active: number
  done: number
  overdue: number
  doneRate: number
  addedInPeriod: number
  byStatus: DashboardCountRow[]
}

export interface DashboardTrendRow {
  month: string
  domains: Record<string, number>
  total: number
}

export interface DashboardOverview {
  from: string
  to: string
  domains: string[]
  cards: DashboardDomainCard[]
  trend: DashboardTrendRow[]
}

export interface DashboardDrillRow {
  id: string
  code: string
  name: string
  status: string
  owner?: string | null
  time?: string | null
}

export const dashboardApi = {
  overview(params: { from?: string; to?: string }): Promise<DashboardOverview> {
    return http.get<DashboardOverview>('/ana/dashboard', params)
  },
  items(
    domain: string,
    params: { status?: string; page?: number; size?: number }
  ): Promise<{ items: DashboardDrillRow[]; total: number; page: number; size: number }> {
    return http.get(`/ana/dashboard/${domain}/items`, params)
  }
}
