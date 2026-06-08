<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <div class="card-header">
          <span>用户注册</span>
        </div>
      </template>
      <el-form :model="registerForm" :rules="rules" ref="registerFormRef">
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="registerForm.email" placeholder="邮箱" prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="registerForm.phone" placeholder="手机号" prefix-icon="Phone" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-link type="primary" @click="$router.push('/login')">已有账号？立即登录</el-link>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

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
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
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
    ElMessage.success('注册成功，请登录')
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
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background:
    radial-gradient(ellipse at 20% 80%, rgba(0, 200, 255, 0.3) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(120, 100, 255, 0.3) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(0, 255, 200, 0.15) 0%, transparent 60%),
    linear-gradient(180deg, #1a2a4a 0%, #2d4a6a 50%, #1e3a5f 100%);
  position: relative;
  overflow: hidden;
}

.register-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 40px 40px;
  animation: gridMove 15s linear infinite;
  pointer-events: none;
}

.register-container::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: conic-gradient(from 0deg, transparent, rgba(100, 200, 255, 0.15), transparent 40%);
  animation: rotate 8s linear infinite;
  pointer-events: none;
}

@keyframes gridMove {
  0% { transform: translate(0, 0); }
  100% { transform: translate(40px, 40px); }
}

@keyframes rotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.register-card {
  width: 400px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2), 0 0 60px rgba(0, 150, 255, 0.15);
}

.register-card :deep(.el-card__header) {
  background: rgba(0, 150, 255, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  color: #0066cc;
  font-weight: 600;
}

.register-card :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(0, 150, 255, 0.4);
  box-shadow: none;
}

.register-card :deep(.el-input__wrapper:hover),
.register-card :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(0, 150, 255, 0.8);
  box-shadow: 0 0 20px rgba(0, 150, 255, 0.25);
}

.register-card :deep(.el-input__inner) {
  color: #1a1a2e;
}

.register-card :deep(.el-input__inner::placeholder) {
  color: rgba(26, 26, 46, 0.5);
}

.register-card :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0088ff 0%, #00aaff 100%);
  border: none;
  box-shadow: 0 4px 20px rgba(0, 150, 255, 0.4);
}

.register-card :deep(.el-link--primary) {
  color: #0066cc;
  font-weight: 500;
}

.card-header {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 1px;
}
</style>
