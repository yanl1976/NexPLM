<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="openCreate">新建用户</el-button>
        </div>
      </template>

      <div class="filters">
        <el-input v-model="keyword" placeholder="用户名 / 姓名 / 邮箱" clearable style="width: 240px" @keyup.enter="reload" />
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="reload">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="displayName" label="姓名" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="mobile" label="手机" width="130" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="name in row.roleNames" :key="name" size="small" class="role-tag">{{ name }}</el-tag>
            <span v-if="row.roleNames.length === 0">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="170">
          <template #default="{ row }">{{ formatTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openReset(row)">重置密码</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        :current-page="page"
        :page-size="size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新建用户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="displayName">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.mobile" />
        </el-form-item>
        <el-form-item label="密级">
          <el-input-number v-model="form.securityLevel" :min="0" :max="9" />
        </el-form-item>
        <el-form-item v-if="editingId" label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%" placeholder="选择角色">
            <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <el-input v-model="newPassword" type="password" show-password placeholder="至少 8 位" />
      <div class="hint">重置后该用户下次登录需修改密码</div>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="onResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { roleApi, userApi, type RoleRow, type UserRow, type UserSaveReq } from '@/api/system'

const rows = ref<UserRow[]>([])
const roles = ref<RoleRow[]>([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const status = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

const dialogVisible = ref(false)
const resetVisible = ref(false)
const editingId = ref<string | null>(null)
const resetUserId = ref<string | null>(null)
const newPassword = ref('')
const formRef = ref<FormInstance>()

const form = reactive<UserSaveReq>({
  username: '',
  password: '',
  displayName: '',
  email: '',
  mobile: '',
  securityLevel: 1,
  status: 'ACTIVE',
  roleIds: []
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 8, message: '密码至少 8 位', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await userApi.page({
      keyword: keyword.value || undefined,
      status: status.value || undefined,
      page: page.value,
      size: size.value
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadRoles(): Promise<void> {
  roles.value = await roleApi.list()
}

function reload(): void {
  page.value = 1
  load()
}

function resetFilters(): void {
  keyword.value = ''
  status.value = ''
  reload()
}

function onPageChange(value: number): void {
  page.value = value
  load()
}

function onSizeChange(value: number): void {
  size.value = value
  page.value = 1
  load()
}

function openCreate(): void {
  editingId.value = null
  Object.assign(form, {
    username: '',
    password: '',
    displayName: '',
    email: '',
    mobile: '',
    securityLevel: 1,
    status: 'ACTIVE',
    roleIds: []
  })
  dialogVisible.value = true
}

function openEdit(row: UserRow): void {
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    password: '',
    displayName: row.displayName,
    email: row.email ?? '',
    mobile: row.mobile ?? '',
    securityLevel: row.securityLevel,
    status: row.status,
    roleIds: [...row.roleIds]
  })
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
      await userApi.update(editingId.value, { ...form })
      ElMessage.success('已保存')
    } else {
      await userApi.create({ ...form })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function openReset(row: UserRow): void {
  resetUserId.value = row.id
  newPassword.value = ''
  resetVisible.value = true
}

async function onResetPassword(): Promise<void> {
  if (!resetUserId.value || newPassword.value.length < 8) {
    ElMessage.warning('新密码至少 8 位')
    return
  }
  try {
    await userApi.resetPassword(resetUserId.value, newPassword.value)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重置失败')
  }
}

async function onDelete(row: UserRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.displayName}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await userApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function formatTime(value: string | null): string {
  return value ? value.replace('T', ' ').substring(0, 19) : '-'
}

onMounted(async () => {
  await Promise.all([load(), loadRoles()])
})
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

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.role-tag {
  margin-right: 4px;
}

.hint {
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
