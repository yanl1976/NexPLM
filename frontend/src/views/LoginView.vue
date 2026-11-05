<template>
  <div class="login-page">
    <div class="login-card">
      <div class="title">NexPLM</div>
      <div class="subtitle">产品生命周期管理平台</div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            autocomplete="current-password"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="onSubmit">登 录</el-button>
      </el-form>
      <div class="tip">首次登录请使用管理员账号，登录后请立即修改密码</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    await router.replace(typeof redirect === 'string' ? redirect : '/')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #16222f 0%, #24435f 55%, #35618c 100%);
}

.login-card {
  width: 360px;
  padding: 32px 32px 24px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.28);
}

.title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 3px;
  color: #1f2d3d;
}

.subtitle {
  margin: 6px 0 22px;
  color: #8a94a6;
  font-size: 13px;
}

.submit {
  width: 100%;
  margin-top: 4px;
}

.tip {
  margin-top: 16px;
  font-size: 12px;
  color: #a0a8b5;
  line-height: 1.6;
}
</style>
