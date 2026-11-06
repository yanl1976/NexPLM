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
  /** 引用的工艺资源（P6-3 引用统计口径）。 */
  resourceId?: string | null
  resourceCode?: string | null
  resourceName?: string | null
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
  /** 工艺负责人（工艺分工）。 */
  ownerId?: string | null
  ownerName?: string | null
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
  ownerId?: string | null
  steps: RoutingStep[]
}

// ---------------------------------------------------------------- 工艺资源库（P6-3）

export interface ResourceClass {
  id: string
  parentId?: string | null
  code: string
  name: string
  sortNo: number
  remark?: string | null
  resourceCount: number
}

export interface ResourceClassSaveReq {
  code: string
  name: string
  parentId?: string | null
  sortNo?: number
  remark?: string | null
}

export interface CappResource {
  id: string
  classId: string
  classCode?: string | null
  className?: string | null
  code: string
  name: string
  spec?: string | null
  unit?: string | null
  status: 'ACTIVE' | 'DISABLED'
  attrs: Record<string, string>
  remark?: string | null
  referenceCount: number
  updatedAt?: string | null
}

export interface ResourceSaveReq {
  classId: string
  code: string
  name: string
  spec?: string | null
  unit?: string | null
  status?: string
  attrs?: Record<string, string>
  remark?: string | null
}

export interface ResourceReference {
  routingId: string
  routingCode: string
  routingName: string
  objectId: string
  objectCode?: string | null
  seqNo: number
  stepName: string
}

export interface ResourceQuery {
  id: string
  name: string
  classId?: string | null
  keyword?: string | null
  attrs: Record<string, string>
  createdAt?: string | null
}

export const resourceApi = {
  classes(): Promise<ResourceClass[]> {
    return http.get<ResourceClass[]>(`/capp/resource-classes`)
  },
  createClass(req: ResourceClassSaveReq): Promise<ResourceClass> {
    return http.post<ResourceClass>(`/capp/resource-classes`, req)
  },
  updateClass(id: string, req: ResourceClassSaveReq): Promise<ResourceClass> {
    return http.put<ResourceClass>(`/capp/resource-classes/${id}`, req)
  },
  removeClass(id: string): Promise<void> {
    return http.delete<void>(`/capp/resource-classes/${id}`)
  },
  list(params: {
    page?: number
    size?: number
    classId?: string
    keyword?: string
    status?: string
    attrs?: string
  }): Promise<{ items: CappResource[]; total: number; page: number; size: number }> {
    return http.get(`/capp/resources`, params)
  },
  detail(id: string): Promise<CappResource> {
    return http.get<CappResource>(`/capp/resources/${id}`)
  },
  create(req: ResourceSaveReq): Promise<CappResource> {
    return http.post<CappResource>(`/capp/resources`, req)
  },
  update(id: string, req: ResourceSaveReq): Promise<CappResource> {
    return http.put<CappResource>(`/capp/resources/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/capp/resources/${id}`)
  },
  references(id: string): Promise<ResourceReference[]> {
    return http.get<ResourceReference[]>(`/capp/resources/${id}/references`)
  },
  queries(): Promise<ResourceQuery[]> {
    return http.get<ResourceQuery[]>(`/capp/resource-queries`)
  },
  saveQuery(req: {
    name: string
    classId?: string | null
    keyword?: string | null
    attrs?: Record<string, string>
  }): Promise<ResourceQuery> {
    return http.post<ResourceQuery>(`/capp/resource-queries`, req)
  },
  removeQuery(id: string): Promise<void> {
    return http.delete<void>(`/capp/resource-queries/${id}`)
  },
  runQuery(
    id: string,
    params?: { page?: number; size?: number }
  ): Promise<{ items: CappResource[]; total: number; page: number; size: number }> {
    return http.get(`/capp/resource-queries/${id}/run`, params)
  }
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
  /** 序号重建：按当前顺序重排为 10/20/30…（仅草稿）。 */
  resequence(id: string): Promise<RoutingDetail> {
    return http.post<RoutingDetail>(`/capp/routings/${id}/resequence`)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/capp/routings/${id}`)
  }
}
