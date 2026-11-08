import { http } from './http'

/** 文档文件夹（ws_folder，space_type='DOC'）：整理维度，与 doc_class 分类并行。 */
export interface DocFolderRow {
  id: string
  parent_id?: string | null
  name: string
  path: string
  sort?: number | null
}

export interface DocFolderTreeNode extends DocFolderRow {
  children: DocFolderTreeNode[]
}

export const docFolderApi = {
  tree() {
    return http.get<DocFolderRow[]>('/doc-folders')
  },
  create(req: { name: string; parentId?: string | null }) {
    return http.post<{ id: string; name: string; path: string }>('/doc-folders', req)
  },
  rename(id: string, name: string) {
    return http.put<void>(`/doc-folders/${id}`, { name })
  },
  remove(id: string) {
    return http.delete<void>(`/doc-folders/${id}`)
  },
  addDocument(folderId: string, docId: string) {
    return http.post<void>(`/doc-folders/${folderId}/documents?docId=${encodeURIComponent(docId)}`, {})
  },
  removeDocument(folderId: string, docId: string) {
    return http.delete<void>(`/doc-folders/${folderId}/documents/${docId}`)
  }
}
