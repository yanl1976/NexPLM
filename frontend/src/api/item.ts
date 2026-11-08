import { downloadFile, http, saveBlob, uploadFile } from './http'

/** 零部件类型（与后端 V2 的 ItemType 枚举一致；注意外购件的码是 PURCHASE 不是 OUTSOURCE）。 */
export const ITEM_TYPE_LABEL: Record<string, string> = {
  PRODUCT: '产品',
  ASSEMBLY: '部件',
  PART: '零件',
  STANDARD: '标准件',
  TOOL: '工装',
  GAUGE: '量具',
  MOLD: '模具',
  PURCHASE: '外购件'
}

/** 供应方式。 */
export const ITEM_SOURCE_LABEL: Record<string, string> = {
  MAKE: '自制',
  BUY: '外购'
}

/** 生命周期（plm_object.lifecycle_state）。 */
export const ITEM_LIFECYCLE_LABEL: Record<string, string> = {
  Draft: '草稿',
  INIT: '草稿',
  InWorkflow: '审批中',
  Released: '已发布',
  Obsolete: '已废弃'
}

/** 生命周期阶段（旧系统 STAGESTATEID：S 试制 / A 小批 / B 量产）。 */
export const ITEM_PHASE_LABEL: Record<string, string> = {
  S: '试制',
  A: '小批',
  B: '量产'
}

/** 使用状态（旧系统 STATEID）。 */
export const ITEM_USING_STATE_LABEL: Record<string, string> = {
  Using: '在用',
  StopUsing: '停用',
  GiveUp: '废弃'
}

/** 流程状态（旧系统 WORKFLOWSTATE）。 */
export const ITEM_WORKFLOW_LABEL: Record<string, string> = {
  New: '新建',
  NONE: '新建',
  Running: '审批中',
  Approved: '已通过',
  Rejected: '已驳回',
  Published: '已发布'
}

/** 视图类型（旧系统 PSEITEMVIEW 的视图类型枚举）。 */
export const ITEM_VIEW_TYPE_LABEL: Record<string, string> = {
  DESIGN: '设计视图',
  PROCESS: '工艺视图',
  MANUFACTURE: '制造视图'
}

/** 设计属性（旧系统 PSEBASEFORM，独立表 item_design_attr）。 */
export interface ItemDesignAttr {
  provider?: string | null
  dwgNo?: string | null
  productCategory?: string | null
  productModel?: string | null
  productSpec?: string | null
  materialType?: string | null
  materialType2?: string | null
  surfaceTreatment?: string | null
  isTooling?: boolean
  weightUnit?: string | null
  montage?: string | null
  colorItem?: boolean
}

/** 检出信息：谁在什么时间检出。 */
export interface ItemCheckout {
  userId: string
  userName?: string | null
  checkedOutAt: string
  expireAt?: string | null
  mine: boolean
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
  /** 生命周期阶段 S/A/B（可能为空，旧数据未填）。 */
  phaseState?: string | null
  /** 使用状态 Using/StopUsing/GiveUp。 */
  usingState?: string | null
  /** 流程状态 New/Running/Approved…。 */
  workflowState?: string | null
  revision: number
  iteration: number
  revisionLabel: string
  createdBy?: string | null
  createdByName?: string | null
  updatedBy?: string | null
  updatedByName?: string | null
  releasedAt?: string | null
  createdAt?: string | null
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
  designAttr?: ItemDesignAttr | null
  checkout?: ItemCheckout | null
  /** ext_json 原文（含标准键与 ERP 客制字段，如 F_ZXCV_ZYCS / FIsProduce）。 */
  ext?: Record<string, unknown> | null
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
  /** 创建时即归类：编码按分类绑定的路径生成（`Kind_xxx/1001/02/*`）。 */
  classId?: string | null
  /** 位置（工作区/目录路径，自由文本）；与组织、权限无关。 */
  location?: string | null
  designAttr?: ItemDesignAttr | null
  /**
   * 客制字段（批次 G·方案 A）：键必须是 PdmItem 元数据里已定义的字段码，
   * 值落 ext_json；不传=保留既有值，传空串=清空。
   */
  ext?: Record<string, unknown> | null
}

/** 编码预览（不消耗流水）。source：`CLASS_PATH` / `CLASS_CODE`。 */
export interface CodePreview {
  code: string
  ruleCode: string
  ruleName: string
  source: string
}

/** 列表查询条件（对齐旧系统 PSEITEM 的 8 项 + 材料/规格 + 分类维度）。 */
export interface ItemQuery {
  page?: number
  size?: number
  keyword?: string
  itemType?: string
  sourceType?: string
  usingState?: string
  phaseState?: string
  workflowState?: string
  createdBy?: string
  createdFrom?: string
  createdTo?: string
  releasedFrom?: string
  releasedTo?: string
  material?: string
  specification?: string
  /** 分类过滤（分类树见 itemClassApi.tree）。 */
  classId?: string
  /** true = 含子分类（对应蓝本 ClassifyUse 的含子分类检索）。 */
  includeChildren?: boolean
  /** 分类属性过滤：键为属性编码，请求时拼成 attr.<code>=值。 */
  classAttrs?: Record<string, string>
}

/** 零部件分类节点（复用 doc_class，entity_code=PdmItem）。 */
export interface ItemClassNode {
  id: string
  parentId?: string | null
  code: string
  name: string
  path: string
  sort: number
  status: string
  remark?: string | null
  /** 关联编码规则 code（对齐蓝本分类的「关联编码」）；为空表示不参与按编码自动归类。 */
  codeRuleCode?: string | null
  /** 编码路径 `种类规则/码值1/码值2/*`（蓝本 CLASSCODEPATH）；为空表示不自动取号。 */
  codePath?: string | null
  /** 该分类下（不含子分类）的对象数。 */
  docCount: number
  children?: ItemClassNode[]
}

/** 分类属性定义（生效属性 = 自身 + 祖先可继承）。 */
export interface ItemClassAttrDef {
  id: string
  code: string
  name: string
  valueType: 'STRING' | 'NUMBER' | 'DATE' | 'ENUM' | 'BOOL' | string
  enumCode?: string | null
  required: boolean
  inheritable: boolean
  defaultValue?: string | null
  sort: number
  ownerClassId?: string | null
  ownerClassName?: string | null
  inherited?: boolean
}

export interface ItemClassSaveReq {
  code: string
  name: string
  parentId?: string | null
  sort?: number
  status?: string
  remark?: string | null
  /** 关联编码规则 code；空串表示解除关联（null 表示不修改）。 */
  codeRuleCode?: string | null
  /** 编码路径 `种类/码值1/码值2/*`；空串表示解除绑定（null 表示不修改）。 */
  codePath?: string | null
}

/** 按编码自动匹配分类的建议（仅零件未归类时给出）。 */
export interface ItemClassifySuggestion {
  matched: boolean
  classId?: string | null
  classCode?: string | null
  className?: string | null
  codeRuleCode?: string | null
  reason?: string | null
}

export interface ItemClassAttrItem {
  code: string
  name: string
  valueType: string
  enumCode?: string | null
  required?: boolean
  inheritable?: boolean
  defaultValue?: string | null
  sort?: number
}

/** 零件的分类信息：基本分类 + 生效属性定义 + 已填值。 */
export interface ItemClassifyInfo {
  classId?: string | null
  classCode?: string | null
  className?: string | null
  classPath?: string | null
  attrs: Record<string, string>
  attrDefs: ItemClassAttrDef[]
}

/** 零件视图行（旧系统 PSEITEMVIEW）。 */
export interface ItemViewRow {
  id: string
  viewType: string
  viewTypeLabel: string
  description?: string | null
  isDefault: boolean
  lifecycleState: string
  workflowState?: string | null
  bomLineCount: number
  createdAt: string
}

/** 版本历史行。 */
export interface ItemVersionRow {
  id: string
  revision: number
  iteration: number
  revisionLabel: string
  lifecycleState: string
  isLatest: boolean
  createdBy?: string | null
  createdByName?: string | null
  createdAt: string
  releasedAt?: string | null
}

/** 设计文档行（零件关联的图文档）。 */
export interface ItemDocRow {
  id: string
  code: string
  name: string
  docType?: string | null
  relationType?: string | null
  lifecycleState: string
  revisionLabel: string
  updatedAt: string
}

/** 操作日志行。 */
export interface ItemLogRow {
  logType: string
  action?: string | null
  fromState?: string | null
  toState?: string | null
  detail?: string | null
  result?: string | null
  operatorId?: string | null
  operatorLabel?: string | null
  ip?: string | null
  createdAt: string
}

/** 实例权限行（与图文档 ACL 同构）。 */
export interface ItemAclRow {
  actorType: string
  actorId?: string | null
  actorName?: string | null
  actions?: string | null
  effect?: string | null
}

export interface ItemBatchResult {
  total: number
  done: number
  skipped: number
  messages: string[]
}

/** 版本逐字段差异行（值为原始码值，展示时用字典映射）。 */
export interface ItemVersionDiff {
  group: string
  field: string
  label: string
  baseValue?: string | null
  targetValue?: string | null
  changed: boolean
}

export interface ItemVersionCompare {
  baseId: string
  baseLabel: string
  targetId: string
  targetLabel: string
  rows: ItemVersionDiff[]
  changedCount: number
  totalCount: number
}

/** 批量改属性补丁：null/undefined = 不修改该字段。 */
export interface ItemBatchPatch {
  code?: string
  name?: string
  itemType?: string
  specification?: string
  material?: string
  unit?: string
  weight?: number
  sourceType?: string
  stdCode?: string
  remark?: string
  securityLevel?: number
  designAttr?: Partial<ItemDesignAttr>
}

/** BOM 视图（设计/工艺/制造）。 */
export const BOM_VIEW_LABEL: Record<string, string> = {
  DESIGN: '设计',
  PROCESS: '工艺',
  MANUFACTURE: '制造'
}

/** BOM 类型（蓝本 `LBOMTYPE`）：新系统默认 DESIGN，迁移的报价 BOM 用 QUOTE。 */
export const BOM_TYPE_LABEL: Record<string, string> = {
  DESIGN: '设计',
  QUOTE: '报价',
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
  /** 总用量（蓝本 `TOTALQTY`）；为空表示按层级累乘计算。 */
  totalQty?: number | null
  /** 剩余用量（蓝本 `SURPLUSQTY`）。 */
  surplusQty?: number | null
  /** 子用量（蓝本 `SUBUSEQTY`）。 */
  subUseQty?: number | null
  /** BOM 类型（蓝本 `LBOMTYPE`）：DESIGN / QUOTE / … */
  bomType?: string | null
  /** 工序路线（蓝本 `ITEMROUTE`）。 */
  itemRoute?: string | null
  /** 数据来源：MANUAL / IMPORT / ERP（蓝本 `DATASOURCE`）。 */
  dataSource?: string | null
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
  /** 总用量（蓝本 TOTALQTY）；空表示按层级累乘。 */
  totalQty?: number | null
  /** 剩余用量（蓝本 SURPLUSQTY）。 */
  surplusQty?: number | null
  /** 子用量（蓝本 SUBUSEQTY）。 */
  subUseQty?: number | null
  /** BOM 类型（蓝本 LBOMTYPE）：DESIGN / QUOTE / MANUFACTURE。 */
  bomType?: string | null
  /** 工序路线（蓝本 ITEMROUTE）。 */
  itemRoute?: string | null
  /** 数据来源（蓝本 DATASOURCE）：MANUAL / IMPORT / ERP。 */
  dataSource?: string | null
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
  list(params: ItemQuery): Promise<{ items: ItemSummary[]; total: number; page: number; size: number }> {
    const { classAttrs, ...rest } = params
    const query: Record<string, string | number | boolean | undefined> = { ...rest }
    // 分类属性过滤按 attr.<编码>=值 展开（后端统一从参数前缀识别）
    Object.entries(classAttrs ?? {}).forEach(([code, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        query[`attr.${code}`] = value
      }
    })
    return http.get('/items', query)
  },
  detail(id: string): Promise<ItemDetail> {
    return http.get<ItemDetail>(`/items/${id}`)
  },
  /** 编码预览（不消耗流水）：选分类后显示将生成的编码。 */
  previewCode(params: { classId?: string; itemType?: string; dwgNo?: string }): Promise<CodePreview> {
    return http.get<CodePreview>('/items/code-preview', params)
  },
  create(req: ItemSaveReq): Promise<ItemDetail> {
    return http.post<ItemDetail>('/items', req)
  },
  update(id: string, req: ItemSaveReq): Promise<ItemDetail> {
    return http.put<ItemDetail>(`/items/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/items/${id}`)
  },
  batchDelete(ids: string[]): Promise<ItemBatchResult> {
    return http.post<ItemBatchResult>('/items/batch-delete', { ids })
  },
  batchCreate(items: ItemSaveReq[]): Promise<ItemBatchResult> {
    return http.post<ItemBatchResult>('/items/batch-create', { items })
  },
  batchUpdate(ids: string[], patch: ItemBatchPatch): Promise<ItemBatchResult> {
    return http.put<ItemBatchResult>('/items/batch-update', { ids, patch })
  },
  transferOwner(ids: string[], targetUserId: string): Promise<ItemBatchResult> {
    return http.post<ItemBatchResult>('/items/transfer-owner', { ids, targetUserId })
  },
  changeUsingState(id: string, usingState: string): Promise<ItemDetail> {
    return http.put<ItemDetail>(`/items/${id}/using-state`, { usingState })
  },
  checkout(id: string): Promise<ItemDetail> {
    return http.post<ItemDetail>(`/items/${id}/checkout`)
  },
  checkin(id: string): Promise<ItemDetail> {
    return http.post<ItemDetail>(`/items/${id}/checkin`)
  },
  cancelCheckout(id: string): Promise<void> {
    return http.post<void>(`/items/${id}/cancel-checkout`)
  },
  revise(id: string): Promise<ItemDetail> {
    return http.post<ItemDetail>(`/items/${id}/revise`)
  },
  versions(id: string): Promise<ItemVersionRow[]> {
    return http.get<ItemVersionRow[]>(`/items/${id}/versions`)
  },
  compareVersions(id: string, baseId: string, targetId: string): Promise<ItemVersionCompare> {
    return http.get<ItemVersionCompare>(`/items/${id}/versions/compare`, { baseId, targetId })
  },
  views(id: string): Promise<ItemViewRow[]> {
    return http.get<ItemViewRow[]>(`/items/${id}/views`)
  },
  createView(
    id: string,
    req: { viewType?: string; description?: string; copyFromViewId?: string }
  ): Promise<ItemViewRow> {
    return http.post<ItemViewRow>(`/items/${id}/views`, req)
  },
  removeView(id: string, viewId: string): Promise<void> {
    return http.delete<void>(`/items/${id}/views/${viewId}`)
  },
  docs(id: string): Promise<ItemDocRow[]> {
    return http.get<ItemDocRow[]>(`/items/${id}/docs`)
  },
  logs(id: string, limit?: number): Promise<ItemLogRow[]> {
    return http.get<ItemLogRow[]>(`/items/${id}/logs`, { limit })
  },
  acl(id: string): Promise<ItemAclRow[]> {
    return http.get<ItemAclRow[]>(`/items/${id}/acl`)
  },
  async exportFile(params: ItemQuery): Promise<void> {
    const { blob, fileName } = await downloadFile(
      '/items/export',
      { ...params } as Record<string, string | number | boolean | undefined>
    )
    saveBlob(blob, fileName)
  },
  async downloadTemplate(): Promise<void> {
    const { blob, fileName } = await downloadFile('/items/import/template')
    saveBlob(blob, fileName)
  },
  previewImport(file: File): Promise<{
    total: number
    createCount: number
    updateCount: number
    errors: string[]
    valid: boolean
  }> {
    return uploadFile('/items/import/preview', file)
  },
  importItems(file: File): Promise<{
    total: number
    created: number
    updated: number
    skipped: number
    messages: string[]
  }> {
    return uploadFile('/items/import', file)
  }
}

/**
 * 零部件分类接口（批次 H·T-H-01）。
 * 分类树与属性复用文档分类表（entity_code=PdmItem），维护需 `pse.classify.manage`，查看只需 `pse.item.read`。
 */
export const itemClassApi = {
  tree(): Promise<ItemClassNode[]> {
    return http.get<ItemClassNode[]>('/item-classes')
  },
  create(req: ItemClassSaveReq): Promise<ItemClassNode> {
    return http.post<ItemClassNode>('/item-classes', req)
  },
  update(id: string, req: ItemClassSaveReq): Promise<ItemClassNode> {
    return http.put<ItemClassNode>(`/item-classes/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/item-classes/${id}`)
  },
  /** 生效属性：自身 + 祖先可继承。 */
  attrs(id: string): Promise<ItemClassAttrDef[]> {
    return http.get<ItemClassAttrDef[]>(`/item-classes/${id}/attributes`)
  },
  /** 全量替换分类自身属性（空数组 = 清空）。 */
  saveAttrs(id: string, items: ItemClassAttrItem[]): Promise<ItemClassAttrDef[]> {
    return http.put<ItemClassAttrDef[]>(`/item-classes/${id}/attributes`, { items })
  },
  /** 读取零件的分类与属性值。 */
  info(itemId: string): Promise<ItemClassifyInfo> {
    return http.get<ItemClassifyInfo>(`/items/${itemId}/classify`)
  },
  /** 归类 / 取消归类（classId 传 null 表示取消）。 */
  assign(itemId: string, classId: string | null, attrs: Record<string, string>): Promise<ItemClassifyInfo> {
    return http.put<ItemClassifyInfo>(`/items/${itemId}/classify`, { classId, attrs })
  },
  /** 按编码自动匹配分类的建议（已归类 / 无匹配时 matched=false，reason 给原因）。 */
  suggestion(itemId: string): Promise<ItemClassifySuggestion> {
    return http.get<ItemClassifySuggestion>(`/items/${itemId}/classify/suggestion`)
  }
}
