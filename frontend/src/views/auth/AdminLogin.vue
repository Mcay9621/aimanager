<template>
  <div class="admin-login-container">
    <div class="admin-login-bg">
      <div class="bg-pattern"></div>
    </div>
    <div class="admin-login-content">
      <div class="admin-logo-section">
        <div class="admin-logo-icon">
          <el-icon :size="40"><Setting /></el-icon>
        </div>
        <h1 class="admin-title">AI Manager</h1>
        <p class="admin-subtitle">管理系统</p>
      </div>
      <el-card class="admin-login-card">
        <h2 class="admin-card-title">管理员登录</h2>
        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="admin-login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入管理员用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="admin-login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="admin-login-options">
          <el-link type="primary" @click="$router.push('/login')">返回前台登录</el-link>
        </div>
      </el-card>
      <div class="admin-footer-tip">
        © 2024 AI Manager 管理后台
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import request from '../../utils/request'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  loading.value = true
  try {
    const response = await request.post('/auth/login', {
      ...loginForm,
      loginType: 'admin'
    })
    userStore.setToken(response.token)
    if (response.refreshToken) {
      localStorage.setItem('refreshToken', response.refreshToken)
    }
    await userStore.fetchUserInfo()
    ElMessage.success('登录成功')
    router.push('/admin/users')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.admin-login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}

.bg-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 25% 25%, rgba(64, 158, 255, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 75% 75%, rgba(124, 58, 237, 0.15) 0%, transparent 50%);
}

.bg-pattern::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.5;
}

.admin-login-content {
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 30px;
}

.admin-logo-section {
  text-align: center;
  color: #fff;
}

.admin-logo-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #e6a23c 0%, #f56c6c 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20px 60px rgba(230, 162, 60, 0.4);
}

.admin-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #fff 0%, #e0f0ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 4px;
}

.admin-subtitle {
  font-size: 16px;
  color: rgba(230, 162, 60, 0.9);
  margin: 8px 0 0;
  letter-spacing: 8px;
  font-weight: 500;
}

.admin-login-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: none;
  border-radius: 16px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.3);
}

.admin-login-card :deep(.el-card__body) {
  padding: 40px;
}

.admin-card-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 30px;
  text-align: center;
}

.admin-login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.admin-login-form :deep(.el-input__wrapper) {
  background: #f5f8ff;
  border: 2px solid #e5eaf5;
  border-radius: 12px;
  padding: 8px 16px;
  box-shadow: none;
  transition: all 0.3s;
}

.admin-login-form :deep(.el-input__wrapper:hover),
.admin-login-form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  border-color: #e6a23c;
  box-shadow: 0 0 0 4px rgba(230, 162, 60, 0.15);
}

.admin-login-form :deep(.el-input__inner) {
  color: #1a1a2e;
  height: 24px;
  font-size: 15px;
}

.admin-login-form :deep(.el-input__inner::placeholder) {
  color: #999;
}

.admin-login-form :deep(.el-input__prefix) {
  color: #e6a23c;
}

.admin-login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #e6a23c 0%, #f56c6c 100%);
  border: none;
  border-radius: 12px;
  box-shadow: 0 8px 25px rgba(230, 162, 60, 0.4);
  transition: all 0.3s;
}

.admin-login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(230, 162, 60, 0.5);
}

.admin-login-options {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
  margin-top: 20px;
}

.admin-login-options :deep(.el-link--primary) {
  color: #409eff;
  font-weight: 500;
}

.admin-footer-tip {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  letter-spacing: 2px;
}
</style>
