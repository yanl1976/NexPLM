<script setup lang="ts">
/**
 * 工装管理。对齐旧系统 CAPP/Tooling 的三段：
 * 台账（ToolingListView）/ 申请单（ToolingApplyListView，批准后生成台账）/
 * 借用占用（WorlflowPlaceOnResourcePage，挂到工艺路线上）。
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { toolingApi } from '@/api/tooling'
import type { Row } from '@/api/tooling'
import { cappApi } from '@/api/capp'
import type { RoutingRow } from '@/api/capp'

const TYPES = [
  { value: 'FIXTURE', label: '夹具' },
  { value: 'GAUGE', label: '量具' },
  { value: 'MOLD', label: '模具' },
  { value: 'CUTTER', label: '刀具' },
  { value: 'AID', label: '辅具' }
]

const STATUS = [
  { value: 'IDLE', label: '在库' },
  { value: 'IN_USE', label: '使用中' },
  { value: 'MAINTAIN', label: '维修' },
  { value: 'SCRAP', label: '报废' }
]

const APPLY_STATUS = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'SUBMITTED', label: '已提交' },
  { value: 'APPROVED', label: '已批准' },
  { value: 'REJECTED', label: '已驳回' }
]

const tab = ref('ledger')
const loading = ref(false)
const rows = ref<Row[]>([])
const applies = ref<Row[]>([])
const borrows = ref<Row[]>([])
const keyword = ref('')
const typeFilter = ref('')
const statusFilter = ref('')

const dialogVisible = ref(false)
const editing = ref<Row | null>(null)
const form = ref<Row>({
  code: '',
  name: '',
  toolingType: 'FIXTURE',
  spec: '',
  status: 'IDLE',
  location: '',
  purchaseDate: null,
  remark: ''
})

const applyVisible = ref(false)
const applyForm = ref<Row>({
  toolingType: 'FIXTURE',
  toolingName: '',
  spec: '',
  reason: ''
})

const borrowVisible = ref(false)
const borrowForm = ref<Row>({ toolingId: '', routingId: '', remark: '' })
/** 借用弹窗里可选把工装挂到某条工艺路线上（占用），路线下拉懒加载一次。 */
const routingOptions = ref<RoutingRow[]>([])
const routingLoading = ref(false)

async function loadRoutings() {
  if (routingOptions.value.length) return
  routingLoading.value = true
  try {
    const page = await cappApi.list({ page: 1, size: 200 })
    routingOptions.value = page.items || []
  } catch {
    routingOptions.value = []
  } finally {
    routingLoading.value = false
  }
}

function labelOf(list: { value: string; label: string }[], value: string) {
  return list.find((i) => i.value === value)?.label || value || '-'
}

async function load() {
  loading.value = true
  try {
    if (tab.value === 'ledger') {
      rows.value = await toolingApi.list({
        keyword: keyword.value || undefined,
        toolingType: typeFilter.value || undefined,
        status: statusFilter.value || undefined
      })
    } else if (tab.value === 'apply') {
      applies.value = await toolingApi.applies()
    } else {
      borrows.value = await toolingApi.borrows()
    }
  } catch (error) {
    ElMessage.error((error as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = {
    code: '',
    name: '',
    toolingType: 'FIXTURE',
    spec: '',
    status: 'IDLE',
    location: '',
    purchaseDate: null,
    remark: ''
  }
  dialogVisible.value = true
}

function openEdit(row: Row) {
  editing.value = row
  form.value = {
    code: row.code,
    name: row.name,
    toolingType: row.tooling_type,
    spec: row.spec || '',
    status: row.status,
    location: row.location || '',
    purchaseDate: row.purchase_date || null,
    remark: row.remark || ''
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.name?.trim()) {
    ElMessage.warning('工装名称必填')
    return
  }
  try {
    const body = {
      code: form.value.code || undefined,
      name: form.value.name,
      toolingType: form.value.toolingType,
      spec: form.value.spec || undefined,
      status: form.value.status,
      location: form.value.location || undefined,
      purchaseDate: form.value.purchaseDate || null,
      remark: form.value.remark || undefined
    }
    if (editing.value) {
      await toolingApi.update(String(editing.value.id), body)
    } else {
      await toolingApi.create(body)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败')
  }
}

async function remove(row: Row) {
  await ElMessageBox.confirm(`删除工装「${row.code} ${row.name}」？`, '删除', { type: 'warning' })
  try {
    await toolingApi.remove(String(row.id))
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message || '删除失败')
  }
}

function openApply() {
  applyForm.value = { toolingType: 'FIXTURE', toolingName: '', spec: '', reason: '' }
  applyVisible.value = true
}

async function saveApply() {
  if (!applyForm.value.toolingName?.trim()) {
    ElMessage.warning('工装名称必填')
    return
  }
  await toolingApi.createApply({
    toolingType: applyForm.value.toolingType,
    toolingName: applyForm.value.toolingName,
    spec: applyForm.value.spec || undefined,
    reason: applyForm.value.reason || undefined
  })
  ElMessage.success('已创建草稿')
  applyVisible.value = false
  await load()
}

async function submitApply(row: Row) {
  await toolingApi.submitApply(String(row.id))
  ElMessage.success('已提交')
  await load()
}

async function approveApply(row: Row, approved: boolean) {
  const { value } = await ElMessageBox.prompt(
    approved ? '批准意见（可空）' : '驳回原因（建议填写）',
    approved ? '批准申请' : '驳回申请',
    { inputValue: '' }
  ).catch(() => ({ value: null as string | null }))
  if (value === null) return
  await toolingApi.approveApply(String(row.id), { approved, comment: value || undefined })
  ElMessage.success(approved ? '已批准，已生成工装台账' : '已驳回')
  await load()
}

function openBorrow(row: Row) {
  borrowForm.value = { toolingId: String(row.id), routingId: '', remark: '' }
  borrowVisible.value = true
  void loadRoutings()
}

async function saveBorrow() {
  await toolingApi.borrow({
    toolingId: borrowForm.value.toolingId,
    routingId: borrowForm.value.routingId ? String(borrowForm.value.routingId) : null,
    remark: borrowForm.value.remark || undefined
  })
  ElMessage.success('已借出')
  borrowVisible.value = false
  await load()
}

async function giveBack(row: Row) {
  await toolingApi.giveBack(String(row.id))
  ElMessage.success('已归还')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="tooling-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span class="title">工装管理</span>
            <span class="hint">台账 / 申请单（批准即建台账）/ 借用占用</span>
          </div>
          <div class="filters">
            <template v-if="tab === 'ledger'">
              <el-input v-model="keyword" placeholder="编码/名称" clearable style="width: 160px" @keyup.enter="load" />
              <el-select v-model="typeFilter" placeholder="全部类型" clearable style="width: 120px" @change="load">
                <el-option v-for="t in TYPES" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
              <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 120px" @change="load">
                <el-option v-for="s in STATUS" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
              <el-button type="primary" :icon="Plus" @click="openCreate">新建工装</el-button>
            </template>
            <el-button v-else-if="tab === 'apply'" type="primary" :icon="Plus" @click="openApply">新建申请单</el-button>
            <el-button :icon="Refresh" @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="tab" @tab-change="load">
        <el-tab-pane label="工装台账" name="ledger">
          <el-table v-loading="loading" :data="rows" size="small" border empty-text="暂无工装">
            <el-table-column prop="code" label="编码" width="140" />
            <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ labelOf(TYPES, row.tooling_type) }}</template>
            </el-table-column>
            <el-table-column prop="spec" label="规格" width="150" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'IDLE' ? 'success' : row.status === 'IN_USE' ? 'warning' : 'info'">
                  {{ labelOf(STATUS, row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="location" label="存放位置" width="130" />
            <el-table-column prop="owner_name" label="责任人" width="110" />
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.status !== 'IDLE'" @click="openBorrow(row)">借出</el-button>
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="申请单" name="apply">
          <el-table v-loading="loading" :data="applies" size="small" border empty-text="暂无申请单">
            <el-table-column prop="code" label="单号" width="140" />
            <el-table-column prop="tooling_name" label="工装名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ labelOf(TYPES, row.tooling_type) }}</template>
            </el-table-column>
            <el-table-column prop="spec" label="规格" width="140" show-overflow-tooltip />
            <el-table-column prop="reason" label="申请原因" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'info'">
                  {{ labelOf(APPLY_STATUS, row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicant_name" label="申请人" width="110" />
            <el-table-column prop="tooling_code" label="生成工装" width="130" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="submitApply(row)">提交</el-button>
                <template v-if="row.status === 'SUBMITTED'">
                  <el-button link type="success" @click="approveApply(row, true)">批准</el-button>
                  <el-button link type="danger" @click="approveApply(row, false)">驳回</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="借用记录" name="borrow">
          <el-table v-loading="loading" :data="borrows" size="small" border empty-text="暂无借用记录">
            <el-table-column prop="tooling_code" label="工装编码" width="140" />
            <el-table-column prop="tooling_name" label="工装名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="borrower_name" label="借用人" width="110" />
            <el-table-column prop="routing_code" label="占用路线" width="140" />
            <el-table-column prop="borrowed_at" label="借出时间" width="170" />
            <el-table-column label="归还时间" width="170">
              <template #default="{ row }">
                <span v-if="row.returned_at">{{ row.returned_at }}</span>
                <el-tag v-else size="small" type="warning">未归还</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button v-if="!row.returned_at" link type="primary" @click="giveBack(row)">归还</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑工装' : '新建工装'" width="560px">
      <el-form label-width="96px">
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="留空自动取号（TL-xxxxxx）" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.toolingType" style="width: 150px">
            <el-option v-for="t in TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-select v-model="form.status" style="width: 150px; margin-left: 8px">
            <el-option v-for="s in STATUS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.spec" />
        </el-form-item>
        <el-form-item label="存放位置">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="购置日期">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applyVisible" title="新建工装申请单" width="520px">
      <el-form label-width="96px">
        <el-form-item label="类型">
          <el-select v-model="applyForm.toolingType" style="width: 150px">
            <el-option v-for="t in TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="工装名称">
          <el-input v-model="applyForm.toolingName" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="applyForm.spec" />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model="applyForm.reason" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="saveApply">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="borrowVisible" title="借出工装" width="480px">
      <el-form label-width="96px">
        <el-form-item label="工装ID">
          <el-input v-model="borrowForm.toolingId" disabled />
        </el-form-item>
        <el-form-item label="占用路线">
          <el-select
            v-model="borrowForm.routingId"
            filterable
            clearable
            :loading="routingLoading"
            placeholder="可选：挂到工艺路线（占用）"
            style="width: 100%"
          >
            <el-option
              v-for="r in routingOptions"
              :key="r.id"
              :label="`${r.code} ${r.name}`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="borrowForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBorrow">借出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tooling-view {
  padding: 4px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header .title {
  font-size: 15px;
  font-weight: 600;
  margin-right: 12px;
}

.card-header .hint {
  font-size: 12px;
  color: var(--plm-text-secondary);
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
