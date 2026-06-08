<template>
  <div class="login-container">
    <div class="login-bg">
      <img src="https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1920&q=80" alt="background" class="bg-image" />
      <div class="bg-overlay"></div>
    </div>
    <div class="login-content">
      <div class="logo-section">
        <h1 class="title">AI Manager</h1>
        <p class="subtitle">智能大模型管理平台</p>
      </div>
      <el-card class="login-card">
        <h2 class="card-title">前台登录</h2>
        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
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
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="login-options">
          <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
          <el-divider direction="vertical" />
          <el-link type="warning" @click="$router.push('/admin/login')">管理员登录</el-link>
        </div>
      </el-card>
      <div class="footer-tip">
        © 2024 AI Manager · 智能科技 引领未来
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
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
      loginType: 'front'
    })
    userStore.setToken(response.token)
    if (response.refreshToken) {
      localStorage.setItem('refreshToken', response.refreshToken)
    }
    await userStore.fetchUserInfo()
    ElMessage.success('登录成功')
    router.push('/front/models')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  animation: scaleSlow 30s ease-in-out infinite;
}

@keyframes scaleSlow {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.bg-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg,
    rgba(255,255,255,0.15) 0%,
    rgba(64,169,255,0.2) 50%,
    rgba(128,96,240,0.15) 100%);
  backdrop-filter: blur(3px);
}

.login-content {
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 30px;
}

.logo-section {
  text-align: center;
  color: #fff;
  text-shadow: 0 2px 20px rgba(0,0,0,0.3);
}

.logo-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #409eff 0%, #7c3aed 100%);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20px 60px rgba(64, 158, 255, 0.5);
  animation: iconFloat 3s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.logo-icon .el-icon {
  color: #fff;
}

.title {
  font-size: 48px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #fff 0%, #e0f0ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 4px;
}

.subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin: 10px 0 0;
  letter-spacing: 8px;
  font-weight: 300;
}

.login-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border: none;
  border-radius: 24px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.2);
}

.login-card :deep(.el-card__body) {
  padding: 40px;
}

.card-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 30px;
  text-align: center;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-input__wrapper) {
  background: #f5f8ff;
  border: 2px solid #e5eaf5;
  border-radius: 12px;
  padding: 8px 16px;
  box-shadow: none;
  transition: all 0.3s;
}

.login-form :deep(.el-input__wrapper:hover),
.login-form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  border-color: #409eff;
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.15);
}

.login-form :deep(.el-input__inner) {
  color: #1a1a2e;
  height: 24px;
  font-size: 15px;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #999;
}

.login-form :deep(.el-input__prefix) {
  color: #409eff;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #409eff 0%, #7c3aed 100%);
  border: none;
  border-radius: 12px;
  box-shadow: 0 8px 25px rgba(64, 158, 255, 0.4);
  transition: all 0.3s;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(64, 158, 255, 0.5);
}

.register-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #666;
  font-size: 14px;
  margin-top: 20px;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
  margin-top: 20px;
}

.login-options :deep(.el-link--primary) {
  color: #409eff;
  font-weight: 500;
}

.login-options :deep(.el-link--warning) {
  color: #e6a23c;
  font-weight: 500;
}

.footer-tip {
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  letter-spacing: 2px;
  text-shadow: 0 1px 3px rgba(0,0,0,0.3);
}
</style>
