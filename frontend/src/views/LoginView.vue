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

      <template v-if="providers.length">
        <el-divider class="divider">企业登录</el-divider>
        <el-button
          v-for="provider in oidcProviders"
          :key="provider.code"
          class="sso-button"
          :loading="ssoLoading"
          @click="startOidc(provider)"
        >
          {{ provider.name }}
        </el-button>
        <el-button v-if="ldapProviders.length" class="sso-button" @click="openLdap">
          域账号登录（{{ ldapProviders[0].name }}）
        </el-button>
      </template>

      <div class="tip">首次登录请使用管理员账号，登录后请立即修改密码</div>
    </div>

    <el-dialog v-model="ldapVisible" title="域账号登录" width="380px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="登录方式">
          <el-select v-model="ldapForm.code" style="width: 100%">
            <el-option v-for="item in ldapProviders" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="域账号">
          <el-input v-model="ldapForm.username" placeholder="如 zhangsan" autocomplete="username" />
        </el-form-item>
        <el-form-item label="域密码">
          <el-input
            v-model="ldapForm.password"
            type="password"
            show-password
            autocomplete="current-password"
            @keyup.enter="submitLdap"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ldapVisible = false">取消</el-button>
        <el-button type="primary" :loading="ssoLoading" @click="submitLdap">登录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { ssoAuthApi, type PublicSsoProvider } from '@/api/sso'

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

/* ------------------------------------------------------------ 企业登录 */

/** 回调时 IdP 只带 code/state，不带我方 provider 编码，故发起跳转前先记在 sessionStorage。 */
const SSO_PROVIDER_KEY = 'nexplm_sso_provider'

const providers = ref<PublicSsoProvider[]>([])
const oidcProviders = computed(() => providers.value.filter((item) => item.type === 'OIDC'))
const ldapProviders = computed(() => providers.value.filter((item) => item.type === 'LDAP'))
const ssoLoading = ref(false)
const ldapVisible = ref(false)
const ldapForm = reactive({ code: '', username: '', password: '' })

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

async function startOidc(provider: PublicSsoProvider): Promise<void> {
  ssoLoading.value = true
  try {
    const { url } = await ssoAuthApi.authorize(provider.code)
    sessionStorage.setItem(SSO_PROVIDER_KEY, provider.code)
    window.location.href = url
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '无法发起统一身份登录')
  } finally {
    ssoLoading.value = false
  }
}

async function completeOidcCallback(providerCode: string, authCode: string, state: string): Promise<void> {
  ssoLoading.value = true
  try {
    userStore.applyToken(await ssoAuthApi.callback(providerCode, authCode, state))
    ElMessage.success('登录成功')
    await router.replace('/')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '统一身份登录失败')
  } finally {
    ssoLoading.value = false
  }
}

function openLdap(): void {
  ldapForm.code = ldapProviders.value[0]?.code ?? ''
  ldapForm.username = ''
  ldapForm.password = ''
  ldapVisible.value = true
}

async function submitLdap(): Promise<void> {
  if (!ldapForm.code || !ldapForm.username.trim() || !ldapForm.password) {
    ElMessage.warning('请填写登录方式、域账号与域密码')
    return
  }
  ssoLoading.value = true
  try {
    userStore.applyToken(await ssoAuthApi.ldapLogin(ldapForm.code, ldapForm.username.trim(), ldapForm.password))
    ldapVisible.value = false
    ElMessage.success('登录成功')
    await router.replace('/')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '域账号登录失败')
  } finally {
    ssoLoading.value = false
  }
}

onMounted(async () => {
  try {
    providers.value = await ssoAuthApi.providers()
  } catch {
    // 未配置 SSO 或接口不可用时不影响本地登录
  }
  const authCode = route.query.code
  const state = route.query.state
  const providerCode = sessionStorage.getItem(SSO_PROVIDER_KEY)
  if (typeof authCode === 'string' && typeof state === 'string' && providerCode) {
    sessionStorage.removeItem(SSO_PROVIDER_KEY)
    await completeOidcCallback(providerCode, authCode, state)
  }
})
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

.divider {
  margin: 18px 0 12px;
  font-size: 12px;
  color: #9aa4b2;
}

.sso-button {
  width: 100%;
  margin: 0 0 8px;
}

.tip {
  margin-top: 16px;
  font-size: 12px;
  color: #a0a8b5;
  line-height: 1.6;
}
</style>
