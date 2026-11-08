<template>
  <div class="drawing-panel">
    <div class="dp-toolbar">
      <div class="dp-filters"><slot name="filters" /></div>
      <div class="dp-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建图纸关联</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" size="small" border stripe height="100%">
      <el-table-column prop="dwg_no" label="图号" min-width="150" />
      <el-table-column prop="doc_name" label="图纸名称" min-width="170" />
      <el-table-column v-if="!itemId" prop="item_code" label="物料编码" width="140" />
      <el-table-column v-if="!itemId" prop="item_name" label="物料名称" min-width="160" />
      <el-table-column label="物料类型" width="110">
        <template #default="{ row }">{{ ITEM_TYPE_LABEL[row.item_type] || row.item_type || '-' }}</template>
      </el-table-column>
      <el-table-column label="图幅" width="80">
        <template #default="{ row }">{{ row.sheet_size || '-' }}</template>
      </el-table-column>
      <el-table-column label="文件" width="90">
        <template #default="{ row }">{{ row.file_ext || '-' }}</template>
      </el-table-column>
      <el-table-column label="关联时间" width="160">
        <template #default="{ row }">{{ fmt(row.created_at) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="remove(row)">解除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <span class="hint">暂无图纸{{ itemId ? '，点击「新建图纸关联」' : '' }}</span>
      </template>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建图纸关联" width="560px">
      <el-form label-width="90px">
        <el-form-item v-if="!itemId" label="物料" required>
          <el-select
            v-model="form.itemId"
            filterable
            remote
            :remote-method="searchItems"
            :loading="searching"
            placeholder="输入编码或名称搜索"
            style="width: 100%"
          >
            <el-option
              v-for="item in itemOptions"
              :key="item.id"
              :label="`${item.code} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="图号" required>
          <el-input v-model="form.dwgNo" placeholder="如 5S719-110134L" />
        </el-form-item>
        <el-form-item label="图纸名称">
          <el-input v-model="form.name" placeholder="留空用图号" />
        </el-form-item>
        <el-form-item label="图幅">
          <el-input v-model="form.sheetSize" placeholder="蓝本 TUFU1，如 3 / 5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { drawingApi, type DrawingRow } from '@/api/drawing'
import { itemApi, ITEM_TYPE_LABEL } from '@/api/item'

const props = defineProps<{
  itemId?: string
  keyword?: string
  itemType?: string
}>()

const emit = defineEmits<{ changed: [] }>()

const rows = ref<DrawingRow[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const searching = ref(false)
const itemOptions = ref<Array<{ id: string; code: string; name: string }>>([])
const form = reactive({ itemId: '', dwgNo: '', name: '', sheetSize: '' })

function fmt(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function load(): Promise<void> {
  loading.value = true
  try {
    rows.value = await drawingApi.list({
      keyword: props.keyword,
      itemId: props.itemId,
      itemType: props.itemType
    })
  } catch (error) {
    ElMessage.error((error as Error).message || '加载图纸失败')
  } finally {
    loading.value = false
  }
}

watch(() => [props.keyword, props.itemType, props.itemId], load)

function openCreate(): void {
  form.itemId = props.itemId ?? ''
  form.dwgNo = ''
  form.name = ''
  form.sheetSize = ''
  itemOptions.value = []
  dialogVisible.value = true
}

async function searchItems(query: string): Promise<void> {
  if (!query || query.trim().length < 2) {
    itemOptions.value = []
    return
  }
  searching.value = true
  try {
    const page = await itemApi.list({ keyword: query.trim(), page: 1, size: 20 } as never)
    itemOptions.value = (page.items ?? []).map((row) => ({ id: row.id, code: row.code, name: row.name }))
  } catch {
    itemOptions.value = []
  } finally {
    searching.value = false
  }
}

async function submit(): Promise<void> {
  if (!form.itemId) {
    ElMessage.warning('请选择物料')
    return
  }
  if (!form.dwgNo.trim()) {
    ElMessage.warning('请填写图号')
    return
  }
  saving.value = true
  try {
    await drawingApi.create({
      itemId: form.itemId,
      dwgNo: form.dwgNo.trim(),
      name: form.name.trim() || undefined,
      sheetSize: form.sheetSize.trim() || undefined
    })
    ElMessage.success('已关联图纸')
    dialogVisible.value = false
    await load()
    emit('changed')
  } catch (error) {
    ElMessage.error((error as Error).message || '关联失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: DrawingRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定解除 ${row.dwg_no} 与 ${row.item_code} 的关联？图纸本身不会被删除。`, '解除关联', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await drawingApi.remove(row.link_id)
    ElMessage.success('已解除')
    await load()
    emit('changed')
  } catch (error) {
    ElMessage.error((error as Error).message || '解除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.drawing-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}

.dp-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px;
  background: #f8fafc;
  border-bottom: 1px solid #d8e2ef;
}

.dp-filters,
.dp-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hint {
  color: #9aa8b8;
  font-size: 12px;
}
</style>
