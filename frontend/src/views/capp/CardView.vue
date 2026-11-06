<template>
  <div class="page">
    <el-tabs v-model="activeTab" class="tabs">
      <!-- ------------------------------------------------ 工艺卡片 -->
      <el-tab-pane label="工艺卡片" name="card">
        <div class="toolbar">
          <el-input
            v-model="cardKeyword"
            placeholder="卡片编码 / 标题 / 零件编码 / 路线编码"
            clearable
            style="width: 300px"
            @keyup.enter="reloadCards"
            @clear="reloadCards"
          />
          <el-button type="primary" @click="reloadCards">查询</el-button>
          <el-button v-if="canWrite" type="primary" plain @click="openCreateCard">生成工艺卡片</el-button>
          <span class="hint">卡片落地时同时快照「版式」与「工序行」，模板改版不影响历史卡片</span>
        </div>

        <el-table v-loading="cardsLoading" :data="cards" border stripe>
          <el-table-column prop="code" label="卡片编码" width="130" />
          <el-table-column label="卡片标题" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.title || '-' }}</template>
          </el-table-column>
          <el-table-column label="模板" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="mono">{{ row.templateCode }}</span>
              <span class="sub">v{{ row.templateVersion }} {{ row.templateName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="零件" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="mono">{{ row.objectCode }}</span>
              <span class="sub">{{ row.objectName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="工艺路线" width="140">
            <template #default="{ row }">{{ row.routingCode || '-' }}</template>
          </el-table-column>
          <el-table-column prop="rowCount" label="工序行" width="90" align="center" />
          <el-table-column label="更新时间" width="160">
            <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPreview(row)">预览</el-button>
              <el-button link type="primary" @click="onExport(row)">导出 Excel</el-button>
              <el-button link type="primary" @click="onExportPdf(row)">导出 PDF</el-button>
              <el-button v-if="canWrite" link type="warning" @click="onRegenerate(row)">重新生成</el-button>
              <el-button v-if="canWrite" link type="danger" @click="onDeleteCard(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="cardPage"
          v-model:page-size="cardSize"
          class="pager"
          :total="cardTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadCards"
          @size-change="loadCards"
        />
      </el-tab-pane>

      <!-- ------------------------------------------------ 卡片模板 -->
      <el-tab-pane label="卡片模板" name="template">
        <div class="toolbar">
          <el-input
            v-model="templateKeyword"
            placeholder="模板编码 / 名称 / 标题"
            clearable
            style="width: 260px"
            @keyup.enter="reloadTemplates"
            @clear="reloadTemplates"
          />
          <el-select v-model="templateStatus" placeholder="全部状态" clearable style="width: 140px" @change="reloadTemplates">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="RELEASED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
          <el-button type="primary" @click="reloadTemplates">查询</el-button>
          <el-button v-if="canWrite" type="primary" plain @click="openCreateTemplate">新建模板</el-button>
          <span class="hint">同一模板编码可并存多版本：改版请用「另存新版本」，发布后旧版本自动归档</span>
        </div>

        <el-table v-loading="templatesLoading" :data="templates" border stripe>
          <el-table-column prop="code" label="模板编码" width="130" />
          <el-table-column prop="name" label="模板名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">
              {{ CARD_TYPE_LABEL[row.cardType as CardType] ?? row.cardType }}
            </template>
          </el-table-column>
          <el-table-column prop="versionNo" label="版本" width="70" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ CARD_TEMPLATE_STATUS_LABEL[row.status as RoutingStatus] ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="cardCount" label="卡片数" width="90" align="center" />
          <el-table-column label="更新时间" width="160">
            <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTemplate(row, true)">查看</el-button>
              <el-button v-if="canWrite && row.status === 'DRAFT'" link type="primary" @click="openTemplate(row, false)">
                编辑
              </el-button>
              <el-button v-if="canWrite && row.status === 'DRAFT'" link type="success" @click="onReleaseTemplate(row)">
                发布
              </el-button>
              <el-button v-if="canWrite" link type="warning" @click="onNewVersion(row)">另存新版本</el-button>
              <el-button v-if="canWrite && row.status === 'DRAFT'" link type="danger" @click="onDeleteTemplate(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="templatePage"
          v-model:page-size="templateSize"
          class="pager"
          :total="templateTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadTemplates"
          @size-change="loadTemplates"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- ------------------------------------------------ 模板编辑 -->
    <el-dialog v-model="templateDialogVisible" :title="templateDialogTitle" width="1000px" top="5vh" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="模板名称" required>
          <el-input v-model="templateForm.name" maxlength="200" :disabled="templateReadonly" />
        </el-form-item>
        <el-form-item label="卡片类型">
          <el-select v-model="templateForm.cardType" :disabled="templateReadonly" style="width: 200px">
            <el-option v-for="(label, value) in CARD_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="卡片标题">
          <el-input v-model="templateForm.layout.title" maxlength="200" :disabled="templateReadonly" placeholder="如：机械加工工序卡" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="templateForm.remark" type="textarea" :rows="2" maxlength="500" :disabled="templateReadonly" />
        </el-form-item>
      </el-form>

      <div class="section-title">
        表头字段（卡片级，按「标签 + 取值变量」两列一组排布）
        <el-button size="small" type="primary" plain :disabled="templateReadonly" @click="addHeaderField">添加字段</el-button>
      </div>
      <el-table :data="templateForm.layout.headerFields" border size="small">
        <el-table-column label="标签" width="220">
          <template #default="{ row }">
            <el-input v-model="row.label" :disabled="templateReadonly" maxlength="40" />
          </template>
        </el-table-column>
        <el-table-column label="取值变量">
          <template #default="{ row }">
            <el-select v-model="row.field" filterable clearable :disabled="templateReadonly" placeholder="选择变量" style="width: 100%">
              <el-option
                v-for="(label, key) in fieldDict.cardFields"
                :key="key"
                :label="`${label}（${key}）`"
                :value="key"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column v-if="!templateReadonly" label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="templateForm.layout.headerFields.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="section-title">
        明细列（工序行；绑定变量为空时输出「固定文本」）
        <el-button size="small" type="primary" plain :disabled="templateReadonly" @click="addColumn">添加列</el-button>
      </div>
      <el-table :data="templateForm.layout.columns" border size="small">
        <el-table-column label="列标题" width="150">
          <template #default="{ row }">
            <el-input v-model="row.title" :disabled="templateReadonly" maxlength="40" />
          </template>
        </el-table-column>
        <el-table-column label="绑定变量" width="200">
          <template #default="{ row }">
            <el-select v-model="row.field" filterable clearable :disabled="templateReadonly" placeholder="选择变量" style="width: 100%">
              <el-option
                v-for="(label, key) in fieldDict.stepFields"
                :key="key"
                :label="`${label}（${key}）`"
                :value="key"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="固定文本" width="140">
          <template #default="{ row }">
            <el-input v-model="row.constValue" :disabled="templateReadonly || !!row.field" maxlength="40" />
          </template>
        </el-table-column>
        <el-table-column label="对齐" width="110">
          <template #default="{ row }">
            <el-select v-model="row.align" :disabled="templateReadonly">
              <el-option label="左" value="LEFT" />
              <el-option label="中" value="CENTER" />
              <el-option label="右" value="RIGHT" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="宽度" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.width" :min="4" :max="60" :controls="false" :disabled="templateReadonly" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column v-if="!templateReadonly" label="操作" width="130" align="center">
          <template #default="{ $index }">
            <el-button link type="primary" :disabled="$index === 0" @click="moveColumn($index, -1)">上移</el-button>
            <el-button link type="primary" :disabled="$index === templateForm.layout.columns.length - 1" @click="moveColumn($index, 1)">下移</el-button>
            <el-button link type="danger" @click="templateForm.layout.columns.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-form label-width="90px" class="footer-form">
        <el-form-item label="表尾">
          <el-input v-model="templateForm.layout.footerText" maxlength="200" :disabled="templateReadonly" placeholder="如：编制：____ 审核：____ 批准：____" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="templateDialogVisible = false">关闭</el-button>
        <el-button v-if="!templateReadonly" type="primary" :loading="templateSaving" @click="onSaveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <!-- ------------------------------------------------ 生成卡片 -->
    <el-dialog v-model="cardDialogVisible" title="生成工艺卡片" width="620px">
      <el-form label-width="100px">
        <el-form-item label="卡片模板" required>
          <el-select v-model="cardForm.templateId" filterable placeholder="仅已发布模板可生成卡片" style="width: 100%">
            <el-option
              v-for="item in releasedTemplates"
              :key="item.id"
              :label="`${item.code} v${item.versionNo} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="工艺路线" required>
          <el-select
            v-model="cardForm.routingId"
            filterable
            remote
            reserve-keyword
            placeholder="输入路线编码/零件搜索"
            :remote-method="searchRoutings"
            :loading="routingsLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in routings"
              :key="item.id"
              :label="`${item.code} ${item.name}（${item.objectCode}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="cardForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cardDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="cardSaving" @click="onCreateCard">生成</el-button>
      </template>
    </el-dialog>

    <!-- ------------------------------------------------ 卡片预览 -->
    <el-dialog v-model="previewVisible" :title="`工艺卡片预览 - ${previewCard?.code ?? ''}`" width="1100px" top="5vh">
      <div v-if="previewCard" class="card-preview">
        <h3 class="card-title">{{ previewCard.layout.title || previewCard.title || '工艺卡片' }}</h3>
        <table class="card-table">
          <tbody>
            <tr v-for="(pair, index) in headerPairs" :key="index">
              <template v-for="cell in pair" :key="cell.label">
                <th class="head-label">{{ cell.label }}</th>
                <td class="head-value">{{ cell.value }}</td>
              </template>
            </tr>
            <tr>
              <th v-for="column in previewCard.layout.columns" :key="columnKey(column)" class="col-head">
                {{ column.title }}
              </th>
            </tr>
            <tr v-for="(row, rowIndex) in previewCard.rows" :key="rowIndex" :class="{ 'summary-row': row.__summary__ === 'true' }">
              <td
                v-for="column in previewCard.layout.columns"
                :key="columnKey(column)"
                :style="{ textAlign: alignStyle(column.align) }"
              >
                {{ row[columnKey(column)] }}
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="previewCard.layout.footerText" class="card-footer">{{ previewCard.layout.footerText }}</div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button v-if="previewCard" @click="onExport(previewCard)">导出 Excel</el-button>
        <el-button v-if="previewCard" type="primary" @click="onExportPdf(previewCard)">导出 PDF</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CARD_TEMPLATE_STATUS_LABEL,
  CARD_TYPE_LABEL,
  cappApi,
  cardApi,
  type CappCardDetail,
  type CappCardRow,
  type CardColumn,
  type CardFieldDict,
  type CardTemplateRow,
  type CardType,
  type RoutingRow,
  type RoutingStatus
} from '@/api/capp'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('capp.card.write'))

const activeTab = ref<'card' | 'template'>('card')
const fieldDict = ref<CardFieldDict>({ stepFields: {}, cardFields: {}, cardTypes: {} })

// ---- 卡片列表
const cards = ref<CappCardRow[]>([])
const cardsLoading = ref(false)
const cardKeyword = ref('')
const cardPage = ref(1)
const cardSize = ref(20)
const cardTotal = ref(0)

// ---- 模板列表
const templates = ref<CardTemplateRow[]>([])
const templatesLoading = ref(false)
const templateKeyword = ref('')
const templateStatus = ref<string>('')
const templatePage = ref(1)
const templateSize = ref(20)
const templateTotal = ref(0)

// ---- 模板编辑
const templateDialogVisible = ref(false)
const templateReadonly = ref(false)
const templateSaving = ref(false)
const templateForm = reactive<{
  id?: string
  name: string
  cardType: string
  remark: string
  layout: { title: string; headerFields: { label: string; field: string | null }[]; columns: CardColumn[]; footerText: string }
}>({
  name: '',
  cardType: 'PROCESS',
  remark: '',
  layout: { title: '', headerFields: [], columns: [], footerText: '' }
})

// ---- 生成卡片
const cardDialogVisible = ref(false)
const cardSaving = ref(false)
const releasedTemplates = ref<CardTemplateRow[]>([])
const routings = ref<RoutingRow[]>([])
const routingsLoading = ref(false)
const cardForm = reactive<{ templateId: string; routingId: string; remark: string }>({
  templateId: '',
  routingId: '',
  remark: ''
})

// ---- 预览
const previewVisible = ref(false)
const previewCard = ref<CappCardDetail | null>(null)

const templateDialogTitle = computed(() => {
  if (templateReadonly.value) {
    return '卡片模板详情'
  }
  return templateForm.id ? '编辑卡片模板（草稿）' : '新建卡片模板'
})

/** 表头字段按「每行 2 组」成对（与后端 Excel 排布一致）。 */
const headerPairs = computed(() => {
  const card = previewCard.value
  if (!card) {
    return []
  }
  const values = card.fieldValues ?? {}
  const cells = (card.layout.headerFields ?? []).map((header) => ({
    label: header.label,
    value: header.field ? values[header.field] ?? '' : ''
  }))
  const pairs: { label: string; value: string }[][] = []
  for (let i = 0; i < cells.length; i += 2) {
    pairs.push(cells.slice(i, i + 2))
  }
  return pairs
})

onMounted(async () => {
  await Promise.all([loadCards(), loadTemplates(), loadFieldDict()])
})

function columnKey(column: CardColumn): string {
  if (column.key) {
    return column.key
  }
  return column.field ?? column.title
}

function alignStyle(align?: string | null): 'left' | 'center' | 'right' {
  if (align === 'CENTER') {
    return 'center'
  }
  return align === 'RIGHT' ? 'right' : 'left'
}

async function loadFieldDict(): Promise<void> {
  try {
    fieldDict.value = await cardApi.fields()
  } catch {
    fieldDict.value = { stepFields: {}, cardFields: {}, cardTypes: {} }
  }
}

// ---------------------------------------------------------------- 卡片

async function loadCards(): Promise<void> {
  cardsLoading.value = true
  try {
    const result = await cardApi.list({
      page: cardPage.value,
      size: cardSize.value,
      keyword: cardKeyword.value || undefined
    })
    cards.value = result.items
    cardTotal.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载工艺卡片失败')
  } finally {
    cardsLoading.value = false
  }
}

function reloadCards(): void {
  cardPage.value = 1
  loadCards()
}

async function openCreateCard(): Promise<void> {
  cardForm.templateId = ''
  cardForm.routingId = ''
  cardForm.remark = ''
  routings.value = []
  cardDialogVisible.value = true
  try {
    const result = await cardApi.templateList({ page: 1, size: 200, status: 'RELEASED' })
    releasedTemplates.value = result.items
    if (!result.items.length) {
      ElMessage.warning('还没有已发布的卡片模板，请先在「卡片模板」页新建并发布')
    }
  } catch {
    releasedTemplates.value = []
  }
  searchRoutings('')
}

async function searchRoutings(keywordText: string): Promise<void> {
  routingsLoading.value = true
  try {
    const result = await cappApi.list({ page: 1, size: 50, keyword: keywordText || undefined })
    routings.value = result.items
  } catch {
    routings.value = []
  } finally {
    routingsLoading.value = false
  }
}

async function onCreateCard(): Promise<void> {
  if (!cardForm.templateId) {
    ElMessage.warning('请选择卡片模板')
    return
  }
  if (!cardForm.routingId) {
    ElMessage.warning('请选择工艺路线')
    return
  }
  cardSaving.value = true
  try {
    const created = await cardApi.create({
      templateId: cardForm.templateId,
      routingId: cardForm.routingId,
      remark: cardForm.remark || null
    })
    ElMessage.success(`已生成卡片 ${created.code}`)
    cardDialogVisible.value = false
    activeTab.value = 'card'
    await loadCards()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '生成卡片失败')
  } finally {
    cardSaving.value = false
  }
}

async function openPreview(row: CappCardRow): Promise<void> {
  try {
    previewCard.value = await cardApi.detail(row.id)
    previewVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载卡片失败')
  }
}

async function onExport(row: CappCardRow): Promise<void> {
  try {
    await cardApi.exportExcel(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  }
}

async function onExportPdf(row: CappCardRow): Promise<void> {
  try {
    await cardApi.exportPdf(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出 PDF 失败')
  }
}

async function onRegenerate(row: CappCardRow): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `按当前工艺路线的工序刷新卡片 ${row.code} 的行数据？版式与模板版本保持不变。`,
      '重新生成',
      { confirmButtonText: '刷新', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await cardApi.regenerate(row.id)
    ElMessage.success('已按最新工序刷新')
    await loadCards()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '刷新失败')
  }
}

async function onDeleteCard(row: CappCardRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除工艺卡片 ${row.code}？`, '删除卡片', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await cardApi.remove(row.id)
    ElMessage.success('已删除')
    await loadCards()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

// ---------------------------------------------------------------- 模板

async function loadTemplates(): Promise<void> {
  templatesLoading.value = true
  try {
    const result = await cardApi.templateList({
      page: templatePage.value,
      size: templateSize.value,
      keyword: templateKeyword.value || undefined,
      status: templateStatus.value || undefined
    })
    templates.value = result.items
    templateTotal.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载卡片模板失败')
  } finally {
    templatesLoading.value = false
  }
}

function reloadTemplates(): void {
  templatePage.value = 1
  loadTemplates()
}

function defaultLayout(): { title: string; headerFields: { label: string; field: string | null }[]; columns: CardColumn[]; footerText: string } {
  return {
    title: '机械加工工序卡',
    headerFields: [
      { label: '零件编码', field: 'objectCode' },
      { label: '零件名称', field: 'objectName' },
      { label: '规格型号', field: 'objectSpec' },
      { label: '材料', field: 'objectMaterial' },
      { label: '路线编码', field: 'routeCode' },
      { label: '工艺负责人', field: 'ownerName' }
    ],
    columns: [
      { title: '工序号', field: 'seqNo', align: 'CENTER', width: 10 },
      { title: '工序名称', field: 'name', align: 'LEFT', width: 18 },
      { title: '工序类型', field: 'opType', align: 'CENTER', width: 12 },
      { title: '工作中心', field: 'workCenter', align: 'LEFT', width: 14 },
      { title: '工艺资源', field: 'resourceName', align: 'LEFT', width: 18 },
      { title: '准备工时', field: 'setupMinutes', align: 'RIGHT', width: 10 },
      { title: '单件工时', field: 'runMinutes', align: 'RIGHT', width: 10 },
      { title: '备注', field: 'remark', align: 'LEFT', width: 20 }
    ],
    footerText: '编制：____________ 审核：____________ 批准：____________'
  }
}

function openCreateTemplate(): void {
  templateReadonly.value = false
  Object.assign(templateForm, {
    id: undefined,
    name: '',
    cardType: 'PROCESS',
    remark: '',
    layout: defaultLayout()
  })
  templateDialogVisible.value = true
}

async function openTemplate(row: CardTemplateRow, readonly: boolean): Promise<void> {
  try {
    const detail = await cardApi.template(row.id)
    templateReadonly.value = readonly
    Object.assign(templateForm, {
      id: detail.id,
      name: detail.name,
      cardType: detail.cardType,
      remark: detail.remark ?? '',
      layout: {
        title: detail.layout.title ?? '',
        headerFields: (detail.layout.headerFields ?? []).map((header) => ({
          label: header.label,
          field: header.field ?? null
        })),
        columns: (detail.layout.columns ?? []).map((column) => ({ ...column })),
        footerText: detail.layout.footerText ?? ''
      }
    })
    templateDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载模板失败')
  }
}

function addHeaderField(): void {
  templateForm.layout.headerFields.push({ label: '', field: null })
}

function addColumn(): void {
  templateForm.layout.columns.push({ title: '', field: null, constValue: null, align: 'LEFT', width: 12 })
}

function moveColumn(index: number, delta: number): void {
  const target = index + delta
  const columns = templateForm.layout.columns
  if (target < 0 || target >= columns.length) {
    return
  }
  const [moved] = columns.splice(index, 1)
  columns.splice(target, 0, moved)
}

async function onSaveTemplate(): Promise<void> {
  if (!templateForm.name.trim()) {
    ElMessage.warning('请填写模板名称')
    return
  }
  if (!templateForm.layout.columns.length) {
    ElMessage.warning('至少需要一列明细')
    return
  }
  if (templateForm.layout.columns.some((column) => !column.title?.trim())) {
    ElMessage.warning('每列都需要填写列标题')
    return
  }
  templateSaving.value = true
  try {
    const payload = {
      name: templateForm.name.trim(),
      cardType: templateForm.cardType,
      title: templateForm.layout.title || null,
      remark: templateForm.remark || null,
      layout: {
        title: templateForm.layout.title || null,
        headerFields: templateForm.layout.headerFields
          .filter((header) => header.label?.trim())
          .map((header) => ({ label: header.label.trim(), field: header.field || null })),
        columns: templateForm.layout.columns.map((column) => ({
          key: column.key ?? null,
          title: column.title.trim(),
          field: column.field || null,
          constValue: column.field ? null : column.constValue || null,
          width: column.width ?? 12,
          align: column.align ?? 'LEFT'
        })),
        footerText: templateForm.layout.footerText || null
      }
    }
    if (templateForm.id) {
      await cardApi.updateTemplate(templateForm.id, payload)
      ElMessage.success('已保存')
    } else {
      await cardApi.createTemplate(payload)
      ElMessage.success('已创建草稿')
    }
    templateDialogVisible.value = false
    await Promise.all([loadTemplates(), loadFieldDict()])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    templateSaving.value = false
  }
}

async function onReleaseTemplate(row: CardTemplateRow): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `发布后该版本不可再修改（同编码的其它已发布版本将自动归档）。确认发布 ${row.code} v${row.versionNo}？`,
      '发布卡片模板',
      { confirmButtonText: '发布', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await cardApi.releaseTemplate(row.id)
    ElMessage.success('已发布')
    await Promise.all([loadTemplates(), loadCards()])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败')
  }
}

async function onNewVersion(row: CardTemplateRow): Promise<void> {
  try {
    const created = await cardApi.newVersion(row.id)
    ElMessage.success(`已另存为草稿 ${created.code} v${created.versionNo}`)
    await loadTemplates()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '另存新版本失败')
  }
}

async function onDeleteTemplate(row: CardTemplateRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除草稿模板 ${row.code} v${row.versionNo}？`, '删除模板', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await cardApi.removeTemplate(row.id)
    ElMessage.success('已删除')
    await loadTemplates()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function statusTagType(value: RoutingStatus): 'success' | 'info' | 'warning' {
  if (value === 'RELEASED') {
    return 'success'
  }
  return value === 'ARCHIVED' ? 'info' : 'warning'
}

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.tabs {
  padding: 0 4px;
}

.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.hint {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12px 0 8px;
  font-weight: 600;
}

.footer-form {
  margin-top: 12px;
}

.mono {
  margin-right: 6px;
  font-family: Consolas, monospace;
}

.sub {
  color: #909399;
  font-size: 12px;
}

.card-preview {
  overflow-x: auto;
}

.card-title {
  margin: 0 0 12px;
  text-align: center;
  font-size: 18px;
}

.card-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.card-table th,
.card-table td {
  border: 1px solid #606266;
  padding: 4px 6px;
  vertical-align: middle;
}

.head-label {
  width: 100px;
  background: #f5f7fa;
  font-weight: 600;
  white-space: nowrap;
}

.head-value {
  min-width: 120px;
}

.col-head {
  background: #ebeef5;
  font-weight: 600;
  white-space: nowrap;
}

.summary-row td {
  font-weight: 600;
  background: #fafafa;
}

.card-footer {
  margin-top: 12px;
  font-size: 13px;
}
</style>
