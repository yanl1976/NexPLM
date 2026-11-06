<template>
  <div class="page">
    <el-card shadow="never" class="block">
      <template #header>
        <div class="card-header">
          <span>齐套性检查</span>
          <span class="hint">只有「没有路线 / 没有工序」算不通过；工时未填、资源未绑、卡片未生成只是提醒</span>
        </div>
      </template>

      <div class="toolbar">
        <el-select
          v-model="checkObjectId"
          filterable
          remote
          reserve-keyword
          clearable
          placeholder="选择零件（编码 / 名称）"
          :remote-method="searchItems"
          :loading="itemsLoading"
          style="width: 340px"
        >
          <el-option
            v-for="item in items"
            :key="item.id"
            :label="`${item.code} ${item.name}`"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" :loading="checking" :disabled="!checkObjectId" @click="onCheck">检查</el-button>
      </div>

      <template v-if="checkResult">
        <div class="summary">
          <div class="summary-item">
            <span class="summary-label">结论</span>
            <el-tag :type="checkResult.ready ? 'success' : 'danger'" size="large">
              {{ checkResult.ready ? '齐套（可投产）' : '不齐套' }}
            </el-tag>
          </div>
          <div class="summary-item">
            <span class="summary-label">通过 / 提醒 / 不通过</span>
            <span class="summary-value">
              {{ checkResult.passCount }} / {{ checkResult.warnCount }} / {{ checkResult.failCount }}
            </span>
          </div>
          <div class="summary-item">
            <span class="summary-label">路线 / 工序 / 工步 / 卡片</span>
            <span class="summary-value">
              {{ checkResult.routeCount }} / {{ checkResult.stepCount }} /
              {{ checkResult.substepCount }} / {{ checkResult.cardCount }}
            </span>
          </div>
        </div>

        <el-table :data="checkResult.checks" border size="small">
          <el-table-column prop="label" label="检查项" width="160" />
          <el-table-column label="结果" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="detail" label="说明" min-width="280" />
        </el-table>
      </template>
      <el-empty v-else description="选择零件后点「检查」" />
    </el-card>

    <el-card shadow="never" class="block">
      <template #header>
        <div class="card-header">
          <span>工艺成册</span>
          <span class="hint">册内卡片必须属于同一零件；卡片顺序即页序</span>
        </div>
      </template>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="册编码 / 名称"
          clearable
          style="width: 240px"
          @keyup.enter="loadVolumes"
          @clear="loadVolumes"
        />
        <el-button type="primary" @click="loadVolumes">查询</el-button>
        <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建成册</el-button>
      </div>

      <el-table v-loading="loading" :data="volumes" border stripe>
        <el-table-column prop="code" label="册编码" width="130" />
        <el-table-column prop="name" label="册名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="零件" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.objectCode }}</span>
            <span class="sub">{{ row.objectName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cardCount" label="卡片数" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'RELEASED' ? 'success' : 'warning'" size="small">
              {{ row.status === 'RELEASED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button v-if="canWrite && row.status === 'DRAFT'" link type="primary" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="canWrite && row.status === 'DRAFT'"
              link
              type="success"
              @click="onRelease(row)"
            >
              发布
            </el-button>
            <el-button link type="primary" @click="onExport(row)">导出 PDF</el-button>
            <el-button v-if="canWrite && row.status === 'DRAFT'" link type="danger" @click="onDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form label-width="90px">
        <el-form-item label="册名称" required>
          <el-input v-model="form.name" maxlength="200" placeholder="如：XX 零件机加工工艺卡片册" />
        </el-form-item>
        <el-form-item label="所属零件" required>
          <el-select
            v-model="form.objectId"
            filterable
            remote
            reserve-keyword
            placeholder="编码 / 名称搜索"
            :remote-method="searchItems"
            :loading="itemsLoading"
            style="width: 100%"
            @change="onObjectChange"
          >
            <el-option
              v-for="item in items"
              :key="item.id"
              :label="`${item.code} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="册内卡片">
          <el-select
            v-model="form.cardIds"
            multiple
            filterable
            :loading="cardsLoading"
            :disabled="!form.objectId"
            :placeholder="form.objectId ? '选择该零件下的工艺卡片（选择顺序即页序）' : '请先选择零件'"
            style="width: 100%"
          >
            <el-option
              v-for="card in objectCards"
              :key="card.id"
              :label="`${card.code} ${card.title ?? ''}（${card.templateName}）`"
              :value="card.id"
            />
          </el-select>
          <div v-if="form.cardIds.length" class="picked">
            页序：
            <el-tag v-for="(id, index) in form.cardIds" :key="id" class="picked-tag" size="small">
              {{ index * 10 + 10 }}. {{ cardLabel(id) }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" :title="`成册详情 - ${detailData?.code ?? ''}`" width="900px">
      <template v-if="detailData">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="册名称">{{ detailData.name }}</el-descriptions-item>
          <el-descriptions-item label="零件">
            {{ detailData.objectCode }} {{ detailData.objectName }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ detailData.status === 'RELEASED' ? '已发布' : '草稿' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-table :data="detailData.cards" border size="small" class="detail-table">
          <el-table-column prop="seqNo" label="页序" width="80" align="center" />
          <el-table-column prop="cardCode" label="卡片编码" width="130" />
          <el-table-column prop="cardTitle" label="卡片标题" min-width="180" show-overflow-tooltip />
          <el-table-column label="模板" min-width="170" show-overflow-tooltip>
            <template #default="{ row }">{{ row.templateName }} v{{ row.templateVersion }}</template>
          </el-table-column>
          <el-table-column prop="routingCode" label="工艺路线" width="130" />
          <el-table-column prop="rowCount" label="工序行" width="90" align="center" />
        </el-table>
        <div v-if="!detailData.cards.length" class="sub">册内还没有卡片</div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailData" type="primary" @click="onExport(detailData)">导出 PDF</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { itemApi, type ItemSummary } from '@/api/item'
import {
  cardApi,
  volumeApi,
  type CappCardRow,
  type CheckStatus,
  type CompletenessResult,
  type VolumeDetail,
  type VolumeRow
} from '@/api/capp'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('capp.card.write'))

// ---- 齐套检查
const checkObjectId = ref<string>('')
const checking = ref(false)
const checkResult = ref<CompletenessResult | null>(null)

const items = ref<ItemSummary[]>([])
const itemsLoading = ref(false)

// ---- 成册列表
const volumes = ref<VolumeRow[]>([])
const loading = ref(false)
const keyword = ref('')

// ---- 新建/编辑
const dialogVisible = ref(false)
const saving = ref(false)
const objectCards = ref<CappCardRow[]>([])
const cardsLoading = ref(false)
const form = reactive<{ id?: string; name: string; objectId: string; cardIds: string[]; remark: string }>({
  name: '',
  objectId: '',
  cardIds: [],
  remark: ''
})

// ---- 详情
const detailVisible = ref(false)
const detailData = ref<VolumeDetail | null>(null)

const dialogTitle = computed(() => (form.id ? '编辑工艺成册' : '新建工艺成册'))

onMounted(async () => {
  await Promise.all([loadVolumes(), searchItems('')])
})

async function searchItems(keywordText: string): Promise<void> {
  itemsLoading.value = true
  try {
    const result = await itemApi.list({ page: 1, size: 30, keyword: keywordText || undefined })
    items.value = result.items
  } catch {
    items.value = []
  } finally {
    itemsLoading.value = false
  }
}

async function onCheck(): Promise<void> {
  if (!checkObjectId.value) {
    return
  }
  checking.value = true
  try {
    checkResult.value = await volumeApi.completeness(checkObjectId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '齐套检查失败')
  } finally {
    checking.value = false
  }
}

async function loadVolumes(): Promise<void> {
  loading.value = true
  try {
    volumes.value = await volumeApi.list({ keyword: keyword.value || undefined })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载成册失败')
  } finally {
    loading.value = false
  }
}

async function loadObjectCards(objectId: string): Promise<void> {
  if (!objectId) {
    objectCards.value = []
    return
  }
  cardsLoading.value = true
  try {
    const result = await cardApi.list({ page: 1, size: 200, objectId })
    objectCards.value = result.items
  } catch {
    objectCards.value = []
  } finally {
    cardsLoading.value = false
  }
}

async function onObjectChange(): Promise<void> {
  form.cardIds = []
  await loadObjectCards(form.objectId)
}

function openCreate(): void {
  Object.assign(form, { id: undefined, name: '', objectId: '', cardIds: [], remark: '' })
  objectCards.value = []
  dialogVisible.value = true
}

async function openEdit(row: VolumeRow): Promise<void> {
  try {
    const detail = await volumeApi.detail(row.id)
    Object.assign(form, {
      id: detail.id,
      name: detail.name,
      objectId: detail.objectId,
      cardIds: detail.cards.map((card) => card.cardId),
      remark: detail.remark ?? ''
    })
    await loadObjectCards(detail.objectId)
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载成册失败')
  }
}

async function openDetail(row: VolumeRow): Promise<void> {
  try {
    detailData.value = await volumeApi.detail(row.id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载成册失败')
  }
}

function cardLabel(cardId: string): string {
  const card = objectCards.value.find((item) => item.id === cardId)
  return card ? card.code : cardId
}

async function onSave(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请填写册名称')
    return
  }
  if (!form.objectId) {
    ElMessage.warning('请选择所属零件')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      objectId: form.objectId,
      cardIds: form.cardIds,
      remark: form.remark || null
    }
    if (form.id) {
      await volumeApi.update(form.id, payload)
      ElMessage.success('已保存')
    } else {
      const created = await volumeApi.create(payload)
      ElMessage.success(`已创建 ${created.code}`)
    }
    dialogVisible.value = false
    await loadVolumes()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onRelease(row: VolumeRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`发布后成册不可再修改。确认发布 ${row.code}？`, '发布成册', {
      confirmButtonText: '发布',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await volumeApi.release(row.id)
    ElMessage.success('已发布')
    await loadVolumes()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败')
  }
}

async function onExport(row: { id: string }): Promise<void> {
  try {
    await volumeApi.exportPdf(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  }
}

async function onDelete(row: VolumeRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除成册 ${row.code}？`, '删除成册', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await volumeApi.remove(row.id)
    ElMessage.success('已删除')
    await loadVolumes()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function statusTagType(status: CheckStatus): 'success' | 'warning' | 'danger' {
  if (status === 'PASS') {
    return 'success'
  }
  return status === 'FAIL' ? 'danger' : 'warning'
}

function statusLabel(status: CheckStatus): string {
  return status === 'PASS' ? '通过' : status === 'FAIL' ? '不通过' : '提醒'
}

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.block {
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.hint {
  color: #909399;
  font-size: 12px;
}

.summary {
  display: flex;
  gap: 40px;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 4px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  color: #909399;
  font-size: 12px;
}

.summary-value {
  font-size: 16px;
  font-weight: 600;
}

.mono {
  margin-right: 6px;
  font-family: Consolas, monospace;
}

.sub {
  color: #909399;
  font-size: 12px;
}

.picked {
  margin-top: 8px;
  color: #606266;
  font-size: 12px;
  line-height: 24px;
}

.picked-tag {
  margin-right: 6px;
}

.detail-table {
  margin-top: 12px;
}
</style>
