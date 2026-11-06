<template>
  <div class="page">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="接口预留（P9 实现真实下发）"
      description="本页只做 PLM 侧的三件事：固化下发载荷、记录下发尝试、提供取数入口。真实 ERP 契约（字段映射/认证/幂等）与回执对账由 P9 的适配器实现。"
      class="notice"
    />

    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px" @change="load">
        <el-option v-for="(label, value) in DISPATCH_STATUS_LABEL" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">生成下发载荷</el-button>
      <span class="hint">只能对已发布路线生成载荷；载荷一次生成即固定，路线后续改动不影响历史记录</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="code" label="下发编号" width="130" />
      <el-table-column prop="routingCode" label="工艺路线" width="140" />
      <el-table-column prop="objectCode" label="零件" width="140" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{ DISPATCH_STATUS_LABEL[row.status] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
      <el-table-column label="载荷 sha256" width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="mono">{{ row.payloadSha ? row.payloadSha.slice(0, 12) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="生成时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="290" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看载荷</el-button>
          <el-button link type="primary" @click="onDownload(row)">下载 JSON</el-button>
          <el-button v-if="canWrite" link type="warning" @click="onCallback(row)">标记回执</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createVisible" title="生成下发载荷" width="560px">
      <el-form label-width="90px">
        <el-form-item label="工艺路线" required>
          <el-select
            v-model="routingId"
            filterable
            remote
            reserve-keyword
            placeholder="编码 / 零件搜索（仅已发布）"
            :remote-method="searchRoutings"
            :loading="routingsLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in routings"
              :key="item.id"
              :label="`${item.code} ${item.name}（${item.objectCode}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="`下发载荷 - ${detailData?.code ?? ''}`" width="900px" top="5vh">
      <el-descriptions v-if="detailData" :column="3" border size="small">
        <el-descriptions-item label="路线">{{ detailData.routingCode }}</el-descriptions-item>
        <el-descriptions-item label="零件">{{ detailData.objectCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ DISPATCH_STATUS_LABEL[detailData.status] ?? detailData.status }}
        </el-descriptions-item>
      </el-descriptions>
      <pre class="payload">{{ payloadText }}</pre>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailData" type="primary" @click="onDownload(detailData)">下载 JSON</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DISPATCH_STATUS_LABEL,
  cappApi,
  dispatchApi,
  type DispatchDetail,
  type DispatchRow,
  type RoutingRow
} from '@/api/capp'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('capp.routing.write'))

const rows = ref<DispatchRow[]>([])
const loading = ref(false)
const status = ref('')

const createVisible = ref(false)
const creating = ref(false)
const routingId = ref('')
const routings = ref<RoutingRow[]>([])
const routingsLoading = ref(false)

const detailVisible = ref(false)
const detailData = ref<DispatchDetail | null>(null)

const payloadText = computed(() =>
  detailData.value ? JSON.stringify(detailData.value.payload, null, 2) : ''
)

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    rows.value = await dispatchApi.list({ status: status.value || undefined })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载下发记录失败')
  } finally {
    loading.value = false
  }
}

async function searchRoutings(keywordText: string): Promise<void> {
  routingsLoading.value = true
  try {
    const result = await cappApi.list({
      page: 1,
      size: 50,
      keyword: keywordText || undefined,
      status: 'RELEASED'
    })
    routings.value = result.items
  } catch {
    routings.value = []
  } finally {
    routingsLoading.value = false
  }
}

function openCreate(): void {
  routingId.value = ''
  createVisible.value = true
  searchRoutings('')
}

async function onCreate(): Promise<void> {
  if (!routingId.value) {
    ElMessage.warning('请选择已发布的工艺路线')
    return
  }
  creating.value = true
  try {
    const created = await dispatchApi.create(routingId.value)
    ElMessage.success(`已生成载荷 ${created.code}（待发送）`)
    createVisible.value = false
    await load()
    detailData.value = created
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '生成失败')
  } finally {
    creating.value = false
  }
}

async function openDetail(row: DispatchRow): Promise<void> {
  try {
    detailData.value = await dispatchApi.detail(row.id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载载荷失败')
  }
}

async function onDownload(row: DispatchRow): Promise<void> {
  try {
    await dispatchApi.downloadPayload(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载失败')
  }
}

async function onCallback(row: DispatchRow): Promise<void> {
  let input: string
  try {
    const result = await ElMessageBox.prompt(
      '模拟 ERP 回执（SENT 已发送 / FAILED 失败 / VOID 作废），可附说明：',
      '标记回执',
      { inputValue: 'SENT', confirmButtonText: '提交', cancelButtonText: '取消' }
    )
    input = result.value
  } catch {
    return
  }
  const value = (input || '').trim().toUpperCase()
  if (!['SENT', 'FAILED', 'VOID'].includes(value)) {
    ElMessage.warning('状态只能是 SENT / FAILED / VOID')
    return
  }
  try {
    await dispatchApi.callback(row.id, value, `人工标记为 ${value}`)
    ElMessage.success('已更新')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败')
  }
}

function statusTagType(value: string): 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'SENT') {
    return 'success'
  }
  if (value === 'FAILED') {
    return 'danger'
  }
  return value === 'VOID' ? 'info' : 'warning'
}

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.notice {
  margin-bottom: 12px;
}

.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.hint {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.mono {
  font-family: Consolas, monospace;
}

.payload {
  max-height: 460px;
  margin-top: 12px;
  padding: 12px;
  overflow: auto;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
}
</style>
