<template>
  <div class="page">
    <el-card shadow="never" class="block">
      <template #header>
        <div class="card-header">
          <span>定额规则</span>
          <span class="hint">
            匹配优先级：工作中心+工序类型 &gt; 工作中心 &gt; 工序类型 &gt; 全局默认；同档按「优先级」数字小的先命中
          </span>
        </div>
      </template>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="规则编码 / 名称 / 工序类型 / 工作中心"
          clearable
          style="width: 280px"
          @keyup.enter="loadRules"
          @clear="loadRules"
        />
        <el-select v-model="enabledFilter" placeholder="全部" clearable style="width: 130px" @change="loadRules">
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
        <el-button type="primary" @click="loadRules">查询</el-button>
        <el-button v-if="canWrite" type="primary" plain @click="openCreateRule">新建规则</el-button>
      </div>

      <el-table v-loading="rulesLoading" :data="rules" border stripe size="small">
        <el-table-column prop="code" label="规则编码" width="120" />
        <el-table-column prop="name" label="规则名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="工序类型" width="110" align="center">
          <template #default="{ row }">
            {{ row.opType ? OP_TYPE_LABEL[row.opType] ?? row.opType : '全部' }}
          </template>
        </el-table-column>
        <el-table-column label="工作中心" width="130">
          <template #default="{ row }">{{ row.workCenter || '全部' }}</template>
        </el-table-column>
        <el-table-column prop="setupBaseMinutes" label="准备基准(h)" width="110" align="right" />
        <el-table-column prop="setupFactor" label="准备系数" width="100" align="right" />
        <el-table-column prop="runFactor" label="单件系数" width="100" align="right" />
        <el-table-column prop="batchQty" label="建议批量" width="100" align="right" />
        <el-table-column prop="priority" label="优先级" width="90" align="right" />
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canWrite" label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditRule(row)">编辑</el-button>
            <el-button link type="danger" @click="onDeleteRule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="block">
      <template #header>
        <span>按工艺路线试算</span>
      </template>

      <div class="toolbar">
        <el-select
          v-model="routingId"
          filterable
          remote
          reserve-keyword
          clearable
          placeholder="输入路线编码 / 零件搜索"
          :remote-method="searchRoutings"
          :loading="routingsLoading"
          style="width: 380px"
        >
          <el-option
            v-for="item in routings"
            :key="item.id"
            :label="`${item.code} ${item.name}（${item.objectCode}）`"
            :value="item.id"
          />
        </el-select>
        <span class="inline-label">批量</span>
        <el-input-number v-model="batchQty" :min="1" :max="99999" :controls="false" style="width: 110px" />
        <el-button type="primary" :loading="calcLoading" :disabled="!routingId" @click="onCalc">试算</el-button>
        <span class="hint">定额 = 基准 + 原工时 × 系数；不落库，规则或路线一改重算即生效</span>
      </div>

      <template v-if="result">
        <div class="summary">
          <div class="summary-item">
            <span class="summary-label">单件工时合计(h)</span>
            <span class="summary-value">{{ formatNumber(result.singlePieceMinutes) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">准备工时合计(h)</span>
            <span class="summary-value">{{ formatNumber(result.totalSetupEffective) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">批量工时合计(h)</span>
            <span class="summary-value highlight">{{ formatNumber(result.batchMinutes) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">规则命中</span>
            <span class="summary-value">
              {{ result.matchedSteps }} / {{ result.steps.length }}
              <el-tag v-if="result.unmatchedSteps" type="warning" size="small" class="tag">
                {{ result.unmatchedSteps }} 道按原值
              </el-tag>
            </span>
          </div>
        </div>

        <el-table :data="result.steps" border stripe size="small">
          <el-table-column prop="seqNo" label="工序号" width="90" align="center" />
          <el-table-column prop="name" label="工序名称" min-width="130" />
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">{{ OP_TYPE_LABEL[row.opType] ?? row.opType }}</template>
          </el-table-column>
          <el-table-column label="工作中心" width="120">
            <template #default="{ row }">{{ row.workCenter || '-' }}</template>
          </el-table-column>
          <el-table-column label="准备工时 原→定额" width="170" align="right">
            <template #default="{ row }">
              {{ formatNumber(row.rawSetup) }} → <b>{{ formatNumber(row.setupEffective) }}</b>
            </template>
          </el-table-column>
          <el-table-column label="单件工时 原→定额" width="170" align="right">
            <template #default="{ row }">
              {{ formatNumber(row.rawRun) }} → <b>{{ formatNumber(row.runEffective) }}</b>
            </template>
          </el-table-column>
          <el-table-column label="命中规则" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.ruleCode">{{ row.ruleCode }} {{ row.ruleName }}</span>
              <span v-else class="sub">未命中（按原值）</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else description="选择一条工艺路线后点「试算」" />
    </el-card>

    <el-dialog v-model="ruleDialogVisible" :title="ruleDialogTitle" width="600px">
      <el-form label-width="110px">
        <el-form-item label="规则名称" required>
          <el-input v-model="ruleForm.name" maxlength="200" />
        </el-form-item>
        <el-form-item label="工序类型">
          <el-select v-model="ruleForm.opType" clearable placeholder="留空 = 全部类型" style="width: 100%">
            <el-option v-for="(label, value) in OP_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作中心">
          <el-input v-model="ruleForm.workCenter" maxlength="100" placeholder="留空 = 全部工作中心" />
        </el-form-item>
        <el-form-item label="准备工时基准">
          <el-input-number v-model="ruleForm.setupBaseMinutes" :min="0" :precision="2" :step="0.1" :controls="false" style="width: 160px" />
          <span class="unit">小时 / 每批</span>
        </el-form-item>
        <el-form-item label="准备工时系数">
          <el-input-number v-model="ruleForm.setupFactor" :min="0.01" :precision="2" :step="0.1" :controls="false" style="width: 160px" />
          <span class="unit">准备工时 × 系数</span>
        </el-form-item>
        <el-form-item label="单件工时系数">
          <el-input-number v-model="ruleForm.runFactor" :min="0.01" :precision="2" :step="0.1" :controls="false" style="width: 160px" />
          <span class="unit">单件工时 × 系数</span>
        </el-form-item>
        <el-form-item label="建议批量">
          <el-input-number v-model="ruleForm.batchQty" :min="1" :max="99999" :controls="false" style="width: 160px" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="ruleForm.priority" :min="0" :max="9999" :controls="false" style="width: 160px" />
          <span class="unit">数字小的先命中（同档位内）</span>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="ruleForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleSaving" @click="onSaveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  OP_TYPE_LABEL,
  cappApi,
  timeApi,
  type RoutingRow,
  type TimeCalcResult,
  type TimeRuleRow
} from '@/api/capp'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('capp.time.write'))

const rules = ref<TimeRuleRow[]>([])
const rulesLoading = ref(false)
const keyword = ref('')
const enabledFilter = ref<boolean | undefined>(undefined)

const routings = ref<RoutingRow[]>([])
const routingsLoading = ref(false)
const routingId = ref<string>('')
const batchQty = ref(1)
const calcLoading = ref(false)
const result = ref<TimeCalcResult | null>(null)

const ruleDialogVisible = ref(false)
const ruleSaving = ref(false)
const ruleForm = reactive<{
  id?: string
  name: string
  opType: string | null
  workCenter: string
  setupBaseMinutes: number
  setupFactor: number
  runFactor: number
  batchQty: number
  priority: number
  enabled: boolean
  remark: string
}>({
  name: '',
  opType: null,
  workCenter: '',
  setupBaseMinutes: 0,
  setupFactor: 1,
  runFactor: 1,
  batchQty: 1,
  priority: 100,
  enabled: true,
  remark: ''
})

const ruleDialogTitle = computed(() => (ruleForm.id ? '编辑定额规则' : '新建定额规则'))

onMounted(async () => {
  await Promise.all([loadRules(), searchRoutings('')])
})

async function loadRules(): Promise<void> {
  rulesLoading.value = true
  try {
    rules.value = await timeApi.list({
      keyword: keyword.value || undefined,
      enabled: enabledFilter.value
    })
    if (result.value) {
      await onCalc()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载定额规则失败')
  } finally {
    rulesLoading.value = false
  }
}

async function searchRoutings(keywordText: string): Promise<void> {
  routingsLoading.value = true
  try {
    const page = await cappApi.list({ page: 1, size: 50, keyword: keywordText || undefined })
    routings.value = page.items
  } catch {
    routings.value = []
  } finally {
    routingsLoading.value = false
  }
}

async function onCalc(): Promise<void> {
  if (!routingId.value) {
    ElMessage.warning('请先选择工艺路线')
    return
  }
  calcLoading.value = true
  try {
    result.value = await timeApi.calc(routingId.value, batchQty.value || undefined)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '试算失败')
  } finally {
    calcLoading.value = false
  }
}

function openCreateRule(): void {
  Object.assign(ruleForm, {
    id: undefined,
    name: '',
    opType: null,
    workCenter: '',
    setupBaseMinutes: 0,
    setupFactor: 1,
    runFactor: 1,
    batchQty: 1,
    priority: 100,
    enabled: true,
    remark: ''
  })
  ruleDialogVisible.value = true
}

function openEditRule(row: TimeRuleRow): void {
  Object.assign(ruleForm, {
    id: row.id,
    name: row.name,
    opType: row.opType ?? null,
    workCenter: row.workCenter ?? '',
    setupBaseMinutes: Number(row.setupBaseMinutes ?? 0),
    setupFactor: Number(row.setupFactor ?? 1),
    runFactor: Number(row.runFactor ?? 1),
    batchQty: row.batchQty ?? 1,
    priority: row.priority ?? 100,
    enabled: row.enabled,
    remark: row.remark ?? ''
  })
  ruleDialogVisible.value = true
}

async function onSaveRule(): Promise<void> {
  if (!ruleForm.name.trim()) {
    ElMessage.warning('请填写规则名称')
    return
  }
  ruleSaving.value = true
  try {
    const payload = {
      name: ruleForm.name.trim(),
      opType: ruleForm.opType || null,
      workCenter: ruleForm.workCenter.trim() || null,
      setupBaseMinutes: ruleForm.setupBaseMinutes ?? 0,
      setupFactor: ruleForm.setupFactor ?? 1,
      runFactor: ruleForm.runFactor ?? 1,
      batchQty: ruleForm.batchQty ?? 1,
      priority: ruleForm.priority ?? 100,
      enabled: ruleForm.enabled,
      remark: ruleForm.remark || null
    }
    if (ruleForm.id) {
      await timeApi.update(ruleForm.id, payload)
      ElMessage.success('已保存')
    } else {
      const created = await timeApi.create(payload)
      ElMessage.success(`已创建 ${created.code}`)
    }
    ruleDialogVisible.value = false
    await loadRules()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    ruleSaving.value = false
  }
}

async function onDeleteRule(row: TimeRuleRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除定额规则 ${row.code}？`, '删除规则', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await timeApi.remove(row.id)
    ElMessage.success('已删除')
    await loadRules()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function formatNumber(value?: number | null): string {
  return value == null ? '-' : Number(value).toFixed(2)
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

.inline-label {
  color: #606266;
  font-size: 13px;
}

.summary {
  display: flex;
  gap: 32px;
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
  font-size: 18px;
  font-weight: 600;
}

.summary-value.highlight {
  color: #409eff;
}

.tag {
  margin-left: 6px;
}

.sub {
  color: #909399;
  font-size: 12px;
}

.unit {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
