import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue')
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/auth/AdminLogin.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/auth/Register.vue')
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
        meta: { title: '仪表盘', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'front/models',
        name: 'ModelList',
        component: () => import('../views/front/ModelList.vue'),
        meta: { title: 'AI模型', requiresAuth: true }
      },
      {
        path: 'front/chat',
        name: 'ChatView',
        component: () => import('../views/front/ChatView.vue'),
        meta: { title: 'AI对话', requiresAuth: true }
      },
      {
        path: 'user/profile',
        name: 'UserProfile',
        component: () => import('../views/user/UserProfile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'admin/users',
        name: 'UserManage',
        component: () => import('../views/admin/UserManage.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/roles',
        name: 'RoleManage',
        component: () => import('../views/admin/RoleManage.vue'),
        meta: { title: '角色管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/models',
        name: 'ModelManage',
        component: () => import('../views/admin/ModelManage.vue'),
        meta: { title: '模型管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/audit-logs',
        name: 'AuditLogManage',
        component: () => import('../views/admin/AuditLogManage.vue'),
        meta: { title: '审计日志', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/models/usage',
        name: 'ModelUsage',
        component: () => import('../views/admin/ModelUsage.vue'),
        meta: { title: '模型用量', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/cloud',
        redirect: '/admin/cloud/resources'
      },
      {
        path: 'admin/cloud/resources',
        name: 'CloudResources',
        component: () => import('../views/admin/CloudResources.vue'),
        meta: { title: '云资源总览', requiresAuth: true }
      },
      {
        path: 'admin/cloud/resources/:resourceType',
        name: 'CloudResourceType',
        component: () => import('../views/admin/CloudResourceTypePage.vue'),
        props: true,
        meta: { title: '云资源', requiresAuth: true }
      },
      {
        path: 'admin/cloud/accounts',
        name: 'CloudAccounts',
        component: () => import('../views/admin/CloudAccounts.vue'),
        meta: { title: '云账号管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/cloud/topology',
        name: 'CloudTopology',
        component: () => import('../views/admin/CloudTopology.vue'),
        meta: { title: '资源拓扑', requiresAuth: true, requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.token || localStorage.getItem('token')

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
