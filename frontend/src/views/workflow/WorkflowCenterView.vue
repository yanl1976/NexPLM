<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">流程中心</span>
          <div>
            <el-button @click="goDelegate">委托设置</el-button>
            <el-button type="primary" @click="reload">刷新</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="tab" @tab-change="onTabChange">
        <el-tab-pane label="我的待办" name="todo">
          <div class="toolbar">
            <el-button type="primary" :disabled="selected.length === 0" @click="openBatch">
              批量通过（{{ selected.length }}）
            </el-button>
            <span class="hint">共 {{ todo.total }} 条待办</span>
          </div>
          <el-table
            v-loading="todo.loading"
            :data="todo.rows"
            border
            @selection-change="onSelectionChange"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column prop="name" label="审批节点" min-width="140" show-overflow-tooltip />
            <el-table-column label="业务对象" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">
                {{ objectText(row.objectCode, row.objectName) }}
              </template>
            </el-table-column>
            <el-table-column prop="objectType" label="对象类型" width="130" />
            <el-table-column label="到达时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openComplete(row)">通过</el-button>
                <el-button link type="warning" @click.stop="openReject(row)">驳回</el-button>
                <el-button link @click.stop="openAddSigner(row)">加签</el-button>
                <el-button link @click.stop="openInstance(row.processInstanceId)">详情</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无待办" />
            </template>
          </el-table>
          <el-pagination
            v-model:current-page="todo.page"
            v-model:page-size="todo.size"
            :total="todo.total"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            class="pager"
            @current-change="loadTodo"
            @size-change="reloadTodo"
          />
        </el-tab-pane>

        <el-tab-pane label="我的已办" name="done">
          <el-table v-loading="done.loading" :data="done.rows" border>
            <el-table-column prop="name" label="审批节点" min-width="140" show-overflow-tooltip />
            <el-table-column label="业务对象" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">
                {{ objectText(row.objectCode, row.objectName) }}
              </template>
            </el-table-column>
            <el-table-column label="开始" width="170">
              <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="结束" width="170">
              <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="耗时" width="110">
              <template #default="{ row }">{{ formatDuration(row.durationInMillis) }}</template>
            </el-table-column>
            <el-table-column label="结果" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <StatusDot v-if="row.deleteReason" tone="warn" :label="row.deleteReason" />
                <StatusDot v-else tone="ok" label="已完成" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openInstance(row.processInstanceId)">详情</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无已办" />
            </template>
          </el-table>
          <el-pagination
            v-model:current-page="done.page"
            v-model:page-size="done.size"
            :total="done.total"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            class="pager"
            @current-change="loadDone"
            @size-change="reloadDone"
          />
        </el-tab-pane>

        <el-tab-pane label="我发起的" name="started">
          <el-table v-loading="started.loading" :data="started.rows" border>
            <el-table-column prop="processKey" label="流程" min-width="150" show-overflow-tooltip />
            <el-table-column label="业务对象" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">
                {{ objectText(row.objectCode, row.objectName) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <StatusDot :tone="statusTone(row.status)" :label="statusLabel(row.status)" />
              </template>
            </el-table-column>
            <el-table-column label="当前节点" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.currentActivities.length ? row.currentActivities.join(' / ') : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="发起时间" width="170">
              <template #default="{ row }">{{ formatTime(row.startedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openInstance(row.processInstanceId)">详情</el-button>
                <el-button
                  v-if="row.status === 'RUNNING'"
                  link
                  type="danger"
                  @click="onWithdraw(row)"
                >
                  撤回
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无发起的流程" />
            </template>
          </el-table>
          <el-pagination
            v-model:current-page="started.page"
            v-model:page-size="started.size"
            :total="started.total"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            class="pager"
            @current-change="loadStarted"
            @size-change="reloadStarted"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="action.visible" :title="actionTitle" width="520px">
      <el-form label-width="90px">
        <el-form-item v-if="action.task" label="审批节点">
          <span>{{ action.task.name }}</span>
        </el-form-item>
        <el-form-item v-if="action.mode === 'reject'" label="驳回至" required>
          <el-select v-model="action.targetActivityId" placeholder="选择已走过的节点" style="width: 100%">
            <el-option
              v-for="item in rejectOptions"
              :key="item.key"
              :label="item.name || item.key"
              :value="item.key"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="action.mode === 'addSigner'" label="加签人" required>
          <el-select
            v-model="action.signerId"
            filterable
            remote
            reserve-keyword
            placeholder="输入姓名或账号搜索"
            :remote-method="searchUsers"
            :loading="action.userLoading"
            style="width: 100%"
          >
            <el-option
              v-for="user in action.users"
              :key="user.id"
              :label="`${user.displayName}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="action.comment" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="action.visible = false">取消</el-button>
        <el-button type="primary" :loading="action.submitting" @click="submitAction">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  workflowApi,
  type ActivityRow,
  type ActorUserRow,
  type DoneTaskRow,
  type InstanceRow,
  type TaskRow
} from '@/api/workflow'
import StatusDot from '@/components/StatusDot.vue'

const router = useRouter()

const tab = ref('todo')
const selected = ref<TaskRow[]>([])

const todo = reactive({ page: 1, size: 20, total: 0, rows: [] as TaskRow[], loading: false })
const done = reactive({ page: 1, size: 20, total: 0, rows: [] as DoneTaskRow[], loading: false })
const started = reactive({ page: 1, size: 20, total: 0, rows: [] as InstanceRow[], loading: false })

const action = reactive<{
  visible: boolean
  mode: 'complete' | 'reject' | 'addSigner'
  task: TaskRow | null
  comment: string
  targetActivityId: string
  signerId: string
  activities: ActivityRow[]
  users: ActorUserRow[]
  userLoading: boolean
  submitting: boolean
}>({
  visible: false,
  mode: 'complete',
  task: null,
  comment: '',
  targetActivityId: '',
  signerId: '',
  activities: [],
  users: [],
  userLoading: false,
  submitting: false
})

const actionTitle = computed(() => {
  if (action.mode === 'reject') {
    return '驳回任务'
  }
  return action.mode === 'addSigner' ? '加签' : '审批通过'
})

/** 驳回目标：已走过的节点，剔除当前节点自身。 */
const rejectOptions = computed(() =>
  action.activities.filter((item) => item.key !== action.task?.taskKey)
)

function objectText(code?: string | null, name?: string | null): string {
  if (!code && !name) {
    return '-'
  }
  return `${code ?? ''} ${name ?? ''}`.trim()
}

function formatTime(value?: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function formatDuration(ms?: number | null): string {
  if (ms === null || ms === undefined) {
    return '-'
  }
  const minutes = Math.floor(ms / 60000)
  if (minutes < 60) {
    return `${minutes} 分钟`
  }
  const hours = Math.floor(minutes / 60)
  return hours < 24 ? `${hours} 小时` : `${Math.floor(hours / 24)} 天`
}

function statusLabel(status: string): string {
  switch (status) {
    case 'RUNNING':
      return '审批中'
    case 'COMPLETED':
      return '已完成'
    case 'TERMINATED':
      return '已终止'
    default:
      return status
  }
}

function statusTone(status: string): 'ok' | 'running' | 'danger' | 'muted' {
  switch (status) {
    case 'COMPLETED':
      return 'ok'
    case 'RUNNING':
      return 'running'
    case 'TERMINATED':
      return 'danger'
    default:
      return 'muted'
  }
}

async function loadTodo(): Promise<void> {
  todo.loading = true
  try {
    const page = await workflowApi.todo(todo.page, todo.size)
    todo.rows = page.items
    todo.total = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载待办失败')
  } finally {
    todo.loading = false
  }
}

async function loadDone(): Promise<void> {
  done.loading = true
  try {
    const page = await workflowApi.done(done.page, done.size)
    done.rows = page.items
    done.total = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载已办失败')
  } finally {
    done.loading = false
  }
}

async function loadStarted(): Promise<void> {
  started.loading = true
  try {
    const page = await workflowApi.started(started.page, started.size)
    started.rows = page.items
    started.total = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载我发起的失败')
  } finally {
    started.loading = false
  }
}

function reloadTodo(): void {
  todo.page = 1
  loadTodo()
}

function reloadDone(): void {
  done.page = 1
  loadDone()
}

function reloadStarted(): void {
  started.page = 1
  loadStarted()
}

function reload(): void {
  if (tab.value === 'done') {
    loadDone()
  } else if (tab.value === 'started') {
    loadStarted()
  } else {
    loadTodo()
  }
}

function onTabChange(name: string | number): void {
  if (name === 'done') {
    loadDone()
  } else if (name === 'started') {
    loadStarted()
  } else {
    loadTodo()
  }
}

function onSelectionChange(rows: TaskRow[]): void {
  selected.value = rows
}

function resetAction(mode: 'complete' | 'reject' | 'addSigner', task: TaskRow): void {
  action.mode = mode
  action.task = task
  action.comment = ''
  action.targetActivityId = ''
  action.signerId = ''
  action.activities = []
  action.visible = true
}

function openComplete(task: TaskRow): void {
  resetAction('complete', task)
}

async function openReject(task: TaskRow): Promise<void> {
  resetAction('reject', task)
  try {
    action.activities = await workflowApi.activities(task.processInstanceId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载可驳回节点失败')
  }
}

async function searchUsers(keyword: string): Promise<void> {
  action.userLoading = true
  try {
    action.users = await workflowApi.actorUsers(keyword)
  } catch {
    action.users = []
  } finally {
    action.userLoading = false
  }
}

function openAddSigner(task: TaskRow): void {
  resetAction('addSigner', task)
  searchUsers('')
}

async function submitAction(): Promise<void> {
  const task = action.task
  if (!task) {
    return
  }
  if (action.mode === 'reject' && !action.targetActivityId) {
    ElMessage.warning('请选择驳回目标节点')
    return
  }
  if (action.mode === 'addSigner' && !action.signerId) {
    ElMessage.warning('请选择加签人')
    return
  }
  action.submitting = true
  try {
    if (action.mode === 'complete') {
      await workflowApi.complete(task.taskId, action.comment || undefined)
      ElMessage.success('已通过')
    } else if (action.mode === 'reject') {
      await workflowApi.reject(task.taskId, action.targetActivityId, action.comment || undefined)
      ElMessage.success('已驳回')
    } else {
      await workflowApi.addSigner(task.taskId, action.signerId, action.comment || undefined)
      ElMessage.success('已加签')
    }
    action.visible = false
    await loadTodo()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  } finally {
    action.submitting = false
  }
}

async function openBatch(): Promise<void> {
  if (selected.value.length === 0) {
    return
  }
  let comment = ''
  try {
    const result = await ElMessageBox.prompt(
      `将对选中的 ${selected.value.length} 条任务执行「通过」，可填写统一意见`,
      '批量通过',
      { inputType: 'textarea', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    comment = result.value ?? ''
  } catch {
    return
  }
  try {
    const result = await workflowApi.batchComplete(
      selected.value.map((row) => row.taskId),
      comment || undefined
    )
    if (result.failed > 0) {
      ElMessage.warning(`成功 ${result.succeeded} 条，失败 ${result.failed} 条`)
    } else {
      ElMessage.success(`已通过 ${result.succeeded} 条`)
    }
    await loadTodo()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量办理失败')
  }
}

async function onWithdraw(row: InstanceRow): Promise<void> {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('撤回后流程将终止，请填写撤回原因', '撤回流程', {
      inputType: 'textarea',
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消'
    })
    reason = result.value ?? ''
  } catch {
    return
  }
  try {
    await workflowApi.withdraw(row.processInstanceId, reason || undefined)
    ElMessage.success('已撤回')
    await loadStarted()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '撤回失败')
  }
}

function openInstance(processInstanceId: string): void {
  router.push({ name: 'workflow-instance', params: { id: processInstanceId } })
}

function goDelegate(): void {
  router.push({ name: 'workflow-delegate' })
}

onMounted(loadTodo)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.hint {
  color: #6b7a90;
  font-size: 13px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
