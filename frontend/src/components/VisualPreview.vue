<template>
  <div v-loading="loading" class="visual-preview">
    <div v-if="info" class="visual-head">
      <el-tag size="small" :type="tagType">{{ engineLabel }}</el-tag>
      <span class="file-name">{{ info.file?.fileName ?? '暂无文件' }}</span>
      <span class="hint">{{ info.hint }}</span>
    </div>

    <template v-if="info">
      <sview-viewer
        v-if="info.engine === 'SVIEW' && !info.needsConvert && info.file"
        :file-id="info.file.fileId"
        :file-name="info.file.fileName"
      />
      <iframe v-else-if="info.engine === 'PDF' && blobUrl" :src="blobUrl" class="pdf-frame" title="PDF 预览" />
      <img v-else-if="info.engine === 'IMAGE' && blobUrl" :src="blobUrl" class="image" alt="图片预览" />
      <docx-preview
        v-else-if="info.engine === 'DOCX' && !info.needsConvert && info.file"
        :file-id="info.file.fileId"
        :file-name="info.file.fileName"
      />
      <sheet-preview
        v-else-if="info.engine === 'SHEET' && !info.needsConvert && info.file"
        :file-id="info.file.fileId"
        :file-name="info.file.fileName"
      />
      <pre v-else-if="info.engine === 'TEXT' && text !== ''" class="text-preview">{{ text }}</pre>
      <el-empty v-else :description="info.hint">
        <div v-if="info.needsConvert" class="convert-tip">
          <p>
            转换命令：<code>{{ info.convertCommand }}</code>
          </p>
          <p class="muted">
            沿用旧系统分布式文件服务（DistributedFileServer + HoteamsoftCommandService）的转换链路：
            FileToSvl 产出 .svlx 给 SView，FileToPDF 产出 PDF 给浏览器查看器；转换完成后刷新即可在线浏览。
          </p>
        </div>
        <el-button v-if="info.file" type="primary" @click="onDownload">下载原文件</el-button>
      </el-empty>
    </template>

    <el-empty v-else-if="!loading" :description="error || '暂无可视化内容'" />
  </div>
</template>

<script setup lang="ts">
/**
 * 在线可视化页签：先问后端该用哪种查看器，再按引擎渲染。
 * - SVIEW：三维轻量化（迁移自旧系统的 SView HTML5 插件）
 * - PDF / IMAGE / TEXT：浏览器直接渲染（原文件已是可看格式）
 * - DOCX / SHEET：前端本地解析（docx-preview / SheetJS），不需要转换代理
 * - needsConvert：原始 CAD / 旧版 Office 需转换代理产出 .svlx / .pdf，给出提示与下载入口
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { downloadFile, saveBlob } from '../api/http'
import { visualApi, type VisualInfo } from '../api/visual'
import DocxPreview from './DocxPreview.vue'
import SheetPreview from './SheetPreview.vue'
import SViewViewer from './SViewViewer.vue'

const props = defineProps<{
  objectType: 'doc' | 'item'
  objectId: string
}>()

const info = ref<VisualInfo | null>(null)
const loading = ref(false)
const error = ref('')
const blobUrl = ref('')
const text = ref('')
let localUrl = ''

const engineLabel = computed(() => {
  switch (info.value?.engine) {
    case 'SVIEW':
      return info.value.needsConvert ? '三维（待转换）' : '三维（SView）'
    case 'PDF':
      return info.value.needsConvert ? 'PDF（待转换）' : 'PDF'
    case 'IMAGE':
      return '图片'
    case 'TEXT':
      return '文本'
    case 'DOCX':
      return info.value.needsConvert ? 'Word（待转换）' : 'Word'
    case 'SHEET':
      return info.value.needsConvert ? '表格（待转换）' : '表格'
    default:
      return '不可预览'
  }
})

const tagType = computed<'primary' | 'success' | 'info' | 'warning' | 'danger'>(() => {
  if (!info.value) {
    return 'info'
  }
  if (info.value.needsConvert || info.value.engine === 'NONE') {
    return 'warning'
  }
  return info.value.engine === 'SVIEW' ? 'success' : 'primary'
})

function cleanup(): void {
  if (localUrl) {
    URL.revokeObjectURL(localUrl)
    localUrl = ''
  }
  blobUrl.value = ''
  text.value = ''
}

/**
 * 文本解码：国内 txt/csv 有大量 GBK/GB18030 存量文件，blob.text() 只按 UTF-8 解会变问号。
 * 先认 BOM，再用 UTF-8 试解；出现替换字符（U+FFFD）说明不是 UTF-8，回退 GB18030。
 */
async function readText(blob: Blob): Promise<string> {
  const bytes = new Uint8Array(await blob.arrayBuffer())
  if (bytes.length >= 3 && bytes[0] === 0xef && bytes[1] === 0xbb && bytes[2] === 0xbf) {
    return new TextDecoder('utf-8').decode(bytes.subarray(3))
  }
  if (bytes.length >= 2 && bytes[0] === 0xff && bytes[1] === 0xfe) {
    return new TextDecoder('utf-16le').decode(bytes.subarray(2))
  }
  if (bytes.length >= 2 && bytes[0] === 0xfe && bytes[1] === 0xff) {
    return new TextDecoder('utf-16be').decode(bytes.subarray(2))
  }
  const utf8 = new TextDecoder('utf-8').decode(bytes)
  if (utf8.includes('\uFFFD')) {
    try {
      return new TextDecoder('gb18030').decode(bytes)
    } catch {
      return utf8
    }
  }
  return utf8
}

async function loadBlob(fileId: string, engine: VisualInfo['engine']): Promise<void> {
  const { blob } = await downloadFile(`/files/${fileId}/content`)
  if (engine === 'TEXT') {
    text.value = await readText(blob)
    return
  }
  localUrl = URL.createObjectURL(blob)
  blobUrl.value = localUrl
}

async function load(): Promise<void> {
  if (!props.objectId) {
    return
  }
  loading.value = true
  error.value = ''
  cleanup()
  try {
    info.value =
      props.objectType === 'doc' ? await visualApi.doc(props.objectId) : await visualApi.item(props.objectId)
    const current = info.value
    if (current.file && !current.needsConvert && ['PDF', 'IMAGE', 'TEXT'].includes(current.engine)) {
      await loadBlob(current.file.fileId, current.engine)
    }
  } catch (err) {
    info.value = null
    error.value = err instanceof Error ? err.message : '加载可视化信息失败'
  } finally {
    loading.value = false
  }
}

async function onDownload(): Promise<void> {
  if (!info.value?.file) {
    return
  }
  try {
    const { blob, fileName } = await downloadFile(`/files/${info.value.file.fileId}/content`)
    saveBlob(blob, fileName || info.value.file.fileName)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '下载失败'
  }
}

watch(() => [props.objectType, props.objectId], load, { immediate: true })

onBeforeUnmount(cleanup)
</script>

<style scoped>
.visual-preview {
  min-height: 240px;
}

.visual-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.file-name {
  font-weight: 600;
  color: #303133;
}

.hint {
  color: #909399;
  font-size: 12px;
}

.pdf-frame {
  width: 100%;
  height: 640px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.image {
  max-width: 100%;
  max-height: 640px;
  display: block;
  margin: 0 auto;
}

.text-preview {
  max-height: 640px;
  overflow: auto;
  padding: 12px;
  margin: 0;
  background: #f7f8fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.convert-tip {
  max-width: 560px;
  text-align: left;
  color: #606266;
  font-size: 13px;
}

.convert-tip .muted {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
</style>
