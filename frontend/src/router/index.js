import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import { buildLoginRedirect, defaultTargetForRole, normalizeLoginTarget } from '../utils/authSession'

const routes = [
  { path: '/', component: () => import('../views/HomeView.vue'), meta: { requiresAuth: true } },
  { path: '/login', component: () => import('../views/auth/LoginView.vue') },
  { path: '/register', component: () => import('../views/auth/RegisterUserView.vue') },
  { path: '/scan', component: () => import('../views/scan/ScanView.vue'), meta: { requiresAuth: true } },
  { path: '/scan/resolve', component: () => import('../views/scan/ScanResolveView.vue') },
  { path: '/activities', component: () => import('../views/ActivityHallView.vue'), meta: { requiresAuth: true } },
  { path: '/activities/ended', component: () => import('../views/student/EndedActivitiesView.vue'), meta: { requiresAuth: true } },
  { path: '/activities/:id', component: () => import('../views/ActivityDetailView.vue'), meta: { requiresAuth: true } },
  { path: '/activities/:id/register', component: () => import('../views/student/RegisterView.vue'), meta: { requiresAuth: true } },
  { path: '/activities/:id/checkin', component: () => import('../views/student/CheckinView.vue'), meta: { requiresAuth: true } },
  { path: '/activities/:id/feedback', component: () => import('../views/student/FeedbackView.vue'), meta: { requiresAuth: true } },
  { path: '/messages', component: () => import('../views/student/MessagesView.vue'), meta: { requiresAuth: true } },
  { path: '/my/registrations', component: () => import('../views/student/MyRegistrationsView.vue'), meta: { requiresAuth: true } },
  { path: '/my/checkins', component: () => import('../views/student/MyCheckinsView.vue'), meta: { requiresAuth: true } },
  { path: '/profile', component: () => import('../views/student/ProfileView.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/student/profile', component: () => import('../views/student/ProfileView.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/profile/security', component: () => import('../views/student/ProfileSecurityView.vue'), meta: { requiresAuth: true, requiresStudent: true } },
  { path: '/admin/dashboard', component: () => import('../views/admin/DashboardView.vue'), meta: { requiresAuth: true, requiresManager: true } },
  { path: '/admin/activity-reviews', component: () => import('../views/admin/ActivityReviewView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/operation-logs', component: () => import('../views/admin/OperationLogView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/notices/system', component: () => import('../views/admin/SystemNoticeView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/users', component: () => import('../views/admin/AdminUsersView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/activities', component: () => import('../views/admin/AdminActivitiesView.vue'), meta: { requiresAuth: true, requiresManager: true } },
  { path: '/admin/activities/create', component: () => import('../views/admin/ActivityFormView.vue'), meta: { requiresAuth: true, requiresManager: true } },
  { path: '/admin/activities/:id/edit', component: () => import('../views/admin/ActivityFormView.vue'), meta: { requiresAuth: true, requiresManager: true } },
  { path: '/admin/activities/:id/qrcodes', component: () => import('../views/admin/QrCodeManageView.vue'), meta: { requiresAuth: true } },
  { path: '/admin/activities/:id/notices', component: () => import('../views/admin/ActivityNoticeView.vue'), meta: { requiresAuth: true, requiresManager: true } },
  { path: '/admin/activities/:id/registrations', component: () => import('../views/admin/RegistrationListView.vue'), meta: { requiresAuth: true, requiresManager: true } },
  { path: '/admin/activities/:id/checkins', component: () => import('../views/admin/CheckinManageView.vue'), meta: { requiresAuth: true, requiresManager: true } },
  { path: '/admin/activities/:id/lottery', component: () => import('../views/admin/LotteryView.vue'), meta: { requiresAuth: true, requiresManager: true } },
  { path: '/admin/activities/:id/tools', component: () => import('../views/admin/ToolsView.vue'), meta: { requiresAuth: true, requiresManager: true } },
  { path: '/admin/activities/:id/feedbacks', component: () => import('../views/admin/FeedbackStatsView.vue'), meta: { requiresAuth: true, requiresManager: true } },
  { path: '/admin/activities/:id/stats', component: () => import('../views/admin/ActivityStatsView.vue'), meta: { requiresAuth: true, requiresManager: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if ((to.path === '/login' || to.path === '/register') && userStore.isLogin) {
    return normalizeLoginTarget(to.query.redirect, defaultTargetForRole(userStore.role))
  }
  if (to.meta.requiresAuth && !userStore.isLogin) {
    return buildLoginRedirect(to.path, to.fullPath.slice(to.path.length))
  }
  if (to.meta.requiresStudent && !['student', 'user'].includes(userStore.role)) {
    return defaultTargetForRole(userStore.role)
  }
  if (to.meta.requiresManager && !userStore.canManage) {
    return '/activities'
  }
  if (to.meta.requiresAdmin && !userStore.canAdmin) {
    return '/activities'
  }
  return true
})

export default router
