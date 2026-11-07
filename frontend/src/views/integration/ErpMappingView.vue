<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span class="title">ERP 字段映射</span>
            <span class="hint">旧系统 ERPConfig.xml 三段落库后可编辑、可试算；缺口不隐藏</span>
          </div>
          <div class="toolbar">
            <el-button :disabled="!canWrite || !dirty" type="primary" :loading="saving" @click="onSave">
              保存{{ dirty ? '（有改动）' : '' }}
            </el-button>
            <el-button :disabled="!detail?.previewSupported" @click="openPreview">试算报文</el-button>
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <el-radio-group v-model="objectCode" @change="onSegmentChange">
        <el-radio-button v-for="segment in segments" :key="segment.objectCode" :value="segment.objectCode">
          {{ segment.label }}
          <el-tag v-if="segment.gapCount" size="small" type="danger" class="seg-tag">
            {{ segment.gapCount }} 缺口
          </el-tag>
        </el-radio-button>
      </el-radio-group>

      <div v-if="detail" class="stats">
        <div class="stat">
          <span class="stat-label">列数</span>
          <span class="stat-value">{{ detail.rules.length }}</span>
        </div>
        <div class="stat">
          <span class="stat-label">已映射</span>
          <span class="stat-value ok">{{ detail.mappedCount }}</span>
        </div>
        <div class="stat">
          <span class="stat-label">缺数据源</span>
          <span class="stat-value danger">{{ detail.gapCount }}</span>
        </div>
        <div class="stat">
          <span class="stat-label">待确认</span>
          <span class="stat-value warn">{{ detail.pendingCount }}</span>
        </div>
        <div class="stat stat-wide">
          <span class="stat-label">模板</span>
          <span class="stat-text">
            {{ detail.sourceSegment }} · 端点 {{ detail.endpointCode }} · {{ detail.remark }}
          </span>
        </div>
      </div>

      <el-alert v-if="detail && !detail.previewSupported" type="info" :closable="false" show-icon class="tip">
        {{ detail.label }}暂不支持试算：本系统没有对应业务实体，这 24 列仅作为字段对照保留，
        待请购模块落地后才能组装报文。
      </el-alert>
      <el-alert v-else type="info" :closable="false" show-icon class="tip">
        「新系统取值路径」为空 = 新系统没有该数据源（缺口）。字段落点确认后在这里补齐即可，
        不需要改代码；数组路径写 <code>lines[].childCode</code>，占位符写
        <code>System$CurrentDate</code> / <code>USERINFO$ECODE</code> / <code>Parent$字段</code>。
      </el-alert>

      <el-table v-loading="loading" :data="detail?.rules ?? []" border size="small" max-height="560">
        <el-table-column label="#" width="52" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="ERP 字段" width="180">
          <template #default="{ row }">
            <div class="cell-main">{{ row.erpName || '-' }}</div>
            <div class="cell-sub">{{ row.caption || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="旧 PDM 列" width="150">
          <template #default="{ row }">
            <div class="cell-main">{{ row.pdmName || row.source }}</div>
            <div class="cell-sub">{{ row.pdmCaption || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="新系统取值路径" min-width="190">
          <template #default="{ row }">
            <el-input v-model="row.sourcePath" size="small" placeholder="留空=缺口" />
          </template>
        </el-table-column>
        <el-table-column label="下发路径（SendPath）" min-width="240">
          <template #default="{ row }">
            <el-input v-model="row.target" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="转换" width="110">
          <template #default="{ row }">
            <el-select v-model="row.transform" size="small" clearable placeholder="无">
              <el-option v-for="(label, value) in TRANSFORM_LABEL" :key="value" :label="label" :value="value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="可空" width="70" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.canNull" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusDot
              :tone="ERP_STATUS_TONE[statusOf(row)] ?? 'muted'"
              :label="ERP_STATUS_LABEL[statusOf(row)] ?? statusOf(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="200">
          <template #default="{ row }">
            <el-input v-model="row.note" size="small" placeholder="缺口原因 / 待确认事项" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="previewVisible" title="下发报文试算" width="980px">
      <div class="preview-toolbar">
        <el-input v-model="previewBizId" placeholder="对象 ID（物料或 BOM 根件）" style="width: 240px" />
        <el-select v-if="objectCode === 'Bom'" v-model="previewViewType" style="width: 140px">
          <el-option v-for="(label, value) in BOM_VIEW_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button type="primary" :loading="previewing" :disabled="!previewBizId.trim()" @click="runPreview">
          生成报文
        </el-button>
        <span v-if="preview" class="hint">
          幂等键 {{ preview.bizKey }} · 已填充 {{ preview.filledCount }} / 缺口 {{ preview.missingCount }}
        </span>
      </div>

      <div v-if="preview" class="preview-body">
        <div class="preview-col">
          <div class="preview-title">组装后的金蝶报文</div>
          <pre class="payload">{{ previewJson }}</pre>
          <div class="hint">{{ preview.entryNote }}</div>
        </div>
        <div class="preview-col">
          <div class="preview-title">缺口清单（{{ preview.missing.length }}）</div>
          <el-table :data="preview.missing" border size="small" max-height="380">
            <el-table-column prop="source" label="来源" width="110" />
            <el-table-column prop="target" label="下发路径" min-width="180" show-overflow-tooltip />
            <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
            <template #empty>
              <el-empty description="没有缺口，全部字段都有值" :image-size="48" />
            </template>
          </el-table>
        </div>
      </div>
      <el-empty v-else description="填对象 ID 后点「生成报文」" :image-size="60" />
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import StatusDot from '@/components/StatusDot.vue'
import {
  ERP_STATUS_LABEL,
  ERP_STATUS_TONE,
  TRANSFORM_LABEL,
  itgApi,
  type ErpMappingDetail,
  type ErpPreviewResult,
  type ErpRuleRow,
  type ErpRuleSaveReq,
  type ErpSegmentRow
} from '@/api/itg'
import { BOM_VIEW_LABEL } from '@/api/item'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = userStore.can('itg.config.write')

const segments = ref<ErpSegmentRow[]>([])
const objectCode = ref('Material')
const detail = ref<ErpMappingDetail | null>(null)
const loading = ref(false)
const saving = ref(false)
const dirty = ref(false)

const previewVisible = ref(false)
const previewing = ref(false)
const previewBizId = ref('')
const previewViewType = ref('DESIGN')
const preview = ref<ErpPreviewResult | null>(null)

const previewJson = computed(() => (preview.value ? JSON.stringify(preview.value.payload, null, 2) : ''))

/** 表格里改任何一格都算"有改动"，保存按钮据此提示（避免改完以为已存）。 */
let suppressDirty = false
watch(
  detail,
  () => {
    if (!suppressDirty) {
      dirty.value = true
    }
  },
  { deep: true }
)

/** 状态在本地按取值路径即时推导，保存后以后端为准。 */
function statusOf(row: ErpRuleRow): string {
  if (!row.sourcePath || !row.sourcePath.trim()) {
    return 'GAP'
  }
  return row.status === 'PENDING' ? 'PENDING' : 'OK'
}

async function loadSegments(): Promise<void> {
  segments.value = await itgApi.erpSegments()
}

async function load(): Promise<void> {
  loading.value = true
  try {
    suppressDirty = true
    detail.value = await itgApi.erpMapping(objectCode.value)
    dirty.value = false
    await nextTick()
    suppressDirty = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载字段映射失败')
  } finally {
    loading.value = false
  }
}

async function onSegmentChange(): Promise<void> {
  preview.value = null
  await load()
}

function toSaveReq(row: ErpRuleRow): ErpRuleSaveReq {
  return {
    erpName: row.erpName,
    caption: row.caption,
    pdmName: row.pdmName,
    pdmCaption: row.pdmCaption,
    source: row.source,
    sourcePath: row.sourcePath?.trim() || null,
    target: row.target,
    transform: row.transform || null,
    canNull: row.canNull,
    sendDbValue: row.sendDbValue,
    status: row.status,
    note: row.note?.trim() || null
  }
}

async function onSave(): Promise<void> {
  if (!detail.value) {
    return
  }
  saving.value = true
  try {
    detail.value = await itgApi.saveErpMapping(objectCode.value, {
      rules: detail.value.rules.map(toSaveReq),
      remark: detail.value.remark ?? null
    })
    dirty.value = false
    await loadSegments()
    ElMessage.success('已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function openPreview(): void {
  preview.value = null
  previewVisible.value = true
}

async function runPreview(): Promise<void> {
  previewing.value = true
  try {
    preview.value = await itgApi.previewErpMapping(objectCode.value, {
      bizId: previewBizId.value.trim(),
      viewType: objectCode.value === 'Bom' ? previewViewType.value : undefined
    })
  } catch (error) {
    preview.value = null
    ElMessage.error(error instanceof Error ? error.message : '试算失败')
  } finally {
    previewing.value = false
  }
}

onMounted(async () => {
  try {
    await loadSegments()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载映射段失败')
  }
  await load()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  margin-right: 12px;
}

.hint {
  color: #909399;
  font-size: 12px;
}

.toolbar {
  display: flex;
  gap: 8px;
}

.seg-tag {
  margin-left: 6px;
}

.stats {
  display: flex;
  gap: 12px;
  margin: 14px 0 10px;
  flex-wrap: wrap;
}

.stat {
  min-width: 92px;
  padding: 8px 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.stat-wide {
  flex: 1;
}

.stat-label {
  display: block;
  color: #909399;
  font-size: 12px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
}

.stat-value.ok {
  color: #67c23a;
}

.stat-value.warn {
  color: #e6a23c;
}

.stat-value.danger {
  color: #f56c6c;
}

.stat-text {
  font-size: 12px;
  color: #606266;
}

.tip {
  margin-bottom: 10px;
}

.cell-main {
  font-size: 12px;
  color: #303133;
}

.cell-sub {
  font-size: 12px;
  color: #909399;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.preview-body {
  display: flex;
  gap: 16px;
}

.preview-col {
  flex: 1;
  min-width: 0;
}

.preview-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.payload {
  margin: 0 0 8px;
  padding: 10px;
  max-height: 380px;
  overflow: auto;
  background: #1f2430;
  color: #d7dae0;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
}
</style>
