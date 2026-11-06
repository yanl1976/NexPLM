<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="编码/标题/范围" clearable style="width: 200px" @keyup.enter="reload" />
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 120px" @change="reload">
        <el-option v-for="(label, value) in AUDIT_STATUS_LABEL" :key="value" :label="label" :value="value" />
      </el-select>
      <el-select v-model="auditType" placeholder="全部类型" clearable style="width: 130px" @change="reload">
        <el-option v-for="(label, value) in AUDIT_TYPE_LABEL" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建审核计划</el-button>
      <span class="hint">不符合项要转成质量问题整改（走 8D 闭环），整改关闭后才能关闭审核</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe @row-dblclick="(row: PlanRow) => openDetail(row.id)">
      <el-table-column prop="code" label="编码" width="120" />
      <el-table-column prop="title" label="审核计划" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">{{ AUDIT_TYPE_LABEL[row.auditType] ?? row.auditType }}</template>
      </el-table-column>
      <el-table-column prop="standard" label="依据" width="150" show-overflow-tooltip />
      <el-table-column label="计划/实际" width="200">
        <template #default="{ row }">{{ row.plannedDate || '-' }} / {{ row.actualDate || '-' }}</template>
      </el-table-column>
      <el-table-column label="主审" width="100">
        <template #default="{ row }">{{ row.auditorName || '-' }}</template>
      </el-table-column>
      <el-table-column label="被审" width="100">
        <template #default="{ row }">{{ row.auditee || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">
            {{ AUDIT_STATUS_LABEL[row.status] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="检查项" width="80" align="center">
        <template #default="{ row }">{{ row.itemCount }}</template>
      </el-table-column>
      <el-table-column label="不符合项" width="110" align="center">
        <template #default="{ row }">
          <span :class="{ 'danger-text': row.openFindingCount > 0 }">
            {{ row.findingCount }}/{{ row.openFindingCount }} 未关闭
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="没有符合条件的审核计划" :image-size="60" />
    <el-pagination
      v-if="total > size"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPageChange"
    />

    <!-- 新建/编辑计划 -->
    <el-dialog v-model="formVisible" :title="form.id ? `编辑 ${form.code}` : '新建审核计划'" width="640px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="审核标题" required>
          <el-input v-model="form.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="审核类型">
          <el-select v-model="form.auditType" style="width: 160px">
            <el-option v-for="(label, value) in AUDIT_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <span class="label-gap">计划日期</span>
          <el-date-picker v-model="form.plannedDate" type="date" value-format="YYYY-MM-DD" style="width: 170px" />
        </el-form-item>
        <el-form-item label="依据标准">
          <el-input v-model="form.standard" maxlength="200" placeholder="IATF16949:2016 / 内部程序文件" />
        </el-form-item>
        <el-form-item label="审核范围">
          <el-input v-model="form.scope" maxlength="500" placeholder="部门 / 过程 / 产品" />
        </el-form-item>
        <el-form-item label="主审">
          <el-select v-model="form.auditorId" filterable clearable style="width: 220px">
            <el-option v-for="user in users" :key="user.id" :label="user.displayName" :value="user.id" />
          </el-select>
          <span class="label-gap">被审部门</span>
          <el-input v-model="form.auditee" maxlength="200" style="width: 200px" />
        </el-form-item>
        <el-form-item label="审核结论">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSavePlan">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :title="detail ? `${detail.code} ${detail.title}` : '审核计划'" size="900px">
      <div v-if="detail" class="detail-body">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)" size="small">
              {{ AUDIT_STATUS_LABEL[detail.status] ?? detail.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            {{ AUDIT_TYPE_LABEL[detail.auditType] ?? detail.auditType }}
          </el-descriptions-item>
          <el-descriptions-item label="依据标准">{{ detail.standard || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核范围">{{ detail.scope || '-' }}</el-descriptions-item>
          <el-descriptions-item label="计划日期">{{ detail.plannedDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际日期">{{ detail.actualDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="主审">{{ detail.auditorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="被审部门">{{ detail.auditee || '-' }}</el-descriptions-item>
          <el-descriptions-item label="检查结果" :span="2">
            <span class="stat-inline">
              共 {{ detail.stats.itemTotal }} 项 ·
              符合 {{ detail.stats.okCount }} ·
              不符合 {{ detail.stats.ngCount }} ·
              不适用 {{ detail.stats.naCount }} ·
              待检 {{ detail.stats.pendingCount }} ·
              不符合项 {{ detail.stats.findingTotal }}（未关闭 {{ detail.stats.openFindingCount }}）
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="审核结论" :span="2">{{ detail.summary || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="action-bar">
          <template v-if="canWrite && detail.status !== 'CLOSED'">
            <el-button size="small" @click="openEditPlan">编辑计划</el-button>
            <el-button size="small" type="primary" plain :loading="itemSaving" @click="onSaveItems">
              保存检查表
            </el-button>
            <el-button size="small" @click="addItem">新增检查项</el-button>
          </template>
          <template v-if="canWrite && detail.status === 'PLANNED'">
            <el-button size="small" type="primary" @click="onStart">开始审核</el-button>
            <el-button size="small" type="danger" plain @click="onRemovePlan">删除</el-button>
          </template>
          <template v-if="canWrite && detail.status === 'IN_PROGRESS'">
            <el-button size="small" type="success" @click="onClose">关闭审核</el-button>
          </template>
          <span v-if="detail.status === 'CLOSED'" class="hint-inline">审核已关闭，计划与检查表冻结</span>
        </div>

        <el-tabs v-model="activeTab">
          <el-tab-pane label="检查表" name="items">
            <el-table :data="itemDrafts" border size="small">
              <el-table-column label="编号" width="90">
                <template #default="{ row }">
                  <el-input v-if="editable" v-model="row.itemNo" maxlength="32" />
                  <span v-else>{{ row.itemNo }}</span>
                </template>
              </el-table-column>
              <el-table-column label="条款" width="100">
                <template #default="{ row }">
                  <el-input v-if="editable" v-model="row.clause" maxlength="64" />
                  <span v-else>{{ row.clause || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="检查内容" min-width="200">
                <template #default="{ row }">
                  <el-input v-if="editable" v-model="row.question" maxlength="500" />
                  <span v-else>{{ row.question }}</span>
                </template>
              </el-table-column>
              <el-table-column label="结果" width="110">
                <template #default="{ row }">
                  <el-select v-if="editable" v-model="row.result" size="small" style="width: 100%">
                    <el-option v-for="(label, value) in AUDIT_RESULT_LABEL" :key="value" :label="label" :value="value" />
                  </el-select>
                  <el-tag v-else :type="resultType(row.result)" size="small">
                    {{ AUDIT_RESULT_LABEL[row.result] ?? row.result }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="证据/记录" min-width="160">
                <template #default="{ row }">
                  <el-input v-if="editable" v-model="row.evidence" maxlength="1000" />
                  <span v-else>{{ row.evidence || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column v-if="editable" label="操作" width="90" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="itemDrafts.splice($index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!itemDrafts.length" description="还没有检查项，点「新增检查项」开始" :image-size="50" />
          </el-tab-pane>

          <el-tab-pane :label="`不符合项（${detail.findings.length}）`" name="findings">
            <div v-if="canWrite && detail.status !== 'CLOSED'" class="finding-bar">
              <el-button size="small" type="primary" plain @click="openFindingDialog(null)">
                登记不符合项
              </el-button>
            </div>
            <el-table :data="detail.findings" border size="small">
              <el-table-column prop="code" label="编号" width="110" />
              <el-table-column prop="title" label="不符合描述" min-width="200" show-overflow-tooltip />
              <el-table-column label="严重度" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="severityType(row.severity)" size="small">
                    {{ FINDING_SEVERITY_LABEL[row.severity] ?? row.severity }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="关联检查项" width="110" align="center">
                <template #default="{ row }">{{ row.itemNo || '-' }}</template>
              </el-table-column>
              <el-table-column label="期限" width="110">
                <template #default="{ row }">{{ row.dueDate || '-' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'CLOSED' ? 'success' : 'warning'" size="small">
                    {{ row.status === 'CLOSED' ? '已关闭' : '未关闭' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="质量问题" width="130">
                <template #default="{ row }">
                  <span v-if="row.issueCode">{{ row.issueCode }}</span>
                  <span v-else class="hint-inline">未转</span>
                </template>
              </el-table-column>
              <el-table-column v-if="canWrite" label="操作" width="200" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="!row.issueId"
                    link
                    type="primary"
                    @click="onToIssue(row)"
                  >转质量问题</el-button>
                  <el-button
                    v-if="row.issueId && row.status !== 'CLOSED'"
                    link
                    type="success"
                    @click="onCloseFinding(row)"
                  >关闭</el-button>
                  <el-button v-if="!row.issueId" link type="primary" @click="openFindingDialog(row)">编辑</el-button>
                  <el-button v-if="!row.issueId" link type="danger" @click="onRemoveFinding(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!detail.findings.length" description="没有不符合项" :image-size="50" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <!-- 不符合项登记/编辑 -->
    <el-dialog v-model="findingVisible" :title="findingForm.id ? '编辑不符合项' : '登记不符合项'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="findingForm.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="findingForm.description" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="严重度">
          <el-select v-model="findingForm.severity" style="width: 140px">
            <el-option v-for="(label, value) in FINDING_SEVERITY_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <span class="label-gap">整改期限</span>
          <el-date-picker v-model="findingForm.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 170px" />
        </el-form-item>
        <el-form-item label="关联检查项">
          <el-select v-model="findingForm.itemId" clearable filterable placeholder="可空" style="width: 100%">
            <el-option
              v-for="item in itemDrafts.filter((entry) => entry.itemNo)"
              :key="item.itemNo"
              :label="`${item.itemNo} ${item.question}`"
              :value="item.id || ''"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="findingVisible = false">取消</el-button>
        <el-button type="primary" :loading="findingSaving" @click="onSaveFinding">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  auditApi,
  AUDIT_RESULT_LABEL,
  AUDIT_STATUS_LABEL,
  AUDIT_TYPE_LABEL,
  FINDING_SEVERITY_LABEL,
  type AuditFindingRow,
  type AuditItemRow,
  type ItemSaveReq,
  type PlanDetail,
  type PlanRow
} from '@/api/audit'
import { workflowApi, type ActorUserRow } from '@/api/workflow'
import { useUserStore } from '@/stores/user'

interface ItemDraft {
  id?: string
  itemNo: string
  clause: string
  question: string
  result: string
  evidence: string
}

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('qlt.audit.write'))

const loading = ref(false)
const rows = ref<PlanRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const status = ref('')
const auditType = ref('')
const users = ref<ActorUserRow[]>([])

const formVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: '',
  code: '',
  title: '',
  auditType: 'INTERNAL',
  standard: '',
  scope: '',
  plannedDate: null as string | null,
  auditorId: null as string | null,
  auditee: '',
  summary: ''
})

const detailVisible = ref(false)
const detail = ref<PlanDetail | null>(null)
const itemDrafts = ref<ItemDraft[]>([])
const itemSaving = ref(false)
const activeTab = ref('items')

const findingVisible = ref(false)
const findingSaving = ref(false)
const findingForm = reactive({
  id: '',
  title: '',
  description: '',
  severity: 'MINOR',
  dueDate: null as string | null,
  itemId: '' as string
})

/** 关闭后检查表与计划都冻结，UI 与后端同口径。 */
const editable = computed(() => canWrite.value && detail.value?.status !== 'CLOSED')

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
    const result = await auditApi.page({
      keyword: keyword.value || undefined,
      status: status.value || undefined,
      auditType: auditType.value || undefined,
      page: page.value,
      size: size.value
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载审核计划失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(value: number): void {
  page.value = value
  void reload()
}

/* ------------------------------------------------------------ 计划 */

function openCreate(): void {
  form.id = ''
  form.code = ''
  form.title = ''
  form.auditType = 'INTERNAL'
  form.standard = 'IATF16949:2016'
  form.scope = ''
  form.plannedDate = new Date().toISOString().slice(0, 10)
  form.auditorId = null
  form.auditee = ''
  form.summary = ''
  formVisible.value = true
}

function openEditPlan(): void {
  if (!detail.value) {
    return
  }
  form.id = detail.value.id
  form.code = detail.value.code
  form.title = detail.value.title
  form.auditType = detail.value.auditType
  form.standard = detail.value.standard ?? ''
  form.scope = detail.value.scope ?? ''
  form.plannedDate = detail.value.plannedDate ?? null
  form.auditorId = detail.value.auditorId ?? null
  form.auditee = detail.value.auditee ?? ''
  form.summary = detail.value.summary ?? ''
  formVisible.value = true
}

async function onSavePlan(): Promise<void> {
  if (!form.title.trim()) {
    ElMessage.warning('请填写审核标题')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      auditType: form.auditType,
      standard: form.standard.trim() || null,
      scope: form.scope.trim() || null,
      plannedDate: form.plannedDate,
      auditorId: form.auditorId,
      auditee: form.auditee.trim() || null,
      summary: form.summary.trim() || null
    }
    const saved = form.id
      ? await auditApi.update(form.id, payload)
      : await auditApi.create(payload)
    ElMessage.success('已保存')
    formVisible.value = false
    await reload()
    if (form.id) {
      applyDetail(saved)
    } else {
      await openDetail(saved.id)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onStart(): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    applyDetail(await auditApi.start(detail.value.id))
    ElMessage.success('已开始审核')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '开始审核失败')
  }
}

async function onClose(): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '关闭后计划与检查表冻结。存在未关闭的不符合项时会被拒绝。',
      '关闭审核',
      { confirmButtonText: '关闭审核', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    applyDetail(await auditApi.close(detail.value.id))
    ElMessage.success('已关闭审核')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '关闭失败')
  }
}

async function onRemovePlan(): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确认删除该审核计划？', '删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await auditApi.remove(detail.value.id)
    ElMessage.success('已删除')
    detailVisible.value = false
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

/* ------------------------------------------------------------ 详情与检查表 */

async function openDetail(id: string): Promise<void> {
  try {
    applyDetail(await auditApi.detail(id))
    activeTab.value = 'items'
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  }
}

function applyDetail(result: PlanDetail): void {
  detail.value = result
  itemDrafts.value = result.items.map((item: AuditItemRow) => ({
    id: item.id,
    itemNo: item.itemNo,
    clause: item.clause ?? '',
    question: item.question,
    result: item.result,
    evidence: item.evidence ?? ''
  }))
}

function addItem(): void {
  itemDrafts.value.push({
    itemNo: String(itemDrafts.value.length + 1),
    clause: '',
    question: '',
    result: 'PENDING',
    evidence: ''
  })
}

async function onSaveItems(): Promise<void> {
  if (!detail.value) {
    return
  }
  const numbers = new Set<string>()
  for (const item of itemDrafts.value) {
    const no = item.itemNo.trim().toUpperCase()
    if (!no || !item.question.trim()) {
      ElMessage.warning('检查项编号与内容都不能为空')
      return
    }
    if (numbers.has(no)) {
      ElMessage.warning(`检查项编号重复：${no}`)
      return
    }
    numbers.add(no)
  }
  itemSaving.value = true
  try {
    const payload: ItemSaveReq[] = itemDrafts.value.map((item, index) => ({
      itemNo: item.itemNo.trim().toUpperCase(),
      clause: item.clause.trim() || null,
      question: item.question.trim(),
      result: item.result,
      evidence: item.evidence.trim() || null,
      sortNo: (index + 1) * 10
    }))
    applyDetail(await auditApi.replaceItems(detail.value.id, payload))
    ElMessage.success('检查表已保存')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存检查表失败')
  } finally {
    itemSaving.value = false
  }
}

/* ------------------------------------------------------------ 不符合项 */

function openFindingDialog(row: AuditFindingRow | null): void {
  findingForm.id = row?.id ?? ''
  findingForm.title = row?.title ?? ''
  findingForm.description = row?.description ?? ''
  findingForm.severity = row?.severity ?? 'MINOR'
  findingForm.dueDate = row?.dueDate ?? null
  findingForm.itemId = row?.itemId ?? ''
  findingVisible.value = true
}

async function onSaveFinding(): Promise<void> {
  if (!detail.value) {
    return
  }
  if (!findingForm.title.trim()) {
    ElMessage.warning('请填写不符合描述')
    return
  }
  findingSaving.value = true
  try {
    const payload = {
      title: findingForm.title.trim(),
      description: findingForm.description.trim() || null,
      severity: findingForm.severity,
      dueDate: findingForm.dueDate,
      itemId: findingForm.itemId || null
    }
    const saved = findingForm.id
      ? await auditApi.updateFinding(findingForm.id, payload)
      : await auditApi.addFinding(detail.value.id, payload)
    applyDetail(saved)
    findingVisible.value = false
    ElMessage.success('已保存')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存不符合项失败')
  } finally {
    findingSaving.value = false
  }
}

async function onToIssue(row: AuditFindingRow): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `把「${row.title}」转成质量问题？转出后整改在「质量问题」页按 8D 闭环处理，这边只保留问题编号。`,
      '转质量问题',
      { confirmButtonText: '转出', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }
  try {
    applyDetail(await auditApi.findingToIssue(row.id))
    ElMessage.success('已转成质量问题')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '转换失败')
  }
}

async function onCloseFinding(row: AuditFindingRow): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    applyDetail(await auditApi.closeFinding(row.id))
    ElMessage.success('不符合项已关闭')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '关闭失败')
  }
}

async function onRemoveFinding(row: AuditFindingRow): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除不符合项 ${row.code}？`, '删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await auditApi.removeFinding(row.id)
    ElMessage.success('已删除')
    applyDetail(await auditApi.detail(detail.value.id))
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

/* ------------------------------------------------------------ 展示辅助 */

function statusType(value: string): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'CLOSED') {
    return 'success'
  }
  if (value === 'IN_PROGRESS') {
    return 'warning'
  }
  return 'info'
}

function resultType(value: string): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'OK') {
    return 'success'
  }
  if (value === 'NG') {
    return 'danger'
  }
  return 'info'
}

function severityType(value: string): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'CRITICAL') {
    return 'danger'
  }
  if (value === 'MAJOR') {
    return 'warning'
  }
  return 'info'
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.hint {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.hint-inline {
  color: #909399;
  font-size: 12px;
}

.action-bar {
  margin: 12px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.finding-bar {
  margin-bottom: 8px;
}

.stat-inline {
  color: #606266;
}

.label-gap {
  margin: 0 8px;
  color: #606266;
}

.danger-text {
  color: #f56c6c;
  font-weight: 600;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
