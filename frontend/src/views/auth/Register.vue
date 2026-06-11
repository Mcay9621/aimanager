<template>
  <div class="register-container">
    <div class="ambient-light light-1"></div>
    <div class="ambient-light light-2"></div>
    <div class="ambient-light light-3"></div>

    <div class="register-content">
      <div class="brand">
        <div class="brand-icon">
          <svg viewBox="0 0 40 40" width="40" height="40" fill="none">
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
        <h1 class="brand-title"><span class="accent-gradient-text">AI</span> Manager</h1>
        <p class="brand-subtitle">{{ $t('register.subtitle') }}</p>
      </div>

      <div class="register-card">
        <h2 class="card-title">{{ $t('register.title') }}</h2>
        <p class="card-desc">{{ $t('register.cardDesc') }}</p>
        <el-form :model="registerForm" :rules="rules" ref="registerFormRef" class="register-form">
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              :placeholder="$t('register.username')"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              :placeholder="$t('register.password')"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              :placeholder="$t('register.email')"
              size="large"
              :prefix-icon="Message"
            />
          </el-form-item>
          <el-form-item prop="phone">
            <el-input
              v-model="registerForm.phone"
              :placeholder="$t('register.phone')"
              size="large"
              :prefix-icon="Phone"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="register-btn" :loading="loading" @click="handleRegister">
              {{ $t('register.registerBtn') }}
            </el-button>
          </el-form-item>
        </el-form>
        <div class="register-footer">
          <el-link type="primary" :underline="false" @click="$router.push('/login')">{{ $t('register.haveAccountImmediate') }}</el-link>
        </div>
      </div>

      <p class="copyright">{{ $t('login.copyright', { appName: 'AI Manager' }) }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { User, Lock, Message, Phone } from '@element-plus/icons-vue'
import request from '../../utils/request'

const { t } = useI18n()
const router = useRouter()
const registerFormRef = ref()
const loading = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  email: '',
  phone: ''
})

const rules = {
  username: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

const handleRegister = async () => {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    await request.post('/auth/register', registerForm)
    ElMessage.success(t('register.registerSuccess'))
    router.push('/login')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--bg-primary);
}

/* 环境光效 */
.ambient-light {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
}
.light-1 {
  width: 600px; height: 600px;
  background: radial-gradient(circle, rgba(74, 111, 165, 0.08), transparent);
  top: -200px; left: -100px;
  animation: floatLight 12s ease-in-out infinite;
}
.light-2 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(74, 128, 212, 0.06), transparent);
  bottom: -100px; right: -50px;
  animation: floatLight 15s ease-in-out infinite reverse;
}
.light-3 {
  width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(74, 111, 165, 0.05), transparent);
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
}

@keyframes floatLight {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(30px, -30px); }
  66% { transform: translate(-20px, 20px); }
}

.register-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

/* 品牌区 */
.brand {
  text-align: center;
}
.brand-icon {
  margin: 0 auto 16px;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-title {
  font-size: 36px;
  font-weight: 300;
  letter-spacing: 6px;
  color: var(--text-primary);
  margin: 0;
}
.brand-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  letter-spacing: 6px;
  margin-top: 8px;
  font-weight: 300;
}

/* 注册卡片 */
.register-card {
  width: 420px;
  max-width: calc(100vw - 32px);
  background: var(--app-bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 40px;
  backdrop-filter: blur(24px);
  box-shadow: 0 8px 32px rgba(0,0,0,0.5), 0 0 60px rgba(74, 111, 165, 0.03);
  transition: all 0.4s ease;
}
.register-card:hover {
  border-color: var(--border-color-hover);
  box-shadow: 0 12px 48px rgba(0,0,0,0.6), 0 0 80px rgba(74, 111, 165, 0.04);
}

.card-title {
  font-size: 22px;
  font-weight: 400;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 2px;
}
.card-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin: 8px 0 28px;
  letter-spacing: 1px;
  font-weight: 300;
}

.register-form {
  margin-top: 0;
}
.register-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 6px;
  border-radius: 10px !important;
  margin-top: 4px;
}

.register-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
}
.register-footer :deep(.el-link) {
  font-size: 13px;
  font-weight: 300;
  letter-spacing: 0.5px;
}

.copyright {
  font-size: 11px;
  color: var(--app-divider-color);
  letter-spacing: 2px;
  font-weight: 300;
}

/* ===== Responsive ===== */
@media (max-width: 480px) {
  .register-card { padding: 28px 24px; }
  .brand-title { font-size: 28px; letter-spacing: 4px; }
  .brand-subtitle { letter-spacing: 4px; }
  .register-content { gap: 24px; }
}
@media (max-width: 360px) {
  .register-card { padding: 24px 16px; }
  .brand-title { font-size: 24px; }
}
</style>
