import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import i18n from '../i18n'

const { t } = i18n.global

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue'),
    meta: { titleKey: 'login.frontLogin' }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/auth/AdminLogin.vue'),
    meta: { titleKey: 'login.adminLogin' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/auth/Register.vue'),
    meta: { titleKey: 'register.title' }
  },
  {
    path: '/',
    component: () => import('../views/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/front/models'
      },
      {
        path: 'admin',
        name: 'Dashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { titleKey: 'dashboard.title', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'front/models',
        name: 'ModelList',
        component: () => import('../views/front/ModelList.vue'),
        meta: { titleKey: 'modelList.title', requiresAuth: true }
      },
      {
        path: 'front/chat',
        name: 'ChatView',
        component: () => import('../views/front/ChatView.vue'),
        meta: { titleKey: 'chat.title', requiresAuth: true }
      },
      {
        path: 'user/profile',
        name: 'UserProfile',
        component: () => import('../views/user/UserProfile.vue'),
        meta: { titleKey: 'user.profile', requiresAuth: true }
      },
      {
        path: 'admin/users',
        name: 'UserManage',
        component: () => import('../views/admin/UserManage.vue'),
        meta: { titleKey: 'admin.users.title', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/roles',
        name: 'RoleManage',
        component: () => import('../views/admin/RoleManage.vue'),
        meta: { titleKey: 'admin.roles.title', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/models',
        name: 'ModelManage',
        component: () => import('../views/admin/ModelManage.vue'),
        meta: { titleKey: 'admin.models.title', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/audit-logs',
        name: 'AuditLogManage',
        component: () => import('../views/admin/AuditLogManage.vue'),
        meta: { titleKey: 'admin.audit.title', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/models/usage',
        name: 'ModelUsage',
        component: () => import('../views/admin/ModelUsage.vue'),
        meta: { titleKey: 'admin.usage.title', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/settings',
        name: 'AdminSettings',
        component: () => import('../views/admin/AdminSettings.vue'),
        meta: { titleKey: 'admin.settings.title', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/cloud',
        redirect: '/admin/cloud/resources'
      },
      {
        path: 'admin/cloud/resources',
        name: 'CloudResources',
        component: () => import('../views/admin/CloudResources.vue'),
        meta: { titleKey: 'admin.cloud.resources.title', requiresAuth: true }
      },
      {
        path: 'admin/cloud/resources/:resourceType',
        name: 'CloudResourceType',
        component: () => import('../views/admin/CloudResourceTypePage.vue'),
        props: true,
        meta: { titleKey: 'admin.cloud.resources.title', requiresAuth: true }
      },
      {
        path: 'admin/cloud/accounts',
        name: 'CloudAccounts',
        component: () => import('../views/admin/CloudAccounts.vue'),
        meta: { titleKey: 'admin.cloud.accounts.title', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/cloud/topology',
        name: 'CloudTopology',
        component: () => import('../views/admin/CloudTopology.vue'),
        meta: { titleKey: 'admin.cloud.topology.title', requiresAuth: true, requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Update document title on each navigation
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.token || localStorage.getItem('token')

  // Set document title from titleKey
  if (to.meta.titleKey) {
    document.title = `${t(to.meta.titleKey)} - ${t('login.title')}`
  }

  // 已登录管理员访问后台登录 -> 跳管理后台
  if (to.path === '/admin/login' && token && userStore.isAdmin) {
    next('/admin/users')
    return
  }

  // 需要认证的路由
  if (to.meta.requiresAuth && !token) {
    if (to.path.startsWith('/admin')) {
      next('/admin/login')
    } else {
      next('/login')
    }
  } else if (to.path.startsWith('/admin') && to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/front/models')
  } else if (to.path === '/login' && token) {
    next('/front/models')
  } else if (to.path === '/admin/login' && token && !userStore.isAdmin) {
    next('/front/models')
  } else {
    next()
  }
})

export default router
