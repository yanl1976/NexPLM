<template>
  <el-dialog
    :model-value="modelValue"
    title="图纸"
    width="880px"
    @update:model-value="close"
  >
    <el-form :model="form" label-width="96px">
      <div class="form-section">基本信息</div>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="关联物料">
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
            <el-input v-model="form.code" placeholder="如 5S719-110134L" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="图纸名称" required>
            <el-input v-model="form.name" />
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
              placeholder="文档分类"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="密级">
            <el-select v-model="form.securityLevel" clearable style="width: 100%">
              <el-option label="公开" :value="0" />
              <el-option label="内部" :value="1" />
              <el-option label="秘密" :value="2" />
              <el-option label="机密" :value="3" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="详细描述">
            <el-input v-model="form.description" type="textarea" :rows="2" />
          </el-form-item>
        </el-col>
      </el-row>

      <div class="form-section">图档属性</div>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="标准号">
            <el-input v-model="form.standardCode" placeholder="如 GB/T 1234" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="年代号">
            <el-input v-model="form.yearNo" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="旧编号">
            <el-input v-model="form.oldCode" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="规格">
            <el-input v-model="form.spec" placeholder="如 5S719-110134L" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="重要度等级">
            <el-input v-model="form.importance" placeholder="如 A / B / C" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="精度等级">
            <el-input v-model="form.precisionGrade" placeholder="如 IT7" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="计量单位">
            <el-input v-model="form.measureUnit" placeholder="如 mm" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="附图序号">
            <el-input-number v-model="form.attachNo" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="附图分类">
            <el-input v-model="form.attachType" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="项目代号">
            <el-input v-model="form.projectCode" placeholder="如 PJ-000001" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="文件类型">
            <el-select v-model="form.fileType" clearable style="width: 100%">
              <el-option label="DWG" value="DWG" />
              <el-option label="PDF" value="PDF" />
              <el-option label="STEP" value="STEP" />
              <el-option label="其它" value="OTHER" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <div class="form-section">图纸文件</div>
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
  code: '',
  name: '',
  description: '',
  classId: null as string | null,
  securityLevel: 1 as number | null,
  standardCode: '',
  yearNo: '',
  oldCode: '',
  spec: '',
  importance: '',
  precisionGrade: '',
  measureUnit: '',
  attachNo: null as number | null,
  attachType: '',
  projectCode: '',
  fileType: 'DWG'
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

function resetForm(): void {
  form.itemId = props.presetItemId ?? ''
  form.code = ''
  form.name = ''
  form.description = ''
  form.classId = null
  form.securityLevel = 1
  form.standardCode = ''
  form.yearNo = ''
  form.oldCode = ''
  form.spec = ''
  form.importance = ''
  form.precisionGrade = ''
  form.measureUnit = ''
  form.attachNo = null
  form.attachType = ''
  form.projectCode = ''
  form.fileType = 'DWG'
  file.value = null
  links.value = []
  itemOptions.value = []
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) {
      return
    }
    resetForm()
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
  if (!form.code.trim()) {
    ElMessage.warning('请填写图号')
    return
  }
  if (!form.name.trim()) {
    ElMessage.warning('请填写图纸名称')
    return
  }
  saving.value = true
  try {
    const doc = await drawingApi.create({
      itemId: form.itemId || null,
      code: form.code.trim(),
      name: form.name.trim(),
      description: form.description,
      classId: form.classId,
      securityLevel: form.securityLevel,
      standardCode: form.standardCode,
      yearNo: form.yearNo,
      oldCode: form.oldCode,
      spec: form.spec,
      importance: form.importance,
      precisionGrade: form.precisionGrade,
      measureUnit: form.measureUnit,
      attachNo: form.attachNo,
      attachType: form.attachType,
      projectCode: form.projectCode,
      fileType: form.fileType
    })
    if (file.value) {
      await docApi.upload(doc.docId, file.value, 'MAIN')
    }
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
.form-section {
  font-size: 13px;
  font-weight: 600;
  color: #2c4a6e;
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 4px;
  margin: 8px 0 12px;
}

.upload-icon {
  font-size: 28px;
  color: var(--el-color-primary);
}

.linked {
  margin-top: 12px;
}

.linked-title {
  font-size: 12px;
  font-weight: 600;
  color: #2c4a6e;
  margin-bottom: 6px;
}
</style>
