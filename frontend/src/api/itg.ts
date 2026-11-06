import { http } from './http'

/* ------------------------------------------------------------ P9-1 集成框架 */

export const SYSTEM_CODE_LABEL: Record<string, string> = {
  KINGDEE: '金蝶云星空',
  WMS: '仓储系统',
  MES: '制造执行',
  OTHER: '其它系统'
}

export const AUTH_TYPE_LABEL: Record<string, string> = {
  NONE: '无认证',
  BASIC: 'Basic 用户名密码',
  BEARER: 'Bearer Token',
  HEADER: '自定义请求头'
}

export const JOB_STATUS_LABEL: Record<string, string> = {
  PREVIEW: '仅预览',
  PENDING: '待发送',
  RUNNING: '发送中',
  SUCCESS: '成功',
  FAILED: '失败待重试',
  DEAD: '死信'
}

export const TRANSFORM_LABEL: Record<string, string> = {
  UPPER: '转大写',
  LOWER: '转小写',
  TRIM: '去空格',
  STRING: '转文本',
  NUMBER: '转数值',
  BOOLEAN: '转布尔'
}

export interface EndpointRow {
  id: string
  code: string
  name: string
  systemCode: string
  baseUrl: string
  authType: string
  authConfigured: boolean
  timeoutMs: number
  enabled: boolean
  remark?: string | null
  updatedAt?: string | null
}

export interface EndpointDetail {
  id: string
  code: string
  name: string
  systemCode: string
  baseUrl: string
  authType: string
  authConfigured: boolean
  headers?: Record<string, unknown>
  timeoutMs: number
  enabled: boolean
  remark?: string | null
}

export interface EndpointSaveReq {
  code: string
  name: string
  systemCode?: string
  baseUrl: string
  authType?: string
  /** 不传表示"保持原有认证信息不变"（接口不回传密钥，避免前端把它清空） */
  authConfig?: Record<string, unknown> | null
  headers?: Record<string, unknown>
  timeoutMs?: number
  enabled?: boolean
  remark?: string | null
}

export interface MappingRule {
  source: string
  target: string
  transform?: string | null
  required?: boolean
}

export interface MappingRow {
  id: string
  endpointId: string
  endpointCode?: string | null
  objectCode: string
  direction: string
  payloadVersion: string
  ruleCount: number
  enabled: boolean
  remark?: string | null
  updatedAt?: string | null
}

export interface MappingDetail {
  id: string
  endpointId: string
  endpointCode?: string | null
  objectCode: string
  direction: string
  payloadVersion: string
  rules: MappingRule[]
  enabled: boolean
  remark?: string | null
}

export interface MappingSaveReq {
  endpointId: string
  objectCode: string
  direction?: string
  payloadVersion?: string
  rules: MappingRule[]
  enabled?: boolean
  remark?: string | null
}

export interface JobRow {
  id: string
  jobNo: string
  endpointId: string
  endpointCode?: string | null
  bizType: string
  bizKey: string
  direction: string
  status: string
  attempt: number
  maxAttempt: number
  httpStatus?: number | null
  durationMs: number
  nextRetryAt?: string | null
  createdAt?: string | null
  finishedAt?: string | null
}

export interface JobLogRow {
  id: string
  attempt: number
  status: string
  httpStatus?: number | null
  durationMs?: number | null
  errorMsg?: string | null
  createdAt?: string | null
}

export interface JobDetail {
  id: string
  jobNo: string
  endpointId: string
  endpointCode?: string | null
  mappingId?: string | null
  bizType: string
  bizKey: string
  direction: string
  status: string
  attempt: number
  maxAttempt: number
  httpStatus?: number | null
  durationMs: number
  errorMsg?: string | null
  requestPayload: unknown
  responseBody?: string | null
  nextRetryAt?: string | null
  startedAt?: string | null
  finishedAt?: string | null
  logs: JobLogRow[]
}

export interface JobSubmitResult {
  job: JobDetail
  duplicated: boolean
}

export const itgApi = {
  endpointPage(params: {
    keyword?: string
    enabled?: boolean
    page?: number
    size?: number
  }): Promise<{ items: EndpointRow[]; total: number; page: number; size: number }> {
    return http.get('/itg/endpoints', params)
  },
  endpointDetail(id: string): Promise<EndpointDetail> {
    return http.get<EndpointDetail>(`/itg/endpoints/${id}`)
  },
  createEndpoint(req: EndpointSaveReq): Promise<EndpointDetail> {
    return http.post<EndpointDetail>('/itg/endpoints', req)
  },
  updateEndpoint(id: string, req: EndpointSaveReq): Promise<EndpointDetail> {
    return http.put<EndpointDetail>(`/itg/endpoints/${id}`, req)
  },
  removeEndpoint(id: string): Promise<void> {
    return http.delete<void>(`/itg/endpoints/${id}`)
  },

  mappingPage(params: {
    endpointId?: string
    objectCode?: string
    page?: number
    size?: number
  }): Promise<{ items: MappingRow[]; total: number; page: number; size: number }> {
    return http.get('/itg/mappings', params)
  },
  mappingDetail(id: string): Promise<MappingDetail> {
    return http.get<MappingDetail>(`/itg/mappings/${id}`)
  },
  createMapping(req: MappingSaveReq): Promise<MappingDetail> {
    return http.post<MappingDetail>('/itg/mappings', req)
  },
  updateMapping(id: string, req: MappingSaveReq): Promise<MappingDetail> {
    return http.put<MappingDetail>(`/itg/mappings/${id}`, req)
  },
  removeMapping(id: string): Promise<void> {
    return http.delete<void>(`/itg/mappings/${id}`)
  },

  jobPage(params: {
    endpointId?: string
    bizType?: string
    status?: string
    page?: number
    size?: number
  }): Promise<{ items: JobRow[]; total: number; page: number; size: number }> {
    return http.get('/itg/jobs', params)
  },
  jobDetail(id: string): Promise<JobDetail> {
    return http.get<JobDetail>(`/itg/jobs/${id}`)
  },
  submitJob(req: {
    endpointId: string
    bizType: string
    bizKey: string
    payload: unknown
    dryRun?: boolean
    maxAttempt?: number
  }): Promise<JobSubmitResult> {
    return http.post<JobSubmitResult>('/itg/jobs', req)
  },
  dispatchJob(id: string): Promise<JobDetail> {
    return http.post<JobDetail>(`/itg/jobs/${id}/dispatch`)
  },
  replayJob(id: string): Promise<JobDetail> {
    return http.post<JobDetail>(`/itg/jobs/${id}/replay`)
  },

  /** 金蝶下发：由服务端按本地对象组装源报文（bizKey 带版本号，同版本重复下发幂等）。 */
  kingdeePreview(req: {
    bizType: string
    bizId: string
    viewType?: string
  }): Promise<{ bizType: string; bizKey: string; payload: unknown }> {
    return http.post('/itg/kingdee/preview', req)
  },
  kingdeePush(req: {
    endpointId: string
    bizType: string
    bizId: string
    viewType?: string
    dryRun?: boolean
    maxAttempt?: number
  }): Promise<JobSubmitResult> {
    return http.post<JobSubmitResult>('/itg/kingdee/push', req)
  }
}

/** 金蝶下发支持的业务类型（与后端 KingdeePayloadService 一致）。 */
export const KINGDEE_BIZ_LABEL: Record<string, string> = {
  Material: '物料',
  Bom: 'BOM',
  Doc: '图文档',
  Route: '工艺路线'
}
