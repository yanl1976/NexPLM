<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="编码/标题/物料" clearable style="width: 200px" @keyup.enter="reload" />
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px" @change="reload">
        <el-option v-for="(label, value) in ISSUE_STATUS_LABEL" :key="value" :label="label" :value="value" />
      </el-select>
      <el-select v-model="severity" placeholder="全部严重度" clearable style="width: 120px" @change="reload">
        <el-option v-for="(label, value) in ISSUE_SEVERITY_LABEL" :key="value" :label="label" :value="value" />
      </el-select>
      <el-checkbox v-model="overdueOnly" @change="reload">只看超期</el-checkbox>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">登记问题</el-button>
      <span class="hint">闭环：登记 → 分派 → 多轮整改 → 验证关闭；验证不通过会开启下一轮</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe @row-dblclick="(row: IssueRow) => openDetail(row.id)">
      <el-table-column prop="code" label="编码" width="120" />
      <el-table-column prop="title" label="问题" min-width="200" show-overflow-tooltip />
      <el-table-column label="来源" width="90" align="center">
        <template #default="{ row }">{{ ISSUE_SOURCE_LABEL[row.source] ?? row.source }}</template>
      </el-table-column>
      <el-table-column label="严重度" width="90" align="center">
        <template #default="{ row }">
          <StatusDot
            :tone="severityTone(row.severity)"
            :label="ISSUE_SEVERITY_LABEL[row.severity] ?? row.severity"
          />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <StatusDot :tone="statusTone(row.status)" :label="ISSUE_STATUS_LABEL[row.status] ?? row.status" />
        </template>
      </el-table-column>
      <el-table-column label="轮次" width="70" align="center">
        <template #default="{ row }">第 {{ row.roundNo }} 轮</template>
      </el-table-column>
      <el-table-column prop="ownerName" label="责任人" width="110">
        <template #default="{ row }">{{ row.ownerName || '-' }}</template>
      </el-table-column>
      <el-table-column label="期限" width="120">
        <template #default="{ row }">
          <span :class="{ 'danger-text': row.overdue }">{{ row.dueDate || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="措施" width="100" align="center">
        <template #default="{ row }">
          {{ row.actionCount - row.openActionCount }} / {{ row.actionCount }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="没有符合条件的质量问题" :image-size="60" />
    <el-pagination
      v-if="total > size"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPageChange"
    />

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="detail ? `${detail.issue.code} ${detail.issue.title}` : '质量问题'" size="760px">
      <div v-if="detail" class="detail-body">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="状态">
            <StatusDot
              :tone="statusTone(detail.issue.status)"
              :label="ISSUE_STATUS_LABEL[detail.issue.status] ?? detail.issue.status"
            />
            <span class="round-badge">第 {{ detail.issue.roundNo }} 轮</span>
          </el-descriptions-item>
          <el-descriptions-item label="严重度">
            {{ ISSUE_SEVERITY_LABEL[detail.issue.severity] ?? detail.issue.severity }}
          </el-descriptions-item>
          <el-descriptions-item label="来源">
            {{ ISSUE_SOURCE_LABEL[detail.issue.source] ?? detail.issue.source }}
          </el-descriptions-item>
          <el-descriptions-item label="责任人">{{ detail.issue.ownerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="期限">
            <span :class="{ 'danger-text': detail.issue.overdue }">{{ detail.issue.dueDate || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="登记人">{{ detail.issue.reportedByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联对象" :span="2">{{ detail.issue.objectCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="8D 模板" :span="2">{{ detail.issue.templateCode || '未应用' }}</el-descriptions-item>
          <el-descriptions-item label="验证意见" :span="2">{{ detail.issue.verifyNote || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="action-bar">
          <el-button v-if="canWrite && detail.issue.status !== 'CLOSED'" size="small" @click="openAssign">
            分派责任人
          </el-button>
          <el-button
            v-if="canWrite && ['OPEN', 'ASSIGNED', 'IN_PROGRESS'].includes(detail.issue.status)"
            size="small"
            type="primary"
            @click="onSubmit"
          >提交验证</el-button>
          <el-button
            v-if="canWrite && detail.issue.status === 'VERIFYING'"
            size="small"
            type="success"
            @click="onVerify(true)"
          >验证通过</el-button>
          <el-button
            v-if="canWrite && detail.issue.status === 'VERIFYING'"
            size="small"
            type="danger"
            plain
            @click="onVerify(false)"
          >验证不通过</el-button>
          <el-button
            v-if="canWrite && !['CLOSED', 'VERIFYING'].includes(detail.issue.status)"
            size="small"
            type="primary"
            plain
            @click="openAddAction"
          >新增措施</el-button>
          <el-button
            v-if="canWrite && !['CLOSED', 'VERIFYING'].includes(detail.issue.status)"
            size="small"
            @click="openApplyTemplate"
          >应用 8D 模板</el-button>
        </div>

        <div v-for="round in rounds" :key="round.roundNo" class="round-block">
          <div class="round-title">第 {{ round.roundNo }} 轮整改（{{ round.items.length }} 条）</div>
          <div v-for="item in round.items" :key="item.id" class="action-item">
            <el-checkbox
              :model-value="item.status === 'DONE'"
              :disabled="!canWrite || detail.issue.status === 'CLOSED' || detail.issue.status === 'VERIFYING'"
              @change="(value: boolean) => onToggle(item, value)"
            />
            <el-tag size="small" type="info">{{ ACTION_TYPE_LABEL[item.actionType] ?? item.actionType }}</el-tag>
            <span class="action-content" :class="{ done: item.status === 'DONE' }">{{ item.content }}</span>
            <span class="action-meta">{{ item.ownerName || '-' }} · {{ item.dueDate || '无期限' }}</span>
            <el-button
              v-if="canWrite && detail.issue.status !== 'CLOSED' && detail.issue.status !== 'VERIFYING'"
              link
              type="danger"
              @click="onRemoveAction(item)"
            >删除</el-button>
          </div>
          <div v-if="!round.items.length" class="sub">本轮暂无措施</div>
        </div>
      </div>
    </el-drawer>

    <!-- 登记/修改 -->
    <el-dialog v-model="formVisible" :title="form.id ? '修改质量问题' : '登记质量问题'" width="600px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="问题标题" required>
          <el-input v-model="form.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="form.source" style="width: 160px">
            <el-option v-for="(label, value) in ISSUE_SOURCE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <span class="label-gap">严重度</span>
          <el-select v-model="form.severity" style="width: 140px">
            <el-option v-for="(label, value) in ISSUE_SEVERITY_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人">
          <el-select v-model="form.ownerId" filterable clearable placeholder="可稍后分派" style="width: 100%">
            <el-option v-for="user in users" :key="user.id" :label="user.displayName" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="期限">
          <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 180px" />
        </el-form-item>
        <el-form-item label="关联对象">
          <el-input v-model="form.objectCode" maxlength="128" placeholder="物料编码/文档编号（可空）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分派 -->
    <el-dialog v-model="assignVisible" title="分派责任人" width="420px">
      <el-form label-width="80px">
        <el-form-item label="责任人" required>
          <el-select v-model="assignForm.ownerId" filterable style="width: 100%">
            <el-option v-for="user in users" :key="user.id" :label="user.displayName" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="期限">
          <el-date-picker v-model="assignForm.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="onAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增措施 -->
    <el-dialog v-model="actionVisible" title="新增整改措施" width="480px">
      <el-form label-width="80px">
        <el-form-item label="类型">
          <el-select v-model="actionForm.actionType" style="width: 100%">
            <el-option v-for="(label, value) in ACTION_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="actionForm.content" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="责任人">
          <el-select v-model="actionForm.ownerId" filterable clearable placeholder="默认同问题责任人" style="width: 100%">
            <el-option v-for="user in users" :key="user.id" :label="user.displayName" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="期限">
          <el-date-picker v-model="actionForm.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionSaving" @click="onAddAction">保存</el-button>
      </template>
    </el-dialog>

    <!-- 应用 8D 模板 -->
    <el-dialog v-model="templateVisible" title="应用 8D 模板" width="680px">
      <el-form label-width="90px">
        <el-form-item label="模板" required>
          <el-select
            v-model="templateId"
            filterable
            placeholder="选择启用的模板"
            style="width: 100%"
            @change="onTemplateChange"
          >
            <el-option
              v-for="item in templates"
              :key="item.id"
              :label="`${item.code} ${item.name}（${item.itemCount} 步）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="应用后会在本轮生成对应数量的措施（内容预填模板提示语），再逐条补实际内容；同一轮只能应用同一模板一次。"
      />
      <el-table v-if="templateDetail" :data="templateDetail.items" border size="small" class="template-table">
        <el-table-column prop="itemCode" label="编号" width="70" />
        <el-table-column prop="name" label="步骤" min-width="150" show-overflow-tooltip />
        <el-table-column label="措施类型" width="110">
          <template #default="{ row }">{{ ACTION_TYPE_LABEL[row.actionType] ?? row.actionType }}</template>
        </el-table-column>
        <el-table-column prop="prompt" label="预填提示语" min-width="200" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="templateVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" :disabled="!templateId" @click="onApplyTemplate">
          应用并生成措施
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  qualityIssueApi,
  qualityTemplateApi,
  ACTION_TYPE_LABEL,
  ISSUE_SEVERITY_LABEL,
  ISSUE_SOURCE_LABEL,
  ISSUE_STATUS_LABEL,
  type IssueActionRow,
  type IssueDetail,
  type IssueRow,
  type IssueSeverity,
  type IssueStatus,
  type TemplateDetail,
  type TemplateRow
} from '@/api/quality'
import { workflowApi, type ActorUserRow } from '@/api/workflow'
import StatusDot from '@/components/StatusDot.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('qlt.issue.write'))

const loading = ref(false)
const saving = ref(false)
const assigning = ref(false)
const actionSaving = ref(false)
const rows = ref<IssueRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const status = ref('')
const severity = ref('')
const overdueOnly = ref(false)
const users = ref<ActorUserRow[]>([])

const detailVisible = ref(false)
const detail = ref<IssueDetail | null>(null)

const formVisible = ref(false)
const form = reactive<{
  id: string
  title: string
  description: string
  source: string
  severity: string
  ownerId: string | null
  dueDate: string | null
  objectCode: string
  remark: string
}>({
  id: '',
  title: '',
  description: '',
  source: 'INTERNAL',
  severity: 'MAJOR',
  ownerId: null,
  dueDate: null,
  objectCode: '',
  remark: ''
})

const assignVisible = ref(false)
const assignForm = reactive<{ ownerId: string | null; dueDate: string | null }>({
  ownerId: null,
  dueDate: null
})

const actionVisible = ref(false)
const actionForm = reactive<{ actionType: string; content: string; ownerId: string | null; dueDate: string | null }>({
  actionType: 'CORRECTIVE',
  content: '',
  ownerId: null,
  dueDate: null
})

/** 应用 8D 模板：模板只给启用的，选中后先预览步骤再确认。 */
const templateVisible = ref(false)
const applying = ref(false)
const templates = ref<TemplateRow[]>([])
const templateId = ref('')
const templateDetail = ref<TemplateDetail | null>(null)

/** 按轮次分组（后端已按 round_no 排序）。 */
const rounds = computed(() => {
  const groups: Array<{ roundNo: number; items: IssueActionRow[] }> = []
  ;(detail.value?.actions ?? []).forEach((item) => {
    let group = groups.find((entry) => entry.roundNo === item.roundNo)
    if (!group) {
      group = { roundNo: item.roundNo, items: [] }
      groups.push(group)
    }
    group.items.push(item)
  })
  return groups
})

onMounted(async () => {
  await loadUsers()
  await reload()
})

async function loadUsers(): Promise<void> {
  try {
    users.value = await workflowApi.actorUsers(undefined, 100)
  } catch {
    users.value = []
  }
}

async function reload(): Promise<void> {
  loading.value = true
  try {
    const result = await qualityIssueApi.page({
      keyword: keyword.value || undefined,
      status: status.value || undefined,
      severity: severity.value || undefined,
      overdue: overdueOnly.value ? true : undefined,
      page: page.value,
      size: size.value
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载质量问题失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(value: number): void {
  page.value = value
  void reload()
}

async function openDetail(id: string): Promise<void> {
  try {
    detail.value = await qualityIssueApi.detail(id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  }
}

function openCreate(): void {
  form.id = ''
  form.title = ''
  form.description = ''
  form.source = 'INTERNAL'
  form.severity = 'MAJOR'
  form.ownerId = null
  form.dueDate = null
  form.objectCode = ''
  form.remark = ''
  formVisible.value = true
}

async function onSave(): Promise<void> {
  if (!form.title.trim()) {
    ElMessage.warning('请填写问题标题')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      description: form.description || null,
      source: form.source,
      severity: form.severity,
      ownerId: form.ownerId,
      dueDate: form.dueDate,
      objectCode: form.objectCode || null,
      remark: form.remark || null
    }
    const result = form.id
      ? await qualityIssueApi.update(form.id, payload)
      : await qualityIssueApi.create(payload)
    ElMessage.success('已保存')
    formVisible.value = false
    detail.value = result
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function openAssign(): void {
  assignForm.ownerId = detail.value?.issue.ownerId ?? null
  assignForm.dueDate = detail.value?.issue.dueDate ?? null
  assignVisible.value = true
}

async function onAssign(): Promise<void> {
  if (!detail.value || !assignForm.ownerId) {
    ElMessage.warning('请选择责任人')
    return
  }
  assigning.value = true
  try {
    detail.value = await qualityIssueApi.assign(detail.value.issue.id, assignForm.ownerId, assignForm.dueDate)
    assignVisible.value = false
    ElMessage.success('已分派')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '分派失败')
  } finally {
    assigning.value = false
  }
}

function openAddAction(): void {
  actionForm.actionType = 'CORRECTIVE'
  actionForm.content = ''
  actionForm.ownerId = detail.value?.issue.ownerId ?? null
  actionForm.dueDate = null
  actionVisible.value = true
}

async function onAddAction(): Promise<void> {
  if (!detail.value || !actionForm.content.trim()) {
    ElMessage.warning('请填写措施内容')
    return
  }
  actionSaving.value = true
  try {
    detail.value = await qualityIssueApi.addAction(detail.value.issue.id, {
      content: actionForm.content.trim(),
      actionType: actionForm.actionType,
      ownerId: actionForm.ownerId,
      dueDate: actionForm.dueDate
    })
    actionVisible.value = false
    ElMessage.success('已添加措施')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '添加失败')
  } finally {
    actionSaving.value = false
  }
}

async function openApplyTemplate(): Promise<void> {
  if (!templates.value.length) {
    try {
      templates.value = (await qualityTemplateApi.page({ enabled: true, size: 100 })).items
    } catch {
      templates.value = []
    }
  }
  if (!templates.value.length) {
    ElMessage.warning('还没有启用的 8D 模板，请先到「8D 模板」页配置')
    return
  }
  templateId.value = ''
  templateDetail.value = null
  templateVisible.value = true
}

async function onTemplateChange(value: string): Promise<void> {
  templateDetail.value = null
  if (!value) {
    return
  }
  try {
    templateDetail.value = await qualityTemplateApi.detail(value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载模板失败')
  }
}

async function onApplyTemplate(): Promise<void> {
  if (!detail.value || !templateId.value) {
    return
  }
  applying.value = true
  try {
    detail.value = await qualityIssueApi.applyTemplate(detail.value.issue.id, templateId.value)
    templateVisible.value = false
    ElMessage.success('已按模板生成本轮措施')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '应用模板失败')
  } finally {
    applying.value = false
  }
}

async function onToggle(item: IssueActionRow, done: boolean): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    detail.value = await qualityIssueApi.markAction(detail.value.issue.id, item.id, done)
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新措施失败')
  }
}

async function onRemoveAction(item: IssueActionRow): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确认删除这条措施？', '删除措施', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await qualityIssueApi.removeAction(detail.value.issue.id, item.id)
    detail.value = await qualityIssueApi.detail(detail.value.issue.id)
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function onSubmit(): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    detail.value = await qualityIssueApi.submit(detail.value.issue.id)
    ElMessage.success('已提交验证')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

async function onVerify(passed: boolean): Promise<void> {
  if (!detail.value) {
    return
  }
  let note = ''
  if (!passed) {
    try {
      const result = await ElMessageBox.prompt('验证不通过原因（必填）', '验证不通过', {
        inputValidator: (value: string) => (value && value.trim() ? true : '请填写原因'),
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })
      note = (result as { value?: string }).value?.trim() ?? ''
    } catch {
      return
    }
  }
  try {
    detail.value = await qualityIssueApi.verify(detail.value.issue.id, passed, note || null)
    ElMessage.success(passed ? '已关闭' : '已退回，进入下一轮整改')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '验证失败')
  }
}

function statusTone(value: IssueStatus): 'ok' | 'running' | 'warn' | 'danger' | 'muted' {
  if (value === 'CLOSED') {
    return 'ok'
  }
  if (value === 'VERIFYING') {
    return 'warn'
  }
  if (value === 'OPEN') {
    return 'danger'
  }
  return 'running'
}

function severityTone(value: IssueSeverity): 'danger' | 'warn' | 'muted' {
  if (value === 'CRITICAL') {
    return 'danger'
  }
  if (value === 'MAJOR') {
    return 'warn'
  }
  return 'muted'
}
</script>

<style scoped>
.detail-body {
  padding-bottom: 20px;
}

.round-badge {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.action-bar {
  margin: 12px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.round-block {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 12px;
}

.round-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  border-bottom: 1px dashed #f2f6fc;
}

.action-content {
  flex: 1;
}

.action-content.done {
  color: #909399;
  text-decoration: line-through;
}

.action-meta {
  color: #909399;
  font-size: 12px;
}

.danger-text {
  color: #f56c6c;
  font-weight: 600;
}

.label-gap {
  margin: 0 8px;
  color: #606266;
}

.template-table {
  margin-top: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
