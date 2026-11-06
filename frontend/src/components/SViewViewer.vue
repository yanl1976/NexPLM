<template>
  <div class="sview-wrap">
    <div v-if="loading" class="sview-mask">正在加载三维模型…</div>
    <el-empty v-else-if="error" :description="error">
      <el-button type="primary" @click="onDownload">下载文件</el-button>
    </el-empty>
    <iframe v-show="!error" :key="frameKey" :src="src" class="sview-frame" title="SView 三维查看器" />
  </div>
</template>

<script setup lang="ts">
/**
 * SView 三维查看器（迁移自旧系统 Server/Base/Ctrls/SView，HTML5 轻量化插件）。
 *
 * 迁移方式与旧系统 `$.hoteamSView.load` 同构：
 * 1. 先把查看参数写进 sessionStorage（键 `Hoteam_<ctrlId>_Params`，main.js 从这里读 `fileUrl`）；
 * 2. 再让 iframe 加载 `SviewLoad.html?ctrlId=<ctrlId>`，插件内部自行拉取模型并渲染。
 *
 * 与旧系统的差异：
 * - 旧系统传的是文件服务 http 地址，这里传同源 blob URL（文件流经过 `GET /files/{id}/content` 的数据范围校验）；
 * - 不再传 annotateUrl（旧系统批注通道 `/Base/Js/wsConnect.js` 未迁移）。
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { downloadFile, saveBlob } from '../api/http'

const props = defineProps<{
  fileId: string
  fileName?: string
}>()

const ctrlId = `nexplm${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`
const paramsKey = `Hoteam_${ctrlId}_Params`
const src = ref('')
const frameKey = ref(0)
const loading = ref(true)
const error = ref('')
let objectUrl = ''

function cleanupObjectUrl(): void {
  if (objectUrl) {
    URL.revokeObjectURL(objectUrl)
    objectUrl = ''
  }
}

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const { blob } = await downloadFile(`/files/${props.fileId}/content`)
    cleanupObjectUrl()
    objectUrl = URL.createObjectURL(blob)
    // main.js 的 else 分支会读 sessionStorage 的 AppSets（旧系统由父页面注入），
    // 这里给一个空对象，避免 JSON.parse(null) 得到 null 后解构报错。
    if (!sessionStorage.getItem('AppSets')) {
      sessionStorage.setItem('AppSets', '{}')
    }
    sessionStorage.setItem(
      paramsKey,
      JSON.stringify({
        fileUrl: objectUrl,
        annotateUrl: null,
        currentCommentIndex: 0,
        language: 'zhs',
        ctrlID: ctrlId,
        mark: false
      })
    )
    src.value = `/sview/SviewLoad.html?ctrlId=${ctrlId}`
    frameKey.value += 1
    // 模型加载完成没有可靠回执，这里只等 iframe 文档就绪后撤掉遮罩
    window.setTimeout(() => {
      loading.value = false
    }, 800)
  } catch (err) {
    loading.value = false
    error.value = err instanceof Error ? err.message : '模型加载失败，请下载后本地查看'
  }
}

async function onDownload(): Promise<void> {
  try {
    const { blob, fileName } = await downloadFile(`/files/${props.fileId}/content`)
    saveBlob(blob, fileName || props.fileName || 'model')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '下载失败'
  }
}

onMounted(load)

onBeforeUnmount(() => {
  sessionStorage.removeItem(paramsKey)
  cleanupObjectUrl()
})
</script>

<style scoped>
.sview-wrap {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 520px;
  background: #f2f5f9;
}

.sview-frame {
  width: 100%;
  height: 100%;
  min-height: 520px;
  border: 0;
  display: block;
}

.sview-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  background: #f2f5f9;
  z-index: 2;
}
</style>
