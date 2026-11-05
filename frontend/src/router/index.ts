import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getAccessToken } from '@/api/http'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '概览' }
      },
      {
        path: 'system/org',
        name: 'system-org',
        component: () => import('@/views/system/OrgView.vue'),
        meta: { title: '组织架构' }
      },
      {
        path: 'system/user',
        name: 'system-user',
        component: () => import('@/views/system/UserView.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'system-role',
        component: () => import('@/views/system/RoleView.vue'),
        meta: { title: '角色与授权' }
      },
      {
        path: 'system/meta',
        name: 'system-meta',
        component: () => import('@/views/system/MetaView.vue'),
        meta: { title: '元数据建模' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} - NexPLM` : 'NexPLM'
  if (to.meta.public) {
    return true
  }
  if (!getAccessToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  const userStore = useUserStore()
  if (!userStore.user) {
    try {
      await userStore.loadMe()
    } catch {
      userStore.logout()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
  return true
})

export default router
