<template>
  <div class="model-list">
    <h2>AI模型列表</h2>
    <el-row :gutter="20">
      <el-col :span="8" v-for="model in models" :key="model.id">
        <el-card class="model-card" shadow="hover">
          <template #header>
            <div class="model-header">
              <span>{{ model.name }}</span>
              <el-tag :type="model.enabled ? 'success' : 'info'">
                {{ model.enabled ? '可用' : '不可用' }}
              </el-tag>
            </div>
          </template>
          <div class="model-info">
            <p><strong>类型：</strong>{{ getTypeName(model.type) }}</p>
            <p><strong>模型：</strong>{{ model.modelName }}</p>
            <p><strong>API地址：</strong>{{ model.endpoint }}</p>
          </div>
          <div class="model-actions">
            <el-button type="primary" size="small" :disabled="!model.enabled" @click="useModel(model)">使用模型</el-button>
          </div>
        </el-card>
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

onMounted(() => {
  fetchModels()
})
</script>

<style scoped>
.model-list h2 {
  margin-bottom: 20px;
}
.model-card {
  margin-bottom: 20px;
}
.model-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.model-info p {
  margin: 8px 0;
  color: #666;
  font-size: 14px;
}
.model-actions {
  margin-top: 15px;
  text-align: right;
}
</style>
