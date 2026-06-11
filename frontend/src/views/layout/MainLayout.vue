<template>
  <div class="main-layout">
    <div class="topbar">
      <el-button v-if="isMobile" text size="default" class="hamburger-btn" @click="drawerOpen = true">
        <el-icon><Expand /></el-icon>
      </el-button>
      <div class="topbar-logo">
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
      <div v-if="!isMobile" class="topbar-menu-wrap">
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
        <el-button text size="default" class="theme-btn" @click="toggleTheme" :title="isDark ? '切换到亮色模式' : '切换到深色模式'">
          <el-icon><Sunny v-if="isDark" /><Moon v-else /></el-icon>
        </el-button>
        <el-button text size="default" class="logout-btn" @click="handleLogout" title="退出登录">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 移动端抽屉导航 -->
    <el-drawer
      v-model="drawerOpen"
      direction="ltr"
      :size="isSmallMobile ? '100%' : '280px'"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer-header">
        <div class="drawer-logo">
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
          <span>AI Manager</span>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        mode="vertical"
        router
        @select="drawerOpen = false"
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
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useTheme } from '../../composables/useTheme'
import { useResponsive } from '../../composables/useResponsive'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { toggleTheme, isDark } = useTheme()
const { isMobile, isSmallMobile } = useResponsive()
const drawerOpen = ref(false)

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
.main-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}

/* ===== Top Bar ===== */
.topbar {
  height: 60px;
  background: var(--app-topbar-bg);
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 8px;
  flex-shrink: 0;
  position: relative;
  z-index: 100;
  border-bottom: 1px solid rgba(74, 111, 165, 0.08);
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.4);
}

.topbar::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 5%;
  right: 5%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(74, 111, 165, 0.15), transparent);
  pointer-events: none;
}

.topbar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-right: 16px;
  flex-shrink: 0;
}

.topbar-logo .logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #4a6fa5, #6b8fc9);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(74, 111, 165, 0.3);
}

.topbar-logo .logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--app-topbar-text);
  letter-spacing: 1px;
}

.topbar-menu-wrap {
  flex: 1;
  overflow: hidden;
  display: flex;
}

/* ===== Horizontal Menu ===== */
.topbar-menu {
  flex: 1;
  border: none !important;
  background: transparent !important;
}

.topbar-menu :deep(.el-menu-item),
.topbar-menu :deep(.el-sub-menu__title) {
  color: var(--app-topbar-text-secondary) !important;
  background: transparent !important;
  border-bottom: none !important;
  height: 60px;
  line-height: 60px;
  transition: all 0.25s ease;
  letter-spacing: 0.5px;
  font-weight: 400;
}

.topbar-menu :deep(.el-menu-item:hover),
.topbar-menu :deep(.el-sub-menu__title:hover) {
  background: var(--app-menu-hover-bg) !important;
  color: var(--app-topbar-menu-active) !important;
}

.topbar-menu :deep(.el-menu-item.is-active) {
  color: var(--app-topbar-menu-active) !important;
  background: var(--app-menu-active-bg) !important;
}

.topbar-menu :deep(.el-menu-item.is-active::after) {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 2px;
  background: var(--accent);
  border-radius: 2px 2px 0 0;
  box-shadow: 0 0 12px rgba(74, 111, 165, 0.4);
}

.topbar-menu :deep(.el-sub-menu.is-active .el-sub-menu__title) {
  color: var(--accent-light) !important;
}

/* ===== Top Bar User ===== */
.topbar-user {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 8px;
  padding-left: 12px;
  border-left: 1px solid rgba(74, 111, 165, 0.15);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 20px;
  transition: background 0.2s;
}

.user-info:hover {
  background: rgba(74, 111, 165, 0.06);
}

.user-avatar {
  background: linear-gradient(135deg, #4a6fa5, #6b8fc9) !important;
  color: #fff !important;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
  border: none;
}

.user-detail {
  display: flex;
  flex-direction: column;
  gap: 1px;
  line-height: 1.2;
}

.user-detail .username {
  color: var(--app-topbar-user-text);
  font-weight: 600;
  font-size: 13px;
}

.user-detail .user-role {
  color: var(--app-topbar-role-text);
  font-size: 11px;
}

.theme-btn {
  color: var(--app-topbar-text-secondary) !important;
  font-size: 18px;
  padding: 6px !important;
  transition: all 0.2s;
}
.theme-btn:hover {
  color: var(--app-topbar-menu-active) !important;
  background: var(--app-menu-hover-bg) !important;
}

.logout-btn {
  color: var(--app-topbar-role-text) !important;
  font-size: 18px;
  padding: 6px !important;
  transition: color 0.2s;
}

.logout-btn:hover {
  color: #f56c6c !important;
  background: rgba(245, 108, 108, 0.1) !important;
}

/* ===== Main Area ===== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-header {
  padding: 24px 32px 0;
  flex-shrink: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 400;
  color: var(--text-primary);
  margin: 0;
  padding-left: 14px;
  position: relative;
  letter-spacing: 1px;
}

.page-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: linear-gradient(180deg, var(--accent), var(--accent-dark));
  border-radius: 2px;
  box-shadow: 0 0 8px rgba(74, 111, 165, 0.3);
}

.main-content {
  flex: 1;
  padding: 20px 32px 28px;
  overflow-y: auto;
  overflow-x: hidden;
}

.main-content::-webkit-scrollbar-thumb {
  background: rgba(74, 111, 165, 0.15);
  border-radius: 3px;
}
.main-content::-webkit-scrollbar-thumb:hover {
  background: rgba(74, 111, 165, 0.3);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ===== Hamburger Button ===== */
.hamburger-btn {
  color: var(--app-topbar-text) !important;
  font-size: 20px;
  padding: 6px !important;
  margin-right: 4px;
}
.hamburger-btn:hover {
  color: var(--app-topbar-menu-active) !important;
  background: var(--app-menu-hover-bg) !important;
}

/* ===== Mobile Drawer ===== */
.mobile-drawer {
  --el-drawer-bg-color: var(--bg-primary) !important;
}
.mobile-drawer :deep(.el-drawer__body) {
  padding: 0;
  display: flex;
  flex-direction: column;
}
.drawer-header {
  padding: 20px 20px 12px;
  border-bottom: 1px solid var(--border-color-light);
  flex-shrink: 0;
}
.drawer-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: var(--app-topbar-text);
  letter-spacing: 1px;
}
.drawer-logo svg {
  flex-shrink: 0;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .topbar {
    padding: 0 12px;
    gap: 4px;
  }
  .topbar-logo .logo-text {
    display: none;
  }
  .user-detail {
    display: none;
  }
  .page-header {
    padding: 16px 16px 0;
  }
  .page-title {
    font-size: 17px;
  }
  .main-content {
    padding: 16px;
  }
}
</style>
