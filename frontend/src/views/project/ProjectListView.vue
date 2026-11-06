<template>
  <div class="page project-page">
    <div class="eps-panel">
      <div class="eps-head">
        <span class="eps-title">EPS 目录</span>
        <el-button v-if="canWriteEps" link type="primary" @click="openEpsCreate(null)">新建根节点</el-button>
      </div>
      <div class="eps-actions">
        <el-button size="small" :disabled="!currentEps" @click="openEpsCreate(currentEps)">新增子节点</el-button>
        <el-button size="small" :disabled="!currentEps" @click="openEpsEdit">编辑</el-button>
        <el-button size="small" type="danger" plain :disabled="!currentEps" @click="onEpsDelete">删除</el-button>
      </div>
      <el-tree
        ref="epsTreeRef"
        :data="epsTreeData"
        node-key="id"
        highlight-current
        default-expand-all
        :expand-on-click-node="false"
        :props="{ label: 'name', children: 'children' }"
        @node-click="onEpsClick"
      >
        <template #default="{ data }">
          <span class="eps-node">
            <span class="eps-name">{{ data.name }}</span>
            <span class="eps-count">{{ data.projectCount }}</span>
          </span>
        </template>
      </el-tree>
      <el-empty v-if="!epsTreeData.length" description="还没有 EPS 节点" :image-size="60" />
    </div>

    <div class="project-main">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="项目编码 / 名称 / 项目经理"
          clearable
          style="width: 240px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="reload">
          <el-option v-for="(label, value) in PROJECT_STATUS_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-checkbox v-model="includeSubtree" :disabled="!currentEps" @change="reload">含子目录</el-checkbox>
        <el-button type="primary" @click="reload">查询</el-button>
        <el-button v-if="canWrite" type="primary" plain @click="openCreate">立项</el-button>
        <el-button v-if="canWrite" type="success" plain @click="openFromTemplate">从模板立项</el-button>
        <span class="hint">当前目录：{{ currentEps ? currentEps.name : '全部项目' }}</span>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="code" label="项目编码" width="120" />
        <el-table-column prop="name" label="项目名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="EPS" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.epsName || '-' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">{{ PROJECT_TYPE_LABEL[row.type] ?? row.type }}</template>
        </el-table-column>
        <el-table-column label="项目经理" width="110">
          <template #default="{ row }">{{ row.managerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <StatusDot
              :tone="statusTone(row.status)"
              :label="PROJECT_STATUS_LABEL[row.status as ProjectStatus] ?? row.status"
            />
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="80" align="center">
          <template #default="{ row }">P{{ row.priority }}</template>
        </el-table-column>
        <el-table-column label="计划起止" width="190" align="center">
          <template #default="{ row }">{{ row.planStart || '-' }} ~ {{ row.planEnd || '-' }}</template>
        </el-table-column>
        <el-table-column label="进度" width="140">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :stroke-width="10" :show-text="false" />
            <span class="sub">{{ row.progress }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="memberCount" label="成员" width="70" align="center" />
        <el-table-column v-if="canWrite" label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">查看</el-button>
            <el-button link type="primary" @click="openTasks(row.id)">任务/甘特</el-button>
            <el-button v-if="row.status !== 'CLOSED'" link type="primary" @click="openEdit(row.id)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="success" @click="onActivate(row)">启动</el-button>
            <el-button v-if="row.status === 'ACTIVE'" link type="warning" @click="onPause(row)">暂停</el-button>
            <el-button
              v-if="row.status === 'PAUSED'"
              link
              type="success"
              @click="onActivate(row)"
            >
              恢复
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE' || row.status === 'PAUSED'"
              link
              type="danger"
              @click="onClose(row)"
            >
              结项
            </el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        class="pager"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="load"
      />
    </div>

    <!-- 立项 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" top="8vh" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" maxlength="200" placeholder="如：某型号电传系统研制" />
        </el-form-item>
        <el-form-item label="EPS 目录">
          <el-select v-model="form.epsId" clearable placeholder="不归类" style="width: 100%">
            <el-option
              v-for="node in epsOptions"
              :key="node.id"
              :label="node.label"
              :value="node.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目经理">
          <el-select
            v-model="form.managerId"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="默认立项人"
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
        <el-form-item label="项目类型">
          <el-select v-model="form.type" style="width: 160px">
            <el-option v-for="(label, value) in PROJECT_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="1" :max="5" :controls="false" style="width: 120px" />
          <span class="sub">1 最高，5 最低</span>
        </el-form-item>
        <el-form-item label="计划开始">
          <el-date-picker v-model="form.planStart" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="计划结束">
          <el-date-picker v-model="form.planEnd" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item v-if="form.id" label="进度(%)">
          <el-input-number v-model="form.progress" :min="0" :max="100" style="width: 140px" />
        </el-form-item>
        <el-form-item label="项目成员">
          <el-select
            v-model="form.memberIds"
            multiple
            filterable
            remote
            reserve-keyword
            placeholder="可搜索添加成员"
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

    <!-- EPS 节点编辑 -->
    <el-dialog v-model="epsDialogVisible" :title="epsDialogTitle" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="编码" required>
          <el-input v-model="epsForm.code" maxlength="64" placeholder="如 EPS-RD" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="epsForm.name" maxlength="200" placeholder="如 研发中心" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="epsForm.sortNo" :min="0" :step="10" :controls="false" style="width: 120px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="epsForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="epsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="epsSaving" @click="onEpsSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 从模板立项 -->
    <el-dialog v-model="tplDialogVisible" title="从模板立项" width="560px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="项目模板" required>
          <el-select v-model="tplForm.templateId" filterable placeholder="选择模板" style="width: 100%">
            <el-option
              v-for="item in templates"
              :key="item.id"
              :label="`${item.code} ${item.name}（${item.taskCount} 任务 / ${item.folderCount} 目录）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称" required>
          <el-input v-model="tplForm.name" maxlength="200" placeholder="如 XX 产品研发项目" />
        </el-form-item>
        <el-form-item label="计划开始" required>
          <el-date-picker v-model="tplForm.planStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          <span class="hint">模板任务按「相对开工日偏移」自动推算计划起止</span>
        </el-form-item>
        <el-form-item label="EPS 目录">
          <el-select v-model="tplForm.epsId" clearable placeholder="不归类" style="width: 100%">
            <el-option v-for="node in epsOptions" :key="node.id" :label="node.label" :value="node.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="tplForm.priority" :min="1" :max="5" :controls="false" style="width: 120px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tplDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="tplSaving" @click="onFromTemplate">立项并生成任务</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :title="detailTitle" size="640px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="项目编码">{{ detail.code }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ PROJECT_STATUS_LABEL[detail.status] ?? detail.status }}
        </el-descriptions-item>
        <el-descriptions-item label="项目名称" :span="2">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="EPS">{{ detail.epsName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目经理">{{ detail.managerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目类型">{{ PROJECT_TYPE_LABEL[detail.type] ?? detail.type }}</el-descriptions-item>
        <el-descriptions-item label="优先级">P{{ detail.priority }}</el-descriptions-item>
        <el-descriptions-item label="计划起止">
          {{ detail.planStart || '-' }} ~ {{ detail.planEnd || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="实际起止">
          {{ detail.actualStart || '-' }} ~ {{ detail.actualEnd || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="进度">{{ detail.progress }}%</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="section-title">项目成员（{{ detail?.members.length ?? 0 }}）</div>
      <el-table :data="detail?.members ?? []" border size="small">
        <el-table-column label="姓名" min-width="140">
          <template #default="{ row }">{{ row.displayName || row.username || row.userId }}</template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">{{ MEMBER_ROLE_LABEL[row.role] ?? row.role }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!detail?.members.length" description="还没有成员" :image-size="60" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  epsApi,
  projectApi,
  templateApi,
  MEMBER_ROLE_LABEL,
  PROJECT_STATUS_LABEL,
  PROJECT_TYPE_LABEL,
  type EpsNode,
  type ProjectDetail,
  type ProjectRow,
  type ProjectStatus,
  type ProjectTemplateRow
} from '@/api/project'
import { workflowApi, type ActorUserRow } from '@/api/workflow'
import StatusDot from '@/components/StatusDot.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const canWrite = computed(() => userStore.can('prj.project.write'))
const canWriteEps = computed(() => userStore.can('prj.eps.write'))

const loading = ref(false)
const saving = ref(false)
const rows = ref<ProjectRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const status = ref<string>('')
const includeSubtree = ref(true)

const epsTreeData = ref<EpsNode[]>([])
const currentEps = ref<EpsNode | null>(null)
const epsTreeRef = ref()

/** EPS 下拉选项（带层级缩进，避免同名节点看不出层级）。 */
const epsOptions = computed(() => {
  const options: Array<{ id: string; label: string }> = []
  const walk = (nodes: EpsNode[], depth: number): void => {
    nodes.forEach((node) => {
      options.push({ id: node.id, label: `${'　'.repeat(depth)}${node.name}（${node.code}）` })
      walk(node.children ?? [], depth + 1)
    })
  }
  walk(epsTreeData.value, 0)
  return options
})

// 从模板立项（P7-6）
const tplDialogVisible = ref(false)
const tplSaving = ref(false)
const templates = ref<ProjectTemplateRow[]>([])
const tplForm = reactive<{
  templateId: string
  name: string
  planStart: string
  epsId: string | null
  priority: number
}>({
  templateId: '',
  name: '',
  planStart: today(),
  epsId: null,
  priority: 3
})

async function openFromTemplate(): Promise<void> {
  tplForm.templateId = ''
  tplForm.name = ''
  tplForm.planStart = today()
  tplForm.epsId = currentEps.value?.id ?? null
  tplForm.priority = 3
  tplSaving.value = false
  try {
    const page = await templateApi.list({ enabled: true, page: 1, size: 100 })
    templates.value = page.items
    if (!templates.value.length) {
      ElMessage.warning('还没有启用的项目模板，请先在「项目模板」里维护')
      return
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载模板失败')
    return
  }
  tplDialogVisible.value = true
}

async function onFromTemplate(): Promise<void> {
  if (!tplForm.templateId || !tplForm.name.trim() || !tplForm.planStart) {
    ElMessage.warning('请选择模板并填写项目名称与计划开始日')
    return
  }
  tplSaving.value = true
  try {
    const project = await templateApi.fromTemplate({
      templateId: tplForm.templateId,
      name: tplForm.name.trim(),
      planStart: tplForm.planStart,
      epsId: tplForm.epsId,
      priority: tplForm.priority
    })
    ElMessage.success(`已立项 ${project.code}，任务与目录已生成`)
    tplDialogVisible.value = false
    await reload()
    router.push(`/project/${project.id}/tasks`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '从模板立项失败')
  } finally {
    tplSaving.value = false
  }
}

function today(): string {
  const now = new Date()
  const month = `${now.getMonth() + 1}`.padStart(2, '0')
  const day = `${now.getDate()}`.padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

const dialogVisible = ref(false)
const dialogTitle = computed(() => (form.id ? '编辑项目' : '立项'))
const users = ref<ActorUserRow[]>([])
const usersLoading = ref(false)

const form = reactive<{
  id?: string
  name: string
  epsId: string | null
  managerId: string | null
  type: string
  priority: number
  planStart: string | null
  planEnd: string | null
  progress: number
  remark: string
  memberIds: string[]
}>({
  id: undefined,
  name: '',
  epsId: null,
  managerId: null,
  type: 'RD',
  priority: 3,
  planStart: null,
  planEnd: null,
  progress: 0,
  remark: '',
  memberIds: []
})

const epsDialogVisible = ref(false)
const epsSaving = ref(false)
const epsEditingId = ref<string | null>(null)
const epsParentId = ref<string | null>(null)
const epsDialogTitle = computed(() => (epsEditingId.value ? '编辑 EPS 节点' : '新建 EPS 节点'))
const epsForm = reactive<{ code: string; name: string; sortNo: number; remark: string }>({
  code: '',
  name: '',
  sortNo: 10,
  remark: ''
})

const detailVisible = ref(false)
const detail = ref<ProjectDetail | null>(null)
const detailTitle = computed(() => (detail.value ? `${detail.value.code} ${detail.value.name}` : '项目详情'))

onMounted(async () => {
  await loadEps()
  await load()
})

async function loadEps(): Promise<void> {
  try {
    epsTreeData.value = await epsApi.tree()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载 EPS 失败')
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await projectApi.list({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      status: status.value || undefined,
      epsId: currentEps.value?.id,
      includeSubtree: currentEps.value ? includeSubtree.value : undefined
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载项目失败')
  } finally {
    loading.value = false
  }
}

function reload(): void {
  page.value = 1
  load()
}

function onEpsClick(node: EpsNode): void {
  currentEps.value = node
  reload()
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

function openCreate(): void {
  Object.assign(form, {
    id: undefined,
    name: '',
    epsId: currentEps.value?.id ?? null,
    managerId: null,
    type: 'RD',
    priority: 3,
    planStart: null,
    planEnd: null,
    progress: 0,
    remark: '',
    memberIds: []
  })
  dialogVisible.value = true
  searchUsers('')
}

async function openEdit(id: string): Promise<void> {
  try {
    const project = await projectApi.detail(id)
    Object.assign(form, {
      id: project.id,
      name: project.name,
      epsId: project.epsId ?? null,
      managerId: project.managerId ?? null,
      type: project.type,
      priority: project.priority,
      planStart: project.planStart ?? null,
      planEnd: project.planEnd ?? null,
      progress: project.progress,
      remark: project.remark ?? '',
      memberIds: project.members.map((member) => member.userId)
    })
    // 已选人员补进候选，否则多选框只显示裸 id
    const merged = new Map(users.value.map((user) => [user.id, user]))
    if (project.managerId && project.managerName) {
      merged.set(project.managerId, {
        id: project.managerId,
        username: project.managerName,
        displayName: project.managerName
      } as ActorUserRow)
    }
    users.value = [...merged.values()]
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载项目详情失败')
  }
}

async function onSave(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请填写项目名称')
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
      epsId: form.epsId || null,
      managerId: form.managerId || null,
      type: form.type,
      priority: form.priority,
      planStart: form.planStart,
      planEnd: form.planEnd,
      progress: form.progress,
      remark: form.remark || null,
      members: form.memberIds.map((userId) => ({ userId, role: 'MEMBER' }))
    }
    if (form.id) {
      await projectApi.update(form.id, payload)
      ElMessage.success('已保存')
    } else {
      await projectApi.create(payload)
      ElMessage.success('已立项')
    }
    dialogVisible.value = false
    await loadEps()
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onActivate(row: ProjectRow): Promise<void> {
  try {
    await projectApi.activate(row.id)
    ElMessage.success('项目已启动')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '启动失败')
  }
}

async function onPause(row: ProjectRow): Promise<void> {
  try {
    await projectApi.pause(row.id)
    ElMessage.success('项目已暂停')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '暂停失败')
  }
}

async function onClose(row: ProjectRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`结项后项目不可再修改，确认结项 ${row.code}？`, '项目结项', {
      confirmButtonText: '结项',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await projectApi.close(row.id)
    ElMessage.success('项目已结项')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '结项失败')
  }
}

async function onDelete(row: ProjectRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除草稿项目 ${row.code}？`, '删除项目', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await projectApi.remove(row.id)
    ElMessage.success('已删除')
    await loadEps()
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function openTasks(id: string): void {
  router.push(`/project/${id}/tasks`)
}

async function openDetail(id: string): Promise<void> {
  try {
    detail.value = await projectApi.detail(id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载项目详情失败')
  }
}

// ---------------------------------------------------------------- EPS 操作

function openEpsCreate(parent: EpsNode | null): void {
  epsEditingId.value = null
  epsParentId.value = parent?.id ?? null
  Object.assign(epsForm, { code: '', name: '', sortNo: 10, remark: '' })
  epsDialogVisible.value = true
}

function openEpsEdit(): void {
  if (!currentEps.value) {
    return
  }
  epsEditingId.value = currentEps.value.id
  epsParentId.value = currentEps.value.parentId ?? null
  Object.assign(epsForm, {
    code: currentEps.value.code,
    name: currentEps.value.name,
    sortNo: currentEps.value.sortNo,
    remark: currentEps.value.remark ?? ''
  })
  epsDialogVisible.value = true
}

async function onEpsSave(): Promise<void> {
  if (!epsForm.code.trim() || !epsForm.name.trim()) {
    ElMessage.warning('请填写编码与名称')
    return
  }
  epsSaving.value = true
  try {
    const payload = {
      code: epsForm.code.trim(),
      name: epsForm.name.trim(),
      parentId: epsEditingId.value ? undefined : epsParentId.value,
      sortNo: epsForm.sortNo,
      remark: epsForm.remark || null
    }
    if (epsEditingId.value) {
      await epsApi.update(epsEditingId.value, payload)
      ElMessage.success('已保存')
    } else {
      await epsApi.create(payload)
      ElMessage.success('已创建')
    }
    epsDialogVisible.value = false
    await loadEps()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存 EPS 失败')
  } finally {
    epsSaving.value = false
  }
}

async function onEpsDelete(): Promise<void> {
  if (!currentEps.value) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除 EPS 节点「${currentEps.value.name}」？`, '删除 EPS', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await epsApi.remove(currentEps.value.id)
    ElMessage.success('已删除')
    currentEps.value = null
    await loadEps()
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除 EPS 失败')
  }
}

function statusTone(value: ProjectStatus): 'ok' | 'danger' | 'running' | 'muted' {
  if (value === 'ACTIVE') {
    return 'ok'
  }
  if (value === 'CLOSED') {
    return 'muted'
  }
  return value === 'PAUSED' ? 'danger' : 'running'
}
</script>

<style scoped>
.project-page {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.eps-panel {
  width: 260px;
  flex: 0 0 260px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 8px;
  background: #fff;
}

.eps-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.eps-title {
  font-weight: 600;
}

.eps-actions {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
}

.eps-node {
  display: flex;
  width: 100%;
  justify-content: space-between;
  padding-right: 6px;
}

.eps-count {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.project-main {
  flex: 1;
  min-width: 0;
}
</style>
