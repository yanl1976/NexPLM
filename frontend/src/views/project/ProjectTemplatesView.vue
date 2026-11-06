<template>
  <div class="page">
    <div class="toolbar">
      <el-button link type="primary" @click="back">← 返回项目列表</el-button>
      <el-input v-model="keyword" placeholder="编码/名称" clearable style="width: 200px" @keyup.enter="load" />
      <el-button @click="load">查询</el-button>
      <el-button type="primary" @click="openCreate">新建模板</el-button>
      <span class="hint">模板 = 任务骨架（相对开工日偏移）+ 文档目录骨架；立项时一次性长成项目</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="code" label="编码" width="130" />
      <el-table-column prop="name" label="模板名称" min-width="200" show-overflow-tooltip />
      <el-table-column label="项目类型" width="100" align="center">
        <template #default="{ row }">{{ PROJECT_TYPE_LABEL[row.type] ?? row.type }}</template>
      </el-table-column>
      <el-table-column prop="taskCount" label="任务" width="80" align="center" />
      <el-table-column prop="folderCount" label="目录" width="80" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="还没有项目模板" :image-size="60" />

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="820px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.name" maxlength="200" />
        </el-form-item>
        <el-form-item label="项目类型">
          <el-radio-group v-model="form.type">
            <el-radio-button v-for="(label, value) in PROJECT_TYPE_LABEL" :key="value" :value="value">
              {{ label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
          <span class="hint">停用后不能再用于立项，但已生成的项目不受影响</span>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <el-tabs v-model="tab">
        <el-tab-pane label="任务骨架" name="task">
          <div class="toolbar inner">
            <el-button size="small" type="primary" plain @click="addTask">新增任务行</el-button>
            <el-button size="small" type="success" :loading="savingTasks" @click="saveTasks">保存任务骨架</el-button>
            <span class="hint">编码按行序自动生成（1 / 2 / 2.1），父任务必须在列表更上方</span>
          </div>
          <el-table :data="taskDrafts" size="small" border>
            <el-table-column label="父任务" width="140">
              <template #default="{ row }">
                <el-select v-model="row.parentWbsCode" clearable placeholder="（一级）" size="small" style="width: 100%">
                  <el-option v-for="option in parentOptions" :key="option" :label="option" :value="option" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="任务名称" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.name" size="small" placeholder="任务名称" />
              </template>
            </el-table-column>
            <el-table-column label="开始偏移(天)" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.startOffset" :min="0" size="small" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="工期(天)" width="110">
              <template #default="{ row }">
                <el-input-number v-model="row.durationDays" :min="1" size="small" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" @click="taskDrafts.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="hint">已生成编码：{{ template?.tasks.map((item) => item.wbsCode).join(' / ') || '-' }}</div>
        </el-tab-pane>

        <el-tab-pane label="文档目录" name="folder">
          <div class="toolbar inner">
            <el-button size="small" type="primary" plain @click="addFolder">新增目录行</el-button>
            <el-button size="small" type="success" :loading="savingFolders" @click="saveFolders">保存目录骨架</el-button>
            <span class="hint">父目录必须是 / 或列表更上方的目录</span>
          </div>
          <el-table :data="folderDrafts" size="small" border>
            <el-table-column label="父目录" width="220">
              <template #default="{ row }">
                <el-select v-model="row.parentPath" size="small" style="width: 100%">
                  <el-option v-for="option in folderOptions" :key="option" :label="option" :value="option" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="目录名称" min-width="220">
              <template #default="{ row }">
                <el-input v-model="row.name" size="small" placeholder="如 01-设计文档" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" @click="folderDrafts.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="drawerVisible = false">关闭</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存主档</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  templateApi,
  PROJECT_TYPE_LABEL,
  type ProjectTemplateRow,
  type TemplateDetail,
  type TemplateFolderSaveReq,
  type TemplateTaskSaveReq
} from '@/api/project'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const savingTasks = ref(false)
const savingFolders = ref(false)
const keyword = ref('')
const rows = ref<ProjectTemplateRow[]>([])

const drawerVisible = ref(false)
const tab = ref<'task' | 'folder'>('task')
const template = ref<TemplateDetail | null>(null)
const taskDrafts = ref<TemplateTaskSaveReq[]>([])
const folderDrafts = ref<TemplateFolderSaveReq[]>([])

const form = reactive<{ id: string; name: string; type: string; enabled: boolean; remark: string }>({
  id: '',
  name: '',
  type: 'RD',
  enabled: true,
  remark: ''
})

const drawerTitle = computed(() => (form.id ? `编辑模板 ${template.value?.code ?? ''}` : '新建模板'))

/** 父任务只能选已生成的编码（服务端生成，前端不猜）。 */
const parentOptions = computed(() => (template.value?.tasks ?? []).map((item) => item.wbsCode))

const folderOptions = computed(() => {
  const options = ['/']
  folderDrafts.value.forEach((row) => {
    if (row.name?.trim()) {
      options.push(`${row.parentPath === '/' ? '' : row.parentPath}${row.name.trim()}/`)
    }
  })
  return options
})

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await templateApi.list({ keyword: keyword.value || undefined, page: 1, size: 100 })
    rows.value = page.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载模板失败')
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  form.id = ''
  form.name = ''
  form.type = 'RD'
  form.enabled = true
  form.remark = ''
  template.value = null
  taskDrafts.value = []
  folderDrafts.value = []
  tab.value = 'task'
  drawerVisible.value = true
}

async function openEdit(row: ProjectTemplateRow): Promise<void> {
  form.id = row.id
  form.name = row.name
  form.type = row.type
  form.enabled = row.enabled
  form.remark = row.remark ?? ''
  drawerVisible.value = true
  await reloadDetail(row.id)
}

async function reloadDetail(id: string): Promise<void> {
  try {
    const detail = await templateApi.detail(id)
    template.value = detail
    form.name = detail.name
    form.type = detail.type
    form.enabled = detail.enabled
    form.remark = detail.remark ?? ''
    const codeById = new Map<string, string>()
    detail.tasks.forEach((item) => codeById.set(item.id, item.wbsCode))
    taskDrafts.value = detail.tasks.map((item) => ({
      parentWbsCode: item.parentId ? codeById.get(item.parentId) ?? null : null,
      name: item.name,
      type: item.type,
      startOffset: item.startOffset,
      durationDays: item.durationDays,
      remark: item.remark ?? null
    }))
    folderDrafts.value = detail.folders.map((item) => ({
      parentPath: item.parentPath,
      name: item.name,
      sortNo: item.sortNo
    }))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载模板详情失败')
  }
}

function addTask(): void {
  taskDrafts.value.push({ parentWbsCode: null, name: '', type: 'TASK', startOffset: 0, durationDays: 1 })
}

function addFolder(): void {
  folderDrafts.value.push({ parentPath: '/', name: '', sortNo: (folderDrafts.value.length + 1) * 10 })
}

async function onSave(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请填写模板名称')
    return
  }
  saving.value = true
  try {
    const payload = { name: form.name.trim(), type: form.type, enabled: form.enabled, remark: form.remark || null }
    if (form.id) {
      template.value = await templateApi.update(form.id, payload)
      ElMessage.success('已保存')
      await load()
    } else {
      const created = await templateApi.create(payload)
      form.id = created.id
      await reloadDetail(created.id)
      ElMessage.success('模板已创建，可继续维护任务与目录骨架')
      await load()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function saveTasks(): Promise<void> {
  if (!form.id) {
    ElMessage.warning('请先保存模板主档')
    return
  }
  if (taskDrafts.value.some((row) => !row.name?.trim())) {
    ElMessage.warning('任务名称不能为空')
    return
  }
  savingTasks.value = true
  try {
    template.value = await templateApi.replaceTasks(form.id, taskDrafts.value)
    await reloadDetail(form.id)
    ElMessage.success('任务骨架已保存（编码已按行序生成）')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存任务骨架失败')
  } finally {
    savingTasks.value = false
  }
}

async function saveFolders(): Promise<void> {
  if (!form.id) {
    ElMessage.warning('请先保存模板主档')
    return
  }
  if (folderDrafts.value.some((row) => !row.name?.trim())) {
    ElMessage.warning('目录名称不能为空')
    return
  }
  savingFolders.value = true
  try {
    template.value = await templateApi.replaceFolders(form.id, folderDrafts.value)
    await reloadDetail(form.id)
    ElMessage.success('目录骨架已保存')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存目录骨架失败')
  } finally {
    savingFolders.value = false
  }
}

async function onDelete(row: ProjectTemplateRow): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认删除模板「${row.name}」？已用该模板立项的项目不受影响（立项是快照）`,
      '删除模板',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await templateApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function back(): void {
  router.push('/project')
}
</script>

<style scoped>
.toolbar.inner {
  margin-bottom: 8px;
}

.hint {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}
</style>
