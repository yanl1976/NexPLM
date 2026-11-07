import { http } from './http'

/** 段定义，对齐旧系统 Coding.coderule：固定文本 / 日期 / 流水码。 */
export interface CodeSegment {
  type: 'TEXT' | 'DATE' | 'SEQ' | string
  value?: string | null
  format?: string | null
  width?: number | null
  radix?: string | null
}

export interface CodeRule {
  id: string
  code: string
  name: string
  objectType: string | null
  segments: CodeSegment[]
  scopeKey: string
  currentNo: number
  lastPeriod: string | null
  resetPolicy: string
  enabled: boolean
  remark: string | null
  updatedAt: string | null
  preview: string | null
}

export const codingApi = {
  rules: () => http.get<CodeRule[]>('/system/coding/rules'),
  rule: (code: string) => http.get<CodeRule>(`/system/coding/rules/${code}`),
  preview: (code: string) => http.get<string>(`/system/coding/rules/${code}/preview`),
  update: (
    code: string,
    body: {
      name?: string
      segments?: CodeSegment[]
      resetPolicy?: string
      enabled?: boolean
      remark?: string
    }
  ) => http.put<CodeRule>(`/system/coding/rules/${code}`, body),
  reset: (code: string, body: { toNo: number; reason: string }) =>
    http.post<CodeRule>(`/system/coding/rules/${code}/reset`, body)
}
