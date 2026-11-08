import { downloadFile, downloadFileByPost, http, saveBlob, uploadFile } from './http'

/** 文档类型（与后端 DocType 枚举一致）。 */
export type DocType = 'DESIGN' | 'TECH' | 'PROCESS' | 'STANDARD' | 'OTHER'
export type DocSource = 'NORMAL' | 'CAD'
export type FileRole = 'MAIN' | 'ATTACHMENT' | 'VIEW' | 'DERIVED'

export const DOC_TYPE_LABEL: Record<string, string> = {
  DESIGN: '设计图纸',
  TECH: '技术文件',
  PROCESS: '工艺文件',
  STANDARD: '标准规范',
  OTHER: '其他'
}

export const DOC_SOURCE_LABEL: Record<string, string> = {
  NORMAL: '普通文档',
  CAD: 'CAD 文档'
}

export const FILE_ROLE_LABEL: Record<string, string> = {
  MAIN: '主文件',
  ATTACHMENT: '附件',
  VIEW: '预览',
  DERIVED: '派生格式'
}

export interface DocFile {
  linkId: string
  fileId: string
  fileName: string
  fileRole: FileRole
  versionNo: number
  isLatest: boolean
  sort: number
  size: number
  mime: string
  ext: string
  sha256: string
  createdAt: string
}

export interface CheckoutInfo {
  userId: string
  userLabel?: string
  checkedOutAt: string
  expireAt: string
  expired?: boolean
}

export interface DocSummary {
  id: string
  code: string
  name: string
  docType: DocType
  source: DocSource
  lifecycleState: string
  revision: number
  iteration: number
  revisionLabel: string
  fileCount: number
  fileExt?: string
  ownerId: string
  updatedAt: string
  classId?: string | null
  className?: string | null
  /** 全文检索命中片段（含 <mark> 标记，前端需转义后渲染）。 */
  highlight?: string | null
}

export interface DocDetail {
  id: string
  code: string
  name: string
  docType: DocType
  source: DocSource
  keyword?: string
  description?: string
  lifecycleState: string
  phaseState?: string
  workflowState: string
  revision: number
  iteration: number
  revisionLabel: string
  isLatest: boolean
  masterId: string
  ownerId: string
  orgId?: string
  securityLevel: number
  mainFileId?: string
  fileExt?: string
  fileCount: number
  pageCount?: number
  checkout?: CheckoutInfo
  files: DocFile[]
  createdAt: string
  updatedAt: string
  classId?: string | null
  className?: string | null
  classAttrs: Record<string, string>
}

export interface DocVersion {
  id: string
  revision: number
  iteration: number
  revisionLabel: string
  lifecycleState: string
  isLatest: boolean
  createdAt: string
  updatedAt: string
}

/** 关联对象（关系双向合并，direction = OUT 本对象为源 / IN 本对象为目标）。 */
export interface DocRelation {
  id: string
  code: string
  name: string
  objectType: string
  lifecycleState: string
  revisionLabel: string
  relationType: string
  direction: 'OUT' | 'IN'
  seq: number
  qty?: number | null
}

/** 操作日志：STATE 状态流转 / AUDIT 审计事件。 */
export interface DocLog {
  logType: 'STATE' | 'AUDIT'
  action: string
  fromState?: string | null
  toState?: string | null
  detail?: string | null
  result?: string | null
  operatorId?: string | null
  operatorLabel?: string | null
  ip?: string | null
  createdAt: string
}

/** 实例权限行（只读展示）。 */
export interface DocAclRow {
  id: string
  objectId: string
  actorType: string
  actorId?: string | null
  actorName?: string | null
  actions: string
  effect: string
  expireAt?: string | null
  source?: string | null
}

export interface DocSaveReq {
  code?: string
  name: string
  docType: string
  source?: string
  keyword?: string
  description?: string
  orgId?: string | null
  securityLevel?: number
  classId?: string | null
  classAttrs?: Record<string, string>
}

/** 文档分类树节点。 */
export interface DocClassNode {
  id: string
  parentId?: string | null
  code: string
  name: string
  path: string
  sort: number
  status: string
  remark?: string | null
  docCount: number
  children: DocClassNode[]
}

export interface DocClassSaveReq {
  code?: string
  name: string
  parentId?: string | null
  sort?: number
  status?: string
  remark?: string | null
}

/** 分类属性（inherited=true 表示继承自祖先分类，仅展示不可改）。 */
export interface DocClassAttrDef {
  id: string
  code: string
  name: string
  valueType: 'STRING' | 'NUMBER' | 'DATE' | 'ENUM' | 'BOOL'
  enumCode?: string | null
  required: boolean
  inheritable: boolean
  defaultValue?: string | null
  sort: number
  ownerClassId: string
  ownerClassName: string
  inherited: boolean
}

export interface DocClassAttrItem {
  code: string
  name: string
  valueType: string
  enumCode?: string | null
  required?: boolean
  inheritable?: boolean
  defaultValue?: string | null
  sort?: number | null
}

export const DOC_CLASS_ATTR_TYPE_LABEL: Record<string, string> = {
  STRING: '文本',
  NUMBER: '数值',
  DATE: '日期',
  ENUM: '枚举',
  BOOL: '布尔'
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export const docApi = {
  list(params: {
    page?: number
    size?: number
    keyword?: string
    docType?: string
    lifecycleState?: string
    classId?: string
    /** 文件夹（整理维度，ws_folder_content）。 */
    folderId?: string
    /** 全文检索关键字（编码/名称/关键词/描述）。 */
    q?: string
  }): Promise<PageResult<DocSummary>> {
    return http.get<PageResult<DocSummary>>('/docs', params)
  },
  detail(id: string): Promise<DocDetail> {
    return http.get<DocDetail>(`/docs/${id}`)
  },
  create(req: DocSaveReq): Promise<DocDetail> {
    return http.post<DocDetail>('/docs', req)
  },
  update(id: string, req: DocSaveReq): Promise<DocDetail> {
    return http.put<DocDetail>(`/docs/${id}`, req)
  },
  versions(id: string): Promise<DocVersion[]> {
    return http.get<DocVersion[]>(`/docs/${id}/versions`)
  },
  relations(id: string): Promise<DocRelation[]> {
    return http.get<DocRelation[]>(`/docs/${id}/relations`)
  },
  logs(id: string, limit = 100): Promise<DocLog[]> {
    return http.get<DocLog[]>(`/docs/${id}/logs`, { limit })
  },
  acl(id: string): Promise<DocAclRow[]> {
    return http.get<DocAclRow[]>(`/docs/${id}/acl`)
  },
  upload(id: string, file: File, fileRole: FileRole = 'ATTACHMENT'): Promise<{ fileId: string }> {
    return uploadFile<{ fileId: string }>(`/docs/${id}/files`, file, { fileRole })
  },
  removeFile(id: string, linkId: string): Promise<void> {
    return http.delete<void>(`/docs/${id}/files/${linkId}`)
  },
  /** 预览状态：传 fileId 时精确取该源文件的记录（一个文档可按源文件存多条） */
  preview(id: string, fileId?: string): Promise<DocPreviewStatus> {
    return http.get<DocPreviewStatus>(`/docs/${id}/preview`, fileId ? { fileId } : undefined)
  },
  generatePreview(id: string, fileId?: string): Promise<DocPreviewStatus> {
    const suffix = fileId ? `?fileId=${encodeURIComponent(fileId)}` : ''
    return http.post<DocPreviewStatus>(`/docs/${id}/preview${suffix}`)
  },
  async downloadPreview(fileId: string, fileName: string): Promise<void> {
    const { blob } = await downloadFile(`/files/${fileId}/content`, { name: fileName })
    saveBlob(blob, fileName)
  },
  async downloadFileBlob(fileId: string): Promise<Blob> {
    const { blob } = await downloadFile(`/files/${fileId}/content`)
    return blob
  },
  async download(file: DocFile): Promise<void> {
    const { blob, fileName } = await downloadFile(`/files/${file.fileId}/content`, {
      name: file.fileName
    })
    saveBlob(blob, fileName)
  },
  checkout(id: string, remark?: string): Promise<DocDetail> {
    return http.post<DocDetail>(`/docs/${id}/checkout`, { remark })
  },
  checkin(id: string, req?: { pageCount?: number; remark?: string }): Promise<DocDetail> {
    return http.post<DocDetail>(`/docs/${id}/checkin`, req ?? {})
  },
  cancelCheckout(id: string): Promise<void> {
    return http.post<void>(`/docs/${id}/cancel-checkout`)
  },
  forceUnlock(id: string): Promise<string | null> {
    return http.post<string | null>(`/docs/${id}/force-unlock`)
  }
}

export const DOC_STATE_LABEL: Record<string, string> = {
  Draft: '草稿',
  InWorkflow: '审批中',
  Released: '已发布',
  Obsolete: '已作废'
}

/** 文档预览状态。 */
export interface DocPreviewStatus {
  id?: string | null
  objectId: string
  sourceFileId?: string | null
  previewFileId?: string | null
  format?: string | null
  /** NONE 未生成、PENDING 处理中、READY 就绪、FAILED 失败可重试、SKIPPED 环境不支持 */
  status: 'NONE' | 'PENDING' | 'READY' | 'FAILED' | 'SKIPPED'
  message?: string | null
  pageCount?: number | null
  fileName?: string | null
  converter?: string | null
  updatedAt?: string | null
}

export const PREVIEW_STATUS_LABEL: Record<string, string> = {
  NONE: '未生成',
  PENDING: '生成中',
  READY: '已就绪',
  FAILED: '生成失败',
  SKIPPED: '暂不支持'
}

/** 实例授权可选类型与动作（与后端 AclService / PermissionRuleService 一致）。 */
export const ACL_ACTOR_TYPES = [
  { value: 'USER', label: '用户' },
  { value: 'ROLE', label: '角色' },
  { value: 'GROUP', label: '用户组' },
  { value: 'GROUP_ROLE', label: '组内角色' },
  { value: 'CREATOR', label: '创建人' },
  { value: 'OWNER', label: '属主' },
  { value: 'ALL', label: '所有人' }
]

export const ACL_ACTIONS = [
  { value: 'READ', label: '读取' },
  { value: 'WRITE', label: '修改' },
  { value: 'DELETE', label: '删除' },
  { value: 'GRANT', label: '授权' },
  { value: 'CHECKOUT', label: '检出' },
  { value: 'RELEASE', label: '发布/解锁' },
  { value: 'APPROVE', label: '审批' }
]

export interface BatchGrantReq {
  ids: string[]
  actorType: string
  actorId?: string | null
  actions: string
  effect?: string
}

export interface BatchGrantResult {
  id: string
  ok: boolean
  message?: string | null
}

export interface BatchImportError {
  line: number
  code?: string | null
  message: string
}

export interface BatchImportResult {
  total: number
  success: number
  failed: number
  errors: BatchImportError[]
  createdIds: string[]
}

/** 批量操作：打包下载、批量授权。 */
export const docBatchApi = {
  async download(ids: string[]): Promise<void> {
    const { blob, fileName } = await downloadFileByPost('/docs/batch/download', { ids })
    saveBlob(blob, fileName)
  },
  grant(req: BatchGrantReq): Promise<BatchGrantResult[]> {
    return http.post<BatchGrantResult[]>('/docs/batch/grant', req)
  },
  importCsv(file: File): Promise<BatchImportResult> {
    return uploadFile<BatchImportResult>('/docs/batch/import', file)
  },
  async downloadTemplate(): Promise<void> {
    const { blob, fileName } = await downloadFile('/docs/batch/import/template')
    saveBlob(blob, fileName || 'doc-import-template.csv')
  }
}

/** 保存的高级查询条件。 */
export interface DocSavedQuery {
  id: string
  name: string
  entityCode: string
  ownerId: string
  ownerName?: string | null
  conditionJson: string
  shared: boolean
  mine: boolean
  createdAt: string
  updatedAt: string
}

export interface DocSavedQueryReq {
  name: string
  conditionJson: string
  shared?: boolean
}

/** 高级查询条件接口：保存/复用/共享。 */
export const docQueryApi = {
  list: (): Promise<DocSavedQuery[]> => http.get<DocSavedQuery[]>('/doc-queries'),
  create(req: DocSavedQueryReq): Promise<DocSavedQuery> {
    return http.post<DocSavedQuery>('/doc-queries', req)
  },
  update(id: string, req: DocSavedQueryReq): Promise<DocSavedQuery> {
    return http.put<DocSavedQuery>(`/doc-queries/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/doc-queries/${id}`)
  }
}

/** 文档分类接口：分类树、增删改、分类属性（含继承）。 */
export const docClassApi = {
  tree: (): Promise<DocClassNode[]> => http.get<DocClassNode[]>('/doc-classes'),
  create(req: DocClassSaveReq): Promise<DocClassNode> {
    return http.post<DocClassNode>('/doc-classes', req)
  },
  update(id: string, req: DocClassSaveReq): Promise<DocClassNode> {
    return http.put<DocClassNode>(`/doc-classes/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/doc-classes/${id}`)
  },
  attrs(id: string): Promise<DocClassAttrDef[]> {
    return http.get<DocClassAttrDef[]>(`/doc-classes/${id}/attributes`)
  },
  saveAttrs(id: string, items: DocClassAttrItem[]): Promise<DocClassAttrDef[]> {
    return http.put<DocClassAttrDef[]>(`/doc-classes/${id}/attributes`, { items })
  }
}
