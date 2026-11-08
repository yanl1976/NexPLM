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
        <el-button size="small" :disabled="!selectedRows.length" @click="openItem('edit')">编辑</el-button>
        <el-button size="small" :disabled="!selectedRows.length" @click="onCheckout">检出</el-button>
        <el-button size="small" :disabled="!selectedRows.length" @click="onCheckin">取消检出</el-button>
        <el-button size="small" disabled>添加</el-button>
        <el-button size="small" :disabled="!selectedRows.length" @click="openItem('bom')">BOM 管理</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { wsConsoleApi, type WsFolder, type WsObject } from '@/api/wsConsole'

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
const router = useRouter()
const selectedRows = ref<WsObject[]>([])
const currentFolder = ref<TreeNode | null>(null)

function onSelectionChange(rows: WsObject[]): void {
  selectedRows.value = rows
}

/** 新建：复用零部件的新增弹窗（跳 /item?create=1，项目可预填） */
function openCreate(): void {
  if (!currentFolder.value?.folderId) {
    ElMessage.warning('请先在左侧选择文件夹')
    return
  }
  const query: Record<string, string> = { create: '1' }
  if (currentFolder.value.space === 'PROJECT' && currentFolder.value.projectId) {
    query.projectId = String(currentFolder.value.projectId)
  }
  router.push({ path: '/item', query })
}

async function refreshObjects(): Promise<void> {
  if (currentFolder.value?.folderId) {
    await onNodeClick(currentFolder.value)
  }
}

/** 检出：锁定对象；成功后刷新列表里的检出人/状态/时间 */
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

/** 编辑 / BOM 管理：跳零部件详情（同一套页面） */
function openItem(path: 'edit' | 'bom'): void {
  const row = selectedRows.value[0]
  if (!row) {
    ElMessage.warning('请先选择一行')
    return
  }
  if (row.object_type !== 'PdmItem') {
    ElMessage.info('当前仅支持零部件，其它对象类型待接入')
    return
  }
  if (path === 'edit') {
    router.push({ path: '/item', query: { edit: row.id } })
  } else {
    router.push(`/item/${row.id}`)
  }
}

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

onMounted(loadTree)
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
</style>
