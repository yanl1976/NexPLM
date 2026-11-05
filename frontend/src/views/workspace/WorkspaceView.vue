<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="left">
            <span class="title">工作空间</span>
            <el-radio-group v-model="space" size="small" @change="onSpaceChange">
              <el-radio-button v-for="(label, value) in SPACE_LABEL" :key="value" :value="value">
                {{ label }}
              </el-radio-button>
            </el-radio-group>
          </div>
          <div>
            <el-button v-if="canWrite" type="primary" @click="openCreate(null)">新建根文件夹</el-button>
            <el-button @click="loadAll">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="body">
        <div class="tree-panel">
          <el-tree
            :data="tree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            default-expand-all
            highlight-current
            @node-click="onSelectFolder"
          >
            <template #default="{ data }">
              <span class="node">
                <el-icon><Folder /></el-icon>
                <span>{{ data.name }}</span>
                <span v-if="canWrite" class="node-actions">
                  <el-button link type="primary" size="small" @click.stop="openCreate(data)">新建下级</el-button>
                  <el-button link type="primary" size="small" @click.stop="openRename(data)">重命名</el-button>
                  <el-button link type="primary" size="small" @click.stop="openMove(data)">移动</el-button>
                  <el-button link type="danger" size="small" @click.stop="onDeleteFolder(data)">删除</el-button>
                </span>
              </span>
            </template>
          </el-tree>
          <el-empty v-if="tree.length === 0" description="该空间还没有文件夹" />
        </div>

        <div class="content-panel">
          <el-tabs v-model="activeTab" @tab-change="onTabChange">
            <el-tab-pane label="文件夹内容" name="content">
              <div class="toolbar">
                <span class="current">
                  当前文件夹：{{ selectedFolder?.name || '未选择' }}
                </span>
                <el-button
                  v-if="canWrite"
                  type="primary"
                  size="small"
                  :disabled="!selectedFolder"
                  @click="openAddDoc"
                >
                  添加文档
                </el-button>
              </div>
              <el-table :data="contents" border>
                <el-table-column prop="code" label="编码" width="150" />
                <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag size="small">{{ stateLabel(row.lifecycleState) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="加入时间" width="170">
                  <template #default="{ row }">{{ formatTime(row.addedAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="160" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openDoc(row.objectId)">打开</el-button>
                    <el-button v-if="canWrite" link type="danger" @click="onRemoveContent(row)">移除</el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty :description="selectedFolder ? '文件夹为空' : '请先选择左侧文件夹'" />
                </template>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="收藏" name="favorites">
              <el-table :data="favorites" border>
                <el-table-column prop="code" label="编码" width="150" />
                <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag size="small">{{ stateLabel(row.lifecycleState) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="160" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openDoc(row.objectId)">打开</el-button>
                    <el-button link type="danger" @click="onRemoveFavorite(row)">取消收藏</el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无收藏" />
                </template>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="最近访问" name="recent">
              <el-table :data="recent" border>
                <el-table-column prop="code" label="编码" width="150" />
                <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag size="small">{{ stateLabel(row.lifecycleState) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="访问时间" width="170">
                  <template #default="{ row }">{{ formatTime(row.accessedAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="100" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openDoc(row.objectId)">打开</el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无访问记录" />
                </template>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="folderDialogVisible" :title="folderDialogTitle" width="480px">
      <el-form label-width="90px">
        <el-form-item label="上级文件夹">
          <el-input :model-value="parentLabel" disabled />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="folderName" placeholder="如 装配图" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="folderDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSaveFolder">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="moveDialogVisible" title="移动文件夹" width="480px">
      <el-form label-width="90px">
        <el-form-item label="文件夹">
          <el-input :model-value="movingFolder?.name" disabled />
        </el-form-item>
        <el-form-item label="目标位置">
          <el-tree-select
            v-model="moveTarget"
            :data="tree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="留空表示移到空间根"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onMoveFolder">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addDialogVisible" title="添加文档到当前文件夹" width="720px">
      <div class="toolbar">
        <el-input
          v-model="docKeyword"
          placeholder="搜索文档编码或名称"
          clearable
          class="doc-input"
          @keyup.enter="searchDocs"
        />
        <el-button type="primary" @click="searchDocs">搜索</el-button>
      </div>
      <el-table v-loading="docLoading" :data="docCandidates" border height="360" @row-click="pickDoc">
        <el-table-column prop="code" label="编码" width="150" />
        <el-table-column prop="name" label="名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="revisionLabel" label="版本" width="80" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="pickDoc(row)">加入</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="docPage"
        :page-size="10"
        :total="docTotal"
        layout="total, prev, pager, next"
        class="pager"
        @current-change="loadDocs"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder } from '@element-plus/icons-vue'
import { docApi, type DocSummary } from '@/api/doc'
import { useUserStore } from '@/stores/user'
import {
  SPACE_LABEL,
  buildTree,
  workspaceApi,
  type ContentItem,
  type FavoriteItem,
  type FolderNode,
  type FolderTreeNode,
  type RecentItem,
  type SpaceType
} from '@/api/workspace'

const router = useRouter()
const userStore = useUserStore()

const space = ref<SpaceType>('MY')
const folders = ref<FolderNode[]>([])
const selectedFolder = ref<FolderTreeNode | null>(null)
const contents = ref<ContentItem[]>([])
const favorites = ref<FavoriteItem[]>([])
const recent = ref<RecentItem[]>([])
const activeTab = ref('content')

const saving = ref(false)
const folderDialogVisible = ref(false)
const folderDialogTitle = ref('新建文件夹')
const folderName = ref('')
const createParent = ref<FolderTreeNode | null>(null)
const renamingFolder = ref<FolderTreeNode | null>(null)

const moveDialogVisible = ref(false)
const movingFolder = ref<FolderTreeNode | null>(null)
const moveTarget = ref<string | null>(null)

const addDialogVisible = ref(false)
const docKeyword = ref('')
const docCandidates = ref<DocSummary[]>([])
const docTotal = ref(0)
const docPage = ref(1)
const docLoading = ref(false)

const tree = computed(() => buildTree(folders.value))
const canWrite = computed(() => userStore.can('pdm.doc.write'))

const parentLabel = computed(() => {
  if (renamingFolder.value) {
    return renamingFolder.value.name
  }
  return createParent.value ? createParent.value.name : '空间根'
})

const DOC_STATE: Record<string, string> = {
  Draft: '草稿',
  InWorkflow: '审批中',
  Released: '已发布',
  Obsolete: '已作废'
}

function stateLabel(state?: string): string {
  return state ? DOC_STATE[state] ?? state : '-'
}

function formatTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function loadFolders(): Promise<void> {
  folders.value = await workspaceApi.folders(space.value)
  if (selectedFolder.value) {
    const still = folders.value.find((item) => item.id === selectedFolder.value?.id)
    selectedFolder.value = still ? (still as FolderTreeNode) : null
  }
}

async function loadContent(): Promise<void> {
  contents.value = selectedFolder.value ? await workspaceApi.content(selectedFolder.value.id) : []
}

async function loadFavorites(): Promise<void> {
  favorites.value = await workspaceApi.favorites()
}

async function loadRecent(): Promise<void> {
  recent.value = await workspaceApi.recent(20)
}

async function loadAll(): Promise<void> {
  try {
    await loadFolders()
    await loadContent()
    await loadFavorites()
    await loadRecent()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  }
}

function onSpaceChange(): void {
  selectedFolder.value = null
  contents.value = []
  loadAll()
}

function onSelectFolder(data: FolderTreeNode): void {
  selectedFolder.value = data
  activeTab.value = 'content'
  loadContent()
}

function onTabChange(name: string): void {
  if (name === 'favorites') {
    loadFavorites()
  } else if (name === 'recent') {
    loadRecent()
  }
}

function openCreate(parent: FolderTreeNode | null): void {
  renamingFolder.value = null
  createParent.value = parent
  folderName.value = ''
  folderDialogTitle.value = parent ? `在「${parent.name}」下新建` : '新建根文件夹'
  folderDialogVisible.value = true
}

function openRename(node: FolderTreeNode): void {
  renamingFolder.value = node
  folderName.value = node.name
  folderDialogTitle.value = '重命名文件夹'
  folderDialogVisible.value = true
}

async function onSaveFolder(): Promise<void> {
  if (!folderName.value.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  saving.value = true
  try {
    if (renamingFolder.value) {
      await workspaceApi.renameFolder(renamingFolder.value.id, folderName.value.trim())
      ElMessage.success('已重命名')
    } else {
      await workspaceApi.createFolder({
        spaceType: space.value,
        parentId: createParent.value?.id ?? null,
        name: folderName.value.trim()
      })
      ElMessage.success('已创建')
    }
    folderDialogVisible.value = false
    await loadFolders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function openMove(node: FolderTreeNode): void {
  movingFolder.value = node
  moveTarget.value = node.parentId ?? null
  moveDialogVisible.value = true
}

async function onMoveFolder(): Promise<void> {
  if (!movingFolder.value) {
    return
  }
  saving.value = true
  try {
    await workspaceApi.moveFolder(movingFolder.value.id, moveTarget.value ?? null)
    ElMessage.success('已移动')
    moveDialogVisible.value = false
    await loadFolders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移动失败')
  } finally {
    saving.value = false
  }
}

async function onDeleteFolder(node: FolderTreeNode): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除文件夹「${node.name}」？需先清空内容`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await workspaceApi.deleteFolder(node.id)
    ElMessage.success('已删除')
    if (selectedFolder.value?.id === node.id) {
      selectedFolder.value = null
      contents.value = []
    }
    await loadFolders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function openAddDoc(): void {
  if (!selectedFolder.value) {
    ElMessage.warning('请先选择文件夹')
    return
  }
  addDialogVisible.value = true
  docPage.value = 1
  docKeyword.value = ''
  loadDocs()
}

async function loadDocs(): Promise<void> {
  docLoading.value = true
  try {
    const page = await docApi.list({ page: docPage.value, size: 10, keyword: docKeyword.value || undefined })
    docCandidates.value = page.items
    docTotal.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载文档失败')
  } finally {
    docLoading.value = false
  }
}

function searchDocs(): void {
  docPage.value = 1
  loadDocs()
}

async function pickDoc(row: DocSummary): Promise<void> {
  if (!selectedFolder.value) {
    return
  }
  try {
    await workspaceApi.addContent(selectedFolder.value.id, row.id)
    ElMessage.success('已加入文件夹')
    addDialogVisible.value = false
    await loadContent()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加入失败')
  }
}

async function onRemoveContent(row: ContentItem): Promise<void> {
  if (!selectedFolder.value) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认把「${row.name}」移出当前文件夹？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await workspaceApi.removeContent(selectedFolder.value.id, row.objectId)
    ElMessage.success('已移除')
    await loadContent()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移除失败')
  }
}

async function onRemoveFavorite(row: FavoriteItem): Promise<void> {
  try {
    await workspaceApi.removeFavorite(row.objectId)
    ElMessage.success('已取消收藏')
    await loadFavorites()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

function openDoc(id: string): void {
  router.push({ name: 'doc-detail', params: { id } })
}

onMounted(loadAll)
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title {
  font-weight: 600;
}

.body {
  display: flex;
  gap: 16px;
}

.tree-panel {
  width: 340px;
  min-height: 460px;
  border-right: 1px solid #eef1f6;
  padding-right: 12px;
}

.content-panel {
  flex: 1;
  min-width: 0;
}

.node {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.node-actions {
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.2s;
  white-space: nowrap;
}

.node:hover .node-actions {
  opacity: 1;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.current {
  color: #606266;
  font-size: 13px;
}

.doc-input {
  width: 260px;
}

.pager {
  margin-top: 8px;
  justify-content: flex-end;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
