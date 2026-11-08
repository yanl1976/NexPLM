<template>
  <el-dialog
    :model-value="modelValue"
    title="图纸"
    width="720px"
    @update:model-value="close"
  >
    <el-form label-width="90px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="关联物料" required>
            <el-select
              v-model="form.itemId"
              filterable
              remote
              :remote-method="searchItems"
              :loading="searching"
              placeholder="输入编码或名称搜索"
              style="width: 100%"
              @change="loadLinks"
            >
              <el-option
                v-for="item in itemOptions"
                :key="item.id"
                :label="`${item.code} ${item.name}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="图号" required>
            <el-input v-model="form.dwgNo" placeholder="图纸文档编码，如 5S719-110134L" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="图纸名称">
            <el-input v-model="form.name" placeholder="留空用图号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="图纸分类">
            <el-tree-select
              v-model="form.classId"
              :data="classTree"
              :props="{ label: 'name', children: 'children' }"
              node-key="id"
              check-strictly
              clearable
              filterable
              placeholder="文档分类（可选）"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="图纸文件">
            <el-upload
              :auto-upload="false"
              :limit="1"
              drag
              style="width: 100%"
              :on-change="onFileChange"
              :on-remove="onFileRemove"
            >
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <div class="el-upload__text">拖入 DWG / PDF，或<em>点击选择</em></div>
            </el-upload>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div v-if="links.length" class="linked">
      <div class="linked-title">该物料已有图纸</div>
      <el-table :data="links" size="small" border>
        <el-table-column prop="dwg_no" label="图号" width="150" />
        <el-table-column prop="doc_name" label="名称" min-width="160" />
        <el-table-column label="版本" width="80">
          <template #default="{ row }">A.{{ String(row.iteration ?? 1).padStart(3, '0') }}</template>
        </el-table-column>
        <el-table-column label="文件" width="90">
          <template #default="{ row }">{{ row.file_ext || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button link type="danger" @click="unlink(row)">解除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button @click="close(false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存图纸</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { docApi, docClassApi, type DocClassNode } from '@/api/doc'
import { drawingApi, type DrawingLinkRow } from '@/api/drawing'
import { itemApi } from '@/api/item'

const props = defineProps<{
  modelValue: boolean
  presetItemId?: string | null
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  saved: []
}>()

const form = reactive({
  itemId: '',
  dwgNo: '',
  name: '',
  classId: null as string | null
})

const classTree = ref<DocClassNode[]>([])
const itemOptions = ref<Array<{ id: string; code: string; name: string }>>([])
const links = ref<DrawingLinkRow[]>([])
const file = ref<File | null>(null)
const saving = ref(false)
const searching = ref(false)

async function loadClassTree(): Promise<void> {
  try {
    classTree.value = await docClassApi.tree()
  } catch {
    classTree.value = []
  }
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) {
      return
    }
    form.itemId = props.presetItemId ?? ''
    form.dwgNo = ''
    form.name = ''
    form.classId = null
    file.value = null
    itemOptions.value = []
    links.value = []
    await loadClassTree()
    if (form.itemId) {
      await loadLinks()
    }
  }
)

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

async function loadLinks(): Promise<void> {
  if (!form.itemId) {
    links.value = []
    return
  }
  try {
    links.value = await drawingApi.byItem(form.itemId)
  } catch {
    links.value = []
  }
}

function onFileChange(uploadFile: { raw?: File }): void {
  file.value = uploadFile.raw ?? null
}

function onFileRemove(): void {
  file.value = null
}

async function submit(): Promise<void> {
  if (!form.itemId) {
    ElMessage.warning('请选择关联物料')
    return
  }
  if (!form.dwgNo.trim()) {
    ElMessage.warning('请填写图号')
    return
  }
  saving.value = true
  try {
    const doc = await docApi.create({
      code: form.dwgNo.trim(),
      name: form.name.trim() || form.dwgNo.trim(),
      docType: 'DESIGN',
      source: 'CAD',
      classId: form.classId
    })
    if (file.value) {
      await docApi.upload(doc.id, file.value, 'MAIN')
    }
    await drawingApi.link(doc.id, form.itemId)
    ElMessage.success('图纸已保存并关联')
    emit('saved')
    close(false)
  } catch (error) {
    ElMessage.error((error as Error).message || '保存图纸失败')
  } finally {
    saving.value = false
  }
}

async function unlink(row: DrawingLinkRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定解除 ${row.dwg_no}？图纸文档不会被删除。`, '解除关联', { type: 'warning' })
  } catch {
    return
  }
  try {
    await drawingApi.remove(row.link_id)
    ElMessage.success('已解除')
    await loadLinks()
    emit('saved')
  } catch (error) {
    ElMessage.error((error as Error).message || '解除失败')
  }
}

function close(value = false): void {
  emit('update:modelValue', value)
}
</script>

<style scoped>
.upload-icon {
  font-size: 28px;
  color: var(--el-color-primary);
}

.linked {
  margin-top: 4px;
}

.linked-title {
  font-size: 12px;
  font-weight: 600;
  color: #2c4a6e;
  margin-bottom: 6px;
}
</style>
