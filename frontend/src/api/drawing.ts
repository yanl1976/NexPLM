import { http } from './http'

/** 图纸关联行：物料 ↔ 图纸文档（蓝本 PSEITEMRELATION.Specification）。 */
export interface DrawingRow {
  link_id: string
  item_id: string
  item_code: string
  item_name: string
  item_type?: string | null
  doc_id: string
  dwg_no: string
  doc_name: string
  file_ext?: string | null
  file_count?: number | null
  sheet_size?: string | null
  created_at?: string | null
}

/** 关联图纸请求；docId 为空时按图号新建图纸文档。 */
export interface DrawingCreateReq {
  itemId: string
  dwgNo: string
  name?: string
  sheetSize?: string
  docId?: string | null
}

export const drawingApi = {
  list(params: { keyword?: string; itemId?: string; itemType?: string }) {
    return http.get<DrawingRow[]>('/drawings', params)
  },
  byItem(itemId: string) {
    return http.get<DrawingRow[]>(`/drawings/items/${itemId}`)
  },
  create(req: DrawingCreateReq) {
    return http.post<Record<string, unknown>>('/drawings', req)
  },
  remove(linkId: string) {
    return http.delete<void>(`/drawings/${linkId}`)
  }
}

/** 图纸管理的物料类型（GAUGE = 蓝本 PSEITEMGAUGEFORM 非标自制件）。 */
export const DRAWING_ITEM_TYPES: Array<{ value: string; label: string }> = [
  { value: 'GAUGE', label: '非标自制件' },
  { value: 'PART', label: '零件' },
  { value: 'ASSEMBLY', label: '部件' },
  { value: 'STANDARD', label: '标准件' },
  { value: 'PRODUCT', label: '产品' },
  { value: 'TOOL', label: '工装' }
]
