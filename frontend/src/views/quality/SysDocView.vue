<template>
  <div class="page">
    <el-row :gutter="12">
      <!-- 文件夹树 -->
      <el-col :span="5">
        <el-card shadow="never" class="tree-card">
          <template #header>
            <div class="tree-head">
              <span>文件夹</span>
              <el-button v-if="canWrite" link type="primary" @click="openFolderDialog(null)">新建</el-button>
            </div>
          </template>
          <div class="tree-all" :class="{ active: !folderId }" @click="selectFolder(null)">
            全部文件
          </div>
          <el-tree
            :data="folderTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            highlight-current
            :expand-on-click-node="false"
            @node-click="(node: FolderNode) => selectFolder(node.id)"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <span>{{ data.name }}</span>
                <span class="tree-count">{{ data.docCount }}</span>
                <el-button
                  v-if="canWrite"
                  link
                  type="primary"
                  class="tree-add"
                  @click.stop="openFolderDialog(data.id)"
                >+</el-button>
              </span>
            </template>
          </el-tree>
          <el-empty v-if="!folderTree.length" description="还没有文件夹" :image-size="40" />
        </el-card>
      </el-col>

      <el-col :span="19">
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="文件编号/名称" clearable style="width: 200px" @keyup.enter="reload" />
          <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px" @change="reload">
            <el-option v-for="(label, value) in DOC_STATUS_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <el-select v-model="docLevel" placeholder="全部层级" clearable style="width: 130px" @change="reload">
            <el-option v-for="(label, value) in DOC_LEVEL_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <el-checkbox v-model="dueOnly" @change="reload">只看到期评审</el-checkbox>
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建文件</el-button>
          <span class="hint">发布 = 写版本快照并算下次评审日；修订后版本号 +1，旧版本可回溯</span>
        </div>

        <el-table v-loading="loading" :data="rows" border stripe @row-dblclick="(row: DocRow) => openDetail(row.id)">
          <el-table-column prop="code" label="编号" width="120" />
          <el-table-column prop="name" label="文件名称" min-width="200" show-overflow-tooltip />
          <el-table-column label="层级" width="100" align="center">
            <template #default="{ row }">{{ DOC_LEVEL_LABEL[row.docLevel] ?? row.docLevel }}</template>
          </el-table-column>
          <el-table-column label="文件夹" width="130">
            <template #default="{ row }">{{ row.folderName || '-' }}</template>
          </el-table-column>
          <el-table-column label="版本" width="70" align="center">
            <template #default="{ row }">v{{ row.versionNo }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">
                {{ DOC_STATUS_LABEL[row.status] ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ownerName" label="责任人" width="100">
            <template #default="{ row }">{{ row.ownerName || '-' }}</template>
          </el-table-column>
          <el-table-column label="生效日" width="110">
            <template #default="{ row }">{{ row.effectiveDate || '-' }}</template>
          </el-table-column>
          <el-table-column label="下次评审" width="120">
            <template #default="{ row }">
              <span :class="{ 'danger-text': row.dueReview }">{{ row.nextReviewDate || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !rows.length" description="没有符合条件的体系文件" :image-size="60" />
        <el-pagination
          v-if="total > size"
          class="pager"
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPageChange"
        />
      </el-col>
    </el-row>

    <!-- 新建文件夹 -->
    <el-dialog v-model="folderVisible" title="新建文件夹" width="420px">
      <el-form label-width="80px">
        <el-form-item label="上级">
          <el-tree-select
            v-model="folderForm.parentId"
            :data="folderTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="不选 = 根文件夹"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="folderForm.code" maxlength="64" placeholder="QP / WI（创建后不可改）" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="folderForm.name" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="folderVisible = false">取消</el-button>
        <el-button type="primary" :loading="folderSaving" @click="onSaveFolder">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建文件 -->
    <el-dialog v-model="createVisible" title="新建体系文件" width="560px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="生成方式">
          <el-radio-group v-model="createForm.mode">
            <el-radio value="template">按模板生成</el-radio>
            <el-radio value="manual">手工新建</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createForm.mode === 'template'" label="模板" required>
          <el-select v-model="createForm.templateId" filterable placeholder="选择启用的模板" style="width: 100%">
            <el-option
              v-for="item in templates"
              :key="item.id"
              :label="`${item.code} ${item.name}（${DOC_LEVEL_LABEL[item.docLevel] ?? item.docLevel} · ${item.sectionCount} 章）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文件编号" required>
          <el-input v-model="createForm.code" maxlength="64" placeholder="QP-01" />
        </el-form-item>
        <el-form-item label="文件名称" required>
          <el-input v-model="createForm.name" maxlength="200" />
        </el-form-item>
        <template v-if="createForm.mode === 'manual'">
          <el-form-item label="层级">
            <el-select v-model="createForm.docLevel" style="width: 160px">
              <el-option v-for="(label, value) in DOC_LEVEL_LABEL" :key="value" :label="label" :value="value" />
            </el-select>
            <span class="label-gap">评审周期</span>
            <el-input-number v-model="createForm.reviewMonths" :min="1" :max="120" style="width: 110px" />
          </el-form-item>
          <el-form-item label="文件夹">
            <el-tree-select
              v-model="createForm.folderId"
              :data="folderTree"
              :props="{ label: 'name', value: 'id', children: 'children' }"
              check-strictly
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </template>
        <el-form-item label="责任人">
          <el-select v-model="createForm.ownerId" filterable clearable style="width: 100%">
            <el-option v-for="user in users" :key="user.id" :label="user.displayName" :value="user.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :title="detail ? `${detail.code} ${detail.name}` : '体系文件'" size="820px">
      <div v-if="detail" class="detail-body">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)" size="small">
              {{ DOC_STATUS_LABEL[detail.status] ?? detail.status }}
            </el-tag>
            <span class="round-badge">v{{ detail.versionNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="层级">
            {{ DOC_LEVEL_LABEL[detail.docLevel] ?? detail.docLevel }}
          </el-descriptions-item>
          <el-descriptions-item label="文件夹">{{ detail.folderName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="模板">{{ detail.templateCode || '手工创建' }}</el-descriptions-item>
          <el-descriptions-item label="责任人">{{ detail.ownerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评审周期">{{ detail.reviewMonths }} 个月</el-descriptions-item>
          <el-descriptions-item label="生效日">{{ detail.effectiveDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下次评审">
            <span :class="{ 'danger-text': dueReview(detail) }">{{ detail.nextReviewDate || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="审批意见" :span="2">{{ detail.approveNote || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="action-bar">
          <template v-if="canWrite && detail.status === 'DRAFT'">
            <el-button size="small" type="primary" :loading="saving" @click="onSaveDraft">保存</el-button>
            <el-button size="small" @click="onSubmit">提交审批</el-button>
            <el-button size="small" type="danger" plain @click="onRemove">删除</el-button>
          </template>
          <template v-if="canWrite && detail.status === 'SUBMITTED'">
            <el-button size="small" type="success" @click="onApprove(true)">审批通过并发布</el-button>
            <el-button size="small" type="danger" plain @click="onApprove(false)">驳回</el-button>
          </template>
          <template v-if="canWrite && detail.status === 'RELEASED'">
            <el-button size="small" @click="onRevise">发起修订</el-button>
            <el-button size="small" type="danger" plain @click="onObsolete">作废</el-button>
          </template>
          <span v-if="detail.status === 'SUBMITTED'" class="hint-inline">待审批期间不可修改，驳回后才能改</span>
        </div>

        <!-- 草稿可编辑基础信息 -->
        <el-form v-if="detail.status === 'DRAFT' && canWrite" label-width="90px" class="edit-form">
          <el-form-item label="名称">
            <el-input v-model="editForm.name" maxlength="200" style="width: 320px" />
            <span class="label-gap">层级</span>
            <el-select v-model="editForm.docLevel" style="width: 140px">
              <el-option v-for="(label, value) in DOC_LEVEL_LABEL" :key="value" :label="label" :value="value" />
            </el-select>
          </el-form-item>
          <el-form-item label="文件夹">
            <el-tree-select
              v-model="editForm.folderId"
              :data="folderTree"
              :props="{ label: 'name', value: 'id', children: 'children' }"
              check-strictly
              clearable
              style="width: 300px"
            />
            <span class="label-gap">评审周期</span>
            <el-input-number v-model="editForm.reviewMonths" :min="1" :max="120" style="width: 110px" />
            <span class="label-gap">个月</span>
          </el-form-item>
        </el-form>

        <div class="section-head">章节内容</div>
        <div v-for="(item, index) in editSections" :key="item.key" class="section-block">
          <div class="section-title">
            <span>{{ item.key }} {{ item.title }}</span>
            <el-tag v-if="item.required" type="danger" size="small" effect="plain">必填</el-tag>
          </div>
          <el-input
            v-if="detail.status === 'DRAFT' && canWrite"
            v-model="editSections[index].content"
            type="textarea"
            :rows="3"
            maxlength="5000"
          />
          <div v-else class="section-readonly">{{ item.content || '（空）' }}</div>
        </div>
        <el-empty v-if="!editSections.length" description="该文件没有章节" :image-size="50" />

        <div class="section-head">版本历史</div>
        <el-table :data="detail.versions" border size="small">
          <el-table-column label="版本" width="70" align="center">
            <template #default="{ row }">v{{ row.versionNo }}</template>
          </el-table-column>
          <el-table-column prop="changeNote" label="修订说明" min-width="180" show-overflow-tooltip />
          <el-table-column label="发布人" width="110">
            <template #default="{ row }">{{ row.releasedByName || '-' }}</template>
          </el-table-column>
          <el-table-column label="发布时间" width="160">
            <template #default="{ row }">{{ formatTime(row.releasedAt) }}</template>
          </el-table-column>
          <el-table-column label="章节数" width="80" align="center">
            <template #default="{ row }">{{ row.sectionCount }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!detail.versions.length" description="还没有发布版本" :image-size="50" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  sysDocApi,
  DOC_LEVEL_LABEL,
  DOC_STATUS_LABEL,
  type DocDetail,
  type DocRow,
  type DocSection,
  type FolderRow,
  type TemplateRow
} from '@/api/sysdoc'
import { workflowApi, type ActorUserRow } from '@/api/workflow'
import { useUserStore } from '@/stores/user'

interface FolderNode {
  id: string
  name: string
  docCount: number
  children?: FolderNode[]
}

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('qlt.sysdoc.write'))

const loading = ref(false)
const rows = ref<DocRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const status = ref('')
const docLevel = ref('')
const dueOnly = ref(false)
const folders = ref<FolderRow[]>([])
const folderId = ref<string | null>(null)

const templates = ref<TemplateRow[]>([])
const users = ref<ActorUserRow[]>([])

const folderVisible = ref(false)
const folderSaving = ref(false)
const folderForm = reactive<{ parentId: string | null; code: string; name: string }>({
  parentId: null,
  code: '',
  name: ''
})

const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({
  mode: 'template' as 'template' | 'manual',
  templateId: '',
  code: '',
  name: '',
  docLevel: 'PROCEDURE',
  folderId: null as string | null,
  reviewMonths: 12,
  ownerId: null as string | null
})

const detailVisible = ref(false)
const detail = ref<DocDetail | null>(null)
const editSections = ref<DocSection[]>([])
const saving = ref(false)
const editForm = reactive({
  name: '',
  docLevel: 'PROCEDURE',
  folderId: null as string | null,
  reviewMonths: 12
})

const folderTree = computed<FolderNode[]>(() => buildTree(folders.value))

onMounted(async () => {
  await Promise.all([loadFolders(), loadTemplates(), loadUsers()])
  await reload()
})

function buildTree(list: FolderRow[]): FolderNode[] {
  const nodes = new Map<string, FolderNode>()
  list.forEach((item) => {
    nodes.set(item.id, { id: item.id, name: item.name, docCount: item.docCount, children: [] })
  })
  const roots: FolderNode[] = []
  list.forEach((item) => {
    const node = nodes.get(item.id)
    if (!node) {
      return
    }
    const parent = item.parentId ? nodes.get(item.parentId) : undefined
    if (parent) {
      parent.children?.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

async function loadFolders(): Promise<void> {
  try {
    folders.value = await sysDocApi.folders()
  } catch {
    folders.value = []
  }
}

async function loadTemplates(): Promise<void> {
  try {
    templates.value = await sysDocApi.templates({ enabled: true })
  } catch {
    templates.value = []
  }
}

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
    const result = await sysDocApi.docs({
      keyword: keyword.value || undefined,
      folderId: folderId.value ?? undefined,
      status: status.value || undefined,
      docLevel: docLevel.value || undefined,
      dueReviewOnly: dueOnly.value ? true : undefined,
      page: page.value,
      size: size.value
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载体系文件失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(value: number): void {
  page.value = value
  void reload()
}

function selectFolder(id: string | null): void {
  folderId.value = id
  page.value = 1
  void reload()
}

/* ------------------------------------------------------------ 文件夹 */

function openFolderDialog(parentId: string | null): void {
  folderForm.parentId = parentId
  folderForm.code = ''
  folderForm.name = ''
  folderVisible.value = true
}

async function onSaveFolder(): Promise<void> {
  if (!folderForm.code.trim() || !folderForm.name.trim()) {
    ElMessage.warning('编码与名称都不能为空')
    return
  }
  folderSaving.value = true
  try {
    await sysDocApi.createFolder({
      code: folderForm.code.trim().toUpperCase(),
      name: folderForm.name.trim(),
      parentId: folderForm.parentId
    })
    ElMessage.success('已创建')
    folderVisible.value = false
    await loadFolders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建文件夹失败')
  } finally {
    folderSaving.value = false
  }
}

/* ------------------------------------------------------------ 新建文件 */

function openCreate(): void {
  createForm.mode = templates.value.length ? 'template' : 'manual'
  createForm.templateId = ''
  createForm.code = ''
  createForm.name = ''
  createForm.docLevel = 'PROCEDURE'
  createForm.folderId = null
  createForm.reviewMonths = 12
  createForm.ownerId = null
  createVisible.value = true
}

async function onCreate(): Promise<void> {
  if (!createForm.code.trim() || !createForm.name.trim()) {
    ElMessage.warning('文件编号与名称都不能为空')
    return
  }
  if (createForm.mode === 'template' && !createForm.templateId) {
    ElMessage.warning('请选择模板')
    return
  }
  creating.value = true
  try {
    const code = createForm.code.trim().toUpperCase()
    const created =
      createForm.mode === 'template'
        ? await sysDocApi.createFromTemplate({
            templateId: createForm.templateId,
            code,
            name: createForm.name.trim(),
            ownerId: createForm.ownerId
          })
        : await sysDocApi.create({
            code,
            name: createForm.name.trim(),
            docLevel: createForm.docLevel,
            folderId: createForm.folderId,
            reviewMonths: createForm.reviewMonths,
            ownerId: createForm.ownerId,
            sections: []
          })
    ElMessage.success('已创建，请补充章节内容后提交审批')
    createVisible.value = false
    await Promise.all([reload(), loadFolders()])
    await openDetail(created.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  } finally {
    creating.value = false
  }
}

/* ------------------------------------------------------------ 详情 */

async function openDetail(id: string): Promise<void> {
  try {
    const result = await sysDocApi.detail(id)
    applyDetail(result)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  }
}

function applyDetail(result: DocDetail): void {
  detail.value = result
  editSections.value = result.sections.map((item) => ({ ...item }))
  editForm.name = result.name
  editForm.docLevel = result.docLevel
  editForm.folderId = result.folderId ?? null
  editForm.reviewMonths = result.reviewMonths
}

async function onSaveDraft(): Promise<void> {
  if (!detail.value) {
    return
  }
  if (!editForm.name.trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  saving.value = true
  try {
    const saved = await sysDocApi.update(detail.value.id, {
      code: detail.value.code,
      name: editForm.name.trim(),
      docLevel: editForm.docLevel,
      folderId: editForm.folderId,
      ownerId: detail.value.ownerId ?? null,
      reviewMonths: editForm.reviewMonths,
      summary: detail.value.summary ?? null,
      sections: editSections.value
    })
    applyDetail(saved)
    ElMessage.success('已保存')
    await reload()
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
  try {
    await sysDocApi.submit(detail.value.id)
    ElMessage.success('已提交审批')
    await refreshDetail()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

async function onApprove(passed: boolean): Promise<void> {
  if (!detail.value) {
    return
  }
  let note = ''
  if (passed) {
    try {
      const result = await ElMessageBox.prompt('审批意见（可空）', '审批通过并发布', {
        confirmButtonText: '发布',
        cancelButtonText: '取消'
      })
      note = (result as { value?: string }).value?.trim() ?? ''
    } catch {
      return
    }
  } else {
    try {
      const result = await ElMessageBox.prompt('驳回原因（必填）', '驳回', {
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
    await sysDocApi.approve(detail.value.id, passed, note || null)
    ElMessage.success(passed ? '已发布' : '已驳回')
    await refreshDetail()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '审批失败')
  }
}

async function onRevise(): Promise<void> {
  if (!detail.value) {
    return
  }
  let note = ''
  try {
    const result = await ElMessageBox.prompt('本次修订说明（可空）', '发起修订', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    note = (result as { value?: string }).value?.trim() ?? ''
  } catch {
    return
  }
  try {
    await sysDocApi.revise(detail.value.id, note || null)
    ElMessage.success('已进入新版本草稿，改完重新提交审批')
    await refreshDetail()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修订失败')
  }
}

async function onObsolete(): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认作废 ${detail.value.code}？作废后不能再修订，但版本历史会保留。`,
      '作废',
      { confirmButtonText: '作废', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await sysDocApi.obsolete(detail.value.id)
    ElMessage.success('已作废')
    await refreshDetail()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '作废失败')
  }
}

async function onRemove(): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确认删除该草稿？', '删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await sysDocApi.remove(detail.value.id)
    ElMessage.success('已删除')
    detailVisible.value = false
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function refreshDetail(): Promise<void> {
  if (!detail.value) {
    return
  }
  const result = await sysDocApi.detail(detail.value.id)
  applyDetail(result)
  await Promise.all([reload(), loadFolders()])
}

/* ------------------------------------------------------------ 展示辅助 */

function statusType(value: string): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'RELEASED') {
    return 'success'
  }
  if (value === 'SUBMITTED') {
    return 'warning'
  }
  if (value === 'OBSOLETE') {
    return 'info'
  }
  return 'danger'
}

function dueReview(row: DocDetail): boolean {
  return (
    row.status === 'RELEASED' &&
    !!row.nextReviewDate &&
    row.nextReviewDate <= new Date().toISOString().slice(0, 10)
  )
}

function formatTime(value?: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.tree-card {
  min-height: 520px;
}

.tree-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tree-all {
  padding: 4px 8px;
  cursor: pointer;
  border-radius: 4px;
  margin-bottom: 4px;
}

.tree-all.active {
  background: #ecf5ff;
  color: #409eff;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.tree-count {
  color: #909399;
  font-size: 12px;
}

.tree-add {
  margin-left: auto;
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
  margin-left: 8px;
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

.edit-form {
  margin-bottom: 8px;
}

.section-head {
  font-weight: 600;
  margin: 16px 0 8px;
  padding-left: 6px;
  border-left: 3px solid #409eff;
}

.section-block {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  margin-bottom: 6px;
}

.section-readonly {
  white-space: pre-wrap;
  color: #303133;
  font-size: 13px;
  line-height: 1.7;
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
