import { http } from './http'

/** 图纸关联行：物料 ↔ 图纸文档（蓝本 PSEITEMRELATION.Specification）。 */
export interface DrawingLinkRow {
  link_id: string
  item_id: string
  doc_id: string
  dwg_no: string
  doc_name: string
  item_code?: string | null
  item_name?: string | null
  lifecycle_state?: string | null
  revision?: number | null
  iteration?: number | null
  file_ext?: string | null
  file_count?: number | null
}

/** 新建图纸请求：对应蓝本 DOCDATASET + DOCBASEFORM 的图档属性。 */
export interface DrawingSaveReq {
  /** 关联物料；为空时只建图纸文档，稍后再关联。 */
  itemId?: string | null
  code: string
  name: string
  description?: string
  classId?: string | null
  securityLevel?: number | null
  standardCode?: string
  yearNo?: string
  oldCode?: string
  spec?: string
  importance?: string
  precisionGrade?: string
  measureUnit?: string
  attachNo?: number | null
  attachType?: string
  projectCode?: string
  fileType?: string
}

export const drawingApi = {
  /** 新建图纸（建文档 + 存图档属性 + 建 Specification 关系）。 */
  create(req: DrawingSaveReq) {
    return http.post<{ docId: string; code: string; name: string }>('/drawings', req)
  },
  /** 某个物料挂了哪些图纸。 */
  byItem(itemId: string) {
    return http.get<DrawingLinkRow[]>(`/drawings/items/${itemId}`)
  },
  /** 某张图纸关联了哪些物料。 */
  byDoc(docId: string) {
    return http.get<DrawingLinkRow[]>(`/drawings/docs/${docId}/items`)
  },
  /** 把已有图纸文档关联到物料。 */
  link(docId: string, itemId: string) {
    return http.post<void>(`/drawings/${docId}/link?itemId=${encodeURIComponent(itemId)}`, {})
  },
  /** 解除关联（不删文档）。 */
  remove(linkId: string) {
    return http.delete<void>(`/drawings/${linkId}`)
  }
}
