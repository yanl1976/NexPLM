<template>
  <div class="m-page">
    <div class="scan-box">
      <video v-show="scanning" ref="videoRef" class="scan-video" playsinline muted />
      <div v-if="!scanning" class="scan-placeholder">
        <el-icon :size="34"><Camera /></el-icon>
        <span>{{ supported ? '点击开始扫码' : '当前浏览器不支持网页扫码' }}</span>
        <span v-if="!supported" class="hint">请用手机相机扫码后从相册/链接打开，或在下面手动输入编码</span>
      </div>
      <div v-if="scanning" class="scan-frame" />
    </div>

    <div class="scan-actions">
      <el-button v-if="!scanning" type="primary" :disabled="!supported" @click="startScan">开始扫码</el-button>
      <el-button v-else type="danger" plain @click="stopScan">停止</el-button>
    </div>

    <div class="manual-box">
      <el-input v-model="codeInput" placeholder="手动输入编码，如 PN-000001" clearable @keyup.enter="search()">
        <template #append>
          <el-button :loading="loading" @click="search()">查询</el-button>
        </template>
      </el-input>
    </div>

    <div v-loading="loading" class="result-list">
      <div v-for="item in results" :key="`${item.objectType}-${item.id}`" class="result-card">
        <div class="result-head">
          <el-tag size="small">{{ OBJECT_TYPE_LABEL[item.objectType] ?? item.objectType }}</el-tag>
          <span class="result-state">{{ item.lifecycleState }}</span>
        </div>
        <div class="result-code">{{ item.code }}</div>
        <div class="result-name">{{ item.name }}</div>
        <div class="result-actions">
          <el-button
            v-if="detailRouteOf(item.objectType, item.id)"
            size="small"
            type="primary"
            @click="openDetail(item)"
          >
            查看详情
          </el-button>
          <span v-else class="hint">该类型暂无移动端详情，请在电脑端查看</span>
        </div>
      </div>
      <el-empty
        v-if="searched && !loading && !results.length"
        description="没有找到该编码的对象"
        :image-size="60"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { OBJECT_TYPE_LABEL, detailRouteOf, lookupApi, type LookupResult } from '@/api/lookup'

const router = useRouter()

const videoRef = ref<HTMLVideoElement | null>(null)
const scanning = ref(false)
const supported = ref(false)
const codeInput = ref('')
const loading = ref(false)
const results = ref<LookupResult[]>([])
const searched = ref(false)

// BarcodeDetector 目前只有部分浏览器（Chrome/Android）提供，因此必须保留手输路径
const DetectorCtor = (window as unknown as { BarcodeDetector?: new (options?: unknown) => {
  detect: (source: CanvasImageSource) => Promise<Array<{ rawValue: string }>>
} }).BarcodeDetector

supported.value = Boolean(DetectorCtor)

let stream: MediaStream | null = null
let timer: number | null = null

async function startScan(): Promise<void> {
  if (!DetectorCtor) {
    ElMessage.warning('当前浏览器不支持网页扫码，请手动输入编码')
    return
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    ElMessage.warning('无法访问摄像头，请手动输入编码')
    return
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment' }
    })
    const video = videoRef.value
    if (!video) {
      stopScan()
      return
    }
    video.srcObject = stream
    await video.play()
    scanning.value = true
    const detector = new DetectorCtor({ formats: ['qr_code', 'code_128', 'ean_13'] })
    void detectLoop(detector)
  } catch (error) {
    stopScan()
    ElMessage.error(error instanceof Error ? error.message : '摄像头启动失败')
  }
}

async function detectLoop(detector: {
  detect: (source: CanvasImageSource) => Promise<Array<{ rawValue: string }>>
}): Promise<void> {
  if (!scanning.value || !videoRef.value) {
    return
  }
  try {
    const codes = await detector.detect(videoRef.value)
    if (codes.length) {
      const value = codes[0].rawValue
      stopScan()
      codeInput.value = value
      await search(value)
      return
    }
  } catch {
    // 单帧识别失败很常见（对焦/反光），忽略后继续下一帧
  }
  timer = window.setTimeout(() => void detectLoop(detector), 400)
}

function stopScan(): void {
  scanning.value = false
  if (timer !== null) {
    window.clearTimeout(timer)
    timer = null
  }
  if (stream) {
    stream.getTracks().forEach((track) => track.stop())
    stream = null
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
}

async function search(code?: string): Promise<void> {
  const value = (code ?? codeInput.value).trim()
  if (!value) {
    ElMessage.warning('请输入编码')
    return
  }
  loading.value = true
  searched.value = true
  try {
    results.value = await lookupApi.byCode(value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '查询失败')
  } finally {
    loading.value = false
  }
}

function openDetail(item: LookupResult): void {
  const route = detailRouteOf(item.objectType, item.id)
  if (route) {
    void router.push(route)
  }
}

onUnmounted(stopScan)
</script>

<style scoped>
.m-page {
  padding-bottom: 8px;
}

.scan-box {
  position: relative;
  height: 200px;
  border-radius: 8px;
  background: #1f2d3d;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.scan-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.scan-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #c8d3e0;
  font-size: 13px;
  padding: 0 16px;
  text-align: center;
}

.scan-frame {
  position: absolute;
  width: 140px;
  height: 140px;
  border: 2px solid #409eff;
  border-radius: 6px;
}

.scan-actions {
  display: flex;
  justify-content: center;
  margin: 10px 0;
}

.scan-actions .el-button {
  width: 160px;
}

.manual-box {
  margin-bottom: 10px;
}

.hint {
  font-size: 11px;
  color: #9aa4b2;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-card {
  background: #fff;
  border: 1px solid #e5e8ef;
  border-radius: 8px;
  padding: 10px 12px;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-state {
  font-size: 11px;
  color: #8a94a6;
}

.result-code {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
}

.result-name {
  margin-top: 4px;
  font-size: 13px;
  color: #303133;
  word-break: break-all;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
