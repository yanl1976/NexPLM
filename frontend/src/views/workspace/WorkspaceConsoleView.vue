<template>
  <div class="ws-console">
    <aside class="side">
      <div class="side-title">导航</div>
      <el-tree
        v-loading="treeLoading"
        :data="treeData"
        node-key="id"
        :props="{ label: 'label', children: 'children' }"
        default-expand-all
        highlight-current
        @node-click="onNodeClick"
      />
    </aside>

    <section class="main">
      <div class="crumb">
        下级对象列表
        <span v-if="pathLabel" class="path">：{{ pathLabel }}</span>
      </div>

      <div class="tools">
        <el-select v-model="filterType" size="small" style="width: 110px">
          <el-option label="全部" value="" />
          <el-option v-for="(label, value) in TYPE_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button size="small" :icon="Refresh" @click="reload">刷新</el-button>
        <el-button size="small" disabled>发送</el-button>
        <el-button size="small" type="primary" @click="openCreate">新建</el-button>
        <el-button size="small" :disabled="!selectedRows.length" @click="openEdit">编辑</el-button>
        <el-button size="small" :disabled="!selectedRows.length" @click="onCheckout">检出</el-button>
        <el-button size="small" :disabled="!selectedRows.length" @click="onCheckin">取消检出</el-button>
        <el-button size="small" disabled>添加</el-button>
        <el-button size="small" :disabled="!selectedRows.length" @click="openBom">BOM 管理</el-button>
        <el-button size="small" disabled>产品完整性检查</el-button>
        <el-button size="small" disabled>发起流程</el-button>
        <el-button size="small" disabled>修订</el-button>
        <el-button size="small" disabled>权限更改</el-button>
        <el-button size="small" disabled>ERP 集成</el-button>
        <el-button size="small" disabled>下载</el-button>
      </div>

      <el-table
        v-loading="objectLoading"
        :data="visibleObjects"
        size="small"
        border
        height="calc(100vh - 220px)"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="42" />
        <el-table-column prop="object_type" label="对象类型" width="110" />
        <el-table-column prop="item_type" label="物料类型" width="100" />
        <el-table-column prop="code" label="编号" width="150" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="project_code" label="项目代号" width="110" />
        <el-table-column label="版本" width="80">
          <template #default="{ row }">A.{{ String(row.iteration ?? 1).padStart(3, '0') }}</template>
        </el-table-column>
        <el-table-column prop="using_state" label="使用状态" width="100" />
        <el-table-column prop="specification" label="规格型号/图号" min-width="150" />
        <el-table-column prop="checkout_user" label="检出人" width="100">
          <template #default="{ row }">{{ row.checkout_user || '-' }}</template>
        </el-table-column>
        <el-table-column label="检出状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.checkout_user" size="small" type="warning">已检出</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="checked_out_at" label="检出时间" width="160">
          <template #default="{ row }">{{ row.checked_out_at?.replace('T', ' ').slice(0, 19) || '-' }}</template>
        </el-table-column>
        <template #empty>
          <span class="hint">该文件夹下暂无对象</span>
        </template>
      </el-table>
    </section>

    <!-- 新建：就地弹窗，不跳页 -->
    <el-dialog v-model="createVisible" title="新建零部件" width="720px">
      <el-form :model="createForm" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="名称" required>
              <el-input v-model="createForm.name" placeholder="零部件名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型">
              <el-select v-model="createForm.itemType" style="width: 100%">
                <el-option label="零件" value="PART" />
                <el-option label="标准件" value="STANDARD" />
                <el-option label="外购件" value="OUTSOURCE" />
                <el-option label="成品" value="PRODUCT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-cascader
                v-model="createForm.classId"
                :options="classTree"
                :props="{ value: 'id', label: 'name', children: 'children', emitPath: false }"
                clearable
                filterable
                placeholder="只能选最末级（小类）"
                style="width: 100%"
                @change="refreshPreview"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目" required>
              <el-select v-model="createForm.projectId" filterable clearable style="width: 100%">
                <el-option
                  v-for="project in projects"
                  :key="project.id"
                  :label="`${project.code} ${project.name}`"
                  :value="String(project.id)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图号">
              <el-input v-model="createForm.dwgNo" placeholder="填图号可自动生成编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="位置">
              <el-input v-model="createForm.location" placeholder="绑定项目后自动生成" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格型号">
              <el-input v-model="createForm.specification" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计量单位">
              <el-input v-model="createForm.unit" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="编码">
              <el-input :model-value="previewCode" readonly class="code-readonly" placeholder="选择分类后自动生成" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSaving" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑：就地弹窗 -->
    <el-dialog v-model="editVisible" title="编辑零部件" width="640px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="editForm.specification" />
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="editForm.unit" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="editForm.location" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- BOM：只读树，不跳页 -->
    <el-dialog v-model="bomVisible" title="BOM 结构" width="720px">
      <el-table v-loading="bomLoading" :data="bomRows" size="small" border max-height="420">
        <el-table-column prop="code" label="编号" width="150" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="qty" label="用量" width="90" align="right" />
        <el-table-column prop="unit" label="单位" width="80" />
      </el-table>
      <template #footer>
        <el-button @click="bomVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { wsConsoleApi, type WsFolder, type WsObject } from '@/api/wsConsole'
import {
  bomApi,
  itemApi,
  itemClassApi,
  type BomNode,
  type CodePreview,
  type ItemClassNode,
  type ItemDesignAttr
} from '@/api/item'

interface TreeNode {
  id: string
  label: string
  space: string
  folderId?: string
  projectId?: string | null
  children: TreeNode[]
}

const TYPE_LABEL: Record<string, string> = {
  PdmItem: '零部件',
  PdmDoc: '文档',
  PdmCad: '图纸',
  PdmBom: 'BOM'
}

const folders = ref<WsFolder[]>([])
const treeLoading = ref(false)
const objects = ref<WsObject[]>([])
const objectLoading = ref(false)
const pathLabel = ref('')
const filterType = ref('')
const selectedRows = ref<WsObject[]>([])
const currentFolder = ref<TreeNode | null>(null)

const visibleObjects = computed(() =>
  filterType.value ? objects.value.filter((row) => row.object_type === filterType.value) : objects.value
)

function buildChildren(space: string): TreeNode[] {
  const rows = folders.value.filter((row) => row.space === space)
  const map = new Map<string, TreeNode>()
  for (const row of rows) {
    const prefix = row.project_name ? `${row.project_name} / ` : ''
    map.set(row.id, {
      id: `${space}:${row.id}`,
      label: `${prefix}${row.name}`,
      space,
      folderId: row.id,
      projectId: row.project_id ?? null,
      children: []
    })
  }
  const roots: TreeNode[] = []
  for (const row of rows) {
    const node = map.get(row.id)!
    const parent = row.parent_id ? map.get(row.parent_id) : undefined
    if (parent) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  }
  return roots
}

const treeData = computed<TreeNode[]>(() => [
  { id: 'root:PUBLIC', label: '公共空间', space: 'PUBLIC', children: buildChildren('PUBLIC') },
  { id: 'root:MY', label: '个人工作区', space: 'MY', children: buildChildren('MY') },
  { id: 'root:PROJECT', label: '项目空间', space: 'PROJECT', children: buildChildren('PROJECT') }
])

async function loadTree(): Promise<void> {
  treeLoading.value = true
  try {
    const [pub, mine, projects] = await Promise.all([
      wsConsoleApi.tree('PUBLIC'),
      wsConsoleApi.tree('MY'),
      wsConsoleApi.tree('PROJECT')
    ])
    folders.value = [...pub, ...mine, ...projects]
  } catch (error) {
    ElMessage.error((error as Error).message || '加载工作空间失败')
  } finally {
    treeLoading.value = false
  }
}

async function onNodeClick(node: TreeNode): Promise<void> {
  currentFolder.value = node
  pathLabel.value = node.label
  objects.value = []
  if (!node.folderId) {
    return
  }
  objectLoading.value = true
  try {
    objects.value = await wsConsoleApi.objects(node.space, node.folderId)
  } catch (error) {
    ElMessage.error((error as Error).message || '加载文件夹内容失败')
  } finally {
    objectLoading.value = false
  }
}

async function reload(): Promise<void> {
  await loadTree()
  objects.value = []
  pathLabel.value = ''
}

function onSelectionChange(rows: WsObject[]): void {
  selectedRows.value = rows
}

async function refreshObjects(): Promise<void> {
  if (currentFolder.value?.folderId) {
    await onNodeClick(currentFolder.value)
  }
}

async function onCheckout(): Promise<void> {
  const row = selectedRows.value[0]
  if (!row) {
    ElMessage.warning('请先选择一行')
    return
  }
  try {
    await wsConsoleApi.checkout(row.id)
    ElMessage.success('已检出')
    await refreshObjects()
  } catch (error) {
    ElMessage.error((error as Error).message || '检出失败（可能已被他人检出）')
  }
}

async function onCheckin(): Promise<void> {
  const row = selectedRows.value[0]
  if (!row) {
    ElMessage.warning('请先选择一行')
    return
  }
  try {
    await wsConsoleApi.checkin(row.id)
    ElMessage.success('已取消检出')
    await refreshObjects()
  } catch (error) {
    ElMessage.error((error as Error).message || '取消检出失败')
  }
}

// ---------------- 新建（就地弹窗） ----------------
const EXCLUDED_ROOTS = ['80', 'CH']

interface SelectableClass extends ItemClassNode {
  selectDisabled: boolean
}

function toSelectable(nodes: ItemClassNode[]): SelectableClass[] {
  return nodes
    .filter((node) => !EXCLUDED_ROOTS.includes(node.code))
    .map((node) => ({
      ...node,
      selectDisabled: (node.children?.length ?? 0) > 0,
      children: toSelectable(node.children ?? [])
    }))
}

const classTree = ref<SelectableClass[]>([])
const projects = ref<Array<{ id: string; code: string; name: string }>>([])
const createVisible = ref(false)
const createSaving = ref(false)
const createPreview = ref<CodePreview | null>(null)
const previewCode = computed(() => createPreview.value?.code ?? '')
const createForm = reactive({
  name: '',
  itemType: 'PART',
  classId: null as string | null,
  projectId: '' as string,
  dwgNo: '',
  location: '',
  specification: '',
  unit: 'EA'
})

async function loadBasics(): Promise<void> {
  try {
    const [tree, projectList] = await Promise.all([itemClassApi.tree(), itemApi.projectOptions()])
    classTree.value = toSelectable(tree)
    projects.value = projectList
  } catch {
    classTree.value = []
    projects.value = []
  }
}

async function refreshPreview(): Promise<void> {
  createPreview.value = null
  try {
    createPreview.value = await itemApi.previewCode({
      classId: createForm.classId ?? undefined,
      itemType: createForm.itemType,
      dwgNo: createForm.dwgNo.trim() || undefined
    })
  } catch {
    createPreview.value = null
  }
}

function openCreate(): void {
  if (!currentFolder.value?.folderId) {
    ElMessage.warning('请先在左侧选择文件夹')
    return
  }
  createForm.name = ''
  createForm.itemType = 'PART'
  createForm.classId = null
  createForm.projectId =
    currentFolder.value.space === 'PROJECT' && currentFolder.value.projectId
      ? String(currentFolder.value.projectId)
      : ''
  createForm.dwgNo = ''
  createForm.location = ''
  createForm.specification = ''
  createForm.unit = 'EA'
  createPreview.value = null
  refreshPreview()
  createVisible.value = true
}

async function submitCreate(): Promise<void> {
  if (!createForm.name.trim()) {
    ElMessage.warning('请填写名称')
    return
  }
  if (!createForm.projectId) {
    ElMessage.warning('请选择项目')
    return
  }
  createSaving.value = true
  try {
    const designAttr = { dwgNo: createForm.dwgNo.trim() || null } as unknown as ItemDesignAttr
    const project = projects.value.find((p) => String(p.id) === String(createForm.projectId))
    await itemApi.create({
      name: createForm.name.trim(),
      itemType: createForm.itemType,
      sourceType: 'MAKE',
      classId: createForm.classId,
      projectId: createForm.projectId,
      location: createForm.location || (project ? `个人空间/${project.code}/物料` : null),
      specification: createForm.specification,
      unit: createForm.unit,
      designAttr
    })
    ElMessage.success('已创建')
    createVisible.value = false
    await loadTree()
    await refreshObjects()
  } catch (error) {
    ElMessage.error((error as Error).message || '创建失败')
  } finally {
    createSaving.value = false
  }
}

// ---------------- 编辑（就地弹窗） ----------------
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = reactive({
  id: '',
  name: '',
  specification: '',
  unit: '',
  remark: '',
  location: ''
})

async function openEdit(): Promise<void> {
  const row = selectedRows.value[0]
  if (!row) {
    ElMessage.warning('请先选择一行')
    return
  }
  if (row.object_type !== 'PdmItem') {
    ElMessage.info('当前仅支持零部件')
    return
  }
  try {
    const detail = await itemApi.detail(row.id)
    editForm.id = row.id
    editForm.name = detail.name ?? ''
    editForm.specification = detail.specification ?? ''
    editForm.unit = detail.unit ?? ''
    editForm.remark = detail.remark ?? ''
    editForm.location = (detail as unknown as { location?: string }).location ?? ''
    editVisible.value = true
  } catch (error) {
    ElMessage.error((error as Error).message || '加载详情失败')
  }
}

async function submitEdit(): Promise<void> {
  editSaving.value = true
  try {
    await itemApi.update(editForm.id, {
      name: editForm.name,
      itemType: 'PART',
      specification: editForm.specification,
      unit: editForm.unit,
      remark: editForm.remark
    })
    ElMessage.success('已保存')
    editVisible.value = false
    await refreshObjects()
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败')
  } finally {
    editSaving.value = false
  }
}

// ---------------- BOM（只读弹窗） ----------------
const bomVisible = ref(false)
const bomLoading = ref(false)
const bomRows = ref<BomNode[]>([])

async function openBom(): Promise<void> {
  const row = selectedRows.value[0]
  if (!row) {
    ElMessage.warning('请先选择一行')
    return
  }
  bomVisible.value = true
  bomLoading.value = true
  try {
    bomRows.value = await bomApi.tree(row.id, 'DESIGN', 3)
  } catch (error) {
    bomRows.value = []
    ElMessage.error((error as Error).message || '加载 BOM 失败')
  } finally {
    bomLoading.value = false
  }
}

onMounted(() => {
  loadTree()
  loadBasics()
})
</script>

<style scoped>
.ws-console {
  display: grid;
  grid-template-columns: 290px 1fr;
  gap: 12px;
  padding: 12px;
}

.side {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  padding: 8px;
  max-height: calc(100vh - 110px);
  overflow: auto;
}

.side-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}

.main {
  min-width: 0;
}

.crumb {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

.path {
  color: var(--el-text-color-primary);
}

.tools {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.code-readonly :deep(.el-input__wrapper) {
  background-color: var(--el-fill-color-dark, #e6e8eb);
}
</style>
