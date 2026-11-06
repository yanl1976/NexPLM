import { downloadFile, http, saveBlob, uploadFile } from './http'

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
  /** 有效性区间（含边界）；为空表示该侧不限。 */
  effectFrom?: string | null
  effectTo?: string | null
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
  effectFrom?: string | null
  effectTo?: string | null
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

/** BOM 展开行：qty 为逐层累乘后的用量，effectiveQty 再叠加本层损耗率；path 为层级路径。 */
export interface BomSummaryRow {
  level: number
  path: string
  childId: string
  code: string
  name: string
  itemType?: string | null
  specification?: string | null
  unit?: string | null
  qty: number
  lossRate?: number | null
  effectiveQty: number
  lineNo: number
  effectFrom?: string | null
  effectTo?: string | null
}

/** BOM 汇总行：同一子件在整棵树中的出现次数与累计用量。 */
export interface BomRollupRow {
  childId: string
  code: string
  name: string
  itemType?: string | null
  specification?: string | null
  unit?: string | null
  totalQty: number
  totalEffectiveQty: number
  occurrences: number
}

/** BOM 替代件：itemObjectId 为被替换件，alternate* 为备选件。 */
export interface BomAlternateRow {
  id: string
  itemObjectId: string
  alternateId: string
  code: string
  name: string
  itemType?: string | null
  specification?: string | null
  unit?: string | null
  viewType: string
  isGlobal: boolean
  isTwoWay: boolean
  priorityLevel?: string | null
  lineNo: number
  remark?: string | null
  version: number
}

export interface BomAlternateReq {
  alternateId?: string
  viewType?: string
  isGlobal?: boolean
  isTwoWay?: boolean
  priorityLevel?: string | null
  remark?: string | null
}

/** BOM 基线头。 */
export interface BomBaselineRow {
  id: string
  rootObjectId: string
  code: string
  name?: string | null
  viewType: string
  lineCount: number
  frozenAt: string
  remark?: string | null
  createdBy?: string | null
}

/** BOM 基线快照行。 */
export interface BomBaselineLineRow {
  id: string
  level: number
  path: string
  parentId?: string | null
  childId: string
  code?: string | null
  name?: string | null
  itemType?: string | null
  specification?: string | null
  unit?: string | null
  qty: number
  lossRate?: number | null
  lineNo: number
  effectFrom?: string | null
  effectTo?: string | null
}

export interface BomBaselineDiffRow {
  changeType: string
  path: string
  level: number
  childId: string
  code?: string | null
  name?: string | null
  unit?: string | null
  baselineQty?: number | null
  currentQty?: number | null
  baselineLossRate?: number | null
  currentLossRate?: number | null
}

export interface BomBaselineDiff {
  baselineId: string
  baselineCode: string
  viewType: string
  added: number
  removed: number
  changed: number
  rows: BomBaselineDiffRow[]
}

export interface BomBaselineReq {
  code: string
  name?: string | null
  viewType?: string
  depth?: number
  remark?: string | null
}

export const BASELINE_CHANGE_LABEL: Record<string, string> = {
  ADDED: '新增',
  REMOVED: '删除',
  QTY_CHANGED: '用量变更',
  CHILD_CHANGED: '子件替换'
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

/** 批量改用量：只提交需要修改的字段，其余留空表示不改。 */
export interface BomBatchQtyReq {
  lineIds: string[]
  qty?: number | null
  lossRate?: number | null
  unit?: string | null
}

/** 批量替换：把该根件树内的 fromChildId 全部换成 toChildId（parentIds 可限定范围）。 */
export interface BomBatchReplaceReq {
  fromChildId: string
  toChildId: string
  viewType?: string
  parentIds?: string[]
}

/** 批量结果：total 为命中行数，skipped 为命中但未处理的行，messages 为逐行原因。 */
export interface BomBatchResult {
  total: number
  updated: number
  skipped: number
  messages: string[]
}

export const bomApi = {
  tree(itemId: string, viewType: string, depth = 5, effectAt?: string): Promise<BomNode[]> {
    return http.get<BomNode[]>(`/items/${itemId}/bom`, { viewType, depth, effectAt })
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
  batchUpdateQty(itemId: string, req: BomBatchQtyReq): Promise<BomBatchResult> {
    return http.post<BomBatchResult>(`/items/${itemId}/bom/batch/update-qty`, req)
  },
  batchReplace(itemId: string, req: BomBatchReplaceReq): Promise<BomBatchResult> {
    return http.post<BomBatchResult>(`/items/${itemId}/bom/batch/replace`, req)
  },
  whereUsed(itemId: string): Promise<WhereUsedRow[]> {
    return http.get<WhereUsedRow[]>(`/items/${itemId}/bom/where-used`)
  },
  validate(itemId: string, viewType: string): Promise<BomValidateResult> {
    return http.get<BomValidateResult>(`/items/${itemId}/bom/validate`, { viewType })
  },
  summary(itemId: string, viewType: string, depth = 10, effectAt?: string): Promise<BomSummaryRow[]> {
    return http.get<BomSummaryRow[]>(`/items/${itemId}/bom/summary`, { viewType, depth, effectAt })
  },
  rollup(
    itemId: string,
    viewType: string,
    depth = 10,
    effectAt?: string,
    leafOnly = false
  ): Promise<BomRollupRow[]> {
    return http.get<BomRollupRow[]>(`/items/${itemId}/bom/rollup`, { viewType, depth, effectAt, leafOnly })
  }
}

/** Excel 导入预览行：action = NEW / UPDATE / SAME。 */
export interface BomExcelPreviewRow {
  rowNum: number
  levelCode: string
  parentLevelCode?: string | null
  childId: string
  code?: string | null
  name?: string | null
  qty: number
  lossRate?: number | null
  unit?: string | null
  effectFrom?: string | null
  effectTo?: string | null
  action: string
}

export interface BomExcelPreview {
  total: number
  valid: number
  invalid: number
  createCount: number
  updateCount: number
  unchangedCount: number
  rows: BomExcelPreviewRow[]
  errors: string[]
}

export interface BomExcelImportResult {
  created: number
  updated: number
  skipped: number
}

export const BOM_IMPORT_ACTION_LABEL: Record<string, string> = {
  NEW: '新增',
  UPDATE: '更新',
  SAME: '不变'
}

/** BOM Excel 导出与增量导入。 */
export const bomExcelApi = {
  async export(
    itemId: string,
    params: { viewType: string; depth?: number; effectAt?: string }
  ): Promise<void> {
    const { blob, fileName } = await downloadFile(`/items/${itemId}/bom/export`, params)
    saveBlob(blob, fileName)
  },
  preview(itemId: string, file: File, viewType: string): Promise<BomExcelPreview> {
    return uploadFile<BomExcelPreview>(`/items/${itemId}/bom/import/preview`, file, { viewType })
  },
  import(itemId: string, file: File, viewType: string): Promise<BomExcelImportResult> {
    return uploadFile<BomExcelImportResult>(`/items/${itemId}/bom/import`, file, { viewType })
  }
}

/** BOM 基线：冻结结构快照并与当前结构对比。 */
export const baselineApi = {
  list(itemId: string, viewType?: string): Promise<BomBaselineRow[]> {
    return http.get<BomBaselineRow[]>(`/items/${itemId}/baselines`, { viewType })
  },
  create(itemId: string, req: BomBaselineReq): Promise<BomBaselineRow> {
    return http.post<BomBaselineRow>(`/items/${itemId}/baselines`, req)
  },
  lines(itemId: string, baselineId: string): Promise<BomBaselineLineRow[]> {
    return http.get<BomBaselineLineRow[]>(`/items/${itemId}/baselines/${baselineId}/lines`)
  },
  diff(itemId: string, baselineId: string, params?: { effectAt?: string; depth?: number }): Promise<BomBaselineDiff> {
    return http.get<BomBaselineDiff>(`/items/${itemId}/baselines/${baselineId}/diff`, params)
  },
  remove(itemId: string, baselineId: string): Promise<void> {
    return http.delete<void>(`/items/${itemId}/baselines/${baselineId}`)
  }
}

/** BOM 替代件：{itemId} 是被替换件（须已出现在 BOM 中）。 */
export const alternateApi = {
  list(itemId: string, viewType?: string): Promise<BomAlternateRow[]> {
    return http.get<BomAlternateRow[]>(`/items/${itemId}/alternates`, { viewType })
  },
  add(itemId: string, req: BomAlternateReq): Promise<BomAlternateRow> {
    return http.post<BomAlternateRow>(`/items/${itemId}/alternates`, req)
  },
  update(itemId: string, rowId: string, req: BomAlternateReq): Promise<BomAlternateRow> {
    return http.put<BomAlternateRow>(`/items/${itemId}/alternates/${rowId}`, req)
  },
  remove(itemId: string, rowId: string): Promise<void> {
    return http.delete<void>(`/items/${itemId}/alternates/${rowId}`)
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
