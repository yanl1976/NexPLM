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
    path: '/m',
    component: () => import('@/layouts/MobileLayout.vue'),
    children: [
      { path: '', redirect: '/m/todo' },
      {
        path: 'todo',
        name: 'm-todo',
        component: () => import('@/views/mobile/MobileTodoView.vue'),
        meta: { title: '待办审批' }
      },
      {
        path: 'doc',
        name: 'm-doc',
        component: () => import('@/views/mobile/MobileDocView.vue'),
        meta: { title: '文档查看' }
      },
      {
        path: 'scan',
        name: 'm-scan',
        component: () => import('@/views/mobile/MobileScanView.vue'),
        meta: { title: '扫码查对象' }
      }
    ]
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
        // 个人工作区：五个入口共用一个视图，靠 tab 属性区分（对齐旧系统 000/001 的五个菜单项）
        path: 'my/checkouts',
        name: 'my-checkouts',
        component: () => import('@/views/my/MyWorkView.vue'),
        props: { tab: 'checkouts' },
        meta: { title: '我的检出' }
      },
      {
        path: 'my/favorites',
        name: 'my-favorites',
        component: () => import('@/views/my/MyWorkView.vue'),
        props: { tab: 'favorites' },
        meta: { title: '收藏夹' }
      },
      {
        path: 'my/recent',
        name: 'my-recent',
        component: () => import('@/views/my/MyWorkView.vue'),
        props: { tab: 'recent' },
        meta: { title: '最近访问' }
      },
      {
        path: 'my/search',
        name: 'my-search',
        component: () => import('@/views/my/MyWorkView.vue'),
        props: { tab: 'search' },
        meta: { title: '高级查询' }
      },
      {
        path: 'my/messages',
        name: 'my-messages',
        component: () => import('@/views/my/MyWorkView.vue'),
        props: { tab: 'messages' },
        meta: { title: '站内消息' }
      },
      {
        path: 'report/dashboard',
        name: 'report-dashboard',
        component: () => import('@/views/report/DashboardView.vue'),
        meta: { title: '统计看板' }
      },
      {
        path: 'report/boards',
        name: 'report-boards',
        component: () => import('@/views/report/BoardView.vue'),
        meta: { title: '自定义看板' }
      },
      {
        path: 'item',
        name: 'item-list',
        component: () => import('@/views/item/ItemListView.vue'),
        meta: { title: '零部件' }
      },
      {
        path: 'item/:id',
        name: 'item-detail',
        component: () => import('@/views/item/ItemDetailView.vue'),
        meta: { title: '零部件详情' }
      },
      {
        path: 'doc',
        name: 'doc-list',
        component: () => import('@/views/doc/DocListView.vue'),
        meta: { title: '图文档' }
      },
      {
        path: 'doc/class',
        name: 'doc-class',
        component: () => import('@/views/doc/DocClassView.vue'),
        meta: { title: '文档分类' }
      },
      {
        path: 'doc/:id',
        name: 'doc-detail',
        component: () => import('@/views/doc/DocDetailView.vue'),
        meta: { title: '文档详情' }
      },
      {
        path: 'workspace',
        name: 'workspace',
        component: () => import('@/views/workspace/WorkspaceView.vue'),
        meta: { title: '工作空间' }
      },
      {
        path: 'workflow',
        name: 'workflow-center',
        component: () => import('@/views/workflow/WorkflowCenterView.vue'),
        meta: { title: '流程中心' }
      },
      {
        path: 'workflow/instance/:id',
        name: 'workflow-instance',
        component: () => import('@/views/workflow/WorkflowInstanceView.vue'),
        meta: { title: '流程详情' }
      },
      {
        path: 'workflow/delegate',
        name: 'workflow-delegate',
        component: () => import('@/views/workflow/DelegateView.vue'),
        meta: { title: '委托设置' }
      },
      {
        path: 'change',
        name: 'change-list',
        component: () => import('@/views/change/ChangeListView.vue'),
        meta: { title: '变更管理' }
      },
      {
        path: 'capp/routing',
        name: 'capp-routing',
        component: () => import('@/views/capp/RoutingListView.vue'),
        meta: { title: '工艺路线' }
      },
      {
        path: 'capp/resource',
        name: 'capp-resource',
        component: () => import('@/views/capp/ResourceView.vue'),
        meta: { title: '工艺资源' }
      },
      {
        path: 'capp/card',
        name: 'capp-card',
        component: () => import('@/views/capp/CardView.vue'),
        meta: { title: '工艺卡片' }
      },
      {
        path: 'capp/time',
        name: 'capp-time',
        component: () => import('@/views/capp/TimeView.vue'),
        meta: { title: '工时定额' }
      },
      {
        path: 'capp/volume',
        name: 'capp-volume',
        component: () => import('@/views/capp/VolumeView.vue'),
        meta: { title: '工艺成册' }
      },
      {
        path: 'capp/quality',
        name: 'capp-quality',
        component: () => import('@/views/capp/QualityView.vue'),
        meta: { title: '质量规划' }
      },
      {
        path: 'capp/dispatch',
        name: 'capp-dispatch',
        component: () => import('@/views/capp/DispatchView.vue'),
        meta: { title: 'ERP下发' }
      },
      {
        path: 'workflow/definitions',
        name: 'workflow-definitions',
        component: () => import('@/views/workflow/DefinitionView.vue'),
        meta: { title: '流程定义' }
      },
      {
        path: 'project',
        name: 'project-list',
        component: () => import('@/views/project/ProjectListView.vue'),
        meta: { title: '项目管理' }
      },
      {
        path: 'project/my-tasks',
        name: 'project-my-tasks',
        component: () => import('@/views/project/MyTasksView.vue'),
        meta: { title: '我的任务' }
      },
      {
        path: 'project/my-timesheets',
        name: 'project-my-timesheets',
        component: () => import('@/views/project/MyTimesheetsView.vue'),
        meta: { title: '我的工时' }
      },
      {
        path: 'project/templates',
        name: 'project-templates',
        component: () => import('@/views/project/ProjectTemplatesView.vue'),
        meta: { title: '项目模板' }
      },
      {
        path: 'project/portal',
        name: 'project-portal',
        component: () => import('@/views/project/ProjectPortalView.vue'),
        meta: { title: '项目门户' }
      },
      {
        path: 'project/:id/tasks',
        name: 'project-tasks',
        component: () => import('@/views/project/ProjectTasksView.vue'),
        meta: { title: 'WBS 任务' }
      },
      {
        path: 'project/:id/deliverables',
        name: 'project-deliverables',
        component: () => import('@/views/project/ProjectDeliverableView.vue'),
        meta: { title: '交付物与齐套' }
      },
      {
        path: 'project/:id/timesheets',
        name: 'project-timesheets',
        component: () => import('@/views/project/ProjectTimesheetsView.vue'),
        meta: { title: '项目工时' }
      },
      {
        path: 'project/:id/calendar',
        name: 'project-calendar',
        component: () => import('@/views/project/ProjectCalendarView.vue'),
        meta: { title: '项目日历' }
      },
      {
        path: 'quality/issues',
        name: 'quality-issues',
        component: () => import('@/views/quality/QualityIssueView.vue'),
        meta: { title: '质量问题' }
      },
      {
        path: 'quality/templates',
        name: 'quality-templates',
        component: () => import('@/views/quality/QualityTemplateView.vue'),
        meta: { title: '8D 模板' }
      },
      {
        path: 'quality/dashboard',
        name: 'quality-dashboard',
        component: () => import('@/views/quality/QualityDashboardView.vue'),
        meta: { title: '质量看板' }
      },
      {
        path: 'quality/sys-docs',
        name: 'quality-sys-docs',
        component: () => import('@/views/quality/SysDocView.vue'),
        meta: { title: '体系文件' }
      },
      {
        path: 'quality/sys-doc-templates',
        name: 'quality-sys-doc-templates',
        component: () => import('@/views/quality/SysDocTemplateView.vue'),
        meta: { title: '体系文件模板' }
      },
      {
        path: 'quality/audits',
        name: 'quality-audits',
        component: () => import('@/views/quality/AuditPlanView.vue'),
        meta: { title: '体系审核' }
      },
      {
        path: 'sup/suppliers',
        name: 'sup-suppliers',
        component: () => import('@/views/supplier/SupplierView.vue'),
        meta: { title: '供应商主数据' }
      },
      {
        path: 'itg',
        name: 'itg-center',
        component: () => import('@/views/integration/IntegrationCenterView.vue'),
        meta: { title: '集成中心' }
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
      },
      {
        path: 'system/security',
        name: 'system-security',
        component: () => import('@/views/system/SecurityView.vue'),
        meta: { title: '安全策略' }
      },
      {
        path: 'system/coding',
        name: 'system-coding',
        component: () => import('@/views/system/CodingView.vue'),
        meta: { title: '编码管理' }
      },
      {
        path: 'system/sso',
        name: 'system-sso',
        component: () => import('@/views/system/SsoView.vue'),
        meta: { title: '单点登录' }
      },
      {
        path: 'system/lifecycle',
        name: 'system-lifecycle',
        component: () => import('@/views/system/LifecycleView.vue'),
        meta: { title: '生命周期设计器' }
      },
      {
        path: 'system/notify',
        name: 'system-notify',
        component: () => import('@/views/system/NotifyView.vue'),
        meta: { title: '通知通道' }
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
