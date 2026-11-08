import { http } from './http'

/** 图纸关联行：物料 ↔ 图纸文档（蓝本 PSEITEMRELATION.Specification）。 */
export interface DrawingLinkRow {
  link_id: string
  item_id: string
  doc_id: string
  dwg_no: string
  doc_name: string
  lifecycle_state?: string | null
  revision?: number | null
  iteration?: number | null
  file_ext?: string | null
  file_count?: number | null
}

export const drawingApi = {
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
