<template>
  <div class="m-page">
    <div class="m-toolbar">
      <el-select
        v-model="status"
        size="small"
        placeholder="未完成（默认）"
        clearable
        style="width: 140px"
        @change="load"
      >
        <el-option v-for="(label, value) in TASK_STATUS_LABEL" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <div v-loading="loading" class="card-list">
      <div v-for="task in rows" :key="task.id" class="task-card">
        <div class="task-head">
          <span class="task-name">{{ task.name }}</span>
          <span class="badge" :class="task.status.toLowerCase()">
            {{ TASK_STATUS_LABEL[task.status] ?? task.status }}
          </span>
        </div>
        <div class="task-object">{{ task.projectCode }} · {{ task.projectName }}</div>
        <div class="task-meta">
          <span>WBS {{ task.wbsCode || '-' }}</span>
          <span>{{ task.planStart || '-' }} ~ {{ task.planEnd || '-' }}</span>
        </div>
        <el-progress :percentage="task.progress" :stroke-width="8" :show-text="false" />
        <div class="task-actions">
          <el-button
            v-if="task.status === 'NOT_STARTED'"
            size="small"
            type="success"
            @click="onStart(task)"
          >
            开始
          </el-button>
          <el-button
            v-if="task.status === 'IN_PROGRESS' || task.status === 'BLOCKED'"
            size="small"
            type="primary"
            @click="onSubmit(task)"
          >
            提交
          </el-button>
          <template v-if="task.status === 'SUBMITTED' && canManage">
            <el-button size="small" type="success" @click="onApprove(task)">通过</el-button>
            <el-button size="small" type="danger" plain @click="onReject(task)">打回</el-button>
          </template>
          <span v-if="task.status === 'DONE'" class="done-hint">已完成 · {{ task.progress }}%</span>
        </div>
      </div>
      <el-empty v-if="!loading && !rows.length" description="暂无任务" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端「我的任务」（P7-3 执行闭环的轻量入口）。
 * 只做执行人视角的动作：开始 / 提交；项目经理（prj.project.write）额外可通过 / 打回。
 * 数据口径与桌面端 MyTasksView 一致（同一 taskApi.myTasks，按执行人跨项目平铺）。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { TASK_STATUS_LABEL, taskApi, type MyTaskRow } from '@/api/project'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canManage = computed(() => userStore.can('prj.project.write'))

const loading = ref(false)
const status = ref('')
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
</script>

<style scoped>
.m-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.m-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 120px;
}

.task-card {
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e5e8ef;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.task-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
  line-height: 1.4;
}

.badge {
  flex: none;
  padding: 1px 6px;
  font-size: 11px;
  border-radius: 10px;
  background: #eef1f6;
  color: #5c6b80;
}

.badge.in_progress {
  background: #e8f3ff;
  color: #2d7ff9;
}

.badge.submitted {
  background: #fff5e6;
  color: #c47b00;
}

.badge.done {
  background: #e9f8ef;
  color: #1f9254;
}

.badge.blocked {
  background: #fdecef;
  color: #c4323f;
}

.task-object {
  font-size: 12px;
  color: #6b7a90;
}

.task-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #8a94a6;
}

.task-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}

.done-hint {
  font-size: 12px;
  color: #1f9254;
}
</style>
