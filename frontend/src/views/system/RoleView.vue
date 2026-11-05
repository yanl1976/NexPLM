<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>角色与功能授权</span>
          <el-button type="primary" @click="openCreate">新建角色</el-button>
        </div>
      </template>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="code" label="角色编码" width="160" />
        <el-table-column prop="name" label="角色名称" min-width="160" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.roleType === 'BUSINESS' ? 'info' : 'warning'">
              {{ roleTypeLabel(row.roleType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataScope" label="数据范围" width="120" />
        <el-table-column prop="userCount" label="用户数" width="90" />
        <el-table-column prop="permissionCount" label="权限点数" width="100" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openGrant(row)">授权</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" :disabled="row.roleType !== 'BUSINESS'" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑角色' : '新建角色'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="全部（集团）" value="ALL" />
            <el-option label="本公司" value="COMPANY" />
            <el-option label="本部门及下级" value="ORG" />
            <el-option label="仅本人" value="SELF" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="grantVisible" :title="`授权：${grantRoleName}`" size="520px">
      <div v-loading="grantLoading" class="grant">
        <div v-for="group in groupedPermissions" :key="group.module" class="grant-group">
          <div class="grant-module">
            <el-checkbox
              :model-value="isModuleAllChecked(group.items)"
              :indeterminate="isModuleIndeterminate(group.items)"
              @change="(checked: boolean) => toggleModule(group.items, checked)"
            >
              {{ group.module }}
            </el-checkbox>
          </div>
          <el-checkbox-group v-model="checkedCodes">
            <el-checkbox v-for="item in group.items" :key="item.code" :value="item.code" class="perm-item">
              {{ item.name }}<span class="perm-code">{{ item.code }}</span>
            </el-checkbox>
          </el-checkbox-group>
        </div>
      </div>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="grantSaving" @click="onGrant">保存授权</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { roleApi, type PermissionItem, type RoleRow } from '@/api/system'

interface PermissionGroup {
  module: string
  items: PermissionItem[]
}

const rows = ref<RoleRow[]>([])
const permissions = ref<PermissionItem[]>([])
const loading = ref(false)
const saving = ref(false)

const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({ code: '', name: '', dataScope: 'COMPANY', status: 'ACTIVE' })
const rules: FormRules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const grantVisible = ref(false)
const grantLoading = ref(false)
const grantSaving = ref(false)
const grantRoleId = ref<string | null>(null)
const grantRoleName = ref('')
const checkedCodes = ref<string[]>([])

const groupedPermissions = computed<PermissionGroup[]>(() => {
  const map = new Map<string, PermissionItem[]>()
  permissions.value.forEach((item) => {
    const list = map.get(item.module) ?? []
    list.push(item)
    map.set(item.module, list)
  })
  return Array.from(map.entries()).map(([module, items]) => ({ module, items }))
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const [roleList, permissionList] = await Promise.all([roleApi.list(), roleApi.permissions()])
    rows.value = roleList
    permissions.value = permissionList
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function roleTypeLabel(type: string): string {
  const map: Record<string, string> = {
    SYS_ADMIN: '系统管理员',
    SEC_ADMIN: '安全保密管理员',
    AUDITOR: '安全审计员',
    BUSINESS: '业务角色'
  }
  return map[type] ?? type
}

function openCreate(): void {
  editingId.value = null
  Object.assign(form, { code: '', name: '', dataScope: 'COMPANY', status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEdit(row: RoleRow): void {
  editingId.value = row.id
  Object.assign(form, { code: row.code, name: row.name, dataScope: row.dataScope, status: row.status })
  dialogVisible.value = true
}

async function onSave(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await roleApi.update(editingId.value, { ...form })
    } else {
      await roleApi.create({ ...form })
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function openGrant(row: RoleRow): Promise<void> {
  grantRoleId.value = row.id
  grantRoleName.value = row.name
  grantVisible.value = true
  grantLoading.value = true
  try {
    checkedCodes.value = await roleApi.rolePermissions(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载授权失败')
  } finally {
    grantLoading.value = false
  }
}

function isModuleAllChecked(items: PermissionItem[]): boolean {
  return items.length > 0 && items.every((item) => checkedCodes.value.includes(item.code))
}

function isModuleIndeterminate(items: PermissionItem[]): boolean {
  const count = items.filter((item) => checkedCodes.value.includes(item.code)).length
  return count > 0 && count < items.length
}

function toggleModule(items: PermissionItem[], checked: boolean): void {
  const codes = items.map((item) => item.code)
  if (checked) {
    checkedCodes.value = Array.from(new Set([...checkedCodes.value, ...codes]))
  } else {
    checkedCodes.value = checkedCodes.value.filter((code) => !codes.includes(code))
  }
}

async function onGrant(): Promise<void> {
  if (!grantRoleId.value) {
    return
  }
  grantSaving.value = true
  try {
    await roleApi.grant(grantRoleId.value, checkedCodes.value)
    ElMessage.success('授权已保存')
    grantVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '授权失败')
  } finally {
    grantSaving.value = false
  }
}

async function onDelete(row: RoleRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await roleApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.grant {
  min-height: 200px;
}

.grant-group {
  margin-bottom: 14px;
}

.grant-module {
  font-weight: 600;
  color: #24435f;
  margin-bottom: 4px;
}

.perm-item {
  width: 48%;
}

.perm-code {
  margin-left: 6px;
  color: #a0a8b5;
  font-size: 11px;
}
</style>
