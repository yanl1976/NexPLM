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

/** BOM 视图（设计/工艺/制造）。 */
export const BOM_VIEW_LABEL: Record<string, string> = {
  DESIGN: '设计',
  PROCESS: '工艺',
  MANUFACTURE: '制造'
}

export interface BomNode {
  lineId: string
  childId: string
  code: string
  name: string
  itemType: string
  specification?: string | null
  unit?: string | null
  qty: number
  lossRate?: number | null
  lineNo: number
  refDesignator?: string | null
  remark?: string | null
  version: number
  hasChildren: boolean
  children: BomNode[]
}

export interface BomLineReq {
  childId?: string
  qty: number
  unit?: string | null
  lossRate?: number | null
  refDesignator?: string | null
  remark?: string | null
}

export interface WhereUsedRow {
  lineId: string
  parentId: string
  parentCode: string
  parentName: string
  viewType: string
  qty: number
  lineNo: number
}

export interface BomIssue {
  type: string
  lineId?: string | null
  parentId?: string | null
  childId?: string | null
  message: string
}

export interface BomValidateResult {
  total: number
  issues: BomIssue[]
}

export const BOM_ISSUE_LABEL: Record<string, string> = {
  QTY_INVALID: '用量异常',
  UNIT_MISSING: '单位缺失',
  ORPHAN: '孤儿件',
  CYCLE: '循环引用'
}

export const bomApi = {
  tree(itemId: string, viewType: string, depth = 5): Promise<BomNode[]> {
    return http.get<BomNode[]>(`/items/${itemId}/bom`, { viewType, depth })
  },
  add(itemId: string, viewType: string, req: BomLineReq): Promise<BomNode> {
    return http.post<BomNode>(`/items/${itemId}/bom?viewType=${encodeURIComponent(viewType)}`, req)
  },
  update(itemId: string, lineId: string, req: BomLineReq): Promise<BomNode> {
    return http.put<BomNode>(`/items/${itemId}/bom/lines/${lineId}`, req)
  },
  remove(itemId: string, lineId: string): Promise<void> {
    return http.delete<void>(`/items/${itemId}/bom/lines/${lineId}`)
  },
  move(itemId: string, lineId: string, req: { newParentId?: string | null; newLineNo?: number | null; viewType?: string }): Promise<BomNode> {
    return http.post<BomNode>(`/items/${itemId}/bom/lines/${lineId}/move`, req)
  },
  whereUsed(itemId: string): Promise<WhereUsedRow[]> {
    return http.get<WhereUsedRow[]>(`/items/${itemId}/bom/where-used`)
  },
  validate(itemId: string, viewType: string): Promise<BomValidateResult> {
    return http.get<BomValidateResult>(`/items/${itemId}/bom/validate`, { viewType })
  }
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
