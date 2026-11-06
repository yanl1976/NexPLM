<template>
  <div class="page">
    <el-tabs v-model="activeTab">
      <!-- ------------------------------------------------ 控制计划 -->
      <el-tab-pane label="控制计划" name="plan">
        <div class="toolbar">
          <el-input
            v-model="planKeyword"
            placeholder="计划编码 / 名称"
            clearable
            style="width: 240px"
            @keyup.enter="loadPlans"
            @clear="loadPlans"
          />
          <el-select v-model="planStatus" placeholder="全部状态" clearable style="width: 140px" @change="loadPlans">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="RELEASED" />
          </el-select>
          <el-button type="primary" @click="loadPlans">查询</el-button>
          <el-button v-if="canWrite" type="primary" plain @click="openCreatePlan">新建控制计划</el-button>
          <span class="hint">行绑工序号；发布前每行都要填受控特性</span>
        </div>

        <el-table v-loading="plansLoading" :data="plans" border stripe>
          <el-table-column prop="code" label="计划编码" width="130" />
          <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="零件" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="mono">{{ row.objectCode }}</span><span class="sub">{{ row.objectName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="routingCode" label="工艺路线" width="140" />
          <el-table-column prop="rowCount" label="行数" width="80" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'RELEASED' ? 'success' : 'warning'" size="small">
                {{ row.status === 'RELEASED' ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="290" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPlan(row, false)">查看</el-button>
              <el-button v-if="canWrite && row.status === 'DRAFT'" link type="primary" @click="openPlan(row, true)">
                编辑
              </el-button>
              <el-button v-if="canWrite && row.status === 'DRAFT'" link type="success" @click="onReleasePlan(row)">
                发布
              </el-button>
              <el-button link type="primary" @click="onExportPlan(row)">导出 Excel</el-button>
              <el-button v-if="canWrite && row.status === 'DRAFT'" link type="danger" @click="onDeletePlan(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ------------------------------------------------ 检验项字典 -->
      <el-tab-pane label="检验项字典" name="item">
        <div class="toolbar">
          <el-input
            v-model="itemKeyword"
            placeholder="编码 / 名称 / 规格"
            clearable
            style="width: 240px"
            @keyup.enter="loadItems"
            @clear="loadItems"
          />
          <el-select v-model="itemCategory" placeholder="全部类别" clearable style="width: 140px" @change="loadItems">
            <el-option v-for="(label, value) in QUALITY_CATEGORY_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <el-button type="primary" @click="loadItems">查询</el-button>
          <el-button v-if="canWrite" type="primary" plain @click="openItemDialog()">新增检验项</el-button>
          <span class="hint">字典只用于带出默认值；改动不会影响已发布的控制计划</span>
        </div>

        <el-table v-loading="itemsLoading" :data="qualityItems" border stripe>
          <el-table-column prop="code" label="编码" width="130" />
          <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="类别" width="100" align="center">
            <template #default="{ row }">
              {{ QUALITY_CATEGORY_LABEL[row.category as QualityCategory] ?? row.category }}
            </template>
          </el-table-column>
          <el-table-column prop="spec" label="规格/公差" min-width="160" show-overflow-tooltip />
          <el-table-column prop="method" label="检验方法" min-width="160" show-overflow-tooltip />
          <el-table-column prop="gauge" label="量具" width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button v-if="canWrite" link type="primary" @click="openItemDialog(row)">编辑</el-button>
              <el-button v-if="canWrite" link type="danger" @click="onDeleteItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- ------------------------------------------------ 控制计划编辑 -->
    <el-dialog v-model="planDialogVisible" :title="planDialogTitle" width="1200px" top="4vh" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="计划名称" required>
          <el-input v-model="planForm.name" maxlength="200" :disabled="planReadonly" />
        </el-form-item>
        <el-form-item label="工艺路线" required>
          <el-select
            v-model="planForm.routingId"
            filterable
            remote
            reserve-keyword
            placeholder="编码 / 零件搜索"
            :remote-method="searchRoutings"
            :loading="routingsLoading"
            :disabled="planReadonly"
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
        <el-form-item label="备注">
          <el-input v-model="planForm.remark" type="textarea" :rows="2" maxlength="500" :disabled="planReadonly" />
        </el-form-item>
      </el-form>

      <div class="section-title">
        控制计划行（{{ planForm.rows.length }} 行）
        <el-button size="small" type="primary" plain :disabled="planReadonly" @click="addPlanRow">添加行</el-button>
      </div>

      <el-table :data="planForm.rows" border size="small" max-height="460">
        <el-table-column label="行号" width="80">
          <template #default="{ row }">
            <el-input-number v-model="row.seqNo" :min="1" :step="10" :controls="false" :disabled="planReadonly" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="工序号" width="80">
          <template #default="{ row }">
            <el-input-number v-model="row.stepSeqNo" :min="1" :controls="false" :disabled="planReadonly" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="工序名称" width="120">
          <template #default="{ row }">
            <el-input v-model="row.stepName" maxlength="100" :disabled="planReadonly" />
          </template>
        </el-table-column>
        <el-table-column label="检验项" width="170">
          <template #default="{ row }">
            <el-select
              v-model="row.itemId"
              filterable
              clearable
              :disabled="planReadonly"
              placeholder="从字典带出"
              style="width: 100%"
              @change="(value: string) => applyItemDefaults(row, value)"
            >
              <el-option
                v-for="item in qualityItemsForPick"
                :key="item.id"
                :label="`${item.code} ${item.name}`"
                :value="item.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="受控特性" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.characteristic" maxlength="200" :disabled="planReadonly" />
          </template>
        </el-table-column>
        <el-table-column label="规格/公差" width="140">
          <template #default="{ row }">
            <el-input v-model="row.spec" maxlength="200" :disabled="planReadonly" />
          </template>
        </el-table-column>
        <el-table-column label="测量技术" width="130">
          <template #default="{ row }">
            <el-input v-model="row.method" maxlength="200" :disabled="planReadonly" />
          </template>
        </el-table-column>
        <el-table-column label="量具" width="120">
          <template #default="{ row }">
            <el-input v-model="row.gauge" maxlength="200" :disabled="planReadonly" />
          </template>
        </el-table-column>
        <el-table-column label="样本" width="90">
          <template #default="{ row }">
            <el-input v-model="row.sampleSize" maxlength="64" :disabled="planReadonly" />
          </template>
        </el-table-column>
        <el-table-column label="频率" width="90">
          <template #default="{ row }">
            <el-input v-model="row.frequency" maxlength="64" :disabled="planReadonly" />
          </template>
        </el-table-column>
        <el-table-column label="控制方法" width="130">
          <template #default="{ row }">
            <el-input v-model="row.controlMethod" maxlength="200" :disabled="planReadonly" />
          </template>
        </el-table-column>
        <el-table-column label="反应计划" width="130">
          <template #default="{ row }">
            <el-input v-model="row.reactionPlan" maxlength="200" :disabled="planReadonly" />
          </template>
        </el-table-column>
        <el-table-column label="关键" width="70" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.critical" :disabled="planReadonly" />
          </template>
        </el-table-column>
        <el-table-column v-if="!planReadonly" label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="planForm.rows.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="planDialogVisible = false">关闭</el-button>
        <el-button v-if="!planReadonly" type="primary" :loading="planSaving" @click="onSavePlan">保存</el-button>
      </template>
    </el-dialog>

    <!-- ------------------------------------------------ 检验项编辑 -->
    <el-dialog v-model="itemDialogVisible" :title="itemForm.id ? '编辑检验项' : '新增检验项'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="itemForm.name" maxlength="200" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="itemForm.category" style="width: 100%">
            <el-option v-for="(label, value) in QUALITY_CATEGORY_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格/公差">
          <el-input v-model="itemForm.spec" maxlength="200" />
        </el-form-item>
        <el-form-item label="检验方法">
          <el-input v-model="itemForm.method" maxlength="200" />
        </el-form-item>
        <el-form-item label="量具">
          <el-input v-model="itemForm.gauge" maxlength="200" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="itemForm.unit" maxlength="32" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSaving" @click="onSaveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  QUALITY_CATEGORY_LABEL,
  cappApi,
  qualityApi,
  type QualityCategory,
  type QualityItemRow,
  type QualityPlanRow,
  type QualityPlanRowView,
  type RoutingRow
} from '@/api/capp'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('capp.quality.write'))

const activeTab = ref<'plan' | 'item'>('plan')

// ---- 控制计划
const plans = ref<QualityPlanRowView[]>([])
const plansLoading = ref(false)
const planKeyword = ref('')
const planStatus = ref('')

const planDialogVisible = ref(false)
const planReadonly = ref(false)
const planSaving = ref(false)
const routings = ref<RoutingRow[]>([])
const routingsLoading = ref(false)
const planForm = reactive<{
  id?: string
  name: string
  routingId: string
  remark: string
  rows: QualityPlanRow[]
}>({ name: '', routingId: '', remark: '', rows: [] })

// ---- 检验项
const qualityItems = ref<QualityItemRow[]>([])
const itemsLoading = ref(false)
const itemKeyword = ref('')
const itemCategory = ref('')
const itemDialogVisible = ref(false)
const itemSaving = ref(false)
const itemForm = reactive<{
  id?: string
  name: string
  category: string
  spec: string
  method: string
  gauge: string
  unit: string
  remark: string
}>({ name: '', category: 'DIMENSION', spec: '', method: '', gauge: '', unit: '', remark: '' })

/** 字典候选项（行编辑用；与列表共用同一份数据）。 */
const qualityItemsForPick = computed(() => qualityItems.value)

const planDialogTitle = computed(() => {
  if (planReadonly.value) {
    return '控制计划详情'
  }
  return planForm.id ? '编辑控制计划（草稿）' : '新建控制计划'
})

onMounted(async () => {
  await Promise.all([loadPlans(), loadItems()])
})

async function loadPlans(): Promise<void> {
  plansLoading.value = true
  try {
    plans.value = await qualityApi.planList({
      keyword: planKeyword.value || undefined,
      status: planStatus.value || undefined
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载控制计划失败')
  } finally {
    plansLoading.value = false
  }
}

async function loadItems(): Promise<void> {
  itemsLoading.value = true
  try {
    qualityItems.value = await qualityApi.itemList({
      keyword: itemKeyword.value || undefined,
      category: itemCategory.value || undefined
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载检验项失败')
  } finally {
    itemsLoading.value = false
  }
}

async function searchRoutings(keywordText: string): Promise<void> {
  routingsLoading.value = true
  try {
    const result = await cappApi.list({ page: 1, size: 50, keyword: keywordText || undefined })
    routings.value = result.items
  } catch {
    routings.value = []
  } finally {
    routingsLoading.value = false
  }
}

function openCreatePlan(): void {
  planReadonly.value = false
  Object.assign(planForm, { id: undefined, name: '', routingId: '', remark: '', rows: [] })
  planDialogVisible.value = true
  searchRoutings('')
  if (!qualityItems.value.length) {
    loadItems()
  }
}

async function openPlan(row: QualityPlanRowView, editable: boolean): Promise<void> {
  try {
    const detail = await qualityApi.plan(row.id)
    planReadonly.value = !editable || detail.status === 'RELEASED'
    Object.assign(planForm, {
      id: detail.id,
      name: detail.name,
      routingId: detail.routingId,
      remark: detail.remark ?? '',
      rows: detail.rows.map((item) => ({ ...item }))
    })
    planDialogVisible.value = true
    if (!qualityItems.value.length) {
      await loadItems()
    }
    // 保证下拉能回显已有路线
    if (!routings.value.some((item) => item.id === detail.routingId)) {
      routings.value = [
        {
          id: detail.routingId,
          code: detail.routingCode ?? '',
          name: '',
          objectCode: detail.objectCode ?? '',
          status: 'RELEASED',
          versionNo: 1,
          stepCount: 0,
          routeString: ''
        } as unknown as RoutingRow,
        ...routings.value
      ]
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载控制计划失败')
  }
}

function addPlanRow(): void {
  const maxSeq = planForm.rows.reduce((max, row) => Math.max(max, row.seqNo || 0), 0)
  planForm.rows.push({
    seqNo: maxSeq + 10,
    stepSeqNo: null,
    stepName: '',
    itemId: null,
    characteristic: '',
    spec: '',
    method: '',
    gauge: '',
    sampleSize: '',
    frequency: '',
    controlMethod: '',
    reactionPlan: '',
    critical: false,
    remark: ''
  })
}

/** 选字典项时把默认值填进尚未手填的字段（不覆盖已有值）。 */
function applyItemDefaults(row: QualityPlanRow, itemId: string): void {
  const item = qualityItems.value.find((entry) => entry.id === itemId)
  if (!item) {
    return
  }
  if (!row.characteristic) {
    row.characteristic = item.name
  }
  if (!row.spec && item.spec) {
    row.spec = item.spec
  }
  if (!row.method && item.method) {
    row.method = item.method
  }
  if (!row.gauge && item.gauge) {
    row.gauge = item.gauge
  }
}

async function onSavePlan(): Promise<void> {
  if (!planForm.name.trim()) {
    ElMessage.warning('请填写计划名称')
    return
  }
  if (!planForm.routingId) {
    ElMessage.warning('请选择工艺路线')
    return
  }
  planSaving.value = true
  try {
    const payload = {
      name: planForm.name.trim(),
      routingId: planForm.routingId,
      remark: planForm.remark || null,
      rows: planForm.rows.map((row) => ({
        seqNo: row.seqNo,
        stepSeqNo: row.stepSeqNo ?? null,
        substepSeqNo: row.substepSeqNo ?? null,
        stepName: row.stepName || null,
        itemId: row.itemId || null,
        characteristic: row.characteristic || null,
        spec: row.spec || null,
        method: row.method || null,
        gauge: row.gauge || null,
        sampleSize: row.sampleSize || null,
        frequency: row.frequency || null,
        controlMethod: row.controlMethod || null,
        reactionPlan: row.reactionPlan || null,
        critical: !!row.critical,
        remark: row.remark || null
      }))
    }
    if (planForm.id) {
      await qualityApi.updatePlan(planForm.id, payload)
      ElMessage.success('已保存')
    } else {
      const created = await qualityApi.createPlan(payload)
      ElMessage.success(`已创建 ${created.code}（${created.rows.length} 行）`)
    }
    planDialogVisible.value = false
    await loadPlans()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    planSaving.value = false
  }
}

async function onReleasePlan(row: QualityPlanRowView): Promise<void> {
  try {
    await ElMessageBox.confirm(`发布后不可修改。确认发布 ${row.code}？`, '发布控制计划', {
      confirmButtonText: '发布',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await qualityApi.releasePlan(row.id)
    ElMessage.success('已发布')
    await loadPlans()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败')
  }
}

async function onExportPlan(row: QualityPlanRowView): Promise<void> {
  try {
    await qualityApi.exportPlan(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  }
}

async function onDeletePlan(row: QualityPlanRowView): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除控制计划 ${row.code}？`, '删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await qualityApi.removePlan(row.id)
    ElMessage.success('已删除')
    await loadPlans()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

// ---------------------------------------------------------------- 检验项

function openItemDialog(row?: QualityItemRow): void {
  Object.assign(itemForm, {
    id: row?.id,
    name: row?.name ?? '',
    category: row?.category ?? 'DIMENSION',
    spec: row?.spec ?? '',
    method: row?.method ?? '',
    gauge: row?.gauge ?? '',
    unit: row?.unit ?? '',
    remark: row?.remark ?? ''
  })
  itemDialogVisible.value = true
}

async function onSaveItem(): Promise<void> {
  if (!itemForm.name.trim()) {
    ElMessage.warning('请填写检验项名称')
    return
  }
  itemSaving.value = true
  try {
    const payload = {
      name: itemForm.name.trim(),
      category: itemForm.category,
      spec: itemForm.spec || null,
      method: itemForm.method || null,
      gauge: itemForm.gauge || null,
      unit: itemForm.unit || null,
      remark: itemForm.remark || null
    }
    if (itemForm.id) {
      await qualityApi.updateItem(itemForm.id, payload)
    } else {
      await qualityApi.createItem(payload)
    }
    ElMessage.success('已保存')
    itemDialogVisible.value = false
    await loadItems()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    itemSaving.value = false
  }
}

async function onDeleteItem(row: QualityItemRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除检验项 ${row.code}？`, '删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await qualityApi.removeItem(row.id)
    ElMessage.success('已删除')
    await loadItems()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}
</script>

<style scoped>
.page {
  padding: 16px;
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

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 14px 0 8px;
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
</style>
