<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="编码/名称/简称" clearable style="width: 200px" @keyup.enter="reload" />
      <el-select v-model="category" placeholder="全部类别" clearable style="width: 130px" @change="reload">
        <el-option v-for="(label, value) in SUPPLIER_CATEGORY_LABEL" :key="value" :label="label" :value="value" />
      </el-select>
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px" @change="reload">
        <el-option v-for="(label, value) in SUPPLIER_STATUS_LABEL" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建供应商</el-button>
      <span class="hint">一个零件可挂多个供应商（多源），但首选只能一个；有在用零件关系的供应商不能删除</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe @row-dblclick="(row: SupplierRow) => openDetail(row.id)">
      <el-table-column prop="code" label="编码" width="130" />
      <el-table-column prop="name" label="供应商名称" min-width="220" show-overflow-tooltip />
      <el-table-column label="类别" width="110" align="center">
        <template #default="{ row }">{{ SUPPLIER_CATEGORY_LABEL[row.category] ?? row.category }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">
            {{ SUPPLIER_STATUS_LABEL[row.status] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="contact" label="联系人" width="100">
        <template #default="{ row }">{{ row.contact || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="电话" width="130">
        <template #default="{ row }">{{ row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column label="归属组织" width="140">
        <template #default="{ row }">{{ row.orgName || '-' }}</template>
      </el-table-column>
      <el-table-column label="零件关系" width="120" align="center">
        <template #default="{ row }">{{ row.partCount }}（首选 {{ row.preferredCount }}）</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="没有符合条件的供应商" :image-size="60" />
    <el-pagination
      v-if="total > size"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPageChange"
    />

    <!-- 新建/编辑供应商 -->
    <el-dialog v-model="formVisible" :title="form.id ? `编辑 ${form.code}` : '新建供应商'" width="680px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="200" />
        </el-form-item>
        <el-form-item label="简称">
          <el-input v-model="form.shortName" maxlength="100" style="width: 240px" />
          <span class="label-gap">归属组织</span>
          <el-tree-select
            v-model="form.orgId"
            :data="orgTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.category" style="width: 150px">
            <el-option v-for="(label, value) in SUPPLIER_CATEGORY_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <span class="label-gap">状态</span>
          <el-select v-model="form.status" style="width: 130px">
            <el-option v-for="(label, value) in SUPPLIER_STATUS_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contact" maxlength="64" style="width: 200px" />
          <span class="label-gap">电话</span>
          <el-input v-model="form.phone" maxlength="64" style="width: 200px" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" maxlength="128" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" maxlength="500" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="form.taxNo" maxlength="64" style="width: 260px" />
          <span class="label-gap">币种</span>
          <el-input v-model="form.currency" maxlength="8" style="width: 100px" />
        </el-form-item>
        <el-form-item label="账期">
          <el-input v-model="form.paymentTerms" maxlength="100" placeholder="月结 60 天 / 预付 30%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :title="detail ? `${detail.code} ${detail.name}` : '供应商'" size="860px">
      <div v-if="detail" class="detail-body">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)" size="small">
              {{ SUPPLIER_STATUS_LABEL[detail.status] ?? detail.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类别">
            {{ SUPPLIER_CATEGORY_LABEL[detail.category] ?? detail.category }}
          </el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.contact || '-' }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="归属组织">{{ detail.orgName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="税号">{{ detail.taxNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="币种/账期">
            {{ detail.currency }} / {{ detail.paymentTerms || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="action-bar">
          <template v-if="canWrite">
            <el-button size="small" @click="openEdit">编辑供应商</el-button>
            <el-button size="small" type="primary" plain @click="openPartDialog(null)">新增零件关系</el-button>
            <el-button size="small" type="danger" plain @click="onRemove">删除供应商</el-button>
          </template>
        </div>

        <div class="section-title">零件关系（{{ detail.partCount }} 条，首选 {{ detail.preferredCount }}）</div>
        <el-table :data="detail.parts" border size="small">
          <el-table-column prop="itemCode" label="零件编码" width="140" />
          <el-table-column prop="itemName" label="零件名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="供货类型" width="100" align="center">
            <template #default="{ row }">{{ SUPPLY_TYPE_LABEL[row.supplyType] ?? row.supplyType }}</template>
          </el-table-column>
          <el-table-column label="首选" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.preferred" type="success" size="small">首选</el-tag>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="交期(天)" width="90" align="center">
            <template #default="{ row }">{{ row.leadTimeDays ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="MOQ" width="90" align="right">
            <template #default="{ row }">{{ row.moq ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="参考单价" width="110" align="right">
            <template #default="{ row }">{{ row.price ?? '-' }} {{ row.currency }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                {{ row.status === 'ACTIVE' ? '在用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="canWrite" label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPartDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="onRemovePart(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!detail.parts.length" description="还没有关联零件" :image-size="50" />
      </div>
    </el-drawer>

    <!-- 零件关系 -->
    <el-dialog v-model="partVisible" :title="partForm.id ? '编辑零件关系' : '新增零件关系'" width="600px">
      <el-form label-width="100px">
        <el-form-item label="零件" required>
          <el-select
            v-model="partForm.objectId"
            filterable
            remote
            clearable
            :remote-method="searchItems"
            :loading="itemLoading"
            placeholder="输入编码/名称搜索"
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
        <el-form-item label="供货类型">
          <el-select v-model="partForm.supplyType" style="width: 150px">
            <el-option v-for="(label, value) in SUPPLY_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <span class="label-gap">首选</span>
          <el-switch v-model="partForm.preferred" />
          <span class="label-gap">状态</span>
          <el-switch v-model="partForm.active" active-text="在用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="交期(天)">
          <el-input-number v-model="partForm.leadTimeDays" :min="0" :max="3650" style="width: 140px" />
          <span class="label-gap">MOQ</span>
          <el-input-number v-model="partForm.moq" :min="0" :precision="0" style="width: 140px" />
        </el-form-item>
        <el-form-item label="参考单价">
          <el-input-number v-model="partForm.price" :min="0" :precision="4" style="width: 160px" />
          <span class="label-gap">币种</span>
          <el-input v-model="partForm.currency" maxlength="8" style="width: 100px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="partForm.remark" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="partVisible = false">取消</el-button>
        <el-button type="primary" :loading="partSaving" @click="onSavePart">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  supplierApi,
  SUPPLIER_CATEGORY_LABEL,
  SUPPLIER_STATUS_LABEL,
  SUPPLY_TYPE_LABEL,
  type SupplierDetail,
  type SupplierPartRow,
  type SupplierRow
} from '@/api/supplier'
import { itemApi, type ItemSummary } from '@/api/item'
import { orgApi, type OrgNode } from '@/api/system'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('sup.supplier.write'))

const loading = ref(false)
const saving = ref(false)
const rows = ref<SupplierRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const category = ref('')
const status = ref('')
const orgTree = ref<OrgNode[]>([])

const formVisible = ref(false)
const form = reactive({
  id: '',
  code: '',
  name: '',
  shortName: '',
  category: 'MATERIAL',
  status: 'PENDING',
  contact: '',
  phone: '',
  email: '',
  address: '',
  taxNo: '',
  currency: 'CNY',
  paymentTerms: '',
  orgId: null as string | null,
  remark: ''
})

const detailVisible = ref(false)
const detail = ref<SupplierDetail | null>(null)

const partVisible = ref(false)
const partSaving = ref(false)
const itemLoading = ref(false)
const itemOptions = ref<ItemSummary[]>([])
const partForm = reactive({
  id: '',
  objectId: '',
  supplyType: 'BUY',
  preferred: false,
  active: true,
  leadTimeDays: 0 as number | null,
  moq: 0 as number | null,
  price: 0 as number | null,
  currency: 'CNY',
  remark: ''
})

onMounted(async () => {
  await loadOrgs()
  await reload()
})

async function loadOrgs(): Promise<void> {
  try {
    orgTree.value = await orgApi.tree()
  } catch {
    orgTree.value = []
  }
}

async function reload(): Promise<void> {
  loading.value = true
  try {
    const result = await supplierApi.page({
      keyword: keyword.value || undefined,
      category: category.value || undefined,
      status: status.value || undefined,
      page: page.value,
      size: size.value
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载供应商失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(value: number): void {
  page.value = value
  void reload()
}

/* ------------------------------------------------------------ 供应商 */

function openCreate(): void {
  form.id = ''
  form.code = ''
  form.name = ''
  form.shortName = ''
  form.category = 'MATERIAL'
  form.status = 'PENDING'
  form.contact = ''
  form.phone = ''
  form.email = ''
  form.address = ''
  form.taxNo = ''
  form.currency = 'CNY'
  form.paymentTerms = ''
  form.orgId = null
  form.remark = ''
  formVisible.value = true
}

function openEdit(): void {
  if (!detail.value) {
    return
  }
  form.id = detail.value.id
  form.code = detail.value.code
  form.name = detail.value.name
  form.shortName = detail.value.shortName ?? ''
  form.category = detail.value.category
  form.status = detail.value.status
  form.contact = detail.value.contact ?? ''
  form.phone = detail.value.phone ?? ''
  form.email = detail.value.email ?? ''
  form.address = detail.value.address ?? ''
  form.taxNo = detail.value.taxNo ?? ''
  form.currency = detail.value.currency
  form.paymentTerms = detail.value.paymentTerms ?? ''
  form.orgId = detail.value.orgId ?? null
  form.remark = detail.value.remark ?? ''
  formVisible.value = true
}

async function onSave(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请填写供应商名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      shortName: form.shortName.trim() || null,
      category: form.category,
      status: form.status,
      contact: form.contact.trim() || null,
      phone: form.phone.trim() || null,
      email: form.email.trim() || null,
      address: form.address.trim() || null,
      taxNo: form.taxNo.trim() || null,
      currency: form.currency.trim() || 'CNY',
      paymentTerms: form.paymentTerms.trim() || null,
      orgId: form.orgId,
      remark: form.remark.trim() || null
    }
    const saved = form.id
      ? await supplierApi.update(form.id, payload)
      : await supplierApi.create(payload)
    ElMessage.success('已保存')
    formVisible.value = false
    await reload()
    if (form.id) {
      detail.value = saved
    } else {
      await openDetail(saved.id)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function openDetail(id: string): Promise<void> {
  try {
    detail.value = await supplierApi.detail(id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  }
}

async function onRemove(): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除供应商 ${detail.value.code}？有在用零件关系时会被拒绝。`,
      '删除供应商',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await supplierApi.remove(detail.value.id)
    ElMessage.success('已删除')
    detailVisible.value = false
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

/* ------------------------------------------------------------ 零件关系 */

function openPartDialog(row: SupplierPartRow | null): void {
  partForm.id = row?.id ?? ''
  partForm.objectId = row?.objectId ?? ''
  partForm.supplyType = row?.supplyType ?? 'BUY'
  partForm.preferred = row?.preferred ?? false
  partForm.active = (row?.status ?? 'ACTIVE') === 'ACTIVE'
  partForm.leadTimeDays = row?.leadTimeDays ?? 0
  partForm.moq = row?.moq ?? 0
  partForm.price = row?.price ?? 0
  partForm.currency = row?.currency ?? 'CNY'
  partForm.remark = row?.remark ?? ''
  itemOptions.value = row
    ? [
        {
          id: row.objectId,
          code: row.itemCode ?? '',
          name: row.itemName ?? ''
        } as ItemSummary
      ]
    : []
  partVisible.value = true
}

async function searchItems(keywordText: string): Promise<void> {
  if (!keywordText) {
    itemOptions.value = []
    return
  }
  itemLoading.value = true
  try {
    const result = await itemApi.list({ keyword: keywordText, page: 1, size: 20 })
    itemOptions.value = result.items
  } catch {
    itemOptions.value = []
  } finally {
    itemLoading.value = false
  }
}

async function onSavePart(): Promise<void> {
  if (!detail.value) {
    return
  }
  if (!partForm.objectId) {
    ElMessage.warning('请选择零件')
    return
  }
  partSaving.value = true
  try {
    const payload = {
      objectType: 'PdmItem',
      objectId: partForm.objectId,
      supplyType: partForm.supplyType,
      preferred: partForm.preferred,
      leadTimeDays: partForm.leadTimeDays,
      moq: partForm.moq,
      price: partForm.price,
      currency: partForm.currency.trim() || 'CNY',
      status: partForm.active ? 'ACTIVE' : 'INACTIVE',
      remark: partForm.remark.trim() || null
    }
    detail.value = partForm.id
      ? await supplierApi.updatePart(partForm.id, payload)
      : await supplierApi.addPart(detail.value.id, payload)
    partVisible.value = false
    ElMessage.success('已保存')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存零件关系失败')
  } finally {
    partSaving.value = false
  }
}

async function onRemovePart(row: SupplierPartRow): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除与零件 ${row.itemCode} 的关系？`, '删除关系', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await supplierApi.removePart(row.id)
    detail.value = await supplierApi.detail(detail.value.id)
    ElMessage.success('已删除')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

/* ------------------------------------------------------------ 展示辅助 */

function statusType(value: string): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'QUALIFIED') {
    return 'success'
  }
  if (value === 'PENDING') {
    return 'warning'
  }
  if (value === 'DISQUALIFIED') {
    return 'danger'
  }
  return 'info'
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

.action-bar {
  margin: 12px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.section-title {
  font-weight: 600;
  margin: 12px 0 8px;
  padding-left: 6px;
  border-left: 3px solid #409eff;
}

.muted {
  color: #909399;
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
