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

/**
 * 编码发放流水（T-CORE-02）：谁在什么时候、按哪条规则、给哪个细分类型取了什么码。
 * 对应蓝本 `CODERES`（60,300 行）的等价物。
 */
export interface CodeIssueRow {
  id: string
  ruleCode: string
  code: string
  /** 细分对象类型，如 `PdmItem:PART`；早期取号可能为空。 */
  objectType: string | null
  issuedBy: string | null
  issuedByName: string | null
  issuedAt: string
}

export const codingApi = {
  rules: () => http.get<CodeRule[]>('/system/coding/rules'),
  /** 发放流水分页（按规则/类型/编码模糊过滤，倒序）。 */
  issues: (params: {
    ruleCode?: string
    objectType?: string
    code?: string
    page?: number
    size?: number
  }) =>
    http.get<{ items: CodeIssueRow[]; total: number; page: number; size: number }>(
      '/system/coding/issues',
      params
    ),
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
