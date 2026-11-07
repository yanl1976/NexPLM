<template>
  <div class="docx-wrap">
    <div v-if="loading" class="docx-mask">正在渲染 Word 文档…</div>
    <el-empty v-else-if="error" :description="error">
      <el-button type="primary" @click="onDownload">下载文件</el-button>
    </el-empty>
    <div v-show="!loading && !error" ref="host" class="docx-host" />
  </div>
</template>

<script setup lang="ts">
/**
 * Word（.docx）在线预览。
 *
 * 走纯前端渲染：浏览器下载原文件后用 docx-preview 解析 OOXML 直接生成 DOM，
 * 不经过转换代理（旧系统是 HoteamAsposeToPDF + ImageView，需要 Windows 节点与商业授权）。
 *
 * 技术要点：
 * - docx-preview 体积较大，用动态 import 拆包，不进首屏；
 * - useBase64URL=true 让图片内联为 data URL，避免页面里 blob URL 随容器销毁失效；
 * - 仅支持 .docx/.docm 这类 OOXML 包，旧版 .doc（OLE 复合文档）由后端归入转换链路。
 */
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { downloadFile, saveBlob } from '../api/http'

const props = defineProps<{
  fileId: string
  fileName?: string
}>()

/** 超过该体积直接提示下载：几十 MB 的 Word 在前端解析会长时间卡住主线程。 */
const MAX_SIZE = 30 * 1024 * 1024

const host = ref<HTMLElement | null>(null)
const loading = ref(true)
const error = ref('')

/** 等待容器可见的布局观察器（见 waitForLayout），卸载时需断开。 */
let layoutObserver: ResizeObserver | null = null

/**
 * 等待容器真正参与布局。
 *
 * docx-preview 用 getBoundingClientRect 测量段落实际宽度来换算 Word 制表位，
 * 而 Word 目录的「标题 …… 页码」完全依赖右对齐 leader 制表位。
 * 容器处于 display:none（未激活的 el-tab-pane、v-show 遮罩）时测量值全为 0，
 * tab 会被算成整页宽度，表现为点线撑满整行、页码换行、标题折行。
 */
function waitForLayout(el: HTMLElement, timeout = 5000): Promise<void> {
  if (el.clientWidth > 0) {
    return Promise.resolve()
  }
  return new Promise<void>((resolve) => {
    let settled = false
    let timer = 0
    const finish = (): void => {
      if (settled) {
        return
      }
      settled = true
      layoutObserver?.disconnect()
      layoutObserver = null
      window.clearTimeout(timer)
      resolve()
    }
    timer = window.setTimeout(finish, timeout)
    layoutObserver = new ResizeObserver(() => {
      if (el.clientWidth > 0) {
        finish()
      }
    })
    layoutObserver.observe(el)
  })
}

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const { blob } = await downloadFile(`/files/${props.fileId}/content`)
    if (blob.size > MAX_SIZE) {
      throw new Error(`文档 ${(blob.size / 1024 / 1024).toFixed(1)}MB，超过在线预览上限 30MB，请下载后查看`)
    }
    const { renderAsync } = await import('docx-preview')
    const container = host.value
    if (!container) {
      return
    }
    container.innerHTML = ''
    // 先撤掉遮罩，再等到容器真正参与布局（未激活的 el-tab-pane 也会走到这里），
    // 最后让出一帧确保 A4 版面宽度稳定，才交给 docx-preview 去测量制表位。
    loading.value = false
    await nextTick()
    await waitForLayout(container)
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
    await renderAsync(blob, container, container, {
      className: 'docx',
      inWrapper: true,
      ignoreWidth: false,
      ignoreHeight: false,
      ignoreFonts: false,
      breakPages: true,
      ignoreLastRenderedPageBreak: false,
      experimental: true,
      useBase64URL: true,
      renderHeaders: true,
      renderFooters: true,
      renderFootnotes: true,
      renderEndnotes: true
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Word 文档渲染失败，请下载后本地查看'
  } finally {
    loading.value = false
  }
}

async function onDownload(): Promise<void> {
  try {
    const { blob, fileName } = await downloadFile(`/files/${props.fileId}/content`)
    saveBlob(blob, fileName || props.fileName || 'document.docx')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '下载失败'
  }
}

onMounted(load)

onBeforeUnmount(() => {
  layoutObserver?.disconnect()
  layoutObserver = null
  if (host.value) {
    host.value.innerHTML = ''
  }
})
</script>

<style scoped>
.docx-wrap {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 520px;
}

.docx-host {
  height: 640px;
  overflow: auto;
  background: #f2f5f9;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.docx-host :deep(.docx-wrapper) {
  background: #f2f5f9;
  padding: 16px 0;
}

.docx-host :deep(.docx-wrapper > section.docx) {
  margin: 0 auto 16px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.12);
  /* .docx-wrapper 是 flex 容器，不锁定就会被压到容器宽度：
     段落变窄后 Word 目录的右对齐制表位（A4 正文 9072 twips ≈ 604.7px）无法到达，
     点线会被算成负宽/整行宽，页码换行。宁可出横向滚动条也不能压缩页面。 */
  flex-shrink: 0;
}

.docx-mask {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 520px;
  color: #606266;
  background: #f2f5f9;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}
</style>
