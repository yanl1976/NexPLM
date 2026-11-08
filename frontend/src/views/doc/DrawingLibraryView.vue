<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="title-group">
            <span>图纸库</span>
            <span class="sub">所有 DWG 图纸（文档域，file_ext=dwg）</span>
          </div>
          <div>
            <el-button type="primary" @click="dialogVisible = true">新建图纸</el-button>
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="filters">
        <el-input
          v-model="keyword"
          placeholder="图号 / 名称"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        />
        <el-tree-select
          v-model="classId"
          :data="classTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          check-strictly
          clearable
          filterable
          placeholder="图纸分类"
          style="width: 200px"
        />
        <el-select v-model="lifecycleState" placeholder="状态" clearable style="width: 140px">
          <el-option v-for="(label, value) in DOC_STATE_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="code" label="图号" width="170" />
        <el-table-column prop="name" label="图纸名称" min-width="220" />
        <el-table-column label="分类" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.className || '-' }}</template>
        </el-table-column>
        <el-table-column prop="revisionLabel" label="版本" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusDot :state="row.lifecycleState" />
          </template>
        </el-table-column>
        <el-table-column label="文件数" width="80" align="right">
          <template #default="{ row }">{{ row.fileCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ fmt(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">打开</el-button>
            <el-button link type="primary" @click="openLink(row)">关联物料</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无图纸，点击「新建图纸」开始" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @current-change="load"
        @size-change="search"
      />
    </el-card>

    <DrawingDialog v-model="dialogVisible" @saved="load" />

    <el-dialog v-model="linkVisible" title="关联物料" width="560px">
      <div class="link-bar">
        <el-select
          v-model="linkItemId"
          filterable
          remote
          :remote-method="searchItems"
          :loading="searching"
          placeholder="输入物料编码或名称"
          style="flex: 1"
        >
          <el-option
            v-for="item in itemOptions"
            :key="item.id"
            :label="`${item.code} ${item.name}`"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" :disabled="!linkItemId" @click="submitLink">关联</el-button>
      </div>
      <el-table :data="linkedItems" size="small" border>
        <el-table-column prop="item_code" label="物料编码" width="150" />
        <el-table-column prop="item_name" label="物料名称" min-width="180" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button link type="danger" @click="unlinkItem(row)">解除</el-button>
          </template>
        </el-table-column>
        <template #empty><span class="hint">尚未关联物料</span></template>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DOC_STATE_LABEL, docApi, docClassApi, type DocClassNode, type DocSummary } from '@/api/doc'
import { drawingApi, type DrawingLinkRow } from '@/api/drawing'
import { itemApi } from '@/api/item'
import StatusDot from '@/components/StatusDot.vue'
import DrawingDialog from '@/components/business/DrawingDialog.vue'

const router = useRouter()
const rows = ref<DocSummary[]>([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const classId = ref('')
const lifecycleState = ref('')
const classTree = ref<DocClassNode[]>([])
const dialogVisible = ref(false)

const linkVisible = ref(false)
const linkDocId = ref('')
const linkItemId = ref('')
const linkedItems = ref<DrawingLinkRow[]>([])
const itemOptions = ref<Array<{ id: string; code: string; name: string }>>([])
const searching = ref(false)

function fmt(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function loadClassTree(): Promise<void> {
  try {
    classTree.value = await docClassApi.tree()
  } catch {
    classTree.value = []
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await docApi.list({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      classId: classId.value || undefined,
      lifecycleState: lifecycleState.value || undefined,
      fileExt: 'dwg'
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error((error as Error).message || '加载图纸失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  load()
}

function openDetail(row: DocSummary): void {
  router.push(`/doc/${row.id}`)
}

async function openLink(row: DocSummary): Promise<void> {
  linkDocId.value = row.id
  linkItemId.value = ''
  itemOptions.value = []
  linkVisible.value = true
  await loadLinked()
}

async function loadLinked(): Promise<void> {
  try {
    linkedItems.value = await drawingApi.byDoc(linkDocId.value)
  } catch {
    linkedItems.value = []
  }
}

async function searchItems(query: string): Promise<void> {
  if (!query || query.trim().length < 2) {
    itemOptions.value = []
    return
  }
  searching.value = true
  try {
    const pageData = await itemApi.list({ keyword: query.trim(), page: 1, size: 20 } as never)
    itemOptions.value = (pageData.items ?? []).map((item) => ({
      id: item.id,
      code: item.code,
      name: item.name
    }))
  } catch {
    itemOptions.value = []
  } finally {
    searching.value = false
  }
}

async function submitLink(): Promise<void> {
  if (!linkItemId.value) {
    return
  }
  try {
    await drawingApi.link(linkDocId.value, linkItemId.value)
    ElMessage.success('已关联')
    linkItemId.value = ''
    await loadLinked()
  } catch (error) {
    ElMessage.error((error as Error).message || '关联失败')
  }
}

async function unlinkItem(row: DrawingLinkRow): Promise<void> {
  try {
    await drawingApi.remove(row.link_id)
    ElMessage.success('已解除')
    await loadLinked()
  } catch (error) {
    ElMessage.error((error as Error).message || '解除失败')
  }
}

onMounted(() => {
  loadClassTree()
  load()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-group {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.title-group > span:first-child {
  font-size: 15px;
  font-weight: 600;
}

.sub {
  font-size: 12px;
  color: #6b7a90;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.pager {
  margin-top: 10px;
  justify-content: flex-end;
}

.link-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.hint {
  font-size: 12px;
  color: #9aa8b8;
}
</style>
