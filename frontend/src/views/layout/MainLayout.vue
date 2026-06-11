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
        <span class="logo-text">{{ brandStore.config.appName }}</span>
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
            <span>{{ $t('nav.modelList') }}</span>
          </el-menu-item>
          <el-menu-item index="/front/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ $t('nav.chat') }}</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin">
            <el-icon><DataAnalysis /></el-icon>
            <span>{{ $t('nav.dashboard') }}</span>
          </el-menu-item>
          <el-sub-menu v-if="userStore.hasCloudAccess" index="cloud">
            <template #title>
              <el-icon><Cloudy /></el-icon>
              <span>{{ $t('nav.cloudResources') }}</span>
            </template>
            <el-menu-item index="/admin/cloud/resources">{{ $t('nav.cloudResources') }}</el-menu-item>
            <el-menu-item v-if="userStore.isAdmin" index="/admin/cloud/topology">{{ $t('nav.cloudTopology') }}</el-menu-item>
            <el-menu-item v-if="userStore.isAdmin" index="/admin/cloud/accounts">{{ $t('nav.cloudAccounts') }}</el-menu-item>
            <el-sub-menu index="cloud-compute">
              <template #title>{{ $t('nav.cloudCompute') }}</template>
              <el-menu-item index="/admin/cloud/resources/cvm">{{ $t('nav.cvm') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/bms">{{ $t('nav.bms') }}</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="cloud-storage">
              <template #title>{{ $t('nav.cloudStorage') }}</template>
              <el-menu-item index="/admin/cloud/resources/cbs">{{ $t('nav.cbs') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/cfs">{{ $t('nav.cfs') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/cos">{{ $t('nav.cos') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/snapshot">{{ $t('nav.snapshot') }}</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="cloud-database">
              <template #title>{{ $t('nav.cloudDatabase') }}</template>
              <el-menu-item index="/admin/cloud/resources/mysql">{{ $t('nav.mysql') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/oracle">{{ $t('nav.oracle') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/redis">{{ $t('nav.redis') }}</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="cloud-network">
              <template #title>{{ $t('nav.cloudNetwork') }}</template>
              <el-menu-item index="/admin/cloud/resources/vpc">{{ $t('nav.vpc') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/vpn">{{ $t('nav.vpn') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/nat">{{ $t('nav.nat') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/eip">{{ $t('nav.eip') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/clb">{{ $t('nav.clb') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/cdn">{{ $t('nav.cdn') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/subnet">{{ $t('nav.subnet') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/sg">{{ $t('nav.sg') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/route_table">{{ $t('nav.routeTable') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/dc">{{ $t('nav.dc') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/ldc">{{ $t('nav.ldc') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/dc_tunnel">{{ $t('nav.dcTunnel') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/peering">{{ $t('nav.peering') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/eni">{{ $t('nav.eni') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/dc_gateway">{{ $t('nav.dcGateway') }}</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/admin/cloud/resources/bastion">{{ $t('nav.bastion') }}</el-menu-item>
            <el-sub-menu index="cloud-security">
              <template #title>{{ $t('nav.cloudSecurity') }}</template>
              <el-menu-item index="/admin/cloud/resources/waf">{{ $t('nav.waf') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/ddos">{{ $t('nav.ddos') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/ssl">{{ $t('nav.ssl') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/kms">{{ $t('nav.kms') }}</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="cloud-container">
              <template #title>{{ $t('nav.cloudContainer') }}</template>
              <el-menu-item index="/admin/cloud/resources/tke">{{ $t('nav.tke') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/image_registry">{{ $t('nav.imageRegistry') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/function">{{ $t('nav.functionCompute') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/mq">{{ $t('nav.messageQueue') }}</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="cloud-others">
              <template #title>{{ $t('nav.cloudOther') }}</template>
              <el-menu-item index="/admin/cloud/resources/dns">{{ $t('nav.dns') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/log_service">{{ $t('nav.logService') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/cloud_monitor">{{ $t('nav.cloudMonitor') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/as">{{ $t('nav.autoScaling') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/global_acceleration">{{ $t('nav.globalAcceleration') }}</el-menu-item>
              <el-menu-item index="/admin/cloud/resources/dts">{{ $t('nav.dts') }}</el-menu-item>
            </el-sub-menu>
          </el-sub-menu>
          <el-sub-menu v-if="userStore.isAdmin" index="admin">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>{{ $t('nav.systemAdmin') }}</span>
            </template>
            <el-menu-item index="/admin/users">{{ $t('nav.userManage') }}</el-menu-item>
            <el-menu-item index="/admin/roles">{{ $t('nav.roleManage') }}</el-menu-item>
            <el-menu-item index="/admin/models">{{ $t('nav.modelManage') }}</el-menu-item>
            <el-menu-item index="/admin/models/usage">{{ $t('nav.modelUsage') }}</el-menu-item>
            <el-menu-item index="/admin/audit-logs">{{ $t('nav.auditLog') }}</el-menu-item>
            <el-menu-item index="/admin/settings">{{ $t('nav.adminSettings') }}</el-menu-item>
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
        <el-button text size="default" class="lang-btn" @click="toggleLang" :title="$t('lang.switch')">
          <span class="lang-label">{{ localeStore.isZhCN ? 'EN' : '中' }}</span>
        </el-button>
        <el-button text size="default" class="theme-btn" @click="toggleTheme" :title="isDark ? $t('theme.light') : $t('theme.dark')">
          <el-icon><Sunny v-if="isDark" /><Moon v-else /></el-icon>
        </el-button>
        <el-button text size="default" class="logout-btn" @click="handleLogout" :title="$t('nav.logout')">
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
          <span>{{ brandStore.config.appName }}</span>
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
          <span>{{ $t('nav.modelList') }}</span>
        </el-menu-item>
        <el-menu-item index="/front/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>{{ $t('nav.chat') }}</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin">
          <el-icon><DataAnalysis /></el-icon>
          <span>{{ $t('nav.dashboard') }}</span>
        </el-menu-item>
        <el-sub-menu v-if="userStore.hasCloudAccess" index="cloud">
          <template #title>
            <el-icon><Cloudy /></el-icon>
            <span>{{ $t('nav.cloudResources') }}</span>
          </template>
          <el-menu-item index="/admin/cloud/resources">{{ $t('nav.cloudResources') }}</el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin/cloud/topology">{{ $t('nav.cloudTopology') }}</el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin/cloud/accounts">{{ $t('nav.cloudAccounts') }}</el-menu-item>
          <el-sub-menu index="cloud-compute">
            <template #title>{{ $t('nav.cloudCompute') }}</template>
            <el-menu-item index="/admin/cloud/resources/cvm">{{ $t('nav.cvm') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/bms">{{ $t('nav.bms') }}</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="cloud-storage">
            <template #title>{{ $t('nav.cloudStorage') }}</template>
            <el-menu-item index="/admin/cloud/resources/cbs">{{ $t('nav.cbs') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/cfs">{{ $t('nav.cfs') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/cos">{{ $t('nav.cos') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/snapshot">{{ $t('nav.snapshot') }}</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="cloud-database">
            <template #title>{{ $t('nav.cloudDatabase') }}</template>
            <el-menu-item index="/admin/cloud/resources/mysql">{{ $t('nav.mysql') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/oracle">{{ $t('nav.oracle') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/redis">{{ $t('nav.redis') }}</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="cloud-network">
            <template #title>{{ $t('nav.cloudNetwork') }}</template>
            <el-menu-item index="/admin/cloud/resources/vpc">{{ $t('nav.vpc') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/vpn">{{ $t('nav.vpn') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/nat">{{ $t('nav.nat') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/eip">{{ $t('nav.eip') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/clb">{{ $t('nav.clb') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/cdn">{{ $t('nav.cdn') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/subnet">{{ $t('nav.subnet') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/sg">{{ $t('nav.sg') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/route_table">{{ $t('nav.routeTable') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/dc">{{ $t('nav.dc') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/ldc">{{ $t('nav.ldc') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/dc_tunnel">{{ $t('nav.dcTunnel') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/peering">{{ $t('nav.peering') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/eni">{{ $t('nav.eni') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/dc_gateway">{{ $t('nav.dcGateway') }}</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/admin/cloud/resources/bastion">{{ $t('nav.bastion') }}</el-menu-item>
          <el-sub-menu index="cloud-security">
            <template #title>{{ $t('nav.cloudSecurity') }}</template>
            <el-menu-item index="/admin/cloud/resources/waf">{{ $t('nav.waf') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/ddos">{{ $t('nav.ddos') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/ssl">{{ $t('nav.ssl') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/kms">{{ $t('nav.kms') }}</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="cloud-container">
            <template #title>{{ $t('nav.cloudContainer') }}</template>
            <el-menu-item index="/admin/cloud/resources/tke">{{ $t('nav.tke') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/image_registry">{{ $t('nav.imageRegistry') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/function">{{ $t('nav.functionCompute') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/mq">{{ $t('nav.messageQueue') }}</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="cloud-others">
            <template #title>{{ $t('nav.cloudOther') }}</template>
            <el-menu-item index="/admin/cloud/resources/dns">{{ $t('nav.dns') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/log_service">{{ $t('nav.logService') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/cloud_monitor">{{ $t('nav.cloudMonitor') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/as">{{ $t('nav.autoScaling') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/global_acceleration">{{ $t('nav.globalAcceleration') }}</el-menu-item>
            <el-menu-item index="/admin/cloud/resources/dts">{{ $t('nav.dts') }}</el-menu-item>
          </el-sub-menu>
        </el-sub-menu>
        <el-sub-menu v-if="userStore.isAdmin" index="admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>{{ $t('nav.systemAdmin') }}</span>
          </template>
          <el-menu-item index="/admin/users">{{ $t('nav.userManage') }}</el-menu-item>
          <el-menu-item index="/admin/roles">{{ $t('nav.roleManage') }}</el-menu-item>
          <el-menu-item index="/admin/models">{{ $t('nav.modelManage') }}</el-menu-item>
          <el-menu-item index="/admin/models/usage">{{ $t('nav.modelUsage') }}</el-menu-item>
          <el-menu-item index="/admin/audit-logs">{{ $t('nav.auditLog') }}</el-menu-item>
          <el-menu-item index="/admin/settings">{{ $t('nav.adminSettings') }}</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-drawer>

    <div class="main-area">
      <div class="page-header" v-if="route.meta.titleKey">
        <h2 class="page-title">{{ $t(route.meta.titleKey) }}</h2>
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
import { useLocaleStore } from '../../stores/locale'
import { useBrandingStore } from '../../stores/branding'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const localeStore = useLocaleStore()
const brandStore = useBrandingStore()
const { toggleTheme, isDark } = useTheme()
const { isMobile, isSmallMobile } = useResponsive()
const { t } = useI18n()
const drawerOpen = ref(false)

const activeMenu = computed(() => {
  return route.path
})

const roleText = computed(() => {
  const roleMap = {
    'SUPER_ADMIN': t('admin.roles.name') + '(SUPER)',
    'ADMIN': t('admin.roles.name'),
    'USER': t('user.role')
  }
  return roleMap[userStore.userRole] || t('common.noData')
})

const toggleLang = () => {
  localeStore.toggleLocale()
}

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

.lang-btn {
  color: var(--app-topbar-text-secondary) !important;
  font-size: 13px;
  padding: 4px 8px !important;
  font-weight: 600;
  transition: all 0.2s;
}
.lang-btn:hover {
  color: var(--app-topbar-menu-active) !important;
  background: var(--app-menu-hover-bg) !important;
}
.lang-label {
  margin-left: 2px;
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
