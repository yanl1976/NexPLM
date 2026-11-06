<template>
  <div class="page">
    <div class="toolbar">
      <el-button link type="primary" @click="back">← 返回项目列表</el-button>
      <span class="prj-title">{{ board?.projectCode }} {{ board?.projectName }}</span>
      <StatusDot
        :tone="board?.ready ? 'ok' : 'danger'"
        :label="board?.ready ? '交付物齐套' : `缺 ${board?.missingTotal ?? 0} 项`"
      />
      <el-button @click="load">刷新</el-button>
      <span class="hint">齐套只看「必需输出」：对象已删除/已废弃算缺件，未发布只提醒不拦提交</span>
    </div>

    <div class="stat-row">
      <el-card shadow="never" class="stat">
        <div class="stat-label">必需交付物</div>
        <div class="stat-value">{{ board?.requiredTotal ?? 0 }}</div>
      </el-card>
      <el-card shadow="never" class="stat">
        <div class="stat-label">已齐备</div>
        <div class="stat-value success">{{ board?.readyTotal ?? 0 }}</div>
      </el-card>
      <el-card shadow="never" class="stat">
        <div class="stat-label">缺件（缺失/废弃）</div>
        <div class="stat-value" :class="{ danger: (board?.missingTotal ?? 0) > 0 }">
          {{ board?.missingTotal ?? 0 }}
        </div>
      </el-card>
      <el-card shadow="never" class="stat">
        <div class="stat-label">未发布（提醒）</div>
        <div class="stat-value" :class="{ warning: (board?.unreleasedTotal ?? 0) > 0 }">
          {{ board?.unreleasedTotal ?? 0 }}
        </div>
      </el-card>
    </div>

    <el-table v-loading="loading" :data="board?.tasks ?? []" row-key="taskId" border>
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand">
            <el-table v-if="row.items.length" :data="row.items" size="small" border>
              <el-table-column label="方向" width="80" align="center">
                <template #default="{ row: item }">
                  <el-tag :type="item.outputType === 'OUTPUT' ? 'primary' : 'info'" size="small">
                    {{ DELIVERABLE_OUTPUT_LABEL[item.outputType] ?? item.outputType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="100" align="center">
                <template #default="{ row: item }">
                  {{ DELIVERABLE_OBJECT_LABEL[item.objectType] ?? item.objectType }}
                </template>
              </el-table-column>
              <el-table-column label="编码" width="160">
                <template #default="{ row: item }">{{ item.code || `#${item.objectId}` }}</template>
              </el-table-column>
              <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
              <el-table-column label="对象状态" width="110" align="center">
                <template #default="{ row: item }">
                  <StatusDot :tone="stateTone(item.state)" :label="DELIVERABLE_STATE_LABEL[item.state] ?? item.state" />
                </template>
              </el-table-column>
              <el-table-column label="齐套必需" width="100" align="center">
                <template #default="{ row: item }">
                  <el-switch
                    v-if="item.outputType === 'OUTPUT'"
                    :model-value="item.required"
                    :disabled="!canWrite"
                    size="small"
                    @change="(value: string | number | boolean) => onRequiredChange(item, Boolean(value))"
                  />
                  <span v-else class="sub">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
              <el-table-column v-if="canWrite" label="操作" width="90" align="center">
                <template #default="{ row: item }">
                  <el-button link type="danger" @click="onRemove(item)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无交付物" :image-size="48" />
            <div v-if="canWrite" class="expand-actions">
              <el-button type="primary" plain size="small" @click="openAdd(row)">
                给「{{ row.wbsCode }} {{ row.taskName }}」挂交付物
              </el-button>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="wbsCode" label="WBS" width="90" />
      <el-table-column prop="taskName" label="任务名称" min-width="220" show-overflow-tooltip />
      <el-table-column label="任务状态" width="100" align="center">
        <template #default="{ row }">
          <StatusDot :tone="statusTone(row.status)" :label="TASK_STATUS_LABEL[row.status] ?? row.status" />
        </template>
      </el-table-column>
      <el-table-column label="必需输出" width="110" align="center">
        <template #default="{ row }">{{ row.readyCount }} / {{ row.requiredCount }}</template>
      </el-table-column>
      <el-table-column label="齐套情况" width="150" align="center">
        <template #default="{ row }">
          <StatusDot :tone="completenessTone(row)" :label="completenessText(row)" />
        </template>
      </el-table-column>
      <el-table-column v-if="canWrite" label="操作" width="110" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAdd(row)">添加交付物</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !(board?.tasks.length ?? 0)" description="该项目还没有 WBS 任务" :image-size="60" />

    <el-dialog v-model="dialogVisible" title="挂接交付物" width="560px">
      <el-form label-width="90px">
        <el-form-item label="所属任务">
          <span>{{ targetTask?.wbsCode }} {{ targetTask?.taskName }}</span>
        </el-form-item>
        <el-form-item label="对象类型" required>
          <el-radio-group v-model="form.objectType" @change="onObjectTypeChange">
            <el-radio-button v-for="(label, value) in DELIVERABLE_OBJECT_LABEL" :key="value" :value="value">
              {{ label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="选择对象" required>
          <el-select
            v-model="form.objectId"
            filterable
            remote
            :remote-method="searchCandidates"
            :loading="candidateLoading"
            placeholder="输入编码或名称搜索"
            style="width: 100%"
          >
            <el-option
              v-for="item in candidates"
              :key="item.objectId"
              :label="`${item.code} ${item.name ?? ''}`"
              :value="item.objectId"
            >
              <span>{{ item.code }}</span>
              <span class="option-sub">{{ item.name }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="交付方向">
          <el-radio-group v-model="form.outputType">
            <el-radio-button value="OUTPUT">输出（要产出）</el-radio-button>
            <el-radio-button value="INPUT">输入（要依赖）</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="齐套必需">
          <el-switch v-model="form.required" :disabled="form.outputType === 'INPUT'" />
          <span class="hint">仅输出项参与齐套判定</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="500" show-word-limit type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deliverableApi,
  DELIVERABLE_OBJECT_LABEL,
  DELIVERABLE_OUTPUT_LABEL,
  DELIVERABLE_STATE_LABEL,
  TASK_STATUS_LABEL,
  type DeliverableBoard,
  type DeliverableCandidate,
  type DeliverableObjectType,
  type DeliverableOutputType,
  type DeliverableRow,
  type TaskCompleteness,
  type TaskStatus
} from '@/api/project'
import StatusDot from '@/components/StatusDot.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const canWrite = computed(() => userStore.can('prj.project.write'))

const projectId = String(route.params.id ?? '')
const loading = ref(false)
const board = ref<DeliverableBoard | null>(null)

const dialogVisible = ref(false)
const saving = ref(false)
const candidateLoading = ref(false)
const candidates = ref<DeliverableCandidate[]>([])
const targetTask = ref<TaskCompleteness | null>(null)
const form = reactive<{
  objectType: DeliverableObjectType
  objectId: string
  outputType: DeliverableOutputType
  required: boolean
  remark: string
}>({
  objectType: 'DOC',
  objectId: '',
  outputType: 'OUTPUT',
  required: true,
  remark: ''
})

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    board.value = await deliverableApi.board(projectId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载交付物失败')
  } finally {
    loading.value = false
  }
}

function back(): void {
  router.push('/project')
}

function openAdd(task: TaskCompleteness): void {
  targetTask.value = task
  form.objectType = 'DOC'
  form.objectId = ''
  form.outputType = 'OUTPUT'
  form.required = true
  form.remark = ''
  candidates.value = []
  dialogVisible.value = true
  void searchCandidates('')
}

function onObjectTypeChange(): void {
  form.objectId = ''
  candidates.value = []
  void searchCandidates('')
}

async function searchCandidates(keyword: string): Promise<void> {
  candidateLoading.value = true
  try {
    candidates.value = await deliverableApi.candidates(form.objectType, keyword, 50)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '搜索对象失败')
  } finally {
    candidateLoading.value = false
  }
}

async function onSave(): Promise<void> {
  if (!targetTask.value) {
    return
  }
  if (!form.objectId) {
    ElMessage.warning('请选择要挂接的对象')
    return
  }
  saving.value = true
  try {
    board.value = await deliverableApi.add(targetTask.value.taskId, {
      objectType: form.objectType,
      objectId: form.objectId,
      outputType: form.outputType,
      required: form.outputType === 'INPUT' ? false : form.required,
      remark: form.remark || null
    })
    ElMessage.success('交付物已挂接')
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '挂接失败')
  } finally {
    saving.value = false
  }
}

async function onRequiredChange(item: DeliverableRow, value: boolean): Promise<void> {
  try {
    board.value = await deliverableApi.update(item.id, { required: value })
    ElMessage.success(value ? '已设为齐套必需' : '已取消齐套必需')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修改失败')
    await load()
  }
}

async function onRemove(item: DeliverableRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认移除交付物「${item.code ?? item.objectId}」？`, '移除交付物', {
      confirmButtonText: '移除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    board.value = await deliverableApi.remove(item.id)
    ElMessage.success('已移除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移除失败')
  }
}

function completenessText(task: TaskCompleteness): string {
  if (task.missingCount > 0) {
    return `缺 ${task.missingCount} 项`
  }
  if (task.requiredCount === 0) {
    return '无强制交付物'
  }
  if (task.unreleasedCount > 0) {
    return `齐套（${task.unreleasedCount} 项未发布）`
  }
  return '齐套'
}

function completenessTone(task: TaskCompleteness): 'ok' | 'warn' | 'danger' | 'muted' {
  if (task.missingCount > 0) {
    return 'danger'
  }
  if (task.requiredCount === 0) {
    return 'muted'
  }
  return task.unreleasedCount > 0 ? 'warn' : 'ok'
}

function stateTone(state: string): 'ok' | 'warn' | 'danger' {
  if (state === 'OK') {
    return 'ok'
  }
  if (state === 'UNRELEASED') {
    return 'warn'
  }
  return 'danger'
}

function statusTone(value: TaskStatus): 'ok' | 'warn' | 'danger' | 'running' | 'muted' {
  if (value === 'DONE') {
    return 'ok'
  }
  if (value === 'SUBMITTED') {
    return 'warn'
  }
  if (value === 'BLOCKED') {
    return 'danger'
  }
  return value === 'IN_PROGRESS' ? 'running' : 'muted'
}
</script>

<style scoped>
.stat-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.stat {
  flex: 1;
}

.stat-label {
  color: #909399;
  font-size: 12px;
}

.stat-value {
  margin-top: 4px;
  font-size: 22px;
  font-weight: 600;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.danger {
  color: #f56c6c;
}

.stat-value.warning {
  color: #e6a23c;
}

.expand {
  padding: 8px 16px 12px;
  background: #fafafa;
}

.expand-actions {
  margin-top: 8px;
  text-align: right;
}

.option-sub {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
