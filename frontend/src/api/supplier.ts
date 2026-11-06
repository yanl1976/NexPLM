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
