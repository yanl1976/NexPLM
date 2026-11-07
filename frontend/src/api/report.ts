import { downloadFileByPost, http } from './http'

/** 复刻批次 D｜报表管理：数据集白名单 + 模板 + 运行结果。 */

export type ReportParamType = 'TEXT' | 'DATE' | 'SELECT'

export interface ReportColumn {
  key: string
  label: string
  /** TEXT / NUMBER / DATE / DATETIME / BOOL（决定对齐与数字格式）。 */
  type: string
}

export interface ReportParamOption {
  value: string
  label: string
}

export interface ReportParamMeta {
  key: string
  label: string
  type: ReportParamType
  options?: ReportParamOption[] | null
}

export interface ReportDataset {
  key: string
  label: string
  domain: string
  columns: ReportColumn[]
  params: ReportParamMeta[]
}

export interface ReportTemplateRow {
  id: string
  code: string
  name: string
  dataset: string
  datasetLabel: string
  category: string
  params: ReportParamMeta[]
  description?: string | null
  /** true = 内置模板（全局可见、只读）。 */
  builtIn: boolean
  sortNo: number
  updatedAt?: string | null
}

export interface ReportTemplateSaveReq {
  code?: string
  name: string
  dataset: string
  category?: string
  params?: { key: string; label?: string }[]
  description?: string | null
  sortNo?: number
}

export interface ReportRunResult {
  dataset: string
  datasetLabel: string
  templateCode: string
  templateName: string
  columns: ReportColumn[]
  rows: Record<string, any>[]
  total: number
  page: number
  size: number
}

export interface ReportPage<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export const REPORT_CATEGORY_LABEL: Record<string, string> = {
  DOC: '文档',
  ITEM: '零部件/BOM',
  CHANGE: '变更',
  CAPP: '工艺',
  PROJECT: '项目',
  OTHER: '其它'
}

export const reportApi = {
  datasets: () => http.get<ReportDataset[]>('/ana/reports/datasets'),
  page: (params: { keyword?: string; dataset?: string; page?: number; size?: number }) =>
    http.get<ReportPage<ReportTemplateRow>>('/ana/reports', params),
  detail: (id: string) => http.get<ReportTemplateRow>(`/ana/reports/${id}`),
  create: (body: ReportTemplateSaveReq) => http.post<ReportTemplateRow>('/ana/reports', body),
  update: (id: string, body: ReportTemplateSaveReq) =>
    http.put<ReportTemplateRow>(`/ana/reports/${id}`, body),
  remove: (id: string) => http.delete<void>(`/ana/reports/${id}`),
  run: (id: string, body: { params?: Record<string, string>; page?: number; size?: number }) =>
    http.post<ReportRunResult>(`/ana/reports/${id}/run`, body),
  /** 导出 xlsx / pdf：后端按模板 + 参数取全部结果后渲染（不分页）。 */
  exportXlsx: (id: string, params: Record<string, string>) =>
    downloadFileByPost(`/ana/reports/${id}/export.xlsx`, { params }),
  exportPdf: (id: string, params: Record<string, string>) =>
    downloadFileByPost(`/ana/reports/${id}/export.pdf`, { params })
}
