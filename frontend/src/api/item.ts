import { http } from './http'

/** 零部件类型（与后端 V2 的 ItemType 枚举一致）。 */
export const ITEM_TYPE_LABEL: Record<string, string> = {
  PRODUCT: '产品',
  ASSEMBLY: '部件',
  PART: '零件',
  STANDARD: '标准件',
  TOOL: '工装',
  GAUGE: '量具',
  MOLD: '模具',
  OUTSOURCE: '外购件'
}

/** 供应方式。 */
export const ITEM_SOURCE_LABEL: Record<string, string> = {
  MAKE: '自制',
  BUY: '外购'
}

export interface ItemSummary {
  id: string
  code: string
  name: string
  itemType: string
  specification?: string | null
  material?: string | null
  unit: string
  sourceType: string
  lifecycleState: string
  revision: number
  iteration: number
  revisionLabel: string
  updatedAt: string
}

export interface ItemDetail extends ItemSummary {
  weight?: number | null
  stdCode?: string | null
  remark?: string | null
  isLatest: boolean
  ownerId: string
  orgId?: string | null
  securityLevel: number
  createdAt: string
}

export interface ItemSaveReq {
  code?: string
  name: string
  itemType: string
  specification?: string | null
  material?: string | null
  unit?: string | null
  weight?: number | null
  sourceType?: string
  stdCode?: string | null
  remark?: string | null
  orgId?: string | null
  securityLevel?: number
}

export const itemApi = {
  list(params: {
    page?: number
    size?: number
    keyword?: string
    itemType?: string
    sourceType?: string
  }): Promise<{ items: ItemSummary[]; total: number; page: number; size: number }> {
    return http.get('/items', params)
  },
  detail(id: string): Promise<ItemDetail> {
    return http.get<ItemDetail>(`/items/${id}`)
  },
  create(req: ItemSaveReq): Promise<ItemDetail> {
    return http.post<ItemDetail>('/items', req)
  },
  update(id: string, req: ItemSaveReq): Promise<ItemDetail> {
    return http.put<ItemDetail>(`/items/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/items/${id}`)
  }
}
