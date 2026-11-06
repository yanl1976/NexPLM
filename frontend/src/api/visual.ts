import { http } from './http'

/** 可视化引擎：SVIEW=旧系统 SView 插件（三维轻量化），PDF/IMAGE/TEXT 由浏览器直接渲染。 */
export type VisualEngine = 'SVIEW' | 'PDF' | 'IMAGE' | 'TEXT' | 'NONE'

export interface VisualFile {
  fileId: string
  fileName: string
  ext: string
  fileRole: string
  size: number
}

export interface VisualInfo {
  objectType: string
  objectId: string
  objectCode: string
  objectName: string
  engine: VisualEngine
  /** true 表示原始格式不能直接看，需要转换代理产出 .svlx / .pdf */
  needsConvert: boolean
  /** 需要转换时对应的旧系统转换命令名（FileToSvl / FileToPDF） */
  convertCommand?: string | null
  hint: string
  file: VisualFile | null
  files: VisualFile[]
}

export const visualApi = {
  doc(docId: string): Promise<VisualInfo> {
    return http.get<VisualInfo>(`/visual/docs/${docId}`)
  },
  item(itemId: string): Promise<VisualInfo> {
    return http.get<VisualInfo>(`/visual/items/${itemId}`)
  }
}
