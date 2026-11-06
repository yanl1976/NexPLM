<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">变更管理</span>
          <el-button type="primary" @click="openCreate">新建变更申请</el-button>
        </div>
      </template>

      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="搜索编码或标题"
          clearable
          style="width: 240px"
          @keyup.enter="reload"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px">
          <el-option v-for="(label, key) in STATUS_LABEL" :key="key" :label="label" :value="key" />
        </el-select>
        <el-button type="primary" @click="reload">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="code" label="变更编码" width="140" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ TYPE_LABEL[row.changeType] ?? row.changeType }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.priority === 'HIGH' ? 'danger' : row.priority === 'LOW' ? 'info' : ''">
              {{ PRIORITY_LABEL[row.priority] ?? row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ STATUS_LABEL[row.status] ?? row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请人" width="120">
          <template #default="{ row }">{{ row.requesterName ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="affectedCount" label="受影响" width="90" align="center" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无变更申请" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @current-change="load"
        @size-change="reload"
      />
    </el-card>

    <el-drawer v-model="detailVisible" title="变更申请详情" size="62%" @closed="detail = null">
      <div v-if="detail" v-loading="detailLoading">
        <div class="detail-head">
          <span class="code">{{ detail.code }}</span>
          <el-tag size="small" :type="statusTag(detail.status)">
            {{ STATUS_LABEL[detail.status] ?? detail.status }}
          </el-tag>
          <div class="actions">
            <el-button v-if="detail.status === 'DRAFT'" size="small" @click="openEdit">编辑</el-button>
            <el-button v-if="detail.status === 'DRAFT'" size="small" type="primary" @click="onSubmit">提交评审</el-button>
            <el-button v-if="detail.objects.length" size="small" @click="openImpact">影响分析</el-button>
            <el-button v-if="EXECUTABLE.includes(detail.status)" size="small" type="success" @click="onExecute">
              执行变更
            </el-button>
            <el-button v-if="CLOSABLE.includes(detail.status)" size="small" type="warning" @click="onClose">
              关闭
            </el-button>
          </div>
        </div>

        <el-descriptions :column="2" border class="block">
          <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="变更类型">{{ TYPE_LABEL[detail.changeType] ?? detail.changeType }}</el-descriptions-item>
          <el-descriptions-item label="优先级">{{ PRIORITY_LABEL[detail.priority] ?? detail.priority }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.requesterName ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收人">{{ detail.receiverName ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目号">{{ detail.projectNo ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="期望完成">{{ detail.targetDate ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="变更原因" :span="2">{{ detail.reason ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="可行性" :span="2">{{ detail.feasibility ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(detail.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <div class="section">
          <div class="section-title">审批流程</div>
          <WorkflowPanel
            v-if="detail.id"
            object-type="ChgEcr"
            :object-id="detail.id"
            hide-start
            @changed="onWorkflowChanged"
          />
        </div>

        <div class="section">
          <div class="section-title">关联对象</div>
          <el-table :data="detail.objects" border size="small">
            <el-table-column label="关系" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.relation === 'AFFECTED' ? 'danger' : row.relation === 'IMPACT' ? 'warning' : 'info'">
                  {{ RELATION_LABEL[row.relation] ?? row.relation }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="objectType" label="类型" width="110" />
            <el-table-column label="对象" min-width="200">
              <template #default="{ row }">
                {{ row.objectCode ?? row.objectId }} {{ row.objectName ?? '' }}
              </template>
            </el-table-column>
            <el-table-column label="意图" width="100">
              <template #default="{ row }">{{ ACTION_LABEL[row.action] ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="当前版本" width="90">
              <template #default="{ row }">v{{ row.revisionFrom ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="执行" width="90">
              <template #default="{ row }">{{ EXEC_LABEL[row.execStatus] ?? row.execStatus }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button link type="danger" @click="onRemoveObject(row)">移除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="还没有关联对象" />
            </template>
          </el-table>

          <div class="add-object">
            <el-select v-model="objForm.objectType" style="width: 130px">
              <el-option label="零部件" value="PdmItem" />
              <el-option label="图文档" value="PdmDoc" />
            </el-select>
            <el-input v-model="objForm.objectId" placeholder="对象 ID" style="width: 200px" />
            <el-select v-model="objForm.relation" style="width: 130px" @change="onRelationChange">
              <el-option label="受影响" value="AFFECTED" />
              <el-option label="影响" value="IMPACT" />
              <el-option label="参考" value="REFERENCE" />
            </el-select>
            <el-select
              v-model="objForm.action"
              :disabled="objForm.relation !== 'AFFECTED'"
              placeholder="操作意图"
              style="width: 130px"
            >
              <el-option label="升版" value="REVISE" />
              <el-option label="废弃" value="OBSOLETE" />
              <el-option label="发布" value="RELEASE" />
            </el-select>
            <el-button type="primary" @click="onAddObject">添加</el-button>
          </div>
        </div>

        <div class="section">
          <div class="section-head">
            <span class="section-title">变更通知</span>
            <el-button
              v-if="EXECUTABLE.includes(detail.status)"
              size="small"
              type="primary"
              @click="openEcnDialog"
            >
              下发通知
            </el-button>
          </div>
          <el-table :data="detail.ecns" border size="small">
            <el-table-column prop="code" label="编码" width="140" />
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'CLOSED' ? 'success' : 'warning'">
                  {{ ECN_LABEL[row.status] ?? row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="executorName" label="执行人" width="110" />
            <el-table-column label="计划日期" width="110">
              <template #default="{ row }">{{ row.planDate ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="onIssueEcn(row)">签发</el-button>
                <el-button v-if="row.status !== 'CLOSED'" link type="primary" @click="openProblems(row)">
                  问题
                </el-button>
                <el-button
                  v-if="row.status === 'ISSUED' || row.status === 'EXECUTING'"
                  link
                  type="warning"
                  @click="onCloseEcn(row)"
                >
                  关闭
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="还没有变更通知" />
            </template>
          </el-table>
        </div>
        <div class="section">
          <div class="section-title">变更追踪</div>
          <el-timeline v-if="timelineRows.length">
            <el-timeline-item
              v-for="(item, index) in timelineRows"
              :key="index"
              :timestamp="formatTime(item.time)"
              :type="stageType(item.stage)"
              placement="top"
            >
              <div class="tl-title">{{ item.title }}</div>
              <div v-if="item.actorName" class="tl-meta">操作人：{{ item.actorName }}</div>
              <div v-if="item.detail" class="tl-meta">{{ item.detail }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无追踪记录" />
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="ecnDialogVisible" title="下发变更通知" width="520px">
      <el-form label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="ecnForm.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="计划日期">
          <el-date-picker v-model="ecnForm.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="ecnForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ecnDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onCreateEcn">下发</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="problemDialogVisible"
      :title="`变更问题 · ${currentEcn?.code ?? ''}`"
      width="700px"
    >
      <div class="add-object">
        <el-input v-model="problemForm.description" placeholder="问题描述" style="flex: 1" />
        <el-date-picker
          v-model="problemForm.dueDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="要求关闭日期"
          style="width: 170px"
        />
        <el-button type="primary" @click="onAddProblem">登记</el-button>
      </div>
      <el-table :data="problems" border size="small" style="margin-top: 12px">
        <el-table-column prop="code" label="编码" width="130" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'CLOSED' ? 'success' : 'warning'">
              {{ row.status === 'CLOSED' ? '已关闭' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="要求关闭" width="120">
          <template #default="{ row }">{{ row.dueDate ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'CLOSED'" link type="primary" @click="onCloseProblem(row)">
              关闭
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无问题" />
        </template>
      </el-table>
      <template #footer>
        <el-button @click="problemDialogVisible = false">关闭窗口</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="impactVisible" title="影响分析（BOM）" width="840px">
      <div class="add-object">
        <span class="label">展开层数</span>
        <el-select v-model="impactDepth" style="width: 110px" @change="loadImpact">
          <el-option :value="1" label="1 层" />
          <el-option :value="2" label="2 层" />
          <el-option :value="3" label="3 层" />
          <el-option :value="5" label="5 层" />
        </el-select>
        <span class="hint">从「受影响」对象出发：向下=组成件，向上=被哪些装配使用</span>
      </div>
      <el-table v-loading="impactLoading" :data="impactRows" border size="small" style="margin-top: 10px">
        <el-table-column label="方向" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.direction === 'DOWN' ? 'info' : 'warning'">
              {{ row.direction === 'DOWN' ? '向下' : '向上' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="depth" label="层级" width="70" align="center" />
        <el-table-column prop="objectType" label="类型" width="110" />
        <el-table-column label="对象" min-width="200">
          <template #default="{ row }">{{ row.code }} {{ row.name }}</template>
        </el-table-column>
        <el-table-column prop="lifecycleState" label="状态" width="110" />
        <el-table-column label="版本" width="80">
          <template #default="{ row }">v{{ row.revision }}.{{ row.iteration }}</template>
        </el-table-column>
        <el-table-column label="已关联" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.linked" size="small" type="success">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="该对象没有 BOM 关联件" />
        </template>
      </el-table>
      <template #footer>
        <el-button @click="impactVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="formVisible" :title="editing ? '编辑变更申请' : '新建变更申请'" width="640px">
      <el-form label-width="96px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="变更类型" required>
          <el-radio-group v-model="form.changeType">
            <el-radio value="DESIGN">设计变更</el-radio>
            <el-radio value="PROCESS">工艺变更</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width: 100%">
            <el-option label="高" value="HIGH" />
            <el-option label="普通" value="NORMAL" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收人">
          <el-select
            v-model="form.receiverId"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="变更专员，提交后由其审批"
            :remote-method="searchUsers"
            :loading="userLoading"
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
        <el-form-item label="项目号">
          <el-input v-model="form.projectNo" placeholder="可选，如 S2026005" />
        </el-form-item>
        <el-form-item label="期望完成">
          <el-date-picker v-model="form.targetDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="变更原因">
          <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="可行性">
          <el-input v-model="form.feasibility" type="textarea" :rows="2" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  changeApi,
  type ChangeObjectRow,
  type EcnRow,
  type EcrDetail,
  type EcrRow,
  type EcrSaveReq,
  type ImpactRow,
  type ProblemRow,
  type TimelineEvent
} from '@/api/change'
import { workflowApi, type ActorUserRow } from '@/api/workflow'
import WorkflowPanel from '@/components/WorkflowPanel.vue'

const STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  REVIEWING: '评审中',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  NOTICED: '已通知',
  EXECUTING: '执行中',
  CLOSED: '已关闭',
  CANCELLED: '已取消'
}
const TYPE_LABEL: Record<string, string> = { DESIGN: '设计变更', PROCESS: '工艺变更' }
const PRIORITY_LABEL: Record<string, string> = { HIGH: '高', NORMAL: '普通', LOW: '低' }
const RELATION_LABEL: Record<string, string> = { AFFECTED: '受影响', IMPACT: '影响', REFERENCE: '参考' }
const ACTION_LABEL: Record<string, string> = { REVISE: '升版', OBSOLETE: '废弃', RELEASE: '发布' }
const EXEC_LABEL: Record<string, string> = { PENDING: '待执行', DONE: '已完成', SKIPPED: '已跳过' }
const CLOSABLE = ['APPROVED', 'NOTICED', 'EXECUTING']
const EXECUTABLE = ['APPROVED', 'NOTICED', 'EXECUTING']
const ECN_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  ISSUED: '已签发',
  EXECUTING: '执行中',
  CLOSED: '已关闭'
}

const rows = ref<EcrRow[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', status: '', page: 1, size: 20 })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<EcrDetail | null>(null)
const timelineRows = ref<TimelineEvent[]>([])

const formVisible = ref(false)
const saving = ref(false)
const editing = ref(false)
const form = reactive<EcrSaveReq>({
  title: '',
  changeType: 'DESIGN',
  priority: 'NORMAL',
  receiverId: null,
  projectNo: '',
  targetDate: null,
  reason: '',
  feasibility: ''
})
/** 接收人（审批人）选人列表。 */
const users = ref<ActorUserRow[]>([])
const userLoading = ref(false)
const objForm = reactive<{ objectType: string; objectId: string; relation: string; action: string }>({
  objectType: 'PdmItem',
  objectId: '',
  relation: 'AFFECTED',
  action: 'REVISE'
})

const ecnDialogVisible = ref(false)
const ecnForm = reactive<{ title: string; planDate: string | null; remark: string }>({
  title: '',
  planDate: null,
  remark: ''
})
const problemDialogVisible = ref(false)
const currentEcn = ref<EcnRow | null>(null)
const problems = ref<ProblemRow[]>([])
const problemForm = reactive<{ description: string; dueDate: string | null }>({
  description: '',
  dueDate: null
})
const impactVisible = ref(false)
const impactLoading = ref(false)
const impactDepth = ref(3)
const impactRows = ref<ImpactRow[]>([])

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function statusTag(status: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (status) {
    case 'CLOSED':
      return 'success'
    case 'APPROVED':
    case 'NOTICED':
    case 'EXECUTING':
      return 'warning'
    case 'REJECTED':
    case 'CANCELLED':
      return 'danger'
    default:
      return 'info'
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await changeApi.list({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      page: query.page,
      size: query.size
    })
    rows.value = page.items
    total.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载变更申请失败')
  } finally {
    loading.value = false
  }
}

function reload(): void {
  query.page = 1
  load()
}

function resetQuery(): void {
  query.keyword = ''
  query.status = ''
  reload()
}

async function openDetail(id: string): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const [ecr, timeline] = await Promise.all([changeApi.detail(id), changeApi.timeline(id)])
    detail.value = ecr
    timelineRows.value = timeline
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

/** 时间线节点配色：批准/执行绿、驳回/撤回红、通知/问题橙、关闭灰。 */
function stageType(stage: TimelineEvent['stage']): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  switch (stage) {
    case 'APPROVE':
    case 'EXECUTE':
      return 'success'
    case 'REJECT':
    case 'WITHDRAW':
      return 'danger'
    case 'NOTICE':
    case 'PROBLEM':
      return 'warning'
    case 'CLOSE':
      return 'info'
    default:
      return 'primary'
  }
}

async function searchUsers(keyword: string): Promise<void> {
  userLoading.value = true
  try {
    users.value = await workflowApi.actorUsers(keyword)
  } catch {
    users.value = []
  } finally {
    userLoading.value = false
  }
}

function openCreate(): void {
  editing.value = false
  Object.assign(form, {
    title: '',
    changeType: 'DESIGN',
    priority: 'NORMAL',
    receiverId: null,
    projectNo: '',
    targetDate: null,
    reason: '',
    feasibility: ''
  })
  formVisible.value = true
  searchUsers('')
}

function openEdit(): void {
  if (!detail.value) {
    return
  }
  editing.value = true
  Object.assign(form, {
    title: detail.value.title,
    changeType: detail.value.changeType,
    priority: detail.value.priority,
    receiverId: detail.value.receiverId ?? null,
    projectNo: detail.value.projectNo ?? '',
    targetDate: detail.value.targetDate ?? null,
    reason: detail.value.reason ?? '',
    feasibility: detail.value.feasibility ?? ''
  })
  formVisible.value = true
  searchUsers('')
}

async function onSave(): Promise<void> {
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  try {
    if (editing.value && detail.value) {
      detail.value = await changeApi.update(detail.value.id, { ...form })
      ElMessage.success('已保存')
    } else {
      const created = await changeApi.create({ ...form })
      ElMessage.success(`已创建 ${created.code}`)
      detail.value = created
      detailVisible.value = true
    }
    formVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onSubmit(): Promise<void> {
  if (!detail.value) {
    return
  }
  if (!detail.value.receiverId) {
    ElMessage.warning('请先在「编辑」中指定接收人（变更专员），提交后由其审批')
    return
  }
  try {
    await ElMessageBox.confirm(
      '提交后主档不可再修改，并立即发起审批流程（接收人初审+会签），确认提交？',
      '提交变更申请',
      { confirmButtonText: '提交', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const ecrId = detail.value.id
  try {
    await changeApi.submit(ecrId)
    ElMessage.success('已提交并发起审批')
    await openDetail(ecrId)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

/** 流程办理/撤回后刷新详情（ECR 状态由流程插件回写，需重新拉取）。 */
async function onWorkflowChanged(): Promise<void> {
  if (detail.value) {
    await openDetail(detail.value.id)
  }
  await load()
}

async function onClose(): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm('关闭后不可再变更，确认关闭？', '关闭变更', {
      type: 'warning',
      confirmButtonText: '关闭',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    detail.value = await changeApi.close(detail.value.id)
    ElMessage.success('已关闭')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '关闭失败')
  }
}

function onRelationChange(): void {
  if (objForm.relation !== 'AFFECTED') {
    objForm.action = ''
  } else if (!objForm.action) {
    objForm.action = 'REVISE'
  }
}

async function onAddObject(): Promise<void> {
  if (!detail.value) {
    return
  }
  if (!objForm.objectId.trim()) {
    ElMessage.warning('请填写对象 ID')
    return
  }
  try {
    await changeApi.addObject(detail.value.id, {
      objectType: objForm.objectType,
      objectId: objForm.objectId.trim(),
      relation: objForm.relation,
      action: objForm.relation === 'AFFECTED' ? objForm.action : null
    })
    ElMessage.success('已添加')
    objForm.objectId = ''
    await openDetail(detail.value.id)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '添加失败')
  }
}

async function onRemoveObject(row: ChangeObjectRow): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确认移除该关联对象？', '移除关联对象', { type: 'warning' })
  } catch {
    return
  }
  try {
    await changeApi.removeObject(detail.value.id, row.id)
    ElMessage.success('已移除')
    await openDetail(detail.value.id)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移除失败')
  }
}

function openEcnDialog(): void {
  ecnForm.title = detail.value ? `${detail.value.code} 执行通知` : ''
  ecnForm.planDate = null
  ecnForm.remark = ''
  ecnDialogVisible.value = true
}

async function onCreateEcn(): Promise<void> {
  if (!detail.value) {
    return
  }
  if (!ecnForm.title.trim()) {
    ElMessage.warning('请填写通知标题')
    return
  }
  saving.value = true
  try {
    const ecn = await changeApi.createEcn(detail.value.id, {
      title: ecnForm.title.trim(),
      planDate: ecnForm.planDate,
      remark: ecnForm.remark || null
    })
    ElMessage.success(`已下发 ${ecn.code}`)
    ecnDialogVisible.value = false
    await openDetail(detail.value.id)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下发失败')
  } finally {
    saving.value = false
  }
}

async function onIssueEcn(row: EcnRow): Promise<void> {
  try {
    await changeApi.issueEcn(row.id)
    ElMessage.success('已签发')
    if (detail.value) {
      await openDetail(detail.value.id)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '签发失败')
  }
}

async function onCloseEcn(row: EcnRow): Promise<void> {
  try {
    await ElMessageBox.confirm('关闭前需确认所有变更问题已关闭，继续？', '关闭变更通知', { type: 'warning' })
  } catch {
    return
  }
  try {
    await changeApi.closeEcn(row.id)
    ElMessage.success('已关闭')
    if (detail.value) {
      await openDetail(detail.value.id)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '关闭失败')
  }
}

async function openProblems(row: EcnRow): Promise<void> {
  currentEcn.value = row
  problemDialogVisible.value = true
  problemForm.description = ''
  problemForm.dueDate = null
  await loadProblems(row.id)
}

async function loadProblems(ecnId: string): Promise<void> {
  try {
    problems.value = await changeApi.problems(ecnId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载变更问题失败')
  }
}

async function onAddProblem(): Promise<void> {
  if (!currentEcn.value) {
    return
  }
  if (!problemForm.description.trim()) {
    ElMessage.warning('请填写问题描述')
    return
  }
  try {
    await changeApi.addProblem(currentEcn.value.id, {
      description: problemForm.description.trim(),
      dueDate: problemForm.dueDate
    })
    problemForm.description = ''
    problemForm.dueDate = null
    await loadProblems(currentEcn.value.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登记失败')
  }
}

async function onCloseProblem(row: ProblemRow): Promise<void> {
  if (!currentEcn.value) {
    return
  }
  try {
    await changeApi.closeProblem(row.id)
    ElMessage.success('问题已关闭')
    await loadProblems(currentEcn.value.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '关闭失败')
  }
}

async function openImpact(): Promise<void> {
  impactVisible.value = true
  await loadImpact()
}

async function loadImpact(): Promise<void> {
  if (!detail.value) {
    return
  }
  impactLoading.value = true
  try {
    impactRows.value = await changeApi.impact(detail.value.id, impactDepth.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '影响分析失败')
  } finally {
    impactLoading.value = false
  }
}

async function onExecute(): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '将按操作意图对「受影响」对象执行升版 / 废弃 / 发布，执行后不可撤销，确认？',
      '执行变更',
      { type: 'warning', confirmButtonText: '执行', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    const result = await changeApi.execute(detail.value.id)
    ElMessage.success(`执行完成：成功 ${result.succeeded} 个，跳过 ${result.skipped} 个`)
    await openDetail(detail.value.id)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '执行失败')
  }
}

onMounted(load)
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

.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-head .code {
  font-size: 16px;
  font-weight: 600;
}

.detail-head .actions {
  margin-left: auto;
}

.block {
  margin-bottom: 16px;
}

.section {
  margin-top: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid #2b4a6f;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.label {
  color: #606266;
  font-size: 13px;
}

.hint {
  color: #909399;
  font-size: 12px;
}

.add-object {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.tl-title {
  font-size: 13px;
  font-weight: 600;
}

.tl-meta {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
  word-break: break-all;
}
</style>
