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
