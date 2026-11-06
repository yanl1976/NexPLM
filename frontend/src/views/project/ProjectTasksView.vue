<template>
  <div class="page">
    <div class="toolbar">
      <el-button link type="primary" @click="back">← 返回项目列表</el-button>
      <span class="prj-title">{{ board?.projectCode }} {{ board?.projectName }}</span>
      <el-tag type="success" size="small">项目进度 {{ board?.projectProgress ?? 0 }}%</el-tag>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate(null)">新增一级任务</el-button>
      <el-button v-if="canWrite" :loading="resequencing" @click="onResequence">WBS 编码重排</el-button>
      <el-button type="success" plain @click="openDeliverables">交付物与齐套</el-button>
      <el-button type="warning" plain @click="openTimesheets">项目工时</el-button>
      <span class="hint">项目进度 = 叶子任务按工期加权汇总（父任务不重复计数）</span>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="WBS 任务" name="task">
        <el-table v-loading="loading" :data="board?.tasks ?? []" row-key="id" border default-expand-all
                  :tree-props="{ children: 'children' }">
          <el-table-column prop="wbsCode" label="WBS" width="90" />
          <el-table-column prop="name" label="任务名称" min-width="220" show-overflow-tooltip />
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">{{ TASK_TYPE_LABEL[row.type] ?? row.type }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ TASK_STATUS_LABEL[row.status] ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="计划起止" width="190" align="center">
            <template #default="{ row }">{{ row.planStart || '-' }} ~ {{ row.planEnd || '-' }}</template>
          </el-table-column>
          <el-table-column label="工期(天)" width="90" align="center">
            <template #default="{ row }">{{ row.durationDays ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="进度" width="130">
            <template #default="{ row }">
              <el-progress :percentage="row.progress" :stroke-width="10" :show-text="false" />
              <span class="sub">{{ row.progress }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="负责人" width="110">
            <template #default="{ row }">{{ row.ownerName || '-' }}</template>
          </el-table-column>
          <el-table-column label="审核信息" min-width="170" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.rejectReason" class="reject-reason">打回：{{ row.rejectReason }}</span>
              <span v-else-if="row.submitNote" class="sub">提交：{{ row.submitNote }}</span>
              <span v-else class="sub">-</span>
            </template>
          </el-table-column>
          <el-table-column v-if="canWrite" label="操作" width="360" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'NOT_STARTED'" link type="success" @click="onStart(row)">开始</el-button>
              <el-button
                v-if="row.status === 'IN_PROGRESS' || row.status === 'BLOCKED'"
                link
                type="primary"
                @click="onSubmit(row)"
              >
                提交
              </el-button>
              <template v-if="row.status === 'SUBMITTED'">
                <el-button link type="success" @click="onApprove(row)">通过</el-button>
                <el-button link type="danger" @click="onReject(row)">打回</el-button>
              </template>
              <el-button v-if="row.status !== 'DONE'" link type="warning" @click="openAssign(row)">派工</el-button>
              <el-button link type="primary" @click="openCreate(row)">加子任务</el-button>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="onDeleteTask(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="甘特图" name="gantt">
        <div class="gantt">
          <div class="gantt-head">
            <span class="sub">{{ ganttStart }} ~ {{ ganttEnd }}（共 {{ totalDays }} 天）</span>
            <span class="sub">灰色=计划，绿色=已完成的进度</span>
          </div>
          <div v-for="task in flatTasks" :key="task.id" class="gantt-row">
            <div class="gantt-label" :style="{ paddingLeft: `${(task.depth - 1) * 14}px` }">
              <span class="mono">{{ task.wbsCode }}</span>
              <span class="gantt-name">{{ task.name }}</span>
            </div>
            <div class="gantt-track">
              <div
                v-if="task.bar"
                class="gantt-bar"
                :class="{ 'is-milestone': task.type === 'MILESTONE' }"
                :style="{ left: task.bar.left, width: task.bar.width }"
                :title="`${task.planStart} ~ ${task.planEnd}｜${task.progress}%`"
              >
                <div class="gantt-bar-done" :style="{ width: `${task.progress}%` }" />
              </div>
              <span v-else class="sub">未排期</span>
            </div>
          </div>
          <el-empty v-if="!flatTasks.length" description="还没有任务" :image-size="60" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="任务依赖" name="dep">
        <div class="toolbar">
          <el-button v-if="canWrite" type="primary" plain @click="openDepCreate">新增依赖</el-button>
          <span class="hint">FS=完成-开始，SS=开始-开始，FF=完成-完成，SF=开始-完成；服务端自动防环</span>
        </div>
        <el-table :data="board?.dependencies ?? []" border size="small">
          <el-table-column label="前置任务" min-width="200">
            <template #default="{ row }">{{ row.predName || row.predTaskId }}</template>
          </el-table-column>
          <el-table-column label="依赖类型" width="120" align="center">
            <template #default="{ row }">{{ DEP_TYPE_LABEL[row.depType] ?? row.depType }}</template>
          </el-table-column>
          <el-table-column label="后续任务" min-width="200">
            <template #default="{ row }">{{ row.succName || row.succTaskId }}</template>
          </el-table-column>
          <el-table-column prop="lagDays" label="滞后(天)" width="100" align="center" />
          <el-table-column v-if="canWrite" label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="onDeleteDep(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!board?.dependencies.length" description="还没有任务依赖" :image-size="60" />
      </el-tab-pane>
    </el-tabs>

    <!-- 任务编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="任务名称" required>
          <el-input v-model="form.name" maxlength="200" placeholder="如：结构设计" />
        </el-form-item>
        <el-form-item label="上级任务">
          <el-select v-model="form.parentId" clearable placeholder="一级任务" style="width: 100%">
            <el-option
              v-for="item in flatTasks"
              :key="item.id"
              :label="`${item.wbsCode} ${item.name}`"
              :value="item.id"
              :disabled="item.id === form.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 160px">
            <el-option v-for="(label, value) in TASK_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" clearable placeholder="按进度自动推断" style="width: 160px">
            <el-option v-for="(label, value) in TASK_STATUS_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开始">
          <el-date-picker v-model="form.planStart" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="计划结束">
          <el-date-picker v-model="form.planEnd" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="工期(天)">
          <el-input-number v-model="form.durationDays" :min="0" :controls="false" style="width: 140px" />
          <span class="sub">留空按计划起止推算</span>
        </el-form-item>
        <el-form-item label="进度(%)">
          <el-input-number v-model="form.progress" :min="0" :max="100" style="width: 140px" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select
            v-model="form.ownerId"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="默认创建人"
            :remote-method="searchUsers"
            :loading="usersLoading"
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 依赖编辑 -->
    <el-dialog v-model="depDialogVisible" title="新增任务依赖" width="560px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="前置任务" required>
          <el-select v-model="depForm.predTaskId" filterable placeholder="选择前置任务" style="width: 100%">
            <el-option
              v-for="item in flatTasks"
              :key="item.id"
              :label="`${item.wbsCode} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="后续任务" required>
          <el-select v-model="depForm.succTaskId" filterable placeholder="选择后续任务" style="width: 100%">
            <el-option
              v-for="item in flatTasks"
              :key="item.id"
              :label="`${item.wbsCode} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="依赖类型">
          <el-select v-model="depForm.depType" style="width: 180px">
            <el-option v-for="(label, value) in DEP_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="滞后(天)">
          <el-input-number v-model="depForm.lagDays" :controls="false" style="width: 140px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="depDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="depSaving" @click="onSaveDep">保存</el-button>
      </template>
    </el-dialog>

    <!-- 派工 -->
    <el-dialog v-model="assignVisible" title="派工（指定执行人）" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="任务">
          <el-input :model-value="assignTarget ? `${assignTarget.wbsCode} ${assignTarget.name}` : ''" readonly />
        </el-form-item>
        <el-form-item label="执行人" required>
          <el-select
            v-model="assignOwnerId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索用户"
            :remote-method="searchUsers"
            :loading="usersLoading"
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
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="onAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  taskApi,
  DEP_TYPE_LABEL,
  TASK_STATUS_LABEL,
  TASK_TYPE_LABEL,
  type ProjectTask,
  type TaskBoard,
  type TaskDependency,
  type TaskStatus
} from '@/api/project'
import { workflowApi, type ActorUserRow } from '@/api/workflow'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const canWrite = computed(() => userStore.can('prj.project.write'))

const projectId = String(route.params.id ?? '')
const loading = ref(false)
const saving = ref(false)
const resequencing = ref(false)
const tab = ref<'task' | 'gantt' | 'dep'>('task')
const board = ref<TaskBoard | null>(null)

const users = ref<ActorUserRow[]>([])
const usersLoading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = computed(() => (form.id ? '编辑任务' : '新增任务'))
const form = reactive<{
  id?: string
  name: string
  parentId: string | null
  type: string
  status: string
  planStart: string | null
  planEnd: string | null
  durationDays: number | null
  progress: number
  ownerId: string | null
  remark: string
}>({
  id: undefined,
  name: '',
  parentId: null,
  type: 'TASK',
  status: '',
  planStart: null,
  planEnd: null,
  durationDays: null,
  progress: 0,
  ownerId: null,
  remark: ''
})

const depDialogVisible = ref(false)
const depSaving = ref(false)
const depForm = reactive<{ predTaskId: string; succTaskId: string; depType: string; lagDays: number }>({
  predTaskId: '',
  succTaskId: '',
  depType: 'FS',
  lagDays: 0
})

interface FlatTask extends ProjectTask {
  depth: number
  bar?: { left: string; width: string }
}

/** 扁平化（甘特图与上级任务下拉都要按显示顺序排）。 */
const flatTasks = computed<FlatTask[]>(() => {
  const result: FlatTask[] = []
  const walk = (nodes: ProjectTask[], depth: number): void => {
    nodes.forEach((node) => {
      result.push({ ...node, depth, bar: barOf(node) })
      walk(node.children ?? [], depth + 1)
    })
  }
  walk(board.value?.tasks ?? [], 1)
  return result
})

/** 甘特时间轴：取任务计划日期并集，没有就退化到今天起 30 天。 */
const ganttRange = computed<{ start: Date; end: Date }>(() => {
  const today = new Date()
  const fallbackStart = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const fallbackEnd = new Date(fallbackStart.getTime() + 29 * 86400000)
  if (!board.value?.rangeStart || !board.value?.rangeEnd) {
    return { start: fallbackStart, end: fallbackEnd }
  }
  return { start: new Date(board.value.rangeStart), end: new Date(board.value.rangeEnd) }
})

const totalDays = computed(() => {
  const diff = Math.round((ganttRange.value.end.getTime() - ganttRange.value.start.getTime()) / 86400000) + 1
  return Math.max(diff, 1)
})

const ganttStart = computed(() => formatDate(ganttRange.value.start))
const ganttEnd = computed(() => formatDate(ganttRange.value.end))

function barOf(task: ProjectTask): { left: string; width: string } | undefined {
  if (!task.planStart || !task.planEnd) {
    return undefined
  }
  const start = new Date(task.planStart)
  const end = new Date(task.planEnd)
  const span = ganttRange.value.end.getTime() - ganttRange.value.start.getTime() + 86400000
  const offset = start.getTime() - ganttRange.value.start.getTime()
  const width = end.getTime() - start.getTime() + 86400000
  const left = Math.max(0, Math.min(100, (offset / span) * 100))
  const widthPct = Math.max(1.2, Math.min(100 - left, (width / span) * 100))
  return { left: `${left}%`, width: `${widthPct}%` }
}

function formatDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    board.value = await taskApi.board(projectId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载 WBS 失败')
  } finally {
    loading.value = false
  }
}

function back(): void {
  router.push('/project')
}

function openDeliverables(): void {
  router.push(`/project/${projectId}/deliverables`)
}

function openTimesheets(): void {
  router.push(`/project/${projectId}/timesheets`)
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

function openCreate(parent: ProjectTask | null): void {
  Object.assign(form, {
    id: undefined,
    name: '',
    parentId: parent?.id ?? null,
    type: 'TASK',
    status: '',
    planStart: parent?.planStart ?? null,
    planEnd: parent?.planEnd ?? null,
    durationDays: null,
    progress: 0,
    ownerId: null,
    remark: ''
  })
  dialogVisible.value = true
  searchUsers('')
}

function openEdit(task: ProjectTask): void {
  Object.assign(form, {
    id: task.id,
    name: task.name,
    parentId: task.parentId ?? null,
    type: task.type,
    status: task.status,
    planStart: task.planStart ?? null,
    planEnd: task.planEnd ?? null,
    durationDays: task.durationDays ?? null,
    progress: task.progress,
    ownerId: task.ownerId ?? null,
    remark: task.remark ?? ''
  })
  if (task.ownerId && task.ownerName) {
    users.value = [
      { id: task.ownerId, username: task.ownerName, displayName: task.ownerName } as ActorUserRow
    ]
  } else {
    searchUsers('')
  }
  dialogVisible.value = true
}

async function onSave(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请填写任务名称')
    return
  }
  if (form.planStart && form.planEnd && form.planEnd < form.planStart) {
    ElMessage.warning('计划结束日不能早于计划开始日')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      parentId: form.parentId || null,
      type: form.type,
      status: form.status || undefined,
      planStart: form.planStart,
      planEnd: form.planEnd,
      durationDays: form.durationDays ?? null,
      progress: form.progress,
      ownerId: form.ownerId || null,
      remark: form.remark || null
    }
    board.value = form.id
      ? await taskApi.update(form.id, payload)
      : await taskApi.create(projectId, payload)
    ElMessage.success(form.id ? '已保存' : '已新增')
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存任务失败')
  } finally {
    saving.value = false
  }
}

async function onDeleteTask(task: ProjectTask): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除任务「${task.name}」？`, '删除任务', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    board.value = await taskApi.remove(task.id)
    ElMessage.success('已删除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除任务失败')
  }
}

async function onResequence(): Promise<void> {
  resequencing.value = true
  try {
    board.value = await taskApi.resequence(projectId)
    ElMessage.success('WBS 编码已重排')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重排失败')
  } finally {
    resequencing.value = false
  }
}

// ---------------------------------------------------------------- 执行闭环（P7-3）

const assignVisible = ref(false)
const assigning = ref(false)
const assignTarget = ref<ProjectTask | null>(null)
const assignOwnerId = ref('')

function openAssign(task: ProjectTask): void {
  assignTarget.value = task
  assignOwnerId.value = task.ownerId ?? ''
  assignVisible.value = true
  searchUsers('')
}

async function onAssign(): Promise<void> {
  if (!assignTarget.value) {
    return
  }
  if (!assignOwnerId.value) {
    ElMessage.warning('请选择执行人')
    return
  }
  assigning.value = true
  try {
    board.value = await taskApi.assign(assignTarget.value.id, assignOwnerId.value)
    ElMessage.success('已派工')
    assignVisible.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '派工失败')
  } finally {
    assigning.value = false
  }
}

async function onStart(task: ProjectTask): Promise<void> {
  try {
    board.value = await taskApi.start(task.id)
    ElMessage.success('任务已开始')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '开始失败')
  }
}

async function onSubmit(task: ProjectTask): Promise<void> {
  let note = ''
  try {
    const result = await ElMessageBox.prompt('提交说明（可留空）', `提交审核 - ${task.name}`, {
      inputValue: '',
      confirmButtonText: '提交',
      cancelButtonText: '取消'
    })
    note = (result as { value?: string }).value ?? ''
  } catch {
    return
  }
  try {
    board.value = await taskApi.submit(task.id, note || undefined)
    ElMessage.success('已提交审核')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

async function onApprove(task: ProjectTask): Promise<void> {
  try {
    await ElMessageBox.confirm(`审核通过后任务进度置为 100%，确认通过「${task.name}」？`, '审核通过', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    board.value = await taskApi.approve(task.id)
    ElMessage.success('已通过')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '审核失败')
  }
}

async function onReject(task: ProjectTask): Promise<void> {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('打回原因（必填，执行人据此返工）', `打回 - ${task.name}`, {
      inputValue: '',
      inputValidator: (value: string) => (value && value.trim() ? true : '请填写打回原因'),
      confirmButtonText: '打回',
      cancelButtonText: '取消'
    })
    reason = (result as { value?: string }).value?.trim() ?? ''
  } catch {
    return
  }
  try {
    board.value = await taskApi.reject(task.id, reason)
    ElMessage.success('已打回')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '打回失败')
  }
}

function openDepCreate(): void {
  depForm.predTaskId = ''
  depForm.succTaskId = ''
  depForm.depType = 'FS'
  depForm.lagDays = 0
  depDialogVisible.value = true
}

async function onSaveDep(): Promise<void> {
  if (!depForm.predTaskId || !depForm.succTaskId) {
    ElMessage.warning('请选择前置与后续任务')
    return
  }
  depSaving.value = true
  try {
    board.value = await taskApi.createDependency(projectId, {
      predTaskId: depForm.predTaskId,
      succTaskId: depForm.succTaskId,
      depType: depForm.depType,
      lagDays: depForm.lagDays
    })
    ElMessage.success('依赖已新增')
    depDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '新增依赖失败')
  } finally {
    depSaving.value = false
  }
}

async function onDeleteDep(dep: TaskDependency): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除「${dep.predName} → ${dep.succName}」这条依赖？`, '删除依赖', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    board.value = await taskApi.removeDependency(dep.id)
    ElMessage.success('已删除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除依赖失败')
  }
}

function statusTagType(value: TaskStatus): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'DONE') {
    return 'success'
  }
  if (value === 'BLOCKED') {
    return 'danger'
  }
  return value === 'IN_PROGRESS' ? 'warning' : 'info'
}
</script>

<style scoped>
.prj-title {
  font-weight: 600;
}

.gantt {
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 10px;
  background: #fff;
}

.gantt-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.gantt-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.gantt-label {
  width: 280px;
  flex: 0 0 280px;
  display: flex;
  gap: 6px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.gantt-name {
  overflow: hidden;
  text-overflow: ellipsis;
}

.gantt-track {
  position: relative;
  flex: 1;
  height: 22px;
  background: repeating-linear-gradient(
    90deg,
    var(--el-fill-color-lighter),
    var(--el-fill-color-lighter) 1px,
    transparent 1px,
    transparent 10%
  );
}

.gantt-bar {
  position: absolute;
  top: 4px;
  height: 14px;
  background: var(--el-color-info-light-5);
  border-radius: 3px;
  overflow: hidden;
}

.gantt-bar.is-milestone {
  background: var(--el-color-warning-light-5);
}

.gantt-bar-done {
  height: 100%;
  background: var(--el-color-success);
}

.reject-reason {
  color: var(--el-color-danger);
}
</style>
