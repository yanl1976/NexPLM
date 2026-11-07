import { downloadFileByPost, http, saveBlob, uploadFile } from './http'

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

/* ------------------------------------------------------------ P8-5-3 数据发放 */

export const ISSUANCE_TYPE_LABEL: Record<string, string> = {
  DESIGN: '设计资料',
  QUALITY: '质量要求',
  OTHER: '其它'
}

export const ISSUANCE_STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  SENT: '已发放',
  RECEIVED: '已回执',
  CANCELLED: '已取消'
}

/** 明细行状态：pullState=附件是否已拉取，replyState=该条图纸是否被确认（批次 E-1 新增）。 */
export interface IssuanceItemRow {
  id: string
  objectType: string
  objectId: string
  itemCode?: string | null
  itemName?: string | null
  revision?: string | null
  sortNo: number
  pullState?: string | null
  replyState?: string | null
}

/** 协同回复（往来记录）。 */
export interface IssuanceReplyRow {
  id: string
  replyType: string
  content?: string | null
  promisedDate?: string | null
  attachmentFileIds?: string[]
  attachmentNames?: string[]
  repliedBy?: string | null
  repliedByName?: string | null
  repliedAt?: string | null
}

export interface IssuanceRow {
  id: string
  code: string
  supplierId: string
  supplierCode?: string | null
  supplierName?: string | null
  title: string
  type: string
  status: string
  itemCount: number
  replyCount?: number
  sentAt?: string | null
  receivedAt?: string | null
  receiptFileName?: string | null
  important?: boolean
  readAt?: string | null
  effectiveDate?: string | null
  expireDate?: string | null
  supplierGroup?: string | null
  updatedAt?: string | null
}

export interface IssuanceDetail extends IssuanceRow {
  remark?: string | null
  receiptFileId?: string | null
  receiptNote?: string | null
  items: IssuanceItemRow[]
  replies: IssuanceReplyRow[]
}

export interface IssuanceReplyReq {
  replyType?: string
  content?: string | null
  promisedDate?: string | null
  attachmentFileIds?: string[]
  attachmentNames?: string[]
  confirmedItemIds?: string[]
  rejectedItemIds?: string[]
}

/** 协同回复类型（旧系统没有业务分类，本批按需求方口径定义）。 */
export const ISSUANCE_REPLY_TYPE_LABEL: Record<string, string> = {
  ACK: '已收阅',
  CONFIRM: '图纸确认',
  COMMIT_DATE: '承诺交期',
  REJECT: '无法满足'
}

export interface IssuanceSaveReq {
  supplierId: string
  title: string
  type?: string
  remark?: string | null
  /** 批次 E-1：供应商分组 / 生效失效 / 重要标记（失效由服务端在 send/reply/receipt 校验）。 */
  supplierGroup?: string | null
  effectiveDate?: string | null
  expireDate?: string | null
  important?: boolean
  items: { objectType?: string; objectId: string }[]
}

export const issuanceApi = {
  page(params: {
    supplierId?: string
    status?: string
    type?: string
    page?: number
    size?: number
  }): Promise<{ items: IssuanceRow[]; total: number; page: number; size: number }> {
    return http.get('/sup/issuances', params)
  },
  detail(id: string): Promise<IssuanceDetail> {
    return http.get<IssuanceDetail>(`/sup/issuances/${id}`)
  },
  create(req: IssuanceSaveReq): Promise<IssuanceDetail> {
    return http.post<IssuanceDetail>('/sup/issuances', req)
  },
  update(id: string, req: IssuanceSaveReq): Promise<IssuanceDetail> {
    return http.put<IssuanceDetail>(`/sup/issuances/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/sup/issuances/${id}`)
  },
  send(id: string): Promise<IssuanceDetail> {
    return http.post<IssuanceDetail>(`/sup/issuances/${id}/send`)
  },
  receipt(
    id: string,
    req: { fileId: string; fileName?: string | null; note?: string | null }
  ): Promise<IssuanceDetail> {
    return http.post<IssuanceDetail>(`/sup/issuances/${id}/receipt`, req)
  },
  bySupplier(supplierId: string): Promise<IssuanceRow[]> {
    return http.get<IssuanceRow[]>(`/sup/suppliers/${supplierId}/issuances`)
  },
  /** 协同回复（多轮，可带附件与逐条确认/驳回）。 */
  reply(id: string, req: IssuanceReplyReq): Promise<IssuanceDetail> {
    return http.post<IssuanceDetail>(`/sup/issuances/${id}/replies`, req)
  },
  markRead(id: string): Promise<void> {
    return http.post<void>(`/sup/issuances/${id}/read`)
  },
  setImportant(id: string, important: boolean): Promise<void> {
    return http.post<void>(`/sup/issuances/${id}/important?important=${important}`)
  },
  markItemPulled(id: string, itemId: string): Promise<void> {
    return http.post<void>(`/sup/issuances/${id}/items/${itemId}/pull`)
  },
  /** 上传回执文件（不绑对象，文件中心只存内容），返回 fileId 供登记。 */
  async uploadReceipt(file: File): Promise<{ fileId: string; fileName: string }> {
    const uploaded = await uploadFile<{ fileId: number | string; fileName: string }>(
      '/files',
      file
    )
    return { fileId: String(uploaded.fileId), fileName: uploaded.fileName }
  },
  /** 打包导出：manifest.csv + 零件主文件，直接触发浏览器下载。 */
  async exportZip(id: string): Promise<void> {
    const { blob, fileName } = await downloadFileByPost(`/sup/issuances/${id}/export`, {})
    saveBlob(blob, fileName)
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
