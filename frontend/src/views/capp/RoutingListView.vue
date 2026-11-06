<template>
  <div class="page">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="路线编码 / 名称 / 零件"
        clearable
        style="width: 260px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="reload">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="RELEASED" />
        <el-option label="已归档" value="ARCHIVED" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建工艺路线</el-button>
      <span class="hint">一个零件同一时刻只保留一条「已发布」路线，发布新版本时旧版本自动归档</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="code" label="路线编码" width="130" />
      <el-table-column prop="name" label="路线名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="零件" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="mono">{{ row.objectCode }}</span>
          <span class="sub">{{ row.objectName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="versionNo" label="版本" width="70" align="center" />
      <el-table-column prop="stepCount" label="工序数" width="80" align="center" />
      <el-table-column label="总工时(h)" width="100" align="right">
        <template #default="{ row }">{{ formatMinutes(row.totalMinutes) }}</template>
      </el-table-column>
      <el-table-column prop="applicability" label="适用性" width="120" show-overflow-tooltip />
      <el-table-column label="工艺负责人" width="120">
        <template #default="{ row }">{{ row.ownerName || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{ ROUTING_STATUS_LABEL[row.status as RoutingStatus] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="160">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column v-if="canWrite" label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">查看</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="openEdit(row.id)">编辑</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="success" @click="onRelease(row)">发布</el-button>
          <el-button v-if="row.status === 'RELEASED'" link type="warning" @click="onArchive(row)">归档</el-button>
          <el-button link type="primary" @click="onCopy(row)">复制</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      class="pager"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @current-change="load"
      @size-change="load"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="960px"
      top="6vh"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="零件" required>
          <el-select
            v-model="form.objectId"
            filterable
            remote
            reserve-keyword
            placeholder="输入零件编码/名称搜索"
            :remote-method="searchItems"
            :loading="itemsLoading"
            :disabled="!!form.id"
            style="width: 100%"
          >
            <el-option
              v-for="item in items"
              :key="item.id"
              :label="`${item.code} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="路线名称" required>
          <el-input v-model="form.name" maxlength="200" placeholder="如：轴类通用车削工艺" />
        </el-form-item>
        <el-form-item label="适用性">
          <el-input v-model="form.applicability" maxlength="200" placeholder="机型 / 项目 / 通用说明" />
        </el-form-item>
        <el-form-item label="工艺负责人">
          <el-select
            v-model="form.ownerId"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="默认创建人"
            :remote-method="searchUsers"
            :loading="usersLoading"
            :disabled="readonly"
            style="width: 100%"
          >
            <el-option
              v-for="user in users"
              :key="user.id"
              :label="`${user.displayName}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="路线串">
          <el-input :model-value="routeString" readonly placeholder="由工序自动拼装" />
        </el-form-item>
      </el-form>

      <div class="section-title">
        工序（批量编辑，保存时整表替换）
        <el-button size="small" type="primary" plain :disabled="readonly" @click="addStep">添加工序</el-button>
      </div>
      <el-table :data="form.steps" border size="small">
        <el-table-column label="工序号" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.seqNo" :min="1" :step="10" :controls="false" :disabled="readonly" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="工序名称" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.name" :disabled="readonly" maxlength="100" />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-select v-model="row.opType" :disabled="readonly">
              <el-option v-for="(label, value) in OP_TYPE_LABEL" :key="value" :label="label" :value="value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="工作中心" width="130">
          <template #default="{ row }">
            <el-input v-model="row.workCenter" :disabled="readonly" maxlength="100" />
          </template>
        </el-table-column>
        <el-table-column label="准备工时(h)" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.setupMinutes" :min="0" :precision="2" :controls="false" :disabled="readonly" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="单件工时(h)" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.runMinutes" :min="0" :precision="2" :controls="false" :disabled="readonly" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.remark" :disabled="readonly" maxlength="500" />
          </template>
        </el-table-column>
        <el-table-column v-if="!readonly" label="排序" width="150" align="center">
          <template #default="{ $index }">
            <el-button link type="primary" :disabled="$index === 0" @click="moveStep($index, -1)">上移</el-button>
            <el-button
              link
              type="primary"
              :disabled="$index === form.steps.length - 1"
              @click="moveStep($index, 1)"
            >
              下移
            </el-button>
            <el-button link type="danger" @click="form.steps.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button v-if="form.id && !readonly" :loading="resequencing" @click="onResequence">
          序号重建（10/20/30…）
        </el-button>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="!readonly" type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { itemApi, type ItemSummary } from '@/api/item'
import {
  cappApi,
  OP_TYPE_LABEL,
  ROUTING_STATUS_LABEL,
  type RoutingRow,
  type RoutingStatus,
  type RoutingStep
} from '@/api/capp'
import { workflowApi, type ActorUserRow } from '@/api/workflow'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('capp.routing.write'))

const loading = ref(false)
const saving = ref(false)
const rows = ref<RoutingRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const status = ref<string>('')

const dialogVisible = ref(false)
const readonly = ref(false)
const resequencing = ref(false)
const items = ref<ItemSummary[]>([])
const itemsLoading = ref(false)
const users = ref<ActorUserRow[]>([])
const usersLoading = ref(false)

const form = reactive<{
  id?: string
  name: string
  objectId: string
  applicability: string
  remark: string
  ownerId: string | null
  steps: RoutingStep[]
}>({
  name: '',
  objectId: '',
  applicability: '',
  remark: '',
  ownerId: null,
  steps: []
})

const dialogTitle = computed(() => {
  if (readonly.value) {
    return '工艺路线详情'
  }
  return form.id ? '编辑工艺路线' : '新建工艺路线'
})

/** 路线串预览：与后端 detail.routeString 同规则（10 下料 → 20 车削）。 */
const routeString = computed(() =>
  form.steps
    .filter((step) => step.name)
    .slice()
    .sort((left, right) => (left.seqNo ?? 0) - (right.seqNo ?? 0))
    .map((step) => `${step.seqNo} ${step.name}`)
    .join(' → ')
)

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await cappApi.list({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      status: status.value || undefined
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载工艺路线失败')
  } finally {
    loading.value = false
  }
}

function reload(): void {
  page.value = 1
  load()
}

async function searchItems(keywordText: string): Promise<void> {
  itemsLoading.value = true
  try {
    const result = await itemApi.list({ page: 1, size: 20, keyword: keywordText || undefined })
    items.value = result.items
  } catch {
    items.value = []
  } finally {
    itemsLoading.value = false
  }
}

async function searchUsers(keywordText: string): Promise<void> {
  usersLoading.value = true
  try {
    users.value = await workflowApi.actorUsers(keywordText || '')
  } catch {
    users.value = []
  } finally {
    usersLoading.value = false
  }
}

function openCreate(): void {
  readonly.value = false
  Object.assign(form, {
    id: undefined,
    name: '',
    objectId: '',
    applicability: '',
    remark: '',
    ownerId: null,
    steps: []
  })
  form.steps.push(emptyStep(10))
  dialogVisible.value = true
  searchItems('')
  searchUsers('')
}

async function openDetail(id: string): Promise<void> {
  await fillForm(id)
  readonly.value = true
  dialogVisible.value = true
}

async function openEdit(id: string): Promise<void> {
  await fillForm(id)
  readonly.value = false
  dialogVisible.value = true
}

async function fillForm(id: string): Promise<void> {
  try {
    const detail = await cappApi.detail(id)
    Object.assign(form, {
      id: detail.id,
      name: detail.name,
      objectId: detail.objectId,
      applicability: detail.applicability ?? '',
      remark: detail.remark ?? '',
      ownerId: detail.ownerId ?? null,
      steps: detail.steps.map((step) => ({ ...step }))
    })
    if (detail.ownerId && detail.ownerName) {
      users.value = [{ id: detail.ownerId, username: detail.ownerName, displayName: detail.ownerName } as ActorUserRow]
    }
    items.value = [
      {
        id: detail.objectId,
        code: detail.objectCode ?? '',
        name: detail.objectName ?? ''
      } as ItemSummary
    ]
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  }
}

function emptyStep(seqNo: number): RoutingStep {
  return { seqNo, name: '', opType: 'MACHINING', workCenter: '', setupMinutes: 0, runMinutes: 0, remark: '' }
}

function addStep(): void {
  const maxSeq = form.steps.reduce((max, step) => Math.max(max, step.seqNo ?? 0), 0)
  form.steps.push(emptyStep(maxSeq + 10))
}

/** 上移/下移：交换位置后按 10/20/30 重排工序号（编排即重排，避免号不连续）。 */
function moveStep(index: number, delta: number): void {
  const target = index + delta
  if (target < 0 || target >= form.steps.length) {
    return
  }
  const [moved] = form.steps.splice(index, 1)
  form.steps.splice(target, 0, moved)
  form.steps.forEach((step, idx) => {
    step.seqNo = (idx + 1) * 10
  })
}

async function onResequence(): Promise<void> {
  if (!form.id) {
    return
  }
  resequencing.value = true
  try {
    const detail = await cappApi.resequence(form.id)
    form.steps = detail.steps.map((step) => ({ ...step }))
    ElMessage.success('工序号已按顺序重建为 10/20/30…')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '序号重建失败')
  } finally {
    resequencing.value = false
  }
}

async function onSave(): Promise<void> {
  if (!form.objectId) {
    ElMessage.warning('请先选择零件')
    return
  }
  if (!form.name.trim()) {
    ElMessage.warning('请填写路线名称')
    return
  }
  if (!form.steps.length) {
    ElMessage.warning('至少需要一道工序')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      objectId: form.objectId,
      applicability: form.applicability || null,
      remark: form.remark || null,
      ownerId: form.ownerId,
      steps: form.steps.map((step) => ({
        seqNo: step.seqNo,
        name: step.name,
        opType: step.opType,
        workCenter: step.workCenter || null,
        setupMinutes: step.setupMinutes ?? 0,
        runMinutes: step.runMinutes ?? 0,
        remark: step.remark || null
      }))
    }
    if (form.id) {
      await cappApi.update(form.id, payload)
      ElMessage.success('已保存')
    } else {
      await cappApi.create(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onRelease(row: RoutingRow): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `发布后不可再修改，同一零件的其它已发布路线会自动归档。确认发布 ${row.code}？`,
      '发布工艺路线',
      { confirmButtonText: '发布', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await cappApi.release(row.id)
    ElMessage.success('已发布')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败')
  }
}

async function onArchive(row: RoutingRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认归档 ${row.code}？归档后不可再投产使用。`, '归档工艺路线', {
      confirmButtonText: '归档',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await cappApi.archive(row.id)
    ElMessage.success('已归档')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '归档失败')
  }
}

async function onCopy(row: RoutingRow): Promise<void> {
  try {
    const copied = await cappApi.copy(row.id)
    ElMessage.success(`已复制为草稿 ${copied.code}`)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '复制失败')
  }
}

async function onDelete(row: RoutingRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除草稿 ${row.code}？`, '删除工艺路线', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await cappApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function statusTagType(value: RoutingStatus): 'success' | 'info' | 'warning' {
  if (value === 'RELEASED') {
    return 'success'
  }
  return value === 'ARCHIVED' ? 'info' : 'warning'
}

function formatMinutes(value?: number | null): string {
  return value == null ? '-' : Number(value).toFixed(2)
}

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
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

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0;
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
