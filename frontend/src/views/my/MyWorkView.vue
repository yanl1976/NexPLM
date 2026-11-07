<script setup lang="ts">
/**
 * 个人工作区。对齐旧系统「000 个人工作区 / 001 常用功能」：
 * 我的检出（MyCheckOut）/ 收藏夹（Favorite）/ 最近访问（AccessRecord）/
 * 高级查询（QueryMachine）/ 站内消息（InternalMail）。
 *
 * 五个菜单项共用一个视图，靠路由 props.tab 切换，避免五份几乎相同的表格代码。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, Bell, Clock, Search, View } from '@element-plus/icons-vue'
import { myApi } from '@/api/my'
import type { CheckoutRow, FavoriteRow, MessageRow, RecentRow, SearchRow } from '@/api/my'

const props = defineProps<{ tab?: string }>()

const route = useRoute()
const router = useRouter()

const TAB_BY_PATH: Record<string, string> = {
  '/my/checkouts': 'checkouts',
  '/my/favorites': 'favorites',
  '/my/recent': 'recent',
  '/my/search': 'search',
  '/my/messages': 'messages'
}

const active = ref(props.tab ?? TAB_BY_PATH[route.path] ?? 'checkouts')
const loading = ref(false)

const checkouts = ref<CheckoutRow[]>([])
const favorites = ref<FavoriteRow[]>([])
const recent = ref<RecentRow[]>([])
const messages = ref<MessageRow[]>([])
const searchRows = ref<SearchRow[]>([])
const unread = ref(0)

const typeLabels = ref<Record<string, string>>({})
const typeOptions = computed(() =>
  Object.entries(typeLabels.value).map(([value, label]) => ({ value, label }))
)

const favFilter = ref('')
const recentFilter = ref('')
const searchForm = ref({ keyword: '', objectType: '' })
const unreadOnly = ref(false)

/** 路由变化时切换页签（侧边栏是 5 个独立菜单项）。 */
watch(
  () => route.path,
  (path) => {
    const tab = TAB_BY_PATH[path]
    if (tab && tab !== active.value) {
      active.value = tab
    }
  }
)

watch(active, (tab) => {
  const path = Object.keys(TAB_BY_PATH).find((key) => TAB_BY_PATH[key] === tab)
  if (path && path !== route.path) {
    router.replace(path)
  }
  void load(tab)
})

function openObject(row: { objectType: string; objectId: string }) {
  const target =
    row.objectType === 'PdmDoc'
      ? `/doc/${row.objectId}`
      : row.objectType === 'PdmItem'
        ? `/item/${row.objectId}`
        : null
  if (!target) {
    ElMessage.info('该对象类型暂无可跳转的详情页')
    return
  }
  // 打开即记一条访问（后端 30 分钟内重复访问只更新时间）
  void myApi.recordAccess({ objectType: row.objectType, objectId: row.objectId })
  void router.push(target)
}

async function load(tab = active.value) {
  loading.value = true
  try {
    if (tab === 'checkouts') {
      checkouts.value = await myApi.checkouts()
    } else if (tab === 'favorites') {
      favorites.value = await myApi.favorites(favFilter.value || undefined)
    } else if (tab === 'recent') {
      recent.value = await myApi.recent(recentFilter.value || undefined)
    } else if (tab === 'search') {
      searchRows.value = await myApi.search({
        keyword: searchForm.value.keyword || undefined,
        objectType: searchForm.value.objectType || undefined,
        limit: 50
      })
    } else if (tab === 'messages') {
      messages.value = await myApi.messages(unreadOnly.value)
      unread.value = (await myApi.unreadCount()).unread
    }
  } catch (error) {
    ElMessage.error((error as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function cancelCheckout(row: CheckoutRow) {
  await ElMessageBox.confirm(`撤销对「${row.code ?? row.objectId}」的检出？`, '撤销检出', {
    type: 'warning'
  })
  await myApi.cancelCheckout(row.objectId)
  ElMessage.success('已撤销检出')
  await load('checkouts')
}

async function addFavoriteFromSearch(row: SearchRow) {
  await myApi.addFavorite({ objectType: row.objectType, objectId: row.objectId })
  ElMessage.success('已加入收藏夹')
}

async function removeFavorite(row: FavoriteRow) {
  await myApi.removeFavorite(row.id)
  ElMessage.success('已取消收藏')
  await load('favorites')
}

async function markRead(row: MessageRow) {
  if (row.readAt) {
    return
  }
  await myApi.markRead(row.id)
  await load('messages')
}

/** 全部已读：只影响本人收件箱（批次 E-2 起已读按收件人记录）。 */
async function markAllRead() {
  try {
    const result = await myApi.markAllRead()
    ElMessage.success(`已标记 ${result.updated} 条为已读`)
    await load('messages')
  } catch (error) {
    ElMessage.error((error as Error).message || '操作失败')
  }
}

onMounted(async () => {
  try {
    const dict = await myApi.objectTypes()
    typeLabels.value = dict.labels || {}
  } catch {
    typeLabels.value = {}
  }
  await load()
})

function fmt(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}
</script>

<template>
  <div class="my-work">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">个人工作区</span>
          <span class="hint">我的检出 / 收藏 / 最近访问 / 高级查询 / 站内消息</span>
        </div>
      </template>

      <el-tabs v-model="active">
        <el-tab-pane name="checkouts">
          <template #label>
            <span class="tab-label"><el-icon><Clock /></el-icon>我的检出</span>
          </template>
          <el-table v-loading="loading" :data="checkouts" size="small" border empty-text="当前没有持有检出锁">
            <el-table-column prop="objectTypeName" label="对象类型" width="110" />
            <el-table-column prop="code" label="编码" width="180" />
            <el-table-column prop="name" label="名称" min-width="220" show-overflow-tooltip />
            <el-table-column prop="revision" label="版本" width="80" />
            <el-table-column label="检出时间" width="170">
              <template #default="{ row }">{{ fmt(row.checkedOutAt) }}</template>
            </el-table-column>
            <el-table-column label="到期时间" width="170">
              <template #default="{ row }">
                <span :class="{ expired: row.expired }">{{ fmt(row.expireAt) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openObject(row)">打开</el-button>
                <el-button link type="danger" @click="cancelCheckout(row)">撤销检出</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane name="favorites">
          <template #label>
            <span class="tab-label"><el-icon><Star /></el-icon>收藏夹</span>
          </template>
          <div class="toolbar">
            <el-select v-model="favFilter" placeholder="全部类型" clearable style="width: 160px" @change="load('favorites')">
              <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-button :icon="View" @click="load('favorites')">刷新</el-button>
          </div>
          <el-table v-loading="loading" :data="favorites" size="small" border empty-text="还没有收藏">
            <el-table-column prop="objectTypeName" label="对象类型" width="110" />
            <el-table-column prop="code" label="编码" width="180" />
            <el-table-column prop="name" label="名称" min-width="220" show-overflow-tooltip />
            <el-table-column prop="lifecycleState" label="状态" width="110" />
            <el-table-column prop="folder" label="分组" width="110" />
            <el-table-column label="收藏时间" width="170">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openObject(row)">打开</el-button>
                <el-button link type="danger" @click="removeFavorite(row)">取消收藏</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane name="recent">
          <template #label>
            <span class="tab-label"><el-icon><View /></el-icon>最近访问</span>
          </template>
          <div class="toolbar">
            <el-select v-model="recentFilter" placeholder="全部类型" clearable style="width: 160px" @change="load('recent')">
              <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-button :icon="View" @click="load('recent')">刷新</el-button>
          </div>
          <el-table v-loading="loading" :data="recent" size="small" border empty-text="暂无访问记录">
            <el-table-column prop="objectTypeName" label="对象类型" width="110" />
            <el-table-column prop="code" label="编码" width="180" />
            <el-table-column prop="name" label="名称" min-width="220" show-overflow-tooltip />
            <el-table-column prop="accessType" label="动作" width="100" />
            <el-table-column label="访问时间" width="170">
              <template #default="{ row }">{{ fmt(row.accessedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openObject(row)">打开</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane name="search">
          <template #label>
            <span class="tab-label"><el-icon><Search /></el-icon>高级查询</span>
          </template>
          <div class="toolbar">
            <el-input
              v-model="searchForm.keyword"
              placeholder="编码 / 名称关键字"
              clearable
              style="width: 260px"
              @keyup.enter="load('search')"
            />
            <el-select v-model="searchForm.objectType" placeholder="全部类型" clearable style="width: 160px">
              <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="load('search')">查询</el-button>
          </div>
          <el-table v-loading="loading" :data="searchRows" size="small" border empty-text="没有匹配的对象">
            <el-table-column prop="objectTypeName" label="对象类型" width="110" />
            <el-table-column prop="code" label="编码" width="190" />
            <el-table-column prop="name" label="名称" min-width="220" show-overflow-tooltip />
            <el-table-column prop="lifecycleState" label="状态" width="110" />
            <el-table-column prop="revision" label="版本" width="80" />
            <el-table-column label="更新时间" width="170">
              <template #default="{ row }">{{ fmt(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openObject(row)">打开</el-button>
                <el-button link type="warning" @click="addFavoriteFromSearch(row)">收藏</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane name="messages">
          <template #label>
            <span class="tab-label"><el-icon><Bell /></el-icon>站内消息</span>
          </template>
          <div class="toolbar">
            <el-switch v-model="unreadOnly" active-text="仅未读" @change="load('messages')" />
            <el-tag type="danger" effect="plain">未读 {{ unread }}</el-tag>
            <el-button :icon="View" @click="load('messages')">刷新</el-button>
            <el-button :disabled="!unread" @click="markAllRead">全部已读</el-button>
          </div>
          <el-table v-loading="loading" :data="messages" size="small" border empty-text="暂无消息">
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag v-if="!row.readAt" type="danger" size="small">未读</el-tag>
                <span v-else class="read">已读</span>
              </template>
            </el-table-column>
            <el-table-column prop="eventType" label="事件" width="180" />
            <el-table-column prop="title" label="标题" width="240" show-overflow-tooltip />
            <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
            <el-table-column prop="status" label="投递" width="90" />
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button v-if="!row.readAt" link type="primary" @click="markRead(row)">标记已读</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.my-work {
  padding: 4px;
}

.card-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.card-header .title {
  font-size: 15px;
  font-weight: 600;
}

.card-header .hint {
  font-size: 12px;
  color: var(--plm-text-secondary);
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.expired {
  color: var(--el-color-danger);
}

.read {
  color: var(--plm-text-secondary);
  font-size: 12px;
}
</style>
