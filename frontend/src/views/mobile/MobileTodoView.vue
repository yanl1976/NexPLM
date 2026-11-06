<template>
  <div class="m-page">
    <div class="m-toolbar">
      <span class="hint">共 {{ total }} 条待办</span>
      <div class="toolbar-actions">
        <el-button size="small" @click="load">刷新</el-button>
        <el-button
          v-if="canAudit"
          size="small"
          type="primary"
          :disabled="!selected.length"
          @click="batchApprove"
        >
          批量同意（{{ selected.length }}）
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="card-list">
      <div v-for="task in tasks" :key="task.taskId" class="task-card" @click="openTask(task)">
        <div class="task-head">
          <el-checkbox
            v-if="canAudit"
            :model-value="selected.includes(task.taskId)"
            @click.stop
            @change="toggleSelect(task.taskId)"
          />
          <span class="task-name">{{ task.name }}</span>
        </div>
        <div class="task-object">{{ task.objectCode || '-' }} · {{ task.objectName || '未关联对象' }}</div>
        <div class="task-foot">
          <span>{{ task.objectType || '流程' }}</span>
          <span>{{ formatTime(task.createTime) }}</span>
        </div>
      </div>
      <el-empty v-if="!loading && !tasks.length" description="没有待办任务" :image-size="60" />
    </div>

    <div v-if="tasks.length < total" class="load-more">
      <el-button link type="primary" :loading="loading" @click="loadMore">加载更多</el-button>
    </div>

    <el-drawer
      v-model="drawerVisible"
      direction="btt"
      size="auto"
      :title="current?.name ?? '审批'"
      class="action-drawer"
    >
      <div v-if="current" class="drawer-body">
        <div class="drawer-object">{{ current.objectCode }} · {{ current.objectName }}</div>
        <el-input
          v-model="comment"
          type="textarea"
          :rows="2"
          maxlength="2000"
          placeholder="审批意见（可空）"
        />

        <div v-if="rejectMode" class="reject-box">
          <div class="reject-title">驳回到节点</div>
          <el-select v-model="target" placeholder="选择节点" style="width: 100%">
            <el-option
              v-for="item in activities"
              :key="item.key"
              :label="item.name || item.key"
              :value="item.key"
            />
          </el-select>
        </div>

        <div class="drawer-actions">
          <el-button v-if="!rejectMode" type="danger" plain :loading="acting" @click="startReject">
            驳回
          </el-button>
          <el-button v-else :loading="acting" @click="rejectMode = false">返回</el-button>
          <el-button
            v-if="!rejectMode"
            type="primary"
            :loading="acting"
            :disabled="!canAudit"
            @click="approve"
          >
            同意
          </el-button>
          <el-button v-else type="danger" :loading="acting" :disabled="!target" @click="reject">
            确认驳回
          </el-button>
        </div>
        <div v-if="!canAudit" class="hint">当前账号没有审批权限（wf.task.audit），只能查看</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowApi, type ActivityRow, type TaskRow } from '@/api/workflow'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canAudit = userStore.can('wf.task.audit')

const PAGE_SIZE = 20

const tasks = ref<TaskRow[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const selected = ref<string[]>([])

const drawerVisible = ref(false)
const current = ref<TaskRow | null>(null)
const comment = ref('')
const rejectMode = ref(false)
const target = ref('')
const activities = ref<ActivityRow[]>([])
const acting = ref(false)

async function load(): Promise<void> {
  page.value = 1
  tasks.value = []
  selected.value = []
  await fetchPage()
}

async function loadMore(): Promise<void> {
  page.value += 1
  await fetchPage()
}

async function fetchPage(): Promise<void> {
  loading.value = true
  try {
    const result = await workflowApi.todo(page.value, PAGE_SIZE)
    tasks.value = page.value === 1 ? result.items : [...tasks.value, ...result.items]
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载待办失败')
  } finally {
    loading.value = false
  }
}

function toggleSelect(taskId: string): void {
  const index = selected.value.indexOf(taskId)
  if (index >= 0) {
    selected.value.splice(index, 1)
  } else {
    selected.value.push(taskId)
  }
}

function openTask(task: TaskRow): void {
  current.value = task
  comment.value = ''
  rejectMode.value = false
  target.value = ''
  activities.value = []
  drawerVisible.value = true
}

async function approve(): Promise<void> {
  if (!current.value) {
    return
  }
  acting.value = true
  try {
    await workflowApi.complete(current.value.taskId, comment.value.trim() || undefined)
    ElMessage.success('已同意')
    drawerVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '审批失败')
  } finally {
    acting.value = false
  }
}

async function startReject(): Promise<void> {
  if (!current.value) {
    return
  }
  rejectMode.value = true
  if (activities.value.length) {
    return
  }
  try {
    activities.value = await workflowApi.activities(current.value.processInstanceId)
    if (!activities.value.length) {
      ElMessage.warning('没有可驳回的历史节点')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载可驳回节点失败')
  }
}

async function reject(): Promise<void> {
  if (!current.value || !target.value) {
    return
  }
  acting.value = true
  try {
    await workflowApi.reject(current.value.taskId, target.value, comment.value.trim() || undefined)
    ElMessage.success('已驳回')
    drawerVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '驳回失败')
  } finally {
    acting.value = false
  }
}

async function batchApprove(): Promise<void> {
  if (!selected.value.length) {
    return
  }
  acting.value = true
  try {
    const result = await workflowApi.batchComplete([...selected.value], '移动端批量同意')
    if (result.failed > 0) {
      ElMessage.warning(`成功 ${result.succeeded} 条，失败 ${result.failed} 条`)
    } else {
      ElMessage.success(`已同意 ${result.succeeded} 条`)
    }
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量审批失败')
  } finally {
    acting.value = false
  }
}

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

onMounted(load)
</script>

<style scoped>
.m-page {
  padding-bottom: 8px;
}

.m-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.toolbar-actions {
  display: flex;
  gap: 6px;
}

.hint {
  font-size: 12px;
  color: #8a94a6;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.task-card {
  background: #fff;
  border: 1px solid #e5e8ef;
  border-radius: 8px;
  padding: 10px 12px;
}

.task-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-name {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.task-object {
  margin-top: 6px;
  font-size: 13px;
  color: #5c6b7a;
  word-break: break-all;
}

.task-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 11px;
  color: #9aa4b2;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 8px;
}

.drawer-object {
  font-size: 13px;
  color: #5c6b7a;
  word-break: break-all;
}

.reject-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.reject-title {
  font-size: 12px;
  color: #8a94a6;
}

.drawer-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.drawer-actions .el-button {
  flex: 1;
}
</style>
