import { http } from './http'

/** P8-3-1 IATF16949 体系文件：模板 + 文件夹 + 版本 + 审批发布。 */

export const DOC_LEVEL_LABEL: Record<string, string> = {
  MANUAL: '手册',
  PROCEDURE: '程序文件',
  WI: '作业指导书',
  RECORD: '记录表单'
}

export const DOC_STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '待审批',
  RELEASED: '已发布',
  OBSOLETE: '已作废'
}

export interface FolderRow {
  id: string
  parentId?: string | null
  code: string
  name: string
  /** /QP/WI，用于子树过滤 */
  path: string
  sortNo: number
  docCount: number
}

export interface FolderSaveReq {
  code: string
  name: string
  parentId?: string | null
  sortNo?: number
}

export interface SectionTemplate {
  key: string
  title: string
  hint?: string | null
  required: boolean
}

export interface TemplateRow {
  id: string
  code: string
  name: string
  docLevel: string
  folderId?: string | null
  folderName?: string | null
  defaultReviewMonths: number
  enabled: boolean
  sectionCount: number
  updatedAt?: string | null
}

export interface TemplateDetail {
  id: string
  code: string
  name: string
  docLevel: string
  folderId?: string | null
  folderName?: string | null
  defaultReviewMonths: number
  enabled: boolean
  remark?: string | null
  sections: SectionTemplate[]
}

export interface TemplateSaveReq {
  name: string
  docLevel?: string
  folderId?: string | null
  defaultReviewMonths?: number
  enabled?: boolean
  remark?: string | null
  sections?: SectionTemplate[]
}

export interface DocSection {
  key: string
  title: string
  content: string
  required: boolean
}

export interface DocRow {
  id: string
  code: string
  name: string
  docLevel: string
  folderId?: string | null
  folderName?: string | null
  templateId?: string | null
  templateCode?: string | null
  versionNo: number
  status: string
  ownerId?: string | null
  ownerName?: string | null
  effectiveDate?: string | null
  nextReviewDate?: string | null
  reviewMonths: number
  /** 已到期/将到期评审（服务端算）。 */
  dueReview: boolean
  updatedAt?: string | null
}

export interface VersionRow {
  id: string
  versionNo: number
  changeNote?: string | null
  status: string
  releasedAt?: string | null
  releasedByName?: string | null
  sectionCount: number
}

export interface DocDetail {
  id: string
  code: string
  name: string
  docLevel: string
  folderId?: string | null
  folderName?: string | null
  templateId?: string | null
  templateCode?: string | null
  versionNo: number
  status: string
  ownerId?: string | null
  ownerName?: string | null
  reviewMonths: number
  effectiveDate?: string | null
  nextReviewDate?: string | null
  summary?: string | null
  fileId?: string | null
  approveNote?: string | null
  approvedAt?: string | null
  obsoleteAt?: string | null
  sections: DocSection[]
  versions: VersionRow[]
}

export interface DocSaveReq {
  code: string
  name: string
  docLevel?: string
  folderId?: string | null
  ownerId?: string | null
  reviewMonths?: number
  summary?: string | null
  sections?: DocSection[]
}

export const sysDocApi = {
  /* 文件夹 */
  folders(): Promise<FolderRow[]> {
    return http.get<FolderRow[]>('/qlt/sys-doc-folders')
  },
  createFolder(req: FolderSaveReq): Promise<FolderRow> {
    return http.post<FolderRow>('/qlt/sys-doc-folders', req)
  },
  updateFolder(id: string, req: FolderSaveReq): Promise<FolderRow> {
    return http.put<FolderRow>(`/qlt/sys-doc-folders/${id}`, req)
  },
  removeFolder(id: string): Promise<void> {
    return http.delete<void>(`/qlt/sys-doc-folders/${id}`)
  },

  /* 模板 */
  templates(params?: { keyword?: string; enabled?: boolean }): Promise<TemplateRow[]> {
    return http.get<TemplateRow[]>('/qlt/sys-doc-templates', params)
  },
  template(id: string): Promise<TemplateDetail> {
    return http.get<TemplateDetail>(`/qlt/sys-doc-templates/${id}`)
  },
  createTemplate(req: TemplateSaveReq): Promise<TemplateDetail> {
    return http.post<TemplateDetail>('/qlt/sys-doc-templates', req)
  },
  updateTemplate(id: string, req: TemplateSaveReq): Promise<TemplateDetail> {
    return http.put<TemplateDetail>(`/qlt/sys-doc-templates/${id}`, req)
  },
  removeTemplate(id: string): Promise<void> {
    return http.delete<void>(`/qlt/sys-doc-templates/${id}`)
  },

  /* 体系文件 */
  docs(params: {
    keyword?: string
    folderId?: string
    status?: string
    docLevel?: string
    dueReviewOnly?: boolean
    page?: number
    size?: number
  }) {
    return http.get<{ items: DocRow[]; total: number; page: number; size: number }>(
      '/qlt/sys-docs',
      params
    )
  },
  detail(id: string): Promise<DocDetail> {
    return http.get<DocDetail>(`/qlt/sys-docs/${id}`)
  },
  createFromTemplate(req: {
    templateId: string
    code: string
    name: string
    ownerId?: string | null
  }): Promise<DocDetail> {
    return http.post<DocDetail>('/qlt/sys-docs/from-template', req)
  },
  create(req: DocSaveReq): Promise<DocDetail> {
    return http.post<DocDetail>('/qlt/sys-docs', req)
  },
  update(id: string, req: DocSaveReq): Promise<DocDetail> {
    return http.put<DocDetail>(`/qlt/sys-docs/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/qlt/sys-docs/${id}`)
  },
  submit(id: string): Promise<DocDetail> {
    return http.post<DocDetail>(`/qlt/sys-docs/${id}/submit`)
  },
  approve(id: string, passed: boolean, note?: string | null): Promise<DocDetail> {
    return http.post<DocDetail>(`/qlt/sys-docs/${id}/approve`, { passed, note })
  },
  revise(id: string, changeNote?: string | null): Promise<DocDetail> {
    return http.post<DocDetail>(`/qlt/sys-docs/${id}/revise`, { changeNote })
  },
  obsolete(id: string): Promise<DocDetail> {
    return http.post<DocDetail>(`/qlt/sys-docs/${id}/obsolete`)
  },
  dueReview(days?: number): Promise<DocRow[]> {
    return http.get<DocRow[]>('/qlt/sys-docs/due-review', { days })
  }
}
