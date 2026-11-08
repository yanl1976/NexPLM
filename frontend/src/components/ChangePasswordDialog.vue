<template>
  <el-dialog
    v-model="visible"
    :title="forced ? '首次登录需修改密码' : '修改密码'"
    :width="'440px'"
    :show-close="!forced"
    :close-on-click-modal="false"
    :close-on-press-escape="!forced"
    @closed="reset"
  >
    <el-alert
      v-if="forced"
      type="warning"
      :closable="false"
      show-icon
      title="当前密码由管理员设置，请先修改后再继续使用"
      class="forced-tip"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" @submit.prevent>
      <el-form-item label="原密码" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          show-password
          autocomplete="current-password"
          placeholder="请输入当前密码"
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          autocomplete="new-password"
          placeholder="至少 8 位，含大小写/数字/符号中的 3 类"
        />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirm">
        <el-input
          v-model="form.confirm"
          type="password"
          show-password
          autocomplete="new-password"
          placeholder="再输入一次新密码"
        />
      </el-form-item>
    </el-form>
    <div class="pwd-hint">
      密码规则：8~64 位，至少包含大写字母、小写字母、数字、符号中的 3 类；不能与最近 3 次用过的密码相同。
    </div>
    <template #footer>
      <!-- 强制模式下不给「取消」，但必须留一条逃生口：不知道原密码（管理员重置后）也要能退出 -->
      <el-button v-if="forced" @click="emit('logout')">退出登录</el-button>
      <el-button v-else @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">
        {{ forced ? '修改并继续' : '确定' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api/auth'

const props = withDefaults(defineProps<{ modelValue: boolean; forced?: boolean }>(), {
  forced: false
})
const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'success'): void
  (event: 'logout'): void
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度需 8-64 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function reset(): void {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirm = ''
  formRef.value?.clearValidate()
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    await authApi.changePassword(form.oldPassword, form.newPassword)
    ElMessage.success('密码已修改')
    emit('success')
    visible.value = false
  } catch (error) {
    // 后端会把强度/历史/原密码错误的原因放在 message 里，原样提示即可
    ElMessage.error(error instanceof Error ? error.message : '修改密码失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.forced-tip {
  margin-bottom: 14px;
}

.pwd-hint {
  margin: -6px 0 4px 92px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}
</style>
