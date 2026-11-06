import { http } from './http'

/** 工艺路线状态。 */
export type RoutingStatus = 'DRAFT' | 'RELEASED' | 'ARCHIVED'

export const ROUTING_STATUS_LABEL: Record<RoutingStatus, string> = {
  DRAFT: '草稿',
  RELEASED: '已发布',
  ARCHIVED: '已归档'
}

/** 工序类型（与后端 CappRoutingService.OP_TYPES 保持一致）。 */
export const OP_TYPE_LABEL: Record<string, string> = {
  MACHINING: '机加工',
  ASSEMBLY: '装配',
  INSPECTION: '检验',
  HEAT: '热处理',
  SURFACE: '表面处理',
  OTHER: '其它'
}

export interface RoutingStep {
  id?: string
  seqNo: number
  name: string
  opType: string
  workCenter?: string | null
  setupMinutes?: number | null
  runMinutes?: number | null
  remark?: string | null
}

export interface RoutingRow {
  id: string
  code: string
  name: string
  objectId: string
  objectCode?: string | null
  objectName?: string | null
  versionNo: number
  status: RoutingStatus
  applicability?: string | null
  stepCount: number
  totalMinutes?: number | null
  releasedAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface RoutingDetail extends RoutingRow {
  remark?: string | null
  /** 路线串：10 下料 → 20 车削 → 30 检验 */
  routeString: string
  steps: RoutingStep[]
}

export interface RoutingSaveReq {
  name: string
  objectId: string
  applicability?: string | null
  remark?: string | null
  steps: RoutingStep[]
}

export const cappApi = {
  list(params: {
    page?: number
    size?: number
    keyword?: string
    objectId?: string
    status?: string
  }): Promise<{ items: RoutingRow[]; total: number; page: number; size: number }> {
    return http.get(`/capp/routings`, params)
  },
  detail(id: string): Promise<RoutingDetail> {
    return http.get<RoutingDetail>(`/capp/routings/${id}`)
  },
  create(req: RoutingSaveReq): Promise<RoutingDetail> {
    return http.post<RoutingDetail>(`/capp/routings`, req)
  },
  update(id: string, req: RoutingSaveReq): Promise<RoutingDetail> {
    return http.put<RoutingDetail>(`/capp/routings/${id}`, req)
  },
  release(id: string): Promise<RoutingDetail> {
    return http.post<RoutingDetail>(`/capp/routings/${id}/release`)
  },
  archive(id: string): Promise<RoutingDetail> {
    return http.post<RoutingDetail>(`/capp/routings/${id}/archive`)
  },
  copy(id: string): Promise<RoutingDetail> {
    return http.post<RoutingDetail>(`/capp/routings/${id}/copy`)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/capp/routings/${id}`)
  }
}
