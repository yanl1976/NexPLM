<template>
  <div class="ws-shell">
    <header class="ws-header">
      <div class="ws-header-left">
        <span class="ws-title">工作空间</span>
        <span class="ws-header-sep"></span>
        <span class="ws-header-sub">个人门户 / 工作空间</span>
      </div>
      <div class="ws-header-right">
        <el-input v-model="keyword" size="small" clearable placeholder="在当前结果中查找" style="width: 210px">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button size="small" :icon="Refresh" @click="reload">刷新</el-button>
      </div>
    </header>

    <div class="ws-body">
      <aside class="side">
        <div class="side-head">
          <span class="side-head-title">空间导航</span>
          <el-input v-model="folderFilter" size="small" clearable placeholder="过滤文件夹" />
        </div>
        <div class="side-tree">
          <el-tree
            ref="treeRef"
            v-loading="treeLoading"
            :data="treeData"
            node-key="id"
            :props="{ label: 'label', children: 'children' }"
            :filter-node-method="filterNode"
            default-expand-all
            highlight-current
            @node-click="onNodeClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node" :class="{ 'tree-root': !data.folderId }">
                <span
                  class="tree-icon"
                  :class="[`space-${String(data.space).toLowerCase()}`, { 'is-root': !data.folderId }]"
                >
                  <el-icon>
                    <HomeFilled v-if="!data.folderId && data.space === 'PUBLIC'" />
                    <User v-else-if="!data.folderId && data.space === 'MY'" />
                    <Collection v-else-if="!data.folderId && data.space === 'PROJECT'" />
                    <FolderOpened v-else-if="node.expanded" />
                    <Folder v-else />
                  </el-icon>
                </span>
                <span class="tree-label">{{ data.label }}</span>
                <span v-if="data.children?.length" class="tree-badge">{{ data.children.length }}</span>
              </span>
            </template>
          </el-tree>
        </div>
        <div class="side-foot">共 {{ folderCount }} 个文件夹</div>
      </aside>

      <section class="main">
        <div class="crumb-bar">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>
              <span class="crumb-root"><el-icon><HomeFilled /></el-icon>工作空间</span>
            </el-breadcrumb-item>
            <el-breadcrumb-item>{{ spaceLabel }}</el-breadcrumb-item>
            <el-breadcrumb-item v-if="pathLabel">{{ pathLabel }}</el-breadcrumb-item>
          </el-breadcrumb>
          <span class="crumb-count">下级对象列表（{{ searchedObjects.length }}）</span>
        </div>

        <div class="toolbar">
          <div class="tool-group">
            <el-select v-model="filterType" size="small" style="width: 100px">
              <el-option label="全部" value="" />
              <el-option v-for="(label, value) in TYPE_LABEL" :key="value" :label="label" :value="value" />
            </el-select>
            <el-button size="small" :icon="Refresh" @click="reload">刷新</el-button>
          </div>
          <el-divider direction="vertical" />
          <div class="tool-group">
            <el-dropdown size="small" trigger="click" @command="onCreateCommand">
              <el-button size="small" type="primary">
                新建<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="item">零部件</el-dropdown-item>
                  <el-dropdown-item command="doc" disabled>文档（待接入）</el-dropdown-item>
                  <el-dropdown-item command="purchase" disabled>请购单（待接入）</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" :disabled="!selectedRows.length" @click="openEdit">编辑</el-button>
            <el-button size="small" @click="openAdd">添加</el-button>
            <el-button size="small" disabled>删除</el-button>
          </div>
          <el-divider direction="vertical" />
          <div class="tool-group">
            <el-button size="small" :disabled="!selectedRows.length" @click="onCheckout">检出</el-button>
            <el-button size="small" :disabled="!selectedRows.length" @click="onCheckin">取消检出</el-button>
            <el-button size="small" :disabled="!selectedRows.length" @click="openBom">BOM 管理</el-button>
            <el-button size="small" disabled>产品完整性检查</el-button>
          </div>
          <el-divider direction="vertical" />
          <div class="tool-group">
            <el-button size="small" disabled>发起流程</el-button>
            <el-button size="small" disabled>修订</el-button>
            <el-button size="small" disabled>权限更改</el-button>
            <el-button size="small" disabled>ERP 集成</el-button>
            <el-button size="small" disabled>下载</el-button>
          </div>
        </div>

        <div class="table-wrap">
          <el-table
            v-loading="objectLoading"
            :data="pagedObjects"
            size="small"
            border
            stripe
            height="100%"
            @selection-change="onSelectionChange"
          >
            <el-table-column type="index" label="序号" width="58" align="center" />
            <el-table-column type="selection" width="38" />
            <el-table-column label="对象类型" width="110">
              <template #default="{ row }">
                <span class="type-cell"><el-icon><component :is="typeIcon(row.object_type)" /></el-icon>{{ TYPE_LABEL[row.object_type] || row.object_type }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="code" label="编号" width="150" />
            <el-table-column prop="name" label="名称" min-width="180" />
            <el-table-column prop="project_code" label="项目代号" width="105" />
            <el-table-column label="版本" width="78">
              <template #default="{ row }">A.{{ String(row.iteration ?? 1).padStart(3, '0') }}</template>
            </el-table-column>
            <el-table-column prop="using_state" label="使用状态" width="95" />
            <el-table-column prop="specification" label="规格型号/图号" min-width="150" />
            <el-table-column prop="item_type" label="物料类型" width="95" />
            <el-table-column prop="checkout_user" label="检出人" width="95">
              <template #default="{ row }">{{ row.checkout_user || '-' }}</template>
            </el-table-column>
            <el-table-column label="检出状态" width="92">
              <template #default="{ row }">
                <el-tag v-if="row.checkout_user" size="small" type="warning" effect="plain">已检出</el-tag>
                <span v-else class="muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="checked_out_at" label="检出时间" width="160">
              <template #default="{ row }">{{ row.checked_out_at?.replace('T', ' ').slice(0, 19) || '-' }}</template>
            </el-table-column>
            <template #empty>
              <span class="hint">该文件夹下暂无对象</span>
            </template>
          </el-table>
        </div>

        <div class="pager">
          <span class="pager-info">共 {{ searchedObjects.length }} 条 · 每页 {{ pageSize }}</span>
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="searchedObjects.length"
            :page-sizes="[20, 50, 100]"
            layout="prev, pager, next, sizes"
            size="small"
          />
          <el-button size="small" :icon="Refresh" @click="reload">刷新</el-button>
        </div>
      </section>
    </div>

    <footer class="status-bar">
      <span>当前用户：{{ userLabel }}</span>
      <span>{{ currentFolder?.space === 'PROJECT' ? '项目空间' : (currentFolder?.space === 'MY' ? '个人工作区' : '公共空间') }}</span>
      <span>对象 {{ searchedObjects.length }} 条</span>
      <span class="status-right">关于我们 · 服务条款 · 法律声明</span>
    </footer>

    <ItemFormDialog
      v-model="formVisible"
      :editing-id="formEditingId"
      :preset-project-id="presetProjectId"
      @saved="onFormSaved"
    />

    <!-- 添加：把已有对象挂到当前文件夹 -->
    <el-dialog v-model="addVisible" title="添加已有对象" width="560px">
      <el-form label-width="80px">
        <el-form-item label="对象">
          <el-select
            v-model="addObjectId"
            filterable
            remote
            :remote-method="searchObjects"
            :loading="addSearching"
            placeholder="输入编码或名称搜索"
            style="width: 100%"
          >
            <el-option
              v-for="item in addOptions"
              :key="item.id"
              :label="`${item.code} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标">
          <span class="hint">{{ pathLabel || '请先选择文件夹' }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addSaving" :disabled="!addObjectId" @click="submitAdd">添加</el-button>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, Box, Collection, Document, Folder, FolderOpened, HomeFilled, Refresh, Search, Tickets, User } from '@element-plus/icons-vue'
import { wsConsoleApi, type WsFolder, type WsObject } from '@/api/wsConsole'
import ItemFormDialog from '@/components/business/ItemFormDialog.vue'
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

const TYPE_ICON: Record<string, unknown> = {
  PdmItem: Box,
  PdmDoc: Document,
  PdmCad: Document,
  PdmBom: Tickets
}

function typeIcon(objectType: string): unknown {
  return TYPE_ICON[objectType] ?? Folder
}

const folders = ref<WsFolder[]>([])
const treeLoading = ref(false)
const objects = ref<WsObject[]>([])
const objectLoading = ref(false)
const pathLabel = ref('')
const filterType = ref('')
const keyword = ref('')
const folderFilter = ref('')
const page = ref(1)
const pageSize = ref(50)
const treeRef = ref<any>(null)
const folderCount = computed(() => folders.value.length)
const spaceLabel = computed(() =>
  currentFolder.value?.space === 'PROJECT' ? '项目空间' : currentFolder.value?.space === 'MY' ? '个人工作区' : '公共空间'
)
const userLabel = computed(() => {
  try {
    const raw = localStorage.getItem('nexplm.user')
    return raw ? (JSON.parse(raw).username ?? '当前用户') : '当前用户'
  } catch {
    return '当前用户'
  }
})

function filterNode(value: string, data: TreeNode): boolean {
  return !value || data.label.toLowerCase().includes(value.toLowerCase())
}
watch(folderFilter, (value) => treeRef.value?.filter(value))
watch([keyword, filterType], () => (page.value = 1))
const selectedRows = ref<WsObject[]>([])
const currentFolder = ref<TreeNode | null>(null)

const visibleObjects = computed(() =>
  filterType.value ? objects.value.filter((row) => row.object_type === filterType.value) : objects.value
)
const searchedObjects = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return key
    ? visibleObjects.value.filter((row) =>
        `${row.code} ${row.name} ${row.specification ?? ''}`.toLowerCase().includes(key))
    : visibleObjects.value
})
const pagedObjects = computed(() =>
  searchedObjects.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value)
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

// ---------------- 新建 / 编辑：共用 ItemFormDialog ----------------
const formVisible = ref(false)
const formEditingId = ref<string | null>(null)
const presetProjectId = ref<string | null>(null)

/** 新建下拉：每类对象挂各自的公共组件 */
function onCreateCommand(command: string): void {
  if (command !== 'item') {
    return
  }
  if (!currentFolder.value?.folderId) {
    ElMessage.warning('请先在左侧选择文件夹')
    return
  }
  formEditingId.value = null
  presetProjectId.value = currentFolder.value.space === 'PROJECT'
    ? currentFolder.value.projectId ?? null
    : null
  formVisible.value = true
}

function openEdit(): void {
  const row = selectedRows.value[0]
  if (!row) {
    ElMessage.warning('请先选择一行')
    return
  }
  if (row.object_type !== 'PdmItem') {
    ElMessage.info('当前仅支持零部件')
    return
  }
  formEditingId.value = row.id
  presetProjectId.value = null
  formVisible.value = true
}

async function onFormSaved(): Promise<void> {
  await loadTree()
  await refreshObjects()
}

// ---------------- 添加：把已有对象挂进当前文件夹 ----------------
const addVisible = ref(false)
const addSaving = ref(false)
const addSearching = ref(false)
const addObjectId = ref('')
const addOptions = ref<Array<{ id: string; code: string; name: string }>>([])

function openAdd(): void {
  if (!currentFolder.value?.folderId) {
    ElMessage.warning('请先在左侧选择文件夹')
    return
  }
  addObjectId.value = ''
  addOptions.value = []
  addVisible.value = true
}

async function searchObjects(keyword: string): Promise<void> {
  if (!keyword || keyword.trim().length < 2) {
    addOptions.value = []
    return
  }
  addSearching.value = true
  try {
    const page = await itemApi.list({ keyword: keyword.trim(), page: 1, size: 20 } as never)
    addOptions.value = (page.items ?? []).map((row) => ({ id: row.id, code: row.code, name: row.name }))
  } catch {
    addOptions.value = []
  } finally {
    addSearching.value = false
  }
}

async function submitAdd(): Promise<void> {
  const folder = currentFolder.value
  if (!folder?.folderId || !addObjectId.value) {
    return
  }
  addSaving.value = true
  try {
    await wsConsoleApi.addObject(folder.space, folder.folderId, addObjectId.value)
    ElMessage.success('已添加')
    addVisible.value = false
    await refreshObjects()
  } catch (error) {
    ElMessage.error((error as Error).message || '添加失败')
  } finally {
    addSaving.value = false
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

onMounted(loadTree)
</script>

<style scoped>
.ws-shell {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
  min-width: 980px;
  background: #f2f5f9;
  border: 1px solid #cfd9e6;
}

.ws-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  padding: 0 12px;
  background: linear-gradient(180deg, #f8fbff, #e8f0fa);
  border-bottom: 1px solid #cfd9e6;
}

.ws-header-left,
.ws-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ws-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f4e94;
}

.ws-header-sep {
  width: 1px;
  height: 16px;
  background: #c7d4e5;
}

.ws-header-sub {
  font-size: 12px;
  color: #6b7a90;
}

.ws-body {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 8px;
  flex: 1;
  min-height: 0;
  padding: 8px;
}

.side,
.main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
  border: 1px solid #cfd9e6;
  border-radius: 2px;
}

.side-head {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  background: #f4f8fd;
  border-bottom: 1px solid #d8e2ef;
}

.side-head-title {
  font-size: 12px;
  font-weight: 600;
  color: #1f4e94;
}

.side-tree {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 4px;
}

.side-foot {
  padding: 5px 8px;
  font-size: 11px;
  color: #6b7a90;
  background: #f8fafc;
  border-top: 1px solid #e2e9f2;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
}

.tree-label {
  white-space: nowrap;
}

/* 彩色文件夹图标：根节点按空间配色，普通文件夹统一暖黄色 */
.tree-icon {
  width: 20px;
  height: 20px;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: linear-gradient(180deg, #fff7e4, #ffe3a3);
  color: #c58315;
  box-shadow: inset 0 0 0 1px rgba(190, 130, 30, 0.28), 0 1px 1px rgba(60, 80, 110, 0.08);
}

.tree-icon .el-icon {
  font-size: 14px;
}

.tree-icon.is-root {
  color: #fff;
  box-shadow: 0 1px 3px rgba(40, 70, 110, 0.18);
}

.tree-icon.is-root.space-public {
  background: linear-gradient(180deg, #5b9bd5, #2f6db3);
}

.tree-icon.is-root.space-my {
  background: linear-gradient(180deg, #5bb98c, #2f8f66);
}

.tree-icon.is-root.space-project {
  background: linear-gradient(180deg, #9b8bd6, #6a55b0);
}

.tree-root .tree-label {
  font-weight: 600;
  color: #2c4a6e;
}

.tree-badge {
  min-width: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #eef3f9;
  color: #6b7a90;
  font-size: 10px;
  line-height: 15px;
  text-align: center;
}

.side-tree :deep(.el-tree-node__content) {
  height: 26px;
  border-radius: 3px;
}

.side-tree :deep(.el-tree-node__content:hover) {
  background: #edf4fc;
}

.side-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: #dbeafe;
}

.crumb-root {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.type-cell .el-icon {
  color: #4a76ad;
}

.crumb-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 32px;
  padding: 0 10px;
  background: #f4f8fd;
  border-bottom: 1px solid #d8e2ef;
  font-size: 12px;
}

.crumb-count {
  color: #6b7a90;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 5px 8px;
  overflow-x: auto;
  background: #fbfdff;
  border-bottom: 1px solid #d8e2ef;
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 3px;
  flex: none;
}

.toolbar :deep(.el-divider--vertical) {
  margin: 0 4px;
  height: 18px;
}

.table-wrap {
  flex: 1;
  min-height: 0;
  padding: 0 8px;
}

.table-wrap :deep(.el-table__header th) {
  background: #eef4fb;
  color: #2c4a6e;
  font-weight: 600;
}

.type-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.muted,
.hint {
  color: #9aa8b8;
  font-size: 12px;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  height: 38px;
  padding: 0 10px;
  background: #f8fafc;
  border-top: 1px solid #d8e2ef;
}

.pager-info {
  font-size: 11px;
  color: #6b7a90;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 18px;
  height: 26px;
  padding: 0 12px;
  font-size: 11px;
  color: #5c6b7f;
  background: #eef3f9;
  border-top: 1px solid #cfd9e6;
}

.status-right {
  margin-left: auto;
}
</style>