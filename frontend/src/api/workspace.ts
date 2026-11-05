import { http } from './http'

export type SpaceType = 'MY' | 'PUBLIC' | 'GROUP'

export const SPACE_LABEL: Record<SpaceType, string> = {
  MY: '我的空间',
  PUBLIC: '公共空间',
  GROUP: '组空间'
}

export interface FolderNode {
  id: string
  parentId?: string | null
  name: string
  spaceType: SpaceType
  path: string
  ownerId: string
  groupId?: string | null
  aclInherit: boolean
  sort: number
  createdAt: string
}

export interface FolderTreeNode extends FolderNode {
  children: FolderTreeNode[]
}

export interface ContentItem {
  id: string
  folderId: string
  objectId: string
  objectType: string
  code: string
  name: string
  lifecycleState: string
  sort: number
  addedAt: string
}

export interface FavoriteItem {
  objectId: string
  objectType: string
  code: string
  name: string
  lifecycleState: string
  createdAt: string
}

export interface RecentItem {
  objectId: string
  objectType: string
  code: string
  name: string
  lifecycleState: string
  accessedAt: string
}

/** 把平铺的文件夹列表组装成树。 */
export function buildTree(folders: FolderNode[]): FolderTreeNode[] {
  const nodes = new Map<string, FolderTreeNode>()
  folders.forEach((folder) => nodes.set(folder.id, { ...folder, children: [] }))
  const roots: FolderTreeNode[] = []
  nodes.forEach((node) => {
    const parent = node.parentId ? nodes.get(node.parentId) : undefined
    if (parent) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })
  const sortNodes = (list: FolderTreeNode[]): void => {
    list.sort((a, b) => a.sort - b.sort || a.name.localeCompare(b.name))
    list.forEach((node) => sortNodes(node.children))
  }
  sortNodes(roots)
  return roots
}

export const workspaceApi = {
  folders(space: SpaceType): Promise<FolderNode[]> {
    return http.get<FolderNode[]>('/workspace/folders', { space })
  },
  createFolder(req: {
    spaceType: SpaceType
    parentId?: string | null
    name: string
    groupId?: string | null
    sort?: number
  }): Promise<FolderNode> {
    return http.post<FolderNode>('/workspace/folders', req)
  },
  renameFolder(id: string, name: string): Promise<FolderNode> {
    return http.put<FolderNode>(`/workspace/folders/${id}/name`, { name })
  },
  moveFolder(id: string, parentId: string | null): Promise<void> {
    return http.put<void>(`/workspace/folders/${id}/parent`, { parentId })
  },
  deleteFolder(id: string): Promise<void> {
    return http.delete<void>(`/workspace/folders/${id}`)
  },
  content(folderId: string): Promise<ContentItem[]> {
    return http.get<ContentItem[]>(`/workspace/folders/${folderId}/content`)
  },
  addContent(folderId: string, objectId: string): Promise<void> {
    return http.post<void>(`/workspace/folders/${folderId}/content`, { objectId })
  },
  removeContent(folderId: string, objectId: string): Promise<void> {
    return http.delete<void>(`/workspace/folders/${folderId}/content/${objectId}`)
  },
  moveContent(objectId: string, fromFolderId: string, toFolderId: string): Promise<void> {
    return http.post<void>('/workspace/content/move', { objectId, fromFolderId, toFolderId })
  },
  favorites(): Promise<FavoriteItem[]> {
    return http.get<FavoriteItem[]>('/workspace/favorites')
  },
  addFavorite(objectId: string): Promise<void> {
    return http.post<void>(`/workspace/favorites/${objectId}`)
  },
  removeFavorite(objectId: string): Promise<void> {
    return http.delete<void>(`/workspace/favorites/${objectId}`)
  },
  recent(limit = 20): Promise<RecentItem[]> {
    return http.get<RecentItem[]>('/workspace/recent', { limit })
  },
  touchRecent(objectId: string): Promise<void> {
    return http.post<void>(`/workspace/recent/${objectId}`)
  }
}
