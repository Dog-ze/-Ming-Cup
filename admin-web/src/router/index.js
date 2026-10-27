import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import LoginView from '@/views/login/LoginView.vue'
import PlaceholderView from '@/views/PlaceholderView.vue'
import { pinia } from '@/stores'
import { useAdminStore } from '@/stores/admin'
import { getCurrentAdmin } from '@/api/admin-auth'

const placeholder = (path, title) => ({
  path,
  component: PlaceholderView,
  meta: { title, requiresAuth: true },
})

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', component: LoginView, meta: { title: '管理员登录' } },
    {
      path: '/',
      component: AdminLayout,
      meta: { requiresAuth: true },
      children: [
        {
  path: '/dashboard',
  component: () => import('@/views/dashboard/DashboardView.vue'),
  meta: { title: '工作台', requiresAuth: true }
},
        { path: '/users', component: () => import('@/views/users/UserListView.vue'), meta: { title: '用户管理', requiresAuth: true } },
        { path: '/tea-knowledge', component: () => import('@/views/tea/TeaKnowledgeView.vue'), meta: { title: '茶叶知识库', requiresAuth: true } },
        { path: '/pairing-rules', component: () => import('@/views/pairing/PairingRuleView.vue'), meta: { title: '茶食规则', requiresAuth: true } },
        { path: '/stores', component: () => import('@/views/stores/StoreManagementView.vue'), meta: { title: '店铺与推广', requiresAuth: true } },
        { path: '/user-data', component: () => import('@/views/data/UserDataOverviewView.vue'), meta: { title: '用户数据', requiresAuth: true } },
        { path: '/tea-records', redirect: '/user-data' },
        { path: '/questionnaires', redirect: '/user-data' },
        { path: '/operation-logs', component: () => import('@/views/logs/OperationLogView.vue'), meta: { title: '操作日志', requiresAuth: true } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

let validating = null

router.beforeEach(async (to) => {
  const store = useAdminStore(pinia)
  store.restore()

  if (!to.meta.requiresAuth) {
    if (to.path === '/login' && store.hasSession && !store.isExpired) return '/dashboard'
    return true
  }

  if (!store.hasSession || store.isExpired) {
    store.clearSession()
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  try {
    validating ||= getCurrentAdmin().finally(() => { validating = null })
    const profile = await validating
    store.setSession(store.token, profile)
    return true
  } catch {
    store.clearSession()
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
