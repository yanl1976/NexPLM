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
