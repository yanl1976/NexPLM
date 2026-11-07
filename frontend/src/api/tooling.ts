import { http } from './http'

/** 后端列表返回原始列名（snake_case），台账型表格直接消费。 */
export type Row = Record<string, any>

export const toolingApi = {
  list: (params: { keyword?: string; toolingType?: string; status?: string }) =>
    http.get<Row[]>('/capp/toolings', params),
  create: (body: Row) => http.post<string>('/capp/toolings', body),
  update: (id: string, body: Row) => http.put<void>(`/capp/toolings/${id}`, body),
  remove: (id: string) => http.delete<void>(`/capp/toolings/${id}`),

  applies: (status?: string) => http.get<Row[]>('/capp/tooling-applies', { status }),
  createApply: (body: Row) => http.post<string>('/capp/tooling-applies', body),
  submitApply: (id: string) => http.post<void>(`/capp/tooling-applies/${id}/submit`, {}),
  approveApply: (id: string, body: { approved: boolean; comment?: string; toolingCode?: string }) =>
    http.post<Record<string, unknown>>(`/capp/tooling-applies/${id}/approve`, body),

  borrows: (toolingId?: string) => http.get<Row[]>('/capp/tooling-borrows', { toolingId }),
  borrow: (body: { toolingId: string; routingId?: string | null; remark?: string }) =>
    http.post<string>('/capp/tooling-borrows', body),
  giveBack: (id: string) => http.post<void>(`/capp/tooling-borrows/${id}/return`, {})
}
