import { downloadFile, http, saveBlob, uploadFile } from './http'

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

export interface DocSaveReq {
  code?: string
  name: string
  docType: string
  source?: string
  keyword?: string
  description?: string
  orgId?: string | null
  securityLevel?: number
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
  upload(id: string, file: File, fileRole: FileRole = 'ATTACHMENT'): Promise<{ fileId: string }> {
    return uploadFile<{ fileId: string }>(`/docs/${id}/files`, file, { fileRole })
  },
  removeFile(id: string, linkId: string): Promise<void> {
    return http.delete<void>(`/docs/${id}/files/${linkId}`)
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
