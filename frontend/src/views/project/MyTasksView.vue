<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="status" placeholder="未完成（默认）" clearable style="width: 160px" @change="load">
        <el-option v-for="(label, value) in TASK_STATUS_LABEL" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button type="primary" @click="load">刷新</el-button>
      <span class="hint">按执行人汇总：默认只列未完成的我负责的任务，可直接开始/提交/审核</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="projectCode" label="项目编码" width="120" />
      <el-table-column prop="projectName" label="项目名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="wbsCode" label="WBS" width="90" />
      <el-table-column prop="name" label="任务名称" min-width="180" show-overflow-tooltip />
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
      <el-table-column label="进度" width="130">
        <template #default="{ row }">
          <el-progress :percentage="row.progress" :stroke-width="10" :show-text="false" />
          <span class="sub">{{ row.progress }}%</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
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
          <template v-if="row.status === 'SUBMITTED' && canManage">
            <el-button link type="success" @click="onApprove(row)">通过</el-button>
            <el-button link type="danger" @click="onReject(row)">打回</el-button>
          </template>
          <el-button link type="primary" @click="openBoard(row)">看板</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!rows.length" description="暂无任务" :image-size="60" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi, TASK_STATUS_LABEL, type MyTaskRow, type TaskStatus } from '@/api/project'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const canManage = computed(() => userStore.can('prj.project.write'))

const loading = ref(false)
const status = ref<string>('')
const rows = ref<MyTaskRow[]>([])

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    rows.value = await taskApi.myTasks(status.value || undefined)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载我的任务失败')
  } finally {
    loading.value = false
  }
}

function openBoard(task: MyTaskRow): void {
  router.push(`/project/${task.projectId}/tasks`)
}

async function onStart(task: MyTaskRow): Promise<void> {
  try {
    await taskApi.start(task.id)
    ElMessage.success('任务已开始')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '开始失败')
  }
}

async function onSubmit(task: MyTaskRow): Promise<void> {
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
    await taskApi.submit(task.id, note || undefined)
    ElMessage.success('已提交审核')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

async function onApprove(task: MyTaskRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认审核通过「${task.name}」？通过后进度置为 100%`, '审核通过', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await taskApi.approve(task.id)
    ElMessage.success('已通过')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '审核失败')
  }
}

async function onReject(task: MyTaskRow): Promise<void> {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('打回原因（必填）', `打回 - ${task.name}`, {
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
    await taskApi.reject(task.id, reason)
    ElMessage.success('已打回')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '打回失败')
  }
}

function statusTagType(value: TaskStatus): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'DONE') {
    return 'success'
  }
  if (value === 'SUBMITTED') {
    return 'warning'
  }
  if (value === 'BLOCKED') {
    return 'danger'
  }
  return value === 'IN_PROGRESS' ? 'warning' : 'info'
}
</script>
