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

/** 编码种类（对应蓝本 CODEKIND）：每个种类一条码段规则，分类通过 codePath 绑定。 */
export interface CodeKindRow {
  code: string
  name: string
  ruleCode: string
  legacyId?: string | null
  valueCount: number
  classCount: number
}

/** 码值（对应蓝本 CODEVALUE）；valuePath 形如 `1001` 或 `1001/02`，前端据此拼树。 */
export interface CodeValueRow {
  valuePath: string
  codeValue: string
  name?: string | null
  segment?: string | null
  sort: number
  /** 已停用：不允许新绑定，历史编码仍引用。 */
  deleted: boolean
  /** 大类名称（分类树 `10`/`11`/`12`），用于左侧分组展示。 */
  majorName?: string | null
}

/** 码段（对应蓝本 CODERULE）：大类 ord1 / 小类 ord2（父=大类）/ 流水码 ord3。 */
export interface CodeSegmentRow {
  code: string
  name: string
  orderNo: number
  /** 段长；0 或 null 表示不定长（枚举型码值）。 */
  length?: number | null
  parentCode?: string | null
  dropMode?: string | null
  status: string
}

/** 新增码值请求；parentPath 为空表示大类。 */
export interface CodeValueCreateReq {
  codeValue: string
  name?: string
  parentPath?: string
}

/** 改码值请求：只有名称与停用标记，**没有 codeValue**（结构上禁止改值）。 */
export interface CodeValueUpdateReq {
  valuePath: string
  name?: string
  deleted?: boolean
}

export const codingApi = {
  rules: () => http.get<CodeRule[]>('/system/coding/rules'),
  /** 编码种类列表（含码值数、已绑定分类数）。 */
  kinds: () => http.get<CodeKindRow[]>('/system/coding/kinds'),
  /** 某个种类的码段（大类/小类/流水码，含段长与父段）。 */
  kindSegments: (code: string) => http.get<CodeSegmentRow[]>(`/system/coding/kinds/${code}/segments`),
  /** 某个种类下的码值（扁平；含已停用，用 deleted 区分）。 */
  kindValues: (code: string) => http.get<CodeValueRow[]>(`/system/coding/kinds/${code}/values`),
  /** 新增码值（只增不改值）。 */
  createValue: (kindCode: string, req: CodeValueCreateReq) =>
    http.post<CodeValueRow>(`/system/coding/kinds/${kindCode}/values`, req),
  /** 改码值：只允许改名/停用（不允许改值）。 */
  updateValue: (kindCode: string, req: CodeValueUpdateReq) =>
    http.put<void>(`/system/coding/kinds/${kindCode}/values`, req),
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
