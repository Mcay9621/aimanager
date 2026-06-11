<template>
  <div class="profile-page">
    <div class="profile-card">
      <div class="card-header">
        <div class="card-header-icon">
          <el-icon :size="20"><User /></el-icon>
        </div>
        <span>{{ $t('user.personalInfo') }}</span>
      </div>
      <el-form :model="profile" :label-width="labelWidth" v-loading="loading" element-loading-background="var(--app-loading-bg)">
        <el-form-item :label="$t('user.username')">
          <el-input v-model="profile.username" disabled />
        </el-form-item>
        <el-form-item :label="$t('user.email')">
          <el-input v-model="profile.email" :placeholder="$t('user.email')" />
        </el-form-item>
        <el-form-item :label="$t('user.phone')">
          <el-input v-model="profile.phone" :placeholder="$t('user.phone')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProfile" round>{{ $t('user.saveProfile') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="profile-card" style="margin-top: 20px;">
      <div class="card-header">
        <div class="card-header-icon lock-icon">
          <el-icon :size="20"><Lock /></el-icon>
        </div>
        <span>{{ $t('user.changePwd') }}</span>
      </div>
      <el-form :model="passwordForm" :label-width="labelWidth" :rules="passwordRules" ref="passwordFormRef">
        <el-form-item :label="$t('user.oldPassword')" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item :label="$t('user.newPassword')" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password :placeholder="$t('user.passwordMin')" />
        </el-form-item>
        <el-form-item :label="$t('user.confirmPassword')" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="changingPassword" @click="changePassword" round>{{ $t('user.changePwd') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { User, Lock } from '@element-plus/icons-vue'
import { useResponsive } from '../../composables/useResponsive'
import request from '../../utils/request'

const { t } = useI18n()
const { isMobile } = useResponsive()
const loading = ref(false)
const saving = ref(false)
const changingPassword = ref(false)
const labelWidth = computed(() => isMobile.value ? '100%' : '100px')
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
  oldPassword: [{ required: true, message: t('user.oldPasswordRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('user.newPasswordRequired'), trigger: 'blur' },
    { min: 6, message: t('user.passwordMin'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: t('user.confirmPasswordRequired'), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error(t('user.passwordMismatch')))
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
    ElMessage.error(t('common.operationFailed'))
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
    ElMessage.success(t('common.saveSuccess'))
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('common.operationFailed'))
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
    ElMessage.success(t('common.saveSuccess'))
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('common.operationFailed'))
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
