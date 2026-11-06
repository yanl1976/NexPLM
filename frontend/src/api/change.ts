import { http } from './http'

/** 变更申请列表行。 */
export interface EcrRow {
  id: string
  code: string
  title: string
  changeType: string
  category?: string | null
  priority: string
  status: string
  requesterId: string
  requesterName?: string | null
  receiverId?: string | null
  projectNo?: string | null
  targetDate?: string | null
  createdAt: string | null
  affectedCount: number
}

/** 关联对象行。 */
export interface ChangeObjectRow {
  id: string
  objectType: string
  objectId: string
  objectCode?: string | null
  objectName?: string | null
  relation: string
  action?: string | null
  revisionFrom?: number | null
  revisionTo?: number | null
  execStatus: string
  remark?: string | null
}

/** 变更通知行（P5-2 接入前通常为空）。 */
export interface EcnRow {
  id: string
  code: string
  ecrId: string
  title: string
  status: string
  executorId?: string | null
  executorName?: string | null
  planDate?: string | null
  issuedAt?: string | null
  closedAt?: string | null
}

export interface EcrDetail {
  id: string
  code: string
  title: string
  changeType: string
  category?: string | null
  reason?: string | null
  feasibility?: string | null
  priority: string
  status: string
  requesterId: string
  requesterName?: string | null
  receiverId?: string | null
  receiverName?: string | null
  projectNo?: string | null
  targetDate?: string | null
  remark?: string | null
  approvedAt?: string | null
  closedAt?: string | null
  createdAt: string | null
  updatedAt: string | null
  objects: ChangeObjectRow[]
  ecns: EcnRow[]
}

export interface EcrSaveReq {
  title: string
  changeType: string
  category?: string | null
  reason?: string | null
  feasibility?: string | null
  priority?: string | null
  receiverId?: string | null
  projectNo?: string | null
  targetDate?: string | null
  remark?: string | null
}

export interface ChangeObjectAddReq {
  objectType: string
  objectId: string
  relation?: string | null
  action?: string | null
  remark?: string | null
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export const changeApi = {
  list(params: { keyword?: string; status?: string; page?: number; size?: number }) {
    return http.get<PageResult<EcrRow>>('/chg/ecrs', params)
  },
  detail(id: string) {
    return http.get<EcrDetail>(`/chg/ecrs/${id}`)
  },
  create(body: EcrSaveReq) {
    return http.post<EcrDetail>('/chg/ecrs', body)
  },
  update(id: string, body: EcrSaveReq) {
    return http.put<EcrDetail>(`/chg/ecrs/${id}`, body)
  },
  submit(id: string) {
    return http.post<EcrDetail>(`/chg/ecrs/${id}/submit`)
  },
  close(id: string) {
    return http.post<EcrDetail>(`/chg/ecrs/${id}/close`)
  },
  addObject(ecrId: string, body: ChangeObjectAddReq) {
    return http.post<ChangeObjectRow>(`/chg/ecrs/${ecrId}/objects`, body)
  },
  removeObject(ecrId: string, linkId: string) {
    return http.delete<void>(`/chg/ecrs/${ecrId}/objects/${linkId}`)
  }
}
