import { http } from './http'

/** P8-4 供应商主数据与零件-供应商关系。 */

export const SUPPLIER_CATEGORY_LABEL: Record<string, string> = {
  MATERIAL: '原材料',
  OUTSOURCE: '外协加工',
  SERVICE: '服务',
  LOGISTICS: '物流'
}

export const SUPPLIER_STATUS_LABEL: Record<string, string> = {
  PENDING: '待准入',
  QUALIFIED: '合格',
  SUSPENDED: '暂停',
  DISQUALIFIED: '淘汰'
}

export const SUPPLY_TYPE_LABEL: Record<string, string> = {
  BUY: '外购',
  OUTSOURCE: '外协',
  DOUBLE: '双源'
}

export interface SupplierRow {
  id: string
  code: string
  name: string
  shortName?: string | null
  category: string
  status: string
  contact?: string | null
  phone?: string | null
  orgId?: string | null
  orgName?: string | null
  partCount: number
  preferredCount: number
  /** 最近一次确认的评级结果（P8-5-1），未评级时为空 */
  grade?: string | null
  ratingScore?: number | null
  ratedAt?: string | null
  updatedAt?: string | null
}

export interface SupplierPartRow {
  id: string
  supplierId: string
  supplierCode?: string | null
  supplierName?: string | null
  objectType: string
  objectId: string
  itemCode?: string | null
  itemName?: string | null
  supplyType: string
  preferred: boolean
  leadTimeDays?: number | null
  moq?: number | null
  price?: number | null
  currency: string
  status: string
  remark?: string | null
  updatedAt?: string | null
}

export interface SupplierDetail {
  id: string
  code: string
  name: string
  shortName?: string | null
  category: string
  status: string
  contact?: string | null
  phone?: string | null
  email?: string | null
  address?: string | null
  taxNo?: string | null
  currency: string
  paymentTerms?: string | null
  orgId?: string | null
  orgName?: string | null
  remark?: string | null
  partCount: number
  preferredCount: number
  grade?: string | null
  ratingScore?: number | null
  ratedAt?: string | null
  parts: SupplierPartRow[]
}

export interface SupplierSaveReq {
  name: string
  shortName?: string | null
  category?: string
  status?: string
  contact?: string | null
  phone?: string | null
  email?: string | null
  address?: string | null
  taxNo?: string | null
  currency?: string
  paymentTerms?: string | null
  orgId?: string | null
  remark?: string | null
}

export interface PartSaveReq {
  objectType?: string
  objectId: string
  supplyType?: string
  preferred?: boolean
  leadTimeDays?: number | null
  moq?: number | null
  price?: number | null
  currency?: string
  status?: string
  remark?: string | null
}

/* ------------------------------------------------------------ P8-5-1 评级 */

export const RATING_STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认'
}

export const SCHEME_STATUS_LABEL: Record<string, string> = {
  ACTIVE: '启用',
  INACTIVE: '停用'
}

/** 等级档位：min 为该档最低总分，列表按 min 从高到低。 */
export interface GradeBand {
  grade: string
  min: number
}

export interface RatingDim {
  id: string
  code: string
  name: string
  weight: number
  maxScore: number
  sortNo: number
  enabled: boolean
}

export interface RatingSchemeRow {
  id: string
  code: string
  name: string
  category?: string | null
  suspendGrade?: string | null
  status: string
  dimCount: number
  weightSum: number
  updatedAt?: string | null
}

export interface RatingSchemeDetail {
  id: string
  code: string
  name: string
  category?: string | null
  gradeRule: GradeBand[]
  suspendGrade?: string | null
  status: string
  remark?: string | null
  ratingCount: number
  dims: RatingDim[]
}

export interface DimSaveReq {
  code?: string | null
  name: string
  weight: number
  maxScore?: number | null
  enabled?: boolean
}

export interface RatingSchemeSaveReq {
  name: string
  category?: string | null
  gradeRule: GradeBand[]
  suspendGrade?: string | null
  status?: string
  remark?: string | null
  dims: DimSaveReq[]
}

export interface RatingItem {
  id: string
  dimCode: string
  dimName: string
  weight: number
  maxScore: number
  score: number
  comment?: string | null
}

export interface SupplierRatingRow {
  id: string
  supplierId: string
  supplierCode?: string | null
  supplierName?: string | null
  schemeId: string
  schemeCode?: string | null
  schemeName?: string | null
  period: string
  status: string
  totalScore?: number | null
  grade?: string | null
  ratedAt?: string | null
  confirmedAt?: string | null
}

export interface SupplierRatingDetail extends SupplierRatingRow {
  remark?: string | null
  items: RatingItem[]
}

export interface RatingItemReq {
  dimId: string
  score: number
  comment?: string | null
}

export interface RatingSaveReq {
  supplierId: string
  schemeId: string
  period: string
  remark?: string | null
  items: RatingItemReq[]
}

export const ratingApi = {
  schemes(params?: { category?: string; status?: string }): Promise<RatingSchemeRow[]> {
    return http.get<RatingSchemeRow[]>('/sup/rating-schemes', params)
  },
  scheme(id: string): Promise<RatingSchemeDetail> {
    return http.get<RatingSchemeDetail>(`/sup/rating-schemes/${id}`)
  },
  createScheme(req: RatingSchemeSaveReq): Promise<RatingSchemeDetail> {
    return http.post<RatingSchemeDetail>('/sup/rating-schemes', req)
  },
  updateScheme(id: string, req: RatingSchemeSaveReq): Promise<RatingSchemeDetail> {
    return http.put<RatingSchemeDetail>(`/sup/rating-schemes/${id}`, req)
  },
  removeScheme(id: string): Promise<void> {
    return http.delete<void>(`/sup/rating-schemes/${id}`)
  },
  page(params: {
    supplierId?: string
    schemeId?: string
    period?: string
    status?: string
    page?: number
    size?: number
  }): Promise<{ items: SupplierRatingRow[]; total: number; page: number; size: number }> {
    return http.get('/sup/ratings', params)
  },
  detail(id: string): Promise<SupplierRatingDetail> {
    return http.get<SupplierRatingDetail>(`/sup/ratings/${id}`)
  },
  create(req: RatingSaveReq): Promise<SupplierRatingDetail> {
    return http.post<SupplierRatingDetail>('/sup/ratings', req)
  },
  update(id: string, req: RatingSaveReq): Promise<SupplierRatingDetail> {
    return http.put<SupplierRatingDetail>(`/sup/ratings/${id}`, req)
  },
  confirm(id: string): Promise<SupplierRatingDetail> {
    return http.post<SupplierRatingDetail>(`/sup/ratings/${id}/confirm`)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/sup/ratings/${id}`)
  },
  bySupplier(supplierId: string): Promise<SupplierRatingRow[]> {
    return http.get<SupplierRatingRow[]>(`/sup/suppliers/${supplierId}/ratings`)
  }
}

/* ------------------------------------------------------------ P8-5-2 准入评审 */

export const ADMISSION_TYPE_LABEL: Record<string, string> = {
  INITIAL: '首次准入',
  ANNUAL: '年度复审',
  SPECIAL: '专项评审'
}

export const ADMISSION_STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

export const ADMISSION_RESULT_LABEL: Record<string, string> = {
  PASS: '通过',
  FAIL: '不通过',
  NA: '不适用'
}

export interface AdmissionItemRow {
  id?: string
  seq: number
  checkItem: string
  result: string
  evidence?: string | null
  remark?: string | null
}

export interface AdmissionRow {
  id: string
  code: string
  supplierId: string
  supplierCode?: string | null
  supplierName?: string | null
  type: string
  status: string
  plannedDate?: string | null
  actualDate?: string | null
  leader?: string | null
  itemCount: number
  passCount: number
  failCount: number
  submittedAt?: string | null
  approvedAt?: string | null
  updatedAt?: string | null
}

export interface AdmissionDetail extends AdmissionRow {
  conclusion?: string | null
  rejectReason?: string | null
  remark?: string | null
  items: AdmissionItemRow[]
}

export interface AdmissionItemReq {
  checkItem: string
  result?: string
  evidence?: string | null
  remark?: string | null
}

export interface AdmissionSaveReq {
  supplierId: string
  type?: string
  plannedDate?: string | null
  leader?: string | null
  remark?: string | null
  items: AdmissionItemReq[]
}

export const admissionApi = {
  page(params: {
    supplierId?: string
    status?: string
    type?: string
    page?: number
    size?: number
  }): Promise<{ items: AdmissionRow[]; total: number; page: number; size: number }> {
    return http.get('/sup/admissions', params)
  },
  detail(id: string): Promise<AdmissionDetail> {
    return http.get<AdmissionDetail>(`/sup/admissions/${id}`)
  },
  create(req: AdmissionSaveReq): Promise<AdmissionDetail> {
    return http.post<AdmissionDetail>('/sup/admissions', req)
  },
  update(id: string, req: AdmissionSaveReq): Promise<AdmissionDetail> {
    return http.put<AdmissionDetail>(`/sup/admissions/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/sup/admissions/${id}`)
  },
  submit(id: string): Promise<AdmissionDetail> {
    return http.post<AdmissionDetail>(`/sup/admissions/${id}/submit`)
  },
  approve(id: string, conclusion?: string): Promise<AdmissionDetail> {
    return http.post<AdmissionDetail>(`/sup/admissions/${id}/approve`, { conclusion: conclusion ?? null })
  },
  reject(id: string, reason: string): Promise<AdmissionDetail> {
    return http.post<AdmissionDetail>(`/sup/admissions/${id}/reject`, { reason })
  },
  bySupplier(supplierId: string): Promise<AdmissionRow[]> {
    return http.get<AdmissionRow[]>(`/sup/suppliers/${supplierId}/admissions`)
  }
}

export const supplierApi = {
  page(params: {
    keyword?: string
    category?: string
    status?: string
    page?: number
    size?: number
  }) {
    return http.get<{ items: SupplierRow[]; total: number; page: number; size: number }>(
      '/sup/suppliers',
      params
    )
  },
  detail(id: string): Promise<SupplierDetail> {
    return http.get<SupplierDetail>(`/sup/suppliers/${id}`)
  },
  create(req: SupplierSaveReq): Promise<SupplierDetail> {
    return http.post<SupplierDetail>('/sup/suppliers', req)
  },
  update(id: string, req: SupplierSaveReq): Promise<SupplierDetail> {
    return http.put<SupplierDetail>(`/sup/suppliers/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/sup/suppliers/${id}`)
  },
  /** 新增零件关系：同一零件多供应商，首选唯一（后端互斥）。 */
  addPart(id: string, req: PartSaveReq): Promise<SupplierDetail> {
    return http.post<SupplierDetail>(`/sup/suppliers/${id}/parts`, req)
  },
  updatePart(partId: string, req: PartSaveReq): Promise<SupplierDetail> {
    return http.put<SupplierDetail>(`/sup/parts/${partId}`, req)
  },
  removePart(partId: string): Promise<void> {
    return http.delete<void>(`/sup/parts/${partId}`)
  },
  /** 按零件反查供应商（多源询价）。 */
  partsByItem(objectId: string, objectType = 'PdmItem'): Promise<SupplierPartRow[]> {
    return http.get<SupplierPartRow[]>('/sup/parts/by-item', { objectId, objectType })
  }
}
