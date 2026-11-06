<template>
  <div class="page">
    <div class="toolbar">
      <el-date-picker
        v-model="week"
        type="week"
        placeholder="选择周"
        format="YYYY 第 ww 周"
        :clearable="false"
        style="width: 190px"
        @change="load"
      />
      <el-button type="primary" @click="openCreate">填报工时</el-button>
      <el-button @click="load">刷新</el-button>
      <el-tag type="success" size="small">本周合计 {{ totalHours }} 小时</el-tag>
      <span class="hint">一天一个任务只填一条；只有草稿/已打回能改，提交后等项目经理审批</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="workDate" label="日期" width="120" />
      <el-table-column prop="projectCode" label="项目编码" width="120" />
      <el-table-column prop="projectName" label="项目名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="任务" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.taskWbsCode }} {{ row.taskName }}</template>
      </el-table-column>
      <el-table-column prop="hours" label="工时(h)" width="90" align="right" />
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">{{ TIMESHEET_WORK_TYPE_LABEL[row.workType] ?? row.workType }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <StatusDot :tone="statusTone(row.status)" :label="TIMESHEET_STATUS_LABEL[row.status] ?? row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column prop="rejectReason" label="打回原因" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'DRAFT' || row.status === 'REJECTED'">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="onSubmit(row)">提交</el-button>
            <el-button link type="danger" @click="onRemove(row)">删除</el-button>
          </template>
          <span v-else class="sub">{{ row.approverName ? `审批：${row.approverName}` : '-' }}</span>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="本周还没有填报工时" :image-size="60" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑工时' : '填报工时'" width="520px">
      <el-form label-width="80px">
        <el-form-item label="项目" required>
          <el-select v-model="form.projectId" filterable placeholder="选择项目" style="width: 100%"
                     @change="onProjectChange">
            <el-option v-for="item in projects" :key="item.id" :label="`${item.code} ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务" required>
          <el-select v-model="form.taskId" filterable :loading="tasksLoading" placeholder="选择 WBS 任务"
                     style="width: 100%">
            <el-option v-for="item in tasks" :key="item.id" :label="`${item.wbsCode} ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" required>
          <el-date-picker v-model="form.workDate" type="date" value-format="YYYY-MM-DD"
                          :disabled-date="isFuture" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工时(h)" required>
          <el-input-number v-model="form.hours" :min="0.5" :max="24" :step="0.5" style="width: 160px" />
          <span class="hint">同一天同一任务只能有一条</span>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.workType">
            <el-radio-button value="NORMAL">正常</el-radio-button>
            <el-radio-button value="OVERTIME">加班</el-radio-button>
          </el-radio-group>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  projectApi,
  taskApi,
  timesheetApi,
  TIMESHEET_STATUS_LABEL,
  TIMESHEET_WORK_TYPE_LABEL,
  type ProjectRow,
  type ProjectTask,
  type TimesheetRow,
  type TimesheetStatus,
  type TimesheetWorkType
} from '@/api/project'
import StatusDot from '@/components/StatusDot.vue'

const loading = ref(false)
const saving = ref(false)
const tasksLoading = ref(false)
const rows = ref<TimesheetRow[]>([])
const totalHours = ref(0)
const week = ref<Date>(startOfWeek(new Date()))

const projects = ref<ProjectRow[]>([])
const tasks = ref<ProjectTask[]>([])

const dialogVisible = ref(false)
const form = reactive<{
  id: string
  projectId: string
  taskId: string
  workDate: string
  hours: number
  workType: TimesheetWorkType
  remark: string
}>({
  id: '',
  projectId: '',
  taskId: '',
  workDate: today(),
  hours: 8,
  workType: 'NORMAL',
  remark: ''
})

onMounted(async () => {
  await loadProjects()
  await load()
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const from = formatDate(week.value)
    const to = formatDate(addDays(week.value, 6))
    const page = await timesheetApi.page({ from, to, size: 200, page: 1 })
    rows.value = page.items
    totalHours.value = page.totalHours
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载工时失败')
  } finally {
    loading.value = false
  }
}

async function loadProjects(): Promise<void> {
  try {
    const page = await projectApi.list({ page: 1, size: 100 })
    projects.value = page.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载项目失败')
  }
}

async function onProjectChange(): Promise<void> {
  form.taskId = ''
  tasks.value = []
  if (!form.projectId) {
    return
  }
  tasksLoading.value = true
  try {
    const board = await taskApi.board(form.projectId)
    tasks.value = flatten(board.tasks)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载任务失败')
  } finally {
    tasksLoading.value = false
  }
}

function openCreate(): void {
  form.id = ''
  form.projectId = ''
  form.taskId = ''
  form.workDate = today()
  form.hours = 8
  form.workType = 'NORMAL'
  form.remark = ''
  tasks.value = []
  dialogVisible.value = true
}

function openEdit(row: TimesheetRow): void {
  form.id = row.id
  form.projectId = row.projectId
  form.taskId = row.taskId
  form.workDate = row.workDate
  form.hours = row.hours
  form.workType = row.workType
  form.remark = row.remark ?? ''
  void onProjectChange().then(() => {
    form.taskId = row.taskId
  })
  dialogVisible.value = true
}

async function onSave(): Promise<void> {
  if (!form.taskId) {
    ElMessage.warning('请选择任务')
    return
  }
  saving.value = true
  try {
    const payload = {
      taskId: form.taskId,
      workDate: form.workDate,
      hours: form.hours,
      workType: form.workType,
      remark: form.remark || null
    }
    if (form.id) {
      await timesheetApi.update(form.id, payload)
    } else {
      await timesheetApi.create(payload)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onSubmit(row: TimesheetRow): Promise<void> {
  try {
    await timesheetApi.submit(row.id)
    ElMessage.success('已提交审批')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

async function onRemove(row: TimesheetRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除 ${row.workDate} 的 ${row.hours} 小时工时？`, '删除工时', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await timesheetApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function statusTone(value: TimesheetStatus): 'ok' | 'warn' | 'danger' | 'muted' {
  if (value === 'APPROVED') {
    return 'ok'
  }
  if (value === 'SUBMITTED') {
    return 'warn'
  }
  if (value === 'REJECTED') {
    return 'danger'
  }
  return 'muted'
}

function flatten(nodes: ProjectTask[]): ProjectTask[] {
  const result: ProjectTask[] = []
  const walk = (list: ProjectTask[]): void => {
    list.forEach((node) => {
      result.push(node)
      if (node.children?.length) {
        walk(node.children)
      }
    })
  }
  walk(nodes)
  return result
}

function isFuture(date: Date): boolean {
  const end = new Date()
  end.setHours(23, 59, 59, 999)
  return date.getTime() > end.getTime()
}

function startOfWeek(date: Date): Date {
  const copy = new Date(date)
  const day = copy.getDay() === 0 ? 7 : copy.getDay()
  copy.setDate(copy.getDate() - day + 1)
  copy.setHours(0, 0, 0, 0)
  return copy
}

function addDays(date: Date, days: number): Date {
  const copy = new Date(date)
  copy.setDate(copy.getDate() + days)
  return copy
}

function today(): string {
  return formatDate(new Date())
}

function formatDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
</script>
