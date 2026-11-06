<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="编码/名称" clearable style="width: 200px" @keyup.enter="reload" />
      <el-select v-model="enabled" placeholder="全部状态" clearable style="width: 120px" @change="reload">
        <el-option label="启用" value="true" />
        <el-option label="停用" value="false" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建模板</el-button>
      <span class="hint">模板 = 一张整改清单：应用到一个质量问题，就按步骤批量生成该轮措施（改删模板不影响已生成的措施）</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="code" label="编码" width="120" />
      <el-table-column prop="name" label="模板名称" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="120" align="center">
        <template #default="{ row }">{{ TEMPLATE_TYPE_LABEL[row.templateType] ?? row.templateType }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="步骤数" width="90" align="center">
        <template #default="{ row }">{{ row.itemCount }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="说明" min-width="180" show-overflow-tooltip />
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
          <el-button v-if="canWrite" link type="danger" @click="onRemove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="还没有 8D 模板，新建一个或用「填入 8D 骨架」快速开始" :image-size="60" />
    <el-pagination
      v-if="total > size"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPageChange"
    />

    <el-dialog v-model="formVisible" :title="form.id ? `编辑模板 ${form.code}` : '新建 8D 模板'" width="900px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.name" maxlength="200" style="width: 320px" />
          <span class="label-gap">类型</span>
          <el-select v-model="form.templateType" style="width: 150px">
            <el-option v-for="(label, value) in TEMPLATE_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <span class="label-gap">启用</span>
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <div class="steps-head">
        <span class="section-title">步骤（顺序即生成措施的先后）</span>
        <el-button size="small" @click="fill8dSkeleton">填入 8D 骨架</el-button>
        <el-button size="small" type="primary" plain @click="addStep">新增步骤</el-button>
      </div>
      <el-table :data="steps" border size="small">
        <el-table-column label="编号" width="100">
          <template #default="{ row }">
            <el-input v-model="row.itemCode" maxlength="16" placeholder="D1" />
          </template>
        </el-table-column>
        <el-table-column label="步骤名称" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.name" maxlength="200" />
          </template>
        </el-table-column>
        <el-table-column label="措施类型" width="130">
          <template #default="{ row }">
            <el-select v-model="row.actionType" style="width: 100%">
              <el-option v-for="(label, value) in ACTION_TYPE_LABEL" :key="value" :label="label" :value="value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="预填提示语（生成措施时的初始内容）" min-width="220">
          <template #default="{ row }">
            <el-input v-model="row.prompt" type="textarea" :rows="1" autosize maxlength="2000" />
          </template>
        </el-table-column>
        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.required" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ $index }">
            <el-button link type="primary" :disabled="$index === 0" @click="moveStep($index, -1)">上移</el-button>
            <el-button link type="primary" :disabled="$index === steps.length - 1" @click="moveStep($index, 1)">下移</el-button>
            <el-button link type="danger" @click="removeStep($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!steps.length" description="还没有步骤，可点「填入 8D 骨架」或「新增步骤」" :image-size="50" />

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  qualityTemplateApi,
  ACTION_TYPE_LABEL,
  TEMPLATE_TYPE_LABEL,
  type TemplateItemSaveReq,
  type TemplateRow
} from '@/api/quality'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('qlt.template.write'))

interface StepDraft {
  key: number
  itemCode: string
  name: string
  actionType: string
  prompt: string
  required: boolean
}

let stepKey = 0

const loading = ref(false)
const saving = ref(false)
const rows = ref<TemplateRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const enabled = ref<string>('')

const formVisible = ref(false)
const form = reactive({
  id: '',
  code: '',
  name: '',
  templateType: '8D',
  enabled: true,
  remark: ''
})
const steps = ref<StepDraft[]>([])

/** 8D 骨架：D1~D8 对应固定措施类型，落到措施里就是 8 条待办。 */
const EIGHT_D_SKELETON: Array<Omit<StepDraft, 'key'>> = [
  { itemCode: 'D1', name: '成立改善小组', actionType: 'CONTAINMENT', prompt: '小组成员与分工：', required: true },
  { itemCode: 'D2', name: '描述问题', actionType: 'CONTAINMENT', prompt: '何时/何地/何物/发生了什么/影响范围：', required: true },
  { itemCode: 'D3', name: '临时遏制措施', actionType: 'CONTAINMENT', prompt: '先止血的措施（隔离/挑选/返工）与有效性：', required: true },
  { itemCode: 'D4', name: '根因分析', actionType: 'ROOT_CAUSE', prompt: '5Why / 鱼骨图结论，根本原因：', required: true },
  { itemCode: 'D5', name: '永久纠正措施', actionType: 'CORRECTIVE', prompt: '针对根因的纠正措施与责任人：', required: true },
  { itemCode: 'D6', name: '实施并验证效果', actionType: 'VERIFY', prompt: '验证方法与数据（前后对比）：', required: true },
  { itemCode: 'D7', name: '预防再发', actionType: 'PREVENTIVE', prompt: '文件/工艺/检验标准更新与横向展开：', required: true },
  { itemCode: 'D8', name: '小组总结与固化', actionType: 'PREVENTIVE', prompt: '经验固化、表彰与关闭意见：', required: true }
]

onMounted(() => {
  void reload()
})

async function reload(): Promise<void> {
  loading.value = true
  try {
    const result = await qualityTemplateApi.page({
      keyword: keyword.value || undefined,
      enabled: enabled.value === '' ? undefined : enabled.value === 'true',
      page: page.value,
      size: size.value
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载模板失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(value: number): void {
  page.value = value
  void reload()
}

function openCreate(): void {
  form.id = ''
  form.code = ''
  form.name = ''
  form.templateType = '8D'
  form.enabled = true
  form.remark = ''
  steps.value = []
  fill8dSkeleton()
  formVisible.value = true
}

async function openEdit(id: string): Promise<void> {
  try {
    const detail = await qualityTemplateApi.detail(id)
    form.id = detail.id
    form.code = detail.code
    form.name = detail.name
    form.templateType = detail.templateType
    form.enabled = detail.enabled
    form.remark = detail.remark ?? ''
    steps.value = detail.items.map((item) => ({
      key: stepKey++,
      itemCode: item.itemCode,
      name: item.name,
      actionType: item.actionType,
      prompt: item.prompt ?? '',
      required: item.required
    }))
    formVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载模板详情失败')
  }
}

function addStep(): void {
  steps.value.push({
    key: stepKey++,
    itemCode: `STEP${steps.value.length + 1}`,
    name: '',
    actionType: 'CORRECTIVE',
    prompt: '',
    required: true
  })
}

function removeStep(index: number): void {
  steps.value.splice(index, 1)
}

function moveStep(index: number, offset: number): void {
  const target = index + offset
  if (target < 0 || target >= steps.value.length) {
    return
  }
  const list = [...steps.value]
  const [moved] = list.splice(index, 1)
  list.splice(target, 0, moved)
  steps.value = list
}

function fill8dSkeleton(): void {
  steps.value = EIGHT_D_SKELETON.map((item) => ({ ...item, key: stepKey++ }))
}

async function onSave(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请填写模板名称')
    return
  }
  if (!steps.value.length) {
    ElMessage.warning('至少配置一个步骤')
    return
  }
  const codes = new Set<string>()
  for (const step of steps.value) {
    const code = step.itemCode.trim().toUpperCase()
    if (!code || !step.name.trim()) {
      ElMessage.warning('步骤编号与名称都不能为空')
      return
    }
    if (codes.has(code)) {
      ElMessage.warning(`步骤编号重复：${code}`)
      return
    }
    codes.add(code)
  }
  const items: TemplateItemSaveReq[] = steps.value.map((step, index) => ({
    itemCode: step.itemCode.trim().toUpperCase(),
    name: step.name.trim(),
    actionType: step.actionType,
    prompt: step.prompt.trim() || null,
    required: step.required,
    sortNo: (index + 1) * 10
  }))
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      templateType: form.templateType,
      enabled: form.enabled,
      remark: form.remark.trim() || null
    }
    const saved = form.id
      ? await qualityTemplateApi.update(form.id, payload)
      : await qualityTemplateApi.create(payload)
    await qualityTemplateApi.replaceItems(saved.id, items)
    ElMessage.success('已保存')
    formVisible.value = false
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onRemove(row: TemplateRow): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认删除模板 ${row.code}？已应用到质量问题的措施不会被删除。`,
      '删除模板',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await qualityTemplateApi.remove(row.id)
    ElMessage.success('已删除')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function formatTime(value?: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.hint {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.steps-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
}

.section-title {
  font-weight: 600;
  margin-right: auto;
}

.label-gap {
  margin: 0 8px;
  color: #606266;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
