<template>
  <div class="main-layout">
    <div class="topbar">
      <div class="topbar-logo">
        <div class="logo-icon">
          <el-icon :size="22"><Monitor /></el-icon>
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
        <el-button text size="default" class="logout-btn" @click="handleLogout" title="退出登录">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </div>
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
import { useUserStore } from '../../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

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
  background: #f0f5ff;
}

/* ===== Top Bar ===== */
.topbar {
  height: 60px;
  background: linear-gradient(90deg, #1a1a2e 0%, #0d1b2a 100%);
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 8px;
  flex-shrink: 0;
  position: relative;
  z-index: 100;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
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
  background: linear-gradient(135deg, #409eff 0%, #7c3aed 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.35);
}

.topbar-logo .logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.topbar-menu-wrap {
  flex: 1;
  overflow: hidden;
  display: flex;
}

/* ===== Horizontal Menu Overrides ===== */
.topbar-menu {
  flex: 1;
  border: none !important;
  background: transparent !important;
}

/* Force dark theme on horizontal menu */
.topbar-menu :deep(.el-menu-item),
.topbar-menu :deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.75) !important;
  background: transparent !important;
  border-bottom: none !important;
  height: 60px;
  line-height: 60px;
  transition: all 0.25s ease;
}

.topbar-menu :deep(.el-menu-item:hover),
.topbar-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #fff !important;
}

.topbar-menu :deep(.el-menu-item.is-active) {
  color: #fff !important;
  background: rgba(64, 158, 255, 0.2) !important;
}

.topbar-menu :deep(.el-menu-item.is-active::after) {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 3px;
  background: linear-gradient(90deg, #409eff, #7c3aed);
  border-radius: 3px 3px 0 0;
}

.topbar-menu :deep(.el-sub-menu.is-active .el-sub-menu__title) {
  color: #fff !important;
}

/* Sub menu / popup menu dark theme */
.topbar-menu :deep(.el-menu--popup) {
  background: #1a1a2e !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  border-radius: 8px !important;
  padding: 6px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4) !important;
}

.topbar-menu :deep(.el-menu--popup .el-menu-item) {
  color: rgba(255, 255, 255, 0.75) !important;
  background: transparent !important;
  height: 40px !important;
  line-height: 40px !important;
  border-radius: 6px !important;
  margin: 2px 0 !important;
}

.topbar-menu :deep(.el-menu--popup .el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1) !important;
  color: #fff !important;
}

.topbar-menu :deep(.el-menu--popup .el-menu-item.is-active) {
  background: rgba(64, 158, 255, 0.25) !important;
  color: #fff !important;
}

/* Nested sub-menu in popup */
.topbar-menu :deep(.el-menu--popup .el-sub-menu .el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.75) !important;
  background: transparent !important;
  height: 40px !important;
  line-height: 40px !important;
  border-radius: 6px !important;
}

.topbar-menu :deep(.el-menu--popup .el-sub-menu .el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.1) !important;
  color: #fff !important;
}

/* ===== Top Bar User ===== */
.topbar-user {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 8px;
  padding-left: 12px;
  border-left: 1px solid rgba(255, 255, 255, 0.12);
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
  background: rgba(255, 255, 255, 0.08);
}

.user-avatar {
  background: linear-gradient(135deg, #409eff 0%, #7c3aed 100%);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.user-detail {
  display: flex;
  flex-direction: column;
  gap: 1px;
  line-height: 1.2;
}

.user-detail .username {
  color: #fff;
  font-weight: 600;
  font-size: 13px;
}

.user-detail .user-role {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
}

.logout-btn {
  color: rgba(255, 255, 255, 0.5) !important;
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
  padding: 20px 32px 0;
  flex-shrink: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
  padding-left: 14px;
  position: relative;
}

.page-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 22px;
  background: linear-gradient(180deg, #409eff 0%, #7c3aed 100%);
  border-radius: 2px;
}

.main-content {
  flex: 1;
  padding: 20px 32px 28px;
  overflow-y: auto;
  overflow-x: hidden;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
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
  .main-content {
    padding: 16px;
  }
}
</style>
