<template>
  <div class="main-layout">
    <div class="topbar">

      <!-- Mobile hamburger -->
      <button class="hamburger-btn" @click="drawerVisible = true">
        <el-icon :size="22"><Menu /></el-icon>
      </button>      <div class="topbar-logo">
        <div class="logo-icon">
          <svg viewBox="0 0 40 40" width="22" height="22" fill="none">
            <rect width="40" height="40" rx="10" fill="url(#logo-grad)"/>
            <path d="M12 28V16l8-6 8 6v12H12z" stroke="#0a0a0f" stroke-width="2" fill="none"/>
            <path d="M16 22h8v6h-8z" fill="#0a0a0f" opacity="0.8"/>
            <defs>
              <linearGradient id="logo-grad" x1="0" y1="0" x2="40" y2="40">
                <stop offset="0%" stop-color="#4a6fa5"/>
                <stop offset="100%" stop-color="#6b8fc9"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <span class="logo-text">AI Manager</span>
      </div>
      <div class="topbar-menu-wrap">
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          router
          class="topbar-menu"
        >
          <el-menu-item index="/front/models">
            <el-icon><Monitor /></el-icon>
            <span>AI模型</span>
          </el-menu-item>
          <el-menu-item index="/front/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI对话</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin">
            <el-icon><DataAnalysis /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-sub-menu v-if="userStore.hasCloudAccess" index="cloud">
            <template #title>
              <el-icon><Cloudy /></el-icon>
              <span>多云管理</span>
            </template>
            <el-menu-item index="/admin/cloud/resources">云资源总览</el-menu-item>
            <el-menu-item v-if="userStore.isAdmin" index="/admin/cloud/topology">资源拓扑</el-menu-item>
            <el-menu-item v-if="userStore.isAdmin" index="/admin/cloud/accounts">云账号管理</el-menu-item>
            <el-sub-menu index="cloud-compute">
              <template #title>计算资源</template>
              <el-menu-item index="/admin/cloud/resources/cvm">CVM 云服务器</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/bms">BMS 裸金属</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="cloud-storage">
              <template #title>存储资源</template>
              <el-menu-item index="/admin/cloud/resources/cbs">CBS 云硬盘</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/cfs">CFS 文件存储</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/cos">COS 对象存储</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/snapshot">快照</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="cloud-database">
              <template #title>数据库</template>
              <el-menu-item index="/admin/cloud/resources/mysql">MySQL</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/oracle">Oracle</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/redis">Redis</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="cloud-network">
              <template #title>网络资源</template>
              <el-menu-item index="/admin/cloud/resources/vpc">VPC</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/vpn">VPN</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/nat">NAT 网关</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/eip">EIP</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/clb">CLB</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/cdn">CDN</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/subnet">子网</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/sg">安全组</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/route_table">路由表</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/dc">物理专线</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/ldc">逻辑专线</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/dc_tunnel">专线通道</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/peering">对等连接</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/eni">弹性网卡</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/dc_gateway">专线网关</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/admin/cloud/resources/bastion">堡垒机</el-menu-item>
            <el-sub-menu index="cloud-security">
              <template #title>安全资源</template>
              <el-menu-item index="/admin/cloud/resources/waf">WAF 防火墙</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/ddos">DDoS 防护</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/ssl">SSL 证书</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/kms">KMS 密钥管理</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="cloud-container">
              <template #title>容器与中间件</template>
              <el-menu-item index="/admin/cloud/resources/tke">容器服务 TKE</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/image_registry">镜像仓库</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/function">函数计算</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/mq">消息队列</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="cloud-others">
              <template #title>其他服务</template>
              <el-menu-item index="/admin/cloud/resources/dns">DNS 解析</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/log_service">日志服务</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/cloud_monitor">云监控</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/as">弹性伸缩 AS</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/global_acceleration">全球加速</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/dts">DTS 数据传输</el-menu-item>
            </el-sub-menu>
          </el-sub-menu>
          <el-sub-menu v-if="userStore.isAdmin" index="admin">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/admin/users">用户管理</el-menu-item>
            <el-menu-item index="/admin/roles">角色管理</el-menu-item>
            <el-menu-item index="/admin/models">模型管理</el-menu-item>
            <el-menu-item index="/admin/models/usage">模型用量</el-menu-item>
            <el-menu-item index="/admin/audit-logs">审计日志</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>
      <button class="theme-toggle-btn" @click="toggleTheme"><el-icon :size="18"><Sunny v-if="isDark" /><Moon v-else /></el-icon></button>
      <div class="topbar-user">
        <div class="user-info" @click="router.push('/user/profile')">
          <el-avatar :size="32" class="user-avatar">
            {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() }}
          </el-avatar>
          <div class="user-detail">
            <span class="username">{{ userStore.userInfo?.username }}</span>
            <span class="user-role">{{ roleText }}</span>
          </div>
        </div>
        <el-button text size="default" class="logout-btn" @click="handleLogout" title="退出登录">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </div>
    <!-- Mobile navigation drawer -->
    <el-drawer v-model="drawerVisible" direction="ltr" size="260px" :with-header="false" class="mobile-drawer">
      <div class="drawer-header">
        <span class="drawer-title">AI Manager</span>
        <button class="drawer-close" @click="drawerVisible = false"><el-icon :size="18"><Close /></el-icon></button>
      </div>
      <el-menu :default-active="activeMenu" router @select="onMenuSelect" class="drawer-menu">
        <el-menu-item index="/front/models"><el-icon><Monitor /></el-icon><span>AI模型</span></el-menu-item>
        <el-menu-item index="/front/chat"><el-icon><ChatDotRound /></el-icon><span>AI对话</span></el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin"><el-icon><DataAnalysis /></el-icon><span>仪表盘</span></el-menu-item>
        <el-menu-item v-if="userStore.hasCloudAccess" index="/admin/cloud/resources"><el-icon><Cloudy /></el-icon><span>多云管理</span></el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin/users"><el-icon><Setting /></el-icon><span>系统管理</span></el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin/audit-logs"><el-icon><Document /></el-icon><span>审计日志</span></el-menu-item>
      </el-menu>
      <div class="drawer-footer" @click="handleLogoutFromDrawer">
        <el-button text style="width:100%;color:var(--app-text-tertiary)">退出登录</el-button>
      </div>
    </el-drawer>

    <div class="main-area">
      <div class="page-header" v-if="pageTitle">
        <h2 class="page-title">{{ pageTitle }}</h2>
      </div>
      <div class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Sunny, Moon, Menu, Close } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { useUserStore } from '../../stores/user'
import { useTheme } from '../../composables/useTheme'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const drawerVisible = ref(false)
const { isDark, toggle: toggleTheme } = useTheme()

const activeMenu = computed(() => {
  return route.path
})

const pageTitle = computed(() => {
  return route.meta?.title || ''
})

const roleText = computed(() => {
  const roleMap = {
    'SUPER_ADMIN': '超级管理员',
    'ADMIN': '管理员',
    'USER': '用户'
  }
  return roleMap[userStore.userRole] || '用户'
})

onMounted(() => {
  userStore.fetchUserInfo()
})

const onMenuSelect = () => { drawerVisible.value = false }
const handleLogoutFromDrawer = () => {
  drawerVisible.value = false
  const wasAdmin = userStore.isAdmin
  userStore.logout()
  if (wasAdmin) { router.push('/admin/login') } else { router.push('/login') }
}
const handleLogout = () => {
  const wasAdmin = userStore.isAdmin
  userStore.logout()
  if (wasAdmin) {
    router.push('/admin/login')
  } else {
    router.push('/login')
  }
}
</script>


<style scoped>
.main-layout { display: flex; flex-direction: column; min-height: 100vh; background: var(--app-bg-primary); }

/* ===== Topbar ===== */
.topbar { display: flex; align-items: center; padding: 0 var(--app-spacing-lg); height: 56px; border-bottom: 1px solid var(--app-border-color); background: var(--app-bg-secondary); position: sticky; top: 0; z-index: 100; }
.topbar-logo { display: flex; align-items: center; gap: var(--app-spacing-sm); margin-right: var(--app-spacing-xl); }
.logo-icon { display: flex; align-items: center; }
.logo-text { font-size: var(--app-font-size-lg); font-weight: var(--app-font-weight-semibold); color: var(--app-accent-light); }

/* ===== Desktop navigation ===== */
.topbar-menu-wrap { flex: 1; display: flex; }
.topbar-menu { background: transparent !important; border-bottom: none !important; }
.topbar-menu .el-menu-item, .topbar-menu .el-sub-menu__title { color: var(--app-text-secondary) !important; font-size: var(--app-font-size-sm); }
.topbar-menu .el-menu-item:hover, .topbar-menu .el-sub-menu__title:hover { color: var(--app-accent-light) !important; background: rgba(74,111,165,0.08) !important; }
.topbar-menu .el-menu-item.is-active { color: var(--app-accent-light) !important; font-weight: var(--app-font-weight-semibold) !important; }

.topbar-user { display: flex; align-items: center; gap: var(--app-spacing-md); margin-left: auto; }
.user-info { display: flex; align-items: center; gap: var(--app-spacing-sm); cursor: pointer; }
.user-detail { display: flex; flex-direction: column; line-height: 1.3; }
.username { font-size: var(--app-font-size-sm); color: var(--app-text-primary); font-weight: var(--app-font-weight-medium); }
.user-role { font-size: var(--app-font-size-xs); color: var(--app-text-tertiary); }
.logout-btn { color: var(--app-text-tertiary) !important; }

/* ===== Theme toggle ===== */
.theme-toggle-btn { display: flex; align-items: center; justify-content: center; width: 36px; height: 36px; border: 1px solid var(--app-border-color); border-radius: var(--app-radius-sm); background: transparent; color: var(--app-text-tertiary); cursor: pointer; transition: all var(--app-transition-fast); margin-right: 12px; }
.theme-toggle-btn:hover { color: var(--app-accent-light); border-color: var(--app-accent); background: rgba(74,111,165,0.06); }

/* ===== Hamburger (mobile) ===== */
.hamburger-btn { display: none; align-items: center; justify-content: center; width: 36px; height: 36px; border: none; background: transparent; color: var(--app-text-secondary); cursor: pointer; margin-right: var(--app-spacing-sm); }
.hamburger-btn:hover { color: var(--app-accent-light); }

/* ===== Mobile drawer ===== */
.mobile-drawer :deep(.el-drawer__body) { padding: 0; display: flex; flex-direction: column; height: 100%; }
.drawer-header { display: flex; align-items: center; justify-content: space-between; padding: var(--app-spacing-md) var(--app-spacing-lg); border-bottom: 1px solid var(--app-border-color); }
.drawer-title { font-size: var(--app-font-size-lg); font-weight: var(--app-font-weight-semibold); color: var(--app-accent-light); }
.drawer-close { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border: none; background: transparent; color: var(--app-text-tertiary); cursor: pointer; border-radius: var(--app-radius-sm); }
.drawer-close:hover { background: var(--app-bg-card); color: var(--app-accent-light); }
.drawer-menu { flex: 1; border-right: none !important; padding: var(--app-spacing-sm) 0; }
.drawer-menu .el-menu-item { color: var(--app-text-secondary) !important; font-size: var(--app-font-size-base); height: 44px; line-height: 44px; }
.drawer-menu .el-menu-item:hover { color: var(--app-accent-light) !important; background: rgba(74,111,165,0.06) !important; }
.drawer-menu .el-menu-item.is-active { color: var(--app-accent-light) !important; font-weight: var(--app-font-weight-semibold) !important; background: rgba(74,111,165,0.08) !important; }
.drawer-footer { padding: var(--app-spacing-md) var(--app-spacing-lg); border-top: 1px solid var(--app-border-color); cursor: pointer; }

/* ===== Main content ===== */
.main-area { flex: 1; display: flex; flex-direction: column; padding: var(--app-spacing-lg); max-width: 1400px; width: 100%; margin: 0 auto; }
.page-header { margin-bottom: var(--app-spacing-lg); }
.page-title { font-size: var(--app-font-size-xl); font-weight: var(--app-font-weight-semibold); color: var(--app-text-primary); margin: 0; }
.main-content { flex: 1; }

/* ===== Responsive: hide/show ===== */
.mobile-only { display: none !important; }
.desktop-only { display: flex; }

/* ===== Responsive Breakpoints ===== */
@media (max-width: 1023px) {
  .topbar { padding: 0 var(--app-spacing-md); }
  .topbar-menu-wrap { display: none !important; }
  .topbar-user { display: none !important; }
  .hamburger-btn { display: flex; }
  .mobile-only { display: flex !important; }
  .desktop-only { display: none !important; }
  .topbar-logo { margin-right: 0; }
  .main-area { padding: var(--app-spacing-md); }
  .page-title { font-size: var(--app-font-size-lg); }
  .theme-toggle-btn { margin-right: 0; margin-left: auto; }
}

@media (max-width: 767px) {
  .topbar { height: 48px; padding: 0 var(--app-spacing-sm); }
  .logo-text { font-size: var(--app-font-size-base); }
  .main-area { padding: var(--app-spacing-sm); }
  .page-header { margin-bottom: var(--app-spacing-sm); }
}
</style>
