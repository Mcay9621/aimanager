<template>
  <div class="page-container">
    <div class="settings-card">
      <h3>{{ $t('admin.settings.title') }}</h3>

      <el-form :model="form" label-width="120px" class="settings-form">
        <el-form-item :label="$t('admin.settings.appName')">
          <el-input v-model="form.appName" placeholder="AI Manager" style="max-width:360px" />
        </el-form-item>

        <el-form-item :label="$t('admin.settings.primaryColor')">
          <div class="color-picker-row">
            <el-color-picker v-model="form.primaryColor" show-alpha :predefine="predefineColors" />
            <span class="color-hex">{{ form.primaryColor }}</span>
          </div>
          <div class="form-item-hint">{{ $t('admin.settings.primaryColorDesc') }}</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">{{ $t('common.save') }}</el-button>
          <el-button @click="handleReset">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <div class="preview-section">
        <h4>{{ $t('admin.settings.preview') }}</h4>
        <p class="preview-desc">{{ $t('admin.settings.previewDesc') }}</p>

        <div class="preview-grid">
          <div class="preview-item">
            <span class="preview-label">{{ $t('admin.settings.appName') }}</span>
            <div class="preview-app-name" :style="{ color: form.primaryColor }">
              {{ form.appName || 'AI Manager' }}
            </div>
          </div>

          <div class="preview-item">
            <span class="preview-label">{{ $t('admin.settings.previewButton') }}</span>
            <el-button type="primary" style="margin-right:8px">{{ $t('admin.settings.previewButton') }}</el-button>
            <el-button type="primary" plain>{{ $t('admin.settings.previewButton') }}</el-button>
          </div>

          <div class="preview-item">
            <span class="preview-label">{{ $t('admin.settings.previewLink') }}</span>
            <a class="preview-link" :style="{ color: form.primaryColor }" href="#">{{ $t('admin.settings.previewLink') }}</a>
          </div>

          <div class="preview-item">
            <span class="preview-label">{{ $t('admin.settings.previewTag') }}</span>
            <el-tag :color="form.primaryColor" effect="dark" style="margin-right:8px">{{ $t('admin.settings.previewTag') }} 1</el-tag>
            <el-tag :color="form.primaryColor" effect="plain">{{ $t('admin.settings.previewTag') }} 2</el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useBrandingStore } from '../../stores/branding'

const { t } = useI18n()

const brandStore = useBrandingStore()
const saving = ref(false)

const form = reactive({
  appName: '',
  primaryColor: '#4a6fa5',
})

const predefineColors = [
  '#4a6fa5', '#409eff', '#67c23a', '#e6a23c', '#f56c6c',
  '#909399', '#8e44ad', '#2c3e50', '#c0392b', '#16a085',
  '#d35400', '#2980b9', '#27ae60', '#8e44ad', '#2c3e50',
]

onMounted(() => {
  form.appName = brandStore.config.appName
  form.primaryColor = brandStore.config.primaryColor
})

const handleSave = () => {
  saving.value = true
  brandStore.save({
    appName: form.appName || 'AI Manager',
    primaryColor: form.primaryColor,
  })
  setTimeout(() => {
    saving.value = false
    ElMessage.success(t('admin.settings.saveSuccess'))
  }, 300)
}

const handleReset = () => {
  form.appName = 'AI Manager'
  form.primaryColor = '#4a6fa5'
  brandStore.save({ appName: 'AI Manager', primaryColor: '#4a6fa5' })
  ElMessage.success(t('common.operationSuccess'))
}
</script>

<style scoped>
.settings-card {
  background: var(--app-bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  backdrop-filter: blur(12px);
  max-width: 720px;
}
.settings-card h3 {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 24px;
  letter-spacing: 0.5px;
}
.settings-form {
  max-width: 520px;
}
.color-picker-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.color-hex {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  color: var(--text-muted);
}
.form-item-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
  line-height: 1.4;
}
.preview-section h4 {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 8px;
}
.preview-desc {
  color: var(--text-muted);
  font-size: 13px;
  margin-bottom: 16px;
}
.preview-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.preview-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.preview-label {
  font-size: 12px;
  color: var(--text-muted);
  min-width: 70px;
  flex-shrink: 0;
}
.preview-app-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 1px;
}
.preview-link {
  font-size: 14px;
  text-decoration: none;
  font-weight: 500;
}
.preview-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .settings-card { padding: 16px; }
}
</style>
