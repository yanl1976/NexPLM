<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="编码/名称" clearable style="width: 200px" @keyup.enter="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建模板</el-button>
      <span class="hint">模板规定「文件层级 + 默认文件夹 + 评审周期 + 章节骨架」，按模板生成文件后章节必有、必填章节不填不能提交审批</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="code" label="编码" width="130" />
      <el-table-column prop="name" label="模板名称" min-width="200" show-overflow-tooltip />
      <el-table-column label="层级" width="110" align="center">
        <template #default="{ row }">{{ DOC_LEVEL_LABEL[row.docLevel] ?? row.docLevel }}</template>
      </el-table-column>
      <el-table-column label="默认文件夹" width="150">
        <template #default="{ row }">{{ row.folderName || '-' }}</template>
      </el-table-column>
      <el-table-column label="评审周期" width="100" align="center">
        <template #default="{ row }">{{ row.defaultReviewMonths }} 个月</template>
      </el-table-column>
      <el-table-column label="章节数" width="90" align="center">
        <template #default="{ row }">{{ row.sectionCount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
          <el-button v-if="canWrite" link type="danger" @click="onRemove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="还没有体系文件模板，新建一个或用「填入程序文件骨架」快速开始" :image-size="60" />

    <el-dialog v-model="formVisible" :title="form.id ? `编辑模板 ${form.code}` : '新建体系文件模板'" width="880px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.name" maxlength="200" style="width: 300px" />
          <span class="label-gap">层级</span>
          <el-select v-model="form.docLevel" style="width: 150px">
            <el-option v-for="(label, value) in DOC_LEVEL_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认文件夹">
          <el-tree-select
            v-model="form.folderId"
            :data="folderTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="生成文件时默认放这里"
            style="width: 300px"
          />
          <span class="label-gap">评审周期</span>
          <el-input-number v-model="form.defaultReviewMonths" :min="1" :max="120" style="width: 120px" />
          <span class="label-gap">个月</span>
          <span class="label-gap">启用</span>
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <div class="steps-head">
        <span class="section-title">章节骨架（顺序即文件里的章节顺序）</span>
        <el-button size="small" @click="fillSkeleton">填入程序文件骨架</el-button>
        <el-button size="small" type="primary" plain @click="addSection">新增章节</el-button>
      </div>
      <el-table :data="sections" border size="small">
        <el-table-column label="编号" width="90">
          <template #default="{ row }"><el-input v-model="row.key" maxlength="64" /></template>
        </el-table-column>
        <el-table-column label="章节标题" min-width="160">
          <template #default="{ row }"><el-input v-model="row.title" maxlength="200" /></template>
        </el-table-column>
        <el-table-column label="填写提示" min-width="200">
          <template #default="{ row }"><el-input v-model="row.hint" maxlength="500" /></template>
        </el-table-column>
        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }"><el-checkbox v-model="row.required" /></template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ $index }">
            <el-button link type="primary" :disabled="$index === 0" @click="moveSection($index, -1)">上移</el-button>
            <el-button link type="primary" :disabled="$index === sections.length - 1" @click="moveSection($index, 1)">下移</el-button>
            <el-button link type="danger" @click="sections.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!sections.length" description="还没有章节，可点「填入程序文件骨架」" :image-size="50" />

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  sysDocApi,
  DOC_LEVEL_LABEL,
  type FolderRow,
  type SectionTemplate,
  type TemplateRow
} from '@/api/sysdoc'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('qlt.sysdoc.write'))

interface SectionDraft {
  key: string
  title: string
  hint: string
  required: boolean
}

const loading = ref(false)
const saving = ref(false)
const rows = ref<TemplateRow[]>([])
const folders = ref<FolderRow[]>([])
const keyword = ref('')

const formVisible = ref(false)
const form = reactive({
  id: '',
  code: '',
  name: '',
  docLevel: 'PROCEDURE',
  folderId: null as string | null,
  defaultReviewMonths: 12,
  enabled: true,
  remark: ''
})
const sections = ref<SectionDraft[]>([])

/** 程序文件通用骨架（IATF 常见 8 章），填进去再按企业实际删改。 */
const SKELETON: SectionDraft[] = [
  { key: 'S1', title: '目的', hint: '这份文件要解决什么问题', required: true },
  { key: 'S2', title: '适用范围', hint: '适用于哪些部门/产品/过程', required: true },
  { key: 'S3', title: '术语与定义', hint: '本文件专用术语', required: false },
  { key: 'S4', title: '职责', hint: '谁编制、谁审批、谁执行', required: true },
  { key: 'S5', title: '工作程序', hint: '按步骤写清输入/活动/输出与判定准则', required: true },
  { key: 'S6', title: '相关文件', hint: '引用的上层/下层文件编号', required: false },
  { key: 'S7', title: '相关记录', hint: '产生哪些记录、保存多久', required: false },
  { key: 'S8', title: '修订履历', hint: '版本/日期/修订内容', required: false }
]

const folderTree = computed(() => buildTree(folders.value))

onMounted(async () => {
  await loadFolders()
  await reload()
})

function buildTree(list: FolderRow[]): Array<Record<string, unknown>> {
  const nodes = new Map<string, Record<string, unknown>>()
  list.forEach((item) => {
    nodes.set(item.id, { id: item.id, name: item.name, children: [] as unknown[] })
  })
  const roots: Array<Record<string, unknown>> = []
  list.forEach((item) => {
    const node = nodes.get(item.id)
    if (!node) {
      return
    }
    const parent = item.parentId ? nodes.get(item.parentId) : undefined
    if (parent) {
      ;(parent.children as Array<Record<string, unknown>>).push(node)
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

async function reload(): Promise<void> {
  loading.value = true
  try {
    rows.value = await sysDocApi.templates({
      keyword: keyword.value || undefined
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载模板失败')
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  form.id = ''
  form.code = ''
  form.name = ''
  form.docLevel = 'PROCEDURE'
  form.folderId = null
  form.defaultReviewMonths = 12
  form.enabled = true
  form.remark = ''
  sections.value = SKELETON.map((item) => ({ ...item }))
  formVisible.value = true
}

async function openEdit(id: string): Promise<void> {
  try {
    const detail = await sysDocApi.template(id)
    form.id = detail.id
    form.code = detail.code
    form.name = detail.name
    form.docLevel = detail.docLevel
    form.folderId = detail.folderId ?? null
    form.defaultReviewMonths = detail.defaultReviewMonths
    form.enabled = detail.enabled
    form.remark = detail.remark ?? ''
    sections.value = detail.sections.map((item) => ({
      key: item.key,
      title: item.title,
      hint: item.hint ?? '',
      required: item.required
    }))
    formVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载模板失败')
  }
}

function addSection(): void {
  sections.value.push({
    key: `S${sections.value.length + 1}`,
    title: '',
    hint: '',
    required: true
  })
}

function moveSection(index: number, offset: number): void {
  const target = index + offset
  if (target < 0 || target >= sections.value.length) {
    return
  }
  const list = [...sections.value]
  const [moved] = list.splice(index, 1)
  list.splice(target, 0, moved)
  sections.value = list
}

function fillSkeleton(): void {
  sections.value = SKELETON.map((item) => ({ ...item }))
}

async function onSave(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请填写模板名称')
    return
  }
  const keys = new Set<string>()
  for (const item of sections.value) {
    const key = item.key.trim().toUpperCase()
    if (!key || !item.title.trim()) {
      ElMessage.warning('章节编号与标题都不能为空')
      return
    }
    if (keys.has(key)) {
      ElMessage.warning(`章节编号重复：${key}`)
      return
    }
    keys.add(key)
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      docLevel: form.docLevel,
      folderId: form.folderId,
      defaultReviewMonths: form.defaultReviewMonths,
      enabled: form.enabled,
      remark: form.remark.trim() || null,
      sections: sections.value.map<SectionTemplate>((item) => ({
        key: item.key.trim().toUpperCase(),
        title: item.title.trim(),
        hint: item.hint.trim() || null,
        required: item.required
      }))
    }
    if (form.id) {
      await sysDocApi.updateTemplate(form.id, payload)
    } else {
      await sysDocApi.createTemplate(payload)
    }
    ElMessage.success('已保存')
    formVisible.value = false
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onRemove(row: TemplateRow): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认删除模板 ${row.code}？已按它生成的体系文件不受影响。`,
      '删除模板',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await sysDocApi.removeTemplate(row.id)
    ElMessage.success('已删除')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
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

.steps-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
}

.section-title {
  font-weight: 600;
  margin-right: auto;
}

.label-gap {
  margin: 0 8px;
  color: #606266;
}
</style>
