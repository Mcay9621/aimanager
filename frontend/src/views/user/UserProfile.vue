<template>
  <div class="profile-page">
    <div class="profile-card">
      <div class="card-header">
        <div class="card-header-icon">
          <el-icon :size="20"><User /></el-icon>
        </div>
        <span>个人信息</span>
      </div>
      <el-form :model="profile" label-width="100px" v-loading="loading" element-loading-background="var(--app-loading-bg)">
        <el-form-item label="用户名">
          <el-input v-model="profile.username" disabled />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profile.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profile.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProfile" round>保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="profile-card" style="margin-top: 20px;">
      <div class="card-header">
        <div class="card-header-icon lock-icon">
          <el-icon :size="20"><Lock /></el-icon>
        </div>
        <span>修改密码</span>
      </div>
      <el-form :model="passwordForm" label-width="100px" :rules="passwordRules" ref="passwordFormRef">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="changingPassword" @click="changePassword" round>修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const saving = ref(false)
const changingPassword = ref(false)
const profile = reactive({
  username: '',
  email: '',
  phone: ''
})
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordFormRef = ref(null)

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次密码输入不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const fetchProfile = async () => {
  loading.value = true
  try {
    const data = await request.get('/user/profile')
    profile.username = data.username || ''
    profile.email = data.email || ''
    profile.phone = data.phone || ''
  } catch (e) {
    ElMessage.error('获取个人信息失败')
  } finally {
    loading.value = false
  }
}

const saveProfile = async () => {
  saving.value = true
  try {
    await request.put('/user/profile', {
      email: profile.email,
      phone: profile.phone
    })
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const changePassword = async () => {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }

  changingPassword.value = true
  try {
    await request.put('/user/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '修改失败')
  } finally {
    changingPassword.value = false
  }
}

onMounted(fetchProfile)
</script>

<style scoped>
.profile-page {
  max-width: 640px;
  margin: 0 auto;
}
.profile-card {
  background: var(--app-bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 28px;
  backdrop-filter: blur(12px);
  transition: all 0.3s ease;
}
.profile-card:hover {
  border-color: var(--border-color-hover);
  box-shadow: 0 8px 32px rgba(0,0,0,0.3), 0 0 40px rgba(74, 111, 165, 0.03);
}
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 500;
  color: var(--accent-light);
  letter-spacing: 0.5px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
}
.card-header-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #4a6fa5, #6b8fc9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-icon-color);
}
.card-header-icon.lock-icon {
  background: linear-gradient(135deg, #4a80d4, #6a9be0);
}
.el-form-item :deep(.el-form-item__label) {
  color: var(--text-secondary);
}
.el-form-item :deep(.el-input.is-disabled .el-input__wrapper) {
  background: var(--bg-card) !important;
  border-color: var(--border-color-light) !important;
}
.el-form-item :deep(.el-input.is-disabled .el-input__inner) {
  color: var(--text-muted) !important;
  -webkit-text-fill-color: var(--text-muted);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .profile-card { padding: 20px; }
  :deep(.el-form-item) {
    flex-direction: column;
    align-items: stretch;
  }
  :deep(.el-form-item__label) {
    width: 100% !important;
    padding-bottom: 4px;
  }
  :deep(.el-form-item__content) {
    margin-left: 0 !important;
    width: 100%;
  }
}
</style>
