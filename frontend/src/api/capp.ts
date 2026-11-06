import { downloadFile, http, saveBlob } from './http'

/** 工艺路线状态。 */
export type RoutingStatus = 'DRAFT' | 'RELEASED' | 'ARCHIVED'

export const ROUTING_STATUS_LABEL: Record<RoutingStatus, string> = {
  DRAFT: '草稿',
  RELEASED: '已发布',
  ARCHIVED: '已归档'
}

/** 工序类型（与后端 CappRoutingService.OP_TYPES 保持一致）。 */
export const OP_TYPE_LABEL: Record<string, string> = {
  MACHINING: '机加工',
  ASSEMBLY: '装配',
  INSPECTION: '检验',
  HEAT: '热处理',
  SURFACE: '表面处理',
  OTHER: '其它'
}

/** 工步：工序内部的操作细分（装夹 / 走刀 / 测量），挂在「工序号」上而非工序行 id。 */
export interface SubstepRow {
  id?: string
  seqNo: number
  name: string
  content?: string | null
  resourceId?: string | null
  resourceCode?: string | null
  resourceName?: string | null
  setupMinutes?: number | null
  runMinutes?: number | null
  remark?: string | null
}

export interface SubstepSaveReq {
  seqNo: number
  name: string
  content?: string | null
  resourceId?: string | null
  setupMinutes?: number
  runMinutes?: number
  remark?: string | null
}

/** 工艺过程树节点（工序 + 其下工步）。 */
export interface ProcessTreeNode {
  stepSeqNo: number
  stepName: string
  opType: string
  workCenter?: string | null
  resourceId?: string | null
  resourceCode?: string | null
  resourceName?: string | null
  setupMinutes?: number | null
  runMinutes?: number | null
  substepCount: number
  /** 工步工时之和（准备 + 单件），用于核对与工序工时的差额。 */
  substepMinutes: number
  substeps: SubstepRow[]
}

export interface RoutingStep {
  id?: string
  seqNo: number
  name: string
  opType: string
  workCenter?: string | null
  /** 引用的工艺资源（P6-3 引用统计口径）。 */
  resourceId?: string | null
  resourceCode?: string | null
  resourceName?: string | null
  setupMinutes?: number | null
  runMinutes?: number | null
  remark?: string | null
  /** 工序下的工步（随工序整表替换提交）。 */
  substeps?: SubstepRow[]
}

export interface RoutingRow {
  id: string
  code: string
  name: string
  objectId: string
  objectCode?: string | null
  objectName?: string | null
  versionNo: number
  status: RoutingStatus
  applicability?: string | null
  /** 工艺负责人（工艺分工）。 */
  ownerId?: string | null
  ownerName?: string | null
  stepCount: number
  totalMinutes?: number | null
  releasedAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface RoutingDetail extends RoutingRow {
  remark?: string | null
  /** 路线串：10 下料 → 20 车削 → 30 检验 */
  routeString: string
  steps: RoutingStep[]
}

export interface RoutingSaveReq {
  name: string
  objectId: string
  applicability?: string | null
  remark?: string | null
  ownerId?: string | null
  steps: RoutingStep[]
}

// ---------------------------------------------------------------- 工艺资源库（P6-3）

export interface ResourceClass {
  id: string
  parentId?: string | null
  code: string
  name: string
  sortNo: number
  remark?: string | null
  resourceCount: number
}

export interface ResourceClassSaveReq {
  code: string
  name: string
  parentId?: string | null
  sortNo?: number
  remark?: string | null
}

export interface CappResource {
  id: string
  classId: string
  classCode?: string | null
  className?: string | null
  code: string
  name: string
  spec?: string | null
  unit?: string | null
  status: 'ACTIVE' | 'DISABLED'
  attrs: Record<string, string>
  remark?: string | null
  referenceCount: number
  updatedAt?: string | null
}

export interface ResourceSaveReq {
  classId: string
  code: string
  name: string
  spec?: string | null
  unit?: string | null
  status?: string
  attrs?: Record<string, string>
  remark?: string | null
}

export interface ResourceReference {
  routingId: string
  routingCode: string
  routingName: string
  objectId: string
  objectCode?: string | null
  seqNo: number
  stepName: string
}

export interface ResourceQuery {
  id: string
  name: string
  classId?: string | null
  keyword?: string | null
  attrs: Record<string, string>
  createdAt?: string | null
}

export const resourceApi = {
  classes(): Promise<ResourceClass[]> {
    return http.get<ResourceClass[]>(`/capp/resource-classes`)
  },
  createClass(req: ResourceClassSaveReq): Promise<ResourceClass> {
    return http.post<ResourceClass>(`/capp/resource-classes`, req)
  },
  updateClass(id: string, req: ResourceClassSaveReq): Promise<ResourceClass> {
    return http.put<ResourceClass>(`/capp/resource-classes/${id}`, req)
  },
  removeClass(id: string): Promise<void> {
    return http.delete<void>(`/capp/resource-classes/${id}`)
  },
  list(params: {
    page?: number
    size?: number
    classId?: string
    keyword?: string
    status?: string
    attrs?: string
  }): Promise<{ items: CappResource[]; total: number; page: number; size: number }> {
    return http.get(`/capp/resources`, params)
  },
  detail(id: string): Promise<CappResource> {
    return http.get<CappResource>(`/capp/resources/${id}`)
  },
  create(req: ResourceSaveReq): Promise<CappResource> {
    return http.post<CappResource>(`/capp/resources`, req)
  },
  update(id: string, req: ResourceSaveReq): Promise<CappResource> {
    return http.put<CappResource>(`/capp/resources/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/capp/resources/${id}`)
  },
  references(id: string): Promise<ResourceReference[]> {
    return http.get<ResourceReference[]>(`/capp/resources/${id}/references`)
  },
  queries(): Promise<ResourceQuery[]> {
    return http.get<ResourceQuery[]>(`/capp/resource-queries`)
  },
  saveQuery(req: {
    name: string
    classId?: string | null
    keyword?: string | null
    attrs?: Record<string, string>
  }): Promise<ResourceQuery> {
    return http.post<ResourceQuery>(`/capp/resource-queries`, req)
  },
  removeQuery(id: string): Promise<void> {
    return http.delete<void>(`/capp/resource-queries/${id}`)
  },
  runQuery(
    id: string,
    params?: { page?: number; size?: number }
  ): Promise<{ items: CappResource[]; total: number; page: number; size: number }> {
    return http.get(`/capp/resource-queries/${id}/run`, params)
  }
}

export const cappApi = {
  list(params: {
    page?: number
    size?: number
    keyword?: string
    objectId?: string
    status?: string
  }): Promise<{ items: RoutingRow[]; total: number; page: number; size: number }> {
    return http.get(`/capp/routings`, params)
  },
  detail(id: string): Promise<RoutingDetail> {
    return http.get<RoutingDetail>(`/capp/routings/${id}`)
  },
  create(req: RoutingSaveReq): Promise<RoutingDetail> {
    return http.post<RoutingDetail>(`/capp/routings`, req)
  },
  update(id: string, req: RoutingSaveReq): Promise<RoutingDetail> {
    return http.put<RoutingDetail>(`/capp/routings/${id}`, req)
  },
  release(id: string): Promise<RoutingDetail> {
    return http.post<RoutingDetail>(`/capp/routings/${id}/release`)
  },
  archive(id: string): Promise<RoutingDetail> {
    return http.post<RoutingDetail>(`/capp/routings/${id}/archive`)
  },
  copy(id: string): Promise<RoutingDetail> {
    return http.post<RoutingDetail>(`/capp/routings/${id}/copy`)
  },
  /** 序号重建：按当前顺序重排为 10/20/30…（仅草稿）。 */
  resequence(id: string): Promise<RoutingDetail> {
    return http.post<RoutingDetail>(`/capp/routings/${id}/resequence`)
  },
  /** 工艺过程树：工序 → 工步（与工序表同源）。 */
  processTree(id: string): Promise<ProcessTreeNode[]> {
    return http.get<ProcessTreeNode[]>(`/capp/routings/${id}/process-tree`)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/capp/routings/${id}`)
  }
}

// ---------------------------------------------------------------- 工艺卡片（P6-4）

export type CardType = 'PROCESS' | 'CRAFT' | 'INSPECT'

export const CARD_TYPE_LABEL: Record<CardType, string> = {
  PROCESS: '工艺卡',
  CRAFT: '工序卡',
  INSPECT: '检验卡'
}

/** 模板状态（与工艺路线同构：草稿可改、已发布可生成卡片、已归档为历史版本）。 */
export const CARD_TEMPLATE_STATUS_LABEL: Record<RoutingStatus, string> = ROUTING_STATUS_LABEL

/** 明细列：field 走内置变量，留空时输出 constValue 固定文本。 */
export interface CardColumn {
  key?: string | null
  title: string
  field?: string | null
  constValue?: string | null
  width?: number | null
  align?: 'LEFT' | 'CENTER' | 'RIGHT' | null
}

export interface CardHeaderField {
  label: string
  field?: string | null
}

export interface CardLayout {
  title?: string | null
  headerFields: CardHeaderField[]
  columns: CardColumn[]
  footerText?: string | null
}

export interface CardTemplateRow {
  id: string
  code: string
  name: string
  cardType: string
  versionNo: number
  status: RoutingStatus
  title?: string | null
  remark?: string | null
  cardCount: number
  releasedAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface CardTemplateDetail extends CardTemplateRow {
  layout: CardLayout
}

export interface CardTemplateSaveReq {
  name: string
  cardType?: string | null
  title?: string | null
  layout: CardLayout
  remark?: string | null
}

export interface CappCardRow {
  id: string
  code: string
  title?: string | null
  templateId: string
  templateCode: string
  templateVersion: number
  templateName: string
  routingId: string
  routingCode?: string | null
  objectId: string
  objectCode?: string | null
  objectName?: string | null
  /** 工序行数（不含合计行）。 */
  rowCount: number
  createdAt?: string | null
  updatedAt?: string | null
}

export interface CappCardDetail extends CappCardRow {
  layout: CardLayout
  fieldValues: Record<string, string>
  rows: Record<string, string>[]
  remark?: string | null
  routeString?: string | null
}

export interface CappCardSaveReq {
  templateId: string
  routingId: string
  fieldValues?: Record<string, string> | null
  remark?: string | null
}

/** 可用变量字典：编辑模板时给出可选项，避免手打错字段名。 */
export interface CardFieldDict {
  stepFields: Record<string, string>
  cardFields: Record<string, string>
  cardTypes: Record<string, string>
}

export const cardApi = {
  fields(): Promise<CardFieldDict> {
    return http.get<CardFieldDict>(`/capp/card-fields`)
  },
  templateList(params: {
    page?: number
    size?: number
    keyword?: string
    cardType?: string
    status?: string
  }): Promise<{ items: CardTemplateRow[]; total: number; page: number; size: number }> {
    return http.get(`/capp/card-templates`, params)
  },
  template(id: string): Promise<CardTemplateDetail> {
    return http.get<CardTemplateDetail>(`/capp/card-templates/${id}`)
  },
  createTemplate(req: CardTemplateSaveReq): Promise<CardTemplateDetail> {
    return http.post<CardTemplateDetail>(`/capp/card-templates`, req)
  },
  updateTemplate(id: string, req: CardTemplateSaveReq): Promise<CardTemplateDetail> {
    return http.put<CardTemplateDetail>(`/capp/card-templates/${id}`, req)
  },
  newVersion(id: string): Promise<CardTemplateDetail> {
    return http.post<CardTemplateDetail>(`/capp/card-templates/${id}/new-version`)
  },
  releaseTemplate(id: string): Promise<CardTemplateDetail> {
    return http.post<CardTemplateDetail>(`/capp/card-templates/${id}/release`)
  },
  removeTemplate(id: string): Promise<void> {
    return http.delete<void>(`/capp/card-templates/${id}`)
  },
  list(params: {
    page?: number
    size?: number
    keyword?: string
    routingId?: string
    templateId?: string
    objectId?: string
  }): Promise<{ items: CappCardRow[]; total: number; page: number; size: number }> {
    return http.get(`/capp/cards`, params)
  },
  detail(id: string): Promise<CappCardDetail> {
    return http.get<CappCardDetail>(`/capp/cards/${id}`)
  },
  create(req: CappCardSaveReq): Promise<CappCardDetail> {
    return http.post<CappCardDetail>(`/capp/cards`, req)
  },
  regenerate(id: string): Promise<CappCardDetail> {
    return http.post<CappCardDetail>(`/capp/cards/${id}/regenerate`)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/capp/cards/${id}`)
  },
  /** 导出 xlsx（后端按卡片自带版式快照渲染）。 */
  async exportExcel(id: string): Promise<void> {
    const { blob, fileName } = await downloadFile(`/capp/cards/${id}/export`)
    saveBlob(blob, fileName)
  },
  /** 导出 PDF（后端嵌入中文字体，版式与 xlsx 同源）。 */
  async exportPdf(id: string): Promise<void> {
    const { blob, fileName } = await downloadFile(`/capp/cards/${id}/export.pdf`)
    saveBlob(blob, fileName)
  }
}

// ---------------------------------------------------------------- 工时定额（P6-6）

/** 定额规则：opType / workCenter 留空表示通配（两者都空 = 全局默认）。 */
export interface TimeRuleRow {
  id: string
  code: string
  name: string
  opType?: string | null
  workCenter?: string | null
  setupBaseMinutes: number
  setupFactor: number
  runFactor: number
  batchQty: number
  priority: number
  enabled: boolean
  remark?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface TimeRuleSaveReq {
  name: string
  opType?: string | null
  workCenter?: string | null
  setupBaseMinutes?: number | null
  setupFactor?: number | null
  runFactor?: number | null
  batchQty?: number | null
  priority?: number | null
  enabled?: boolean | null
  remark?: string | null
}

/** 单道工序的定额明细（原始工时 → 定额工时）。 */
export interface TimeStepRow {
  seqNo: number
  name: string
  opType: string
  workCenter?: string | null
  rawSetup: number
  rawRun: number
  setupEffective: number
  runEffective: number
  ruleCode?: string | null
  ruleName?: string | null
}

/** 试算结果：单件工时不含准备工时，批量工时 = 准备合计 + 单件 × 批量。 */
export interface TimeCalcResult {
  routingId: string
  routingCode: string
  objectCode?: string | null
  objectName?: string | null
  batchQty: number
  steps: TimeStepRow[]
  totalSetupEffective: number
  singlePieceMinutes: number
  batchMinutes: number
  matchedSteps: number
  unmatchedSteps: number
}

export const timeApi = {
  list(params?: { keyword?: string; enabled?: boolean }): Promise<TimeRuleRow[]> {
    return http.get<TimeRuleRow[]>(`/capp/time-rules`, params)
  },
  create(req: TimeRuleSaveReq): Promise<TimeRuleRow> {
    return http.post<TimeRuleRow>(`/capp/time-rules`, req)
  },
  update(id: string, req: TimeRuleSaveReq): Promise<TimeRuleRow> {
    return http.put<TimeRuleRow>(`/capp/time-rules/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/capp/time-rules/${id}`)
  },
  /** 只读试算：规则或路线改动后重查即得新值。 */
  calc(routingId: string, batchQty?: number): Promise<TimeCalcResult> {
    return http.get<TimeCalcResult>(`/capp/time-calc`, { routingId, batchQty })
  }
}

// ---------------------------------------------------------------- 工艺成册与齐套（P6-5）

export interface VolumeRow {
  id: string
  code: string
  name: string
  objectId: string
  objectCode?: string | null
  objectName?: string | null
  status: 'DRAFT' | 'RELEASED'
  cardCount: number
  remark?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

/** 册内条目：seqNo 即页序（10/20/30 步进）。 */
export interface VolumeCardRow {
  seqNo: number
  cardId: string
  cardCode: string
  cardTitle?: string | null
  templateName: string
  templateVersion: number
  routingCode?: string | null
  rowCount: number
}

export interface VolumeDetail {
  id: string
  code: string
  name: string
  objectId: string
  objectCode?: string | null
  objectName?: string | null
  status: 'DRAFT' | 'RELEASED'
  remark?: string | null
  cards: VolumeCardRow[]
  createdAt?: string | null
  updatedAt?: string | null
}

/** cardIds 的顺序即页序。 */
export interface VolumeSaveReq {
  name: string
  objectId: string
  cardIds: string[]
  remark?: string | null
}

export type CheckStatus = 'PASS' | 'WARN' | 'FAIL'

export interface CheckItem {
  key: string
  label: string
  status: CheckStatus
  detail: string
}

/** 齐套检查：只有「没有路线 / 没有工序」是 FAIL，其余是 WARN（工艺还没写完，不是数据错误）。 */
export interface CompletenessResult {
  objectId: string
  objectCode?: string | null
  objectName?: string | null
  routeCount: number
  releasedRouteCount: number
  stepCount: number
  substepCount: number
  cardCount: number
  zeroMinuteSteps: number
  unboundResourceSteps: number
  checks: CheckItem[]
  passCount: number
  warnCount: number
  failCount: number
  ready: boolean
}

export const volumeApi = {
  list(params?: { keyword?: string; objectId?: string }): Promise<VolumeRow[]> {
    return http.get<VolumeRow[]>(`/capp/volumes`, params)
  },
  detail(id: string): Promise<VolumeDetail> {
    return http.get<VolumeDetail>(`/capp/volumes/${id}`)
  },
  create(req: VolumeSaveReq): Promise<VolumeDetail> {
    return http.post<VolumeDetail>(`/capp/volumes`, req)
  },
  update(id: string, req: VolumeSaveReq): Promise<VolumeDetail> {
    return http.put<VolumeDetail>(`/capp/volumes/${id}`, req)
  },
  release(id: string): Promise<VolumeDetail> {
    return http.post<VolumeDetail>(`/capp/volumes/${id}/release`)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/capp/volumes/${id}`)
  },
  /** 齐套性检查（零件维度）。 */
  completeness(objectId: string): Promise<CompletenessResult> {
    return http.get<CompletenessResult>(`/capp/completeness`, { objectId })
  },
  /** 导出成册 PDF（封页 + 目录 + 每张卡分页）。 */
  async exportPdf(id: string): Promise<void> {
    const { blob, fileName } = await downloadFile(`/capp/volumes/${id}/export.pdf`)
    saveBlob(blob, fileName)
  }
}

// ---------------------------------------------------------------- 质量规划（P6-8）

export type QualityCategory = 'DIMENSION' | 'APPEARANCE' | 'PERFORMANCE' | 'MATERIAL'

export const QUALITY_CATEGORY_LABEL: Record<QualityCategory, string> = {
  DIMENSION: '尺寸',
  APPEARANCE: '外观',
  PERFORMANCE: '性能',
  MATERIAL: '材料'
}

export interface QualityItemRow {
  id: string
  code: string
  name: string
  category: string
  spec?: string | null
  method?: string | null
  gauge?: string | null
  unit?: string | null
  remark?: string | null
  updatedAt?: string | null
}

export interface QualityItemSaveReq {
  name: string
  category?: string | null
  spec?: string | null
  method?: string | null
  gauge?: string | null
  unit?: string | null
  remark?: string | null
}

/** 控制计划行：itemId 只用于从字典带出默认值，行上的值才是权威。 */
export interface QualityPlanRow {
  id?: string
  seqNo: number
  stepSeqNo?: number | null
  substepSeqNo?: number | null
  stepName?: string | null
  itemId?: string | null
  itemCode?: string | null
  characteristic?: string | null
  spec?: string | null
  method?: string | null
  gauge?: string | null
  sampleSize?: string | null
  frequency?: string | null
  controlMethod?: string | null
  reactionPlan?: string | null
  critical?: boolean
  remark?: string | null
}

export interface QualityPlanRowView {
  id: string
  code: string
  name: string
  routingId: string
  routingCode?: string | null
  objectId: string
  objectCode?: string | null
  objectName?: string | null
  status: 'DRAFT' | 'RELEASED'
  rowCount: number
  remark?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface QualityPlanDetail extends Omit<QualityPlanRowView, 'rowCount'> {
  rows: QualityPlanRow[]
}

export interface QualityPlanSaveReq {
  name: string
  routingId: string
  rows?: QualityPlanRow[]
  remark?: string | null
}

export const qualityApi = {
  itemList(params?: { keyword?: string; category?: string }): Promise<QualityItemRow[]> {
    return http.get<QualityItemRow[]>(`/capp/quality-items`, params)
  },
  createItem(req: QualityItemSaveReq): Promise<QualityItemRow> {
    return http.post<QualityItemRow>(`/capp/quality-items`, req)
  },
  updateItem(id: string, req: QualityItemSaveReq): Promise<QualityItemRow> {
    return http.put<QualityItemRow>(`/capp/quality-items/${id}`, req)
  },
  removeItem(id: string): Promise<void> {
    return http.delete<void>(`/capp/quality-items/${id}`)
  },
  planList(params?: {
    keyword?: string
    objectId?: string
    routingId?: string
    status?: string
  }): Promise<QualityPlanRowView[]> {
    return http.get<QualityPlanRowView[]>(`/capp/quality-plans`, params)
  },
  plan(id: string): Promise<QualityPlanDetail> {
    return http.get<QualityPlanDetail>(`/capp/quality-plans/${id}`)
  },
  createPlan(req: QualityPlanSaveReq): Promise<QualityPlanDetail> {
    return http.post<QualityPlanDetail>(`/capp/quality-plans`, req)
  },
  updatePlan(id: string, req: QualityPlanSaveReq): Promise<QualityPlanDetail> {
    return http.put<QualityPlanDetail>(`/capp/quality-plans/${id}`, req)
  },
  releasePlan(id: string): Promise<QualityPlanDetail> {
    return http.post<QualityPlanDetail>(`/capp/quality-plans/${id}/release`)
  },
  removePlan(id: string): Promise<void> {
    return http.delete<void>(`/capp/quality-plans/${id}`)
  },
  async exportPlan(id: string): Promise<void> {
    const { blob, fileName } = await downloadFile(`/capp/quality-plans/${id}/export`)
    saveBlob(blob, fileName)
  }
}

// ---------------------------------------------------------------- ERP 下发（P6-9 预留）

export interface DispatchRow {
  id: string
  code: string
  routingId: string
  routingCode?: string | null
  objectId: string
  objectCode?: string | null
  status: 'PENDING' | 'SENT' | 'FAILED' | 'VOID'
  payloadSha?: string | null
  message?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface DispatchDetail extends DispatchRow {
  payload: Record<string, unknown>
}

export const DISPATCH_STATUS_LABEL: Record<string, string> = {
  PENDING: '待发送',
  SENT: '已发送',
  FAILED: '失败',
  VOID: '作废'
}

export const dispatchApi = {
  list(params?: { routingId?: string; status?: string }): Promise<DispatchRow[]> {
    return http.get<DispatchRow[]>(`/capp/erp-dispatches`, params)
  },
  detail(id: string): Promise<DispatchDetail> {
    return http.get<DispatchDetail>(`/capp/erp-dispatches/${id}`)
  },
  create(routingId: string): Promise<DispatchDetail> {
    return http.post<DispatchDetail>(`/capp/erp-dispatches`, { routingId })
  },
  callback(id: string, status: string, message?: string): Promise<DispatchDetail> {
    return http.post<DispatchDetail>(`/capp/erp-dispatches/${id}/callback`, { status, message })
  },
  /** 下载原始载荷 JSON（交给 ERP 适配器）。 */
  async downloadPayload(id: string): Promise<void> {
    const { blob, fileName } = await downloadFile(`/capp/erp-dispatches/${id}/payload`)
    saveBlob(blob, fileName)
  }
}
