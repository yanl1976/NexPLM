<template>
  <div class="page">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="标题 / 内容"
        clearable
        style="width: 220px"
        @keyup.enter="reload"
      />
      <el-select v-model="state" placeholder="全部状态" clearable style="width: 140px">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已撤回" value="REVOKED" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建广播</el-button>
      <span class="hint">
        发布后全体成员登录时会弹窗（每人对同一条只弹一次）；发布时可留「生效时间」做预约广播，未到点不会弹出。
      </span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="级别" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="levelTag(row.level)">{{ LEVEL_LABEL[row.level] ?? row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="stateTag(row.publishState)">
            {{ STATE_LABEL[row.publishState] ?? row.publishState }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生效时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.publishAt) }}</template>
      </el-table-column>
      <el-table-column label="失效时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.expireAt) }}</template>
      </el-table-column>
      <el-table-column label="弹窗" width="80" align="center">
        <template #default="{ row }">{{ row.popup ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="已读" width="90" align="center">
        <template #default="{ row }">{{ row.readCount }} 人</template>
      </el-table-column>
      <el-table-column label="创建人" width="130">
        <template #default="{ row }">{{ row.createdBy ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column v-if="canWrite" label="操作" width="230" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          <el-button v-if="row.publishState !== 'PUBLISHED'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.publishState !== 'PUBLISHED'" link type="success" @click="publish(row)">发布</el-button>
          <el-button v-if="row.publishState === 'PUBLISHED'" link type="warning" @click="revoke(row)">撤回</el-button>
          <el-button v-if="row.publishState !== 'PUBLISHED'" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty
      v-if="!loading && !rows.length"
      :description="canRead ? '还没有广播' : '当前账号没有「系统广播查看」权限（sys.broadcast.read）'"
      :image-size="60"
    />

    <div v-if="total > 0" class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="load"
        @size-change="reload"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="级别">
          <el-radio-group v-model="form.level">
            <el-radio value="NORMAL">普通</el-radio>
            <el-radio value="IMPORTANT">重要</el-radio>
            <el-radio value="URGENT">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="6" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker
            v-model="form.publishAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空=发布即生效"
            style="width: 220px"
          />
          <span class="label-gap">失效时间</span>
          <el-date-picker
            v-model="form.expireAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空=长期有效"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="登录弹窗">
          <el-switch v-model="form.popup" />
          <span class="label-gap muted">关闭时只出现在本页，不打扰登录</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="readOnly" type="primary" @click="dialogVisible = false">关闭</el-button>
        <el-button v-else type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { broadcastApi, type BroadcastRow, type BroadcastSaveReq } from '@/api/system'
import { useUserStore } from '@/stores/user'

const LEVEL_LABEL: Record<string, string> = { NORMAL: '普通', IMPORTANT: '重要', URGENT: '紧急' }
const STATE_LABEL: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', REVOKED: '已撤回' }

const userStore = useUserStore()
const canRead = userStore.can('sys.broadcast.read')
const canWrite = userStore.can('sys.broadcast.write')

const rows = ref<BroadcastRow[]>([])
const loading = ref(false)
const keyword = ref('')
const state = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

const dialogVisible = ref(false)
const readOnly = ref(false)
const editingId = ref<string | null>(null)
const form = ref<BroadcastSaveReq>(emptyForm())

const dialogTitle = computed(() => {
  if (readOnly.value) {
    return '广播详情'
  }
  return editingId.value ? '编辑广播' : '新建广播'
})

function emptyForm(): BroadcastSaveReq {
  return { title: '', content: '', level: 'NORMAL', publishAt: null, expireAt: null, popup: true }
}

async function load(): Promise<void> {
  if (!canRead) {
    return
  }
  loading.value = true
  try {
    const result = await broadcastApi.page({
      keyword: keyword.value || undefined,
      state: state.value || undefined,
      page: page.value,
      size: size.value
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

function reload(): void {
  page.value = 1
  void load()
}

function openCreate(): void {
  editingId.value = null
  readOnly.value = false
  form.value = emptyForm()
  dialogVisible.value = true
}

function openEdit(row: BroadcastRow): void {
  editingId.value = row.id
  readOnly.value = false
  form.value = {
    title: row.title,
    content: row.content,
    level: row.level,
    publishAt: row.publishAt ? toLocalText(row.publishAt) : null,
    expireAt: row.expireAt ? toLocalText(row.expireAt) : null,
    popup: row.popup
  }
  dialogVisible.value = true
}

function openDetail(row: BroadcastRow): void {
  openEdit(row)
  readOnly.value = true
}

async function submit(): Promise<void> {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning('标题与内容不能为空')
    return
  }
  try {
    if (editingId.value) {
      await broadcastApi.update(editingId.value, form.value)
      ElMessage.success('已保存')
    } else {
      await broadcastApi.create(form.value)
      ElMessage.success('已保存为草稿，确认无误后点「发布」')
    }
    dialogVisible.value = false
    void load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function publish(row: BroadcastRow): Promise<void> {
  const when = row.publishAt && new Date(row.publishAt) > new Date()
    ? `将于 ${formatTime(row.publishAt)} 生效（预约广播）`
    : '发布后立即生效'
  try {
    await ElMessageBox.confirm(`发布「${row.title}」？${when}。`, '发布广播', { type: 'info' })
  } catch {
    return
  }
  try {
    await broadcastApi.publish(row.id)
    ElMessage.success('已发布')
    void load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function revoke(row: BroadcastRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`撤回后成员不再看到该广播，已读记录保留。确定撤回「${row.title}」？`, '撤回广播', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await broadcastApi.revoke(row.id)
    ElMessage.success('已撤回')
    void load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function remove(row: BroadcastRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」？`, '删除广播', { type: 'warning' })
  } catch {
    return
  }
  try {
    await broadcastApi.remove(row.id)
    ElMessage.success('已删除')
    void load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

function levelTag(level: string): 'info' | 'warning' | 'danger' {
  if (level === 'URGENT') {
    return 'danger'
  }
  return level === 'IMPORTANT' ? 'warning' : 'info'
}

function stateTag(state: string): 'success' | 'info' | 'warning' {
  if (state === 'PUBLISHED') {
    return 'success'
  }
  return state === 'REVOKED' ? 'warning' : 'info'
}

function formatTime(value: string | null): string {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 回填编辑框时转成日期选择器要的本地时间文本。 */
function toLocalText(value: string): string {
  const date = new Date(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

void load()
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.muted {
  color: var(--el-text-color-secondary);
}
</style>
