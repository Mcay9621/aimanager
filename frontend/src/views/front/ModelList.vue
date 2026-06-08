<template>
  <div class="model-list-page">
    <h2 class="page-title-sm">AI 模型列表</h2>
    <p class="page-desc">选择模型以开始智能对话</p>
    <el-row :gutter="20">
      <el-col :span="8" v-for="model in models" :key="model.id">
        <div class="model-card" :class="{ 'model-disabled': !model.enabled }">
          <div class="model-card-top">
            <div class="model-type-badge" :class="'badge-' + (model.type || 'openai')">
              {{ getTypeName(model.type) }}
            </div>
            <el-tag :type="model.enabled ? 'success' : 'info'" size="small" effect="dark">
              {{ model.enabled ? '可用' : '不可用' }}
            </el-tag>
          </div>
          <h3 class="model-name">{{ model.name }}</h3>
          <p class="model-id">{{ model.modelName }}</p>
          <div class="model-card-footer">
            <el-button
              type="primary"
              size="small"
              :disabled="!model.enabled"
              @click="useModel(model)"
              round
            >
              {{ model.enabled ? '使用模型' : '暂不可用' }}
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'

const router = useRouter()
const models = ref([])

const typeMap = {
  openai: 'OpenAI',
  anthropic: 'Anthropic',
  ali: '阿里云',
  baidu: '百度',
  byte: '字节跳动',
  tencent: '腾讯'
}

const getTypeName = (type) => typeMap[type] || type

const fetchModels = async () => {
  const data = await request.get('/models')
  models.value = data
}

const useModel = (model) => {
  router.push(`/front/chat?modelId=${model.id}`)
}

onMounted(() => { fetchModels() })
</script>

<style scoped>
.model-list-page {
  max-width: 1200px;
}
.page-title-sm {
  font-size: 20px;
  font-weight: 400;
  color: var(--text-primary);
  margin: 0 0 4px;
  letter-spacing: 1px;
}
.page-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0 0 24px;
  font-weight: 300;
}

.model-card {
  background: linear-gradient(180deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  backdrop-filter: blur(12px);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.model-card:hover {
  border-color: var(--border-color-hover);
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0,0,0,0.4), 0 0 40px rgba(74, 111, 165, 0.04);
}
.model-card.model-disabled {
  opacity: 0.5;
}
.model-card.model-disabled:hover {
  transform: none;
  box-shadow: none;
}

.model-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.model-type-badge {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
  padding: 3px 10px;
  border-radius: 6px;
  text-transform: uppercase;
}
.badge-openai { background: rgba(0, 200, 150, 0.15); color: #00c896; }
.badge-anthropic { background: rgba(200, 150, 255, 0.15); color: #c896ff; }
.badge-ali { background: rgba(255, 107, 53, 0.15); color: #ff6b35; }
.badge-baidu { background: rgba(50, 100, 255, 0.15); color: #3264ff; }
.badge-byte { background: rgba(0, 200, 255, 0.15); color: #00c8ff; }
.badge-tencent { background: rgba(0, 180, 255, 0.15); color: #00b4ff; }

.model-name {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.5px;
}
.model-id {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
  font-weight: 300;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
}
.model-card-footer {
  margin-top: 4px;
}

@media (max-width: 768px) {
  .el-col {
    width: 100% !important;
  }
}
</style>
