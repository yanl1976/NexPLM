import { http } from './http'

/** 公式参数定义（旧系统 para_Grid）。 */
export interface FormulaParam {
  name: string
  label?: string | null
  defaultValue?: number | null
  unit?: string | null
}

export interface Formula {
  id: string
  code: string
  name: string
  formulaType: string
  expression: string
  params: FormulaParam[]
  resultUnit: string | null
  explain: string | null
  enabled: boolean
}

export const formulaApi = {
  list: (keyword?: string, formulaType?: string) =>
    http.get<Formula[]>('/capp/formulas', { keyword, formulaType }),
  get: (id: string) => http.get<Formula>(`/capp/formulas/${id}`),
  create: (body: Record<string, unknown>) => http.post<Formula>('/capp/formulas', body),
  update: (id: string, body: Record<string, unknown>) => http.put<Formula>(`/capp/formulas/${id}`, body),
  remove: (id: string) => http.delete<void>(`/capp/formulas/${id}`),
  evaluate: (id: string, values: Record<string, number>) =>
    http.post<{ value: number; display: string }>(`/capp/formulas/${id}/evaluate`, values)
}
