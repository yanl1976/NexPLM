import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi, type UserBrief } from '@/api/auth'
import { clearTokens, getAccessToken, setTokens } from '@/api/http'

export const useUserStore = defineStore('user', () => {
  const user = ref<UserBrief | null>(null)
  const permissions = ref<Set<string>>(new Set())

  const isLoggedIn = computed(() => !!getAccessToken())
  const displayName = computed(() => user.value?.displayName ?? '未登录')

  async function login(username: string, password: string): Promise<void> {
    const res = await authApi.login(username, password)
    setTokens(res.accessToken, res.refreshToken)
    user.value = res.user
    permissions.value = new Set(res.user.permissions)
  }

  async function loadMe(): Promise<void> {
    const me = await authApi.me()
    user.value = me
    permissions.value = new Set(me.permissions)
  }

  function logout(): void {
    clearTokens()
    user.value = null
    permissions.value = new Set()
  }

  function can(permission: string): boolean {
    return permissions.value.has(permission)
  }

  return { user, permissions, isLoggedIn, displayName, login, loadMe, logout, can }
})
