<template>
  <div class="model-list-page">
    <h2 class="page-title-sm">AI 模型列表</h2>
    <p class="page-desc">选择模型以开始智能对话</p>

    <div class="model-toolbar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索模型名称或标识..."
        clearable
        :prefix-icon="Search"
        size="default"
        class="model-search"
      />
      <div class="filter-tabs">
        <el-button
          v-for="f in filterOptions"
          :key="f.value"
          :type="statusFilter === f.value ? f.type : 'default'"
          size="small"
          @click="statusFilter = f.value"
        >{{ f.label }}</el-button>
      </div>
      <el-button size="small" @click="handleRefresh()" :loading="refreshing">刷新状态</el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="8" v-for="model in paginatedModels" :key="model.id">
        <div class="model-card" :class="{ 'model-offline': model._available === false }">
          <div class="model-card-top">
            <div class="model-type-badge" :style="{ background: getTypeBg(model.type), color: getTypeColor(model.type) }">
              {{ getTypeName(model.type) }}
            </div>
            <el-tag v-if="model._available === true" type="success" size="small" effect="dark">在线</el-tag>
            <el-tag v-else-if="model._available === false" type="danger" size="small" effect="dark">离线</el-tag>
            <el-tag v-else type="info" size="small" effect="dark">未检测</el-tag>
          </div>
          <h3 class="model-name">{{ model.name }}</h3>
          <p class="model-id">{{ model.modelName }}</p>
          <div v-if="model._latency" class="model-latency">{{ model._latency }}ms</div>
          <div class="model-card-footer">
            <el-button
              type="primary"
              size="small"
              :disabled="model._available === false"
              @click="useModel(model)"
              round
            >
              {{ model._available === false ? '暂不可用' : '使用模型' }}
            </el-button>
            <el-button size="small" @click="handleRefresh(model)" :loading="model._pinging" circle>
              <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M17.65 6.35A7.96 7.96 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="pagination-wrapper" v-if="sortedModels.length > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="sortedModels.length"
        :page-sizes="[9, 18, 36]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { getModelTypes } from '../../utils/modelTypes'

const router = useRouter()
const models = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(9)
const searchQuery = ref('')
const refreshing = ref(false)
const statusFilter = ref('all')
const filterOptions = [
  { label: '全部', value: 'all', type: 'primary' },
  { label: '在线', value: 'online', type: 'success' },
  { label: '离线', value: 'offline', type: 'danger' },
  { label: '未检测', value: 'unknown', type: 'info' }
]

// 动态模型类型
const modelTypes = ref([])
const typeMap = ref({})
const typeColorMap = ref({})
const getTypeName = (key) => typeMap.value[key] || key
const getTypeColor = (key) => typeColorMap.value[key] || '#888'
const getTypeBg = (key) => getTypeColor(key) + '26'

const sortedModels = computed(() => {
  const q = searchQuery.value.toLowerCase().trim()
  let list = models.value
  if (q) {
    list = list.filter(m =>
      (m.name || '').toLowerCase().includes(q) ||
      (m.modelName || '').toLowerCase().includes(q)
    )
  }
  // 状态筛选
  const sf = statusFilter.value
  if (sf === 'online') {
    list = list.filter(m => m._available === true)
  } else if (sf === 'offline') {
    list = list.filter(m => m._available === false)
  } else if (sf === 'unknown') {
    list = list.filter(m => m._available == null)
  }
  list = [...list]
  list.sort((a, b) => {
    const aAvail = a._available === true ? 1 : 0
    const bAvail = b._available === true ? 1 : 0
    return bAvail - aAvail
  })
  return list
})

const paginatedModels = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return sortedModels.value.slice(start, start + pageSize.value)
})

const fetchModels = async () => {
  loading.value = true
  currentPage.value = 1

  // 获取模型列表（后端附带 Redis 缓存的状态）
  try {
    const data = await request.get('/models')
    models.value = (data || []).map(m => ({
      ...m,
      _available: m._available,
      _latency: m._latency,
      _pinging: false
    }))
  } catch {
    ElMessage.warning('获取模型列表失败')
  }
  loading.value = false

  // 静默刷新 — 缓存过期时不阻塞 UI
  const needsRefresh = models.value.some(m => m._available == null)
  if (needsRefresh) {
    refreshing.value = true
    try {
      const statuses = await request.post('/models/refresh')
      if (Array.isArray(statuses)) {
        const map = {}
        statuses.forEach(s => { map[s.id] = s })
        models.value.forEach(m => {
          const s = map[m.id]
          if (s) { m._available = s.available; m._latency = s.latency }
        })
      }
    } catch {
      // 静默处理，保持 null 状态显示"未检测"
    } finally {
      refreshing.value = false
    }
  }
}

const handleRefresh = async (model) => {
  // 无参数 = 批量刷新全部，有参数 = 刷新单个
  if (model) {
    model._pinging = true
    try {
      const res = await request.get('/models/' + model.id + '/ping')
      if (res) { model._available = res.available; model._latency = res.latency }
    } catch { /* ignore */ }
    model._pinging = false
    return
  }

  // 手动批量刷新
  refreshing.value = true
  try {
    const statuses = await request.post('/models/refresh')
    if (Array.isArray(statuses)) {
      const map = {}
      statuses.forEach(s => { map[s.id] = s })
      models.value.forEach(m => {
        const s = map[m.id]
        if (s) { m._available = s.available; m._latency = s.latency }
      })
    }
  } catch {
    ElMessage.warning('刷新失败')
  } finally {
    refreshing.value = false
  }
}

const useModel = (model) => {
  router.push(`/front/chat?modelId=${model.id}`)
}

watch(searchQuery, () => {
  currentPage.value = 1
})

onMounted(async () => {
  // 加载动态模型类型
  const types = await getModelTypes()
  modelTypes.value = types
  const km = {}, cm = {}
  types.forEach(t => { km[t.key] = t.label; cm[t.key] = t.color })
  typeMap.value = km
  typeColorMap.value = cm

  fetchModels()
})
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
.model-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.model-search {
  width: 320px;
}
.model-search :deep(.el-input__wrapper) {
  background: var(--bg-card) !important;
  border: 1px solid var(--border-color-light) !important;
  border-radius: 10px !important;
  box-shadow: none !important;
  transition: all 0.3s;
}
.model-search :deep(.el-input__wrapper:hover),
.model-search :deep(.el-input__wrapper.is-focus) {
  border-color: var(--border-color-hover) !important;
  background: var(--bg-card-hover) !important;
}
.model-search :deep(.el-input__inner) {
  color: var(--text-primary);
}
.model-search :deep(.el-input__inner::placeholder) {
  color: var(--text-muted);
}
.filter-tabs {
  display: flex;
  gap: 4px;
}

.model-card {
  background: var(--app-bg-glass);
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
.model-card.model-offline {
  opacity: 0.5;
}
.model-card.model-offline:hover {
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
.model-latency {
  font-size: 11px;
  color: var(--text-muted);
  font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
}
.model-card-footer {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .el-col {
    width: 100% !important;
  }
  .model-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .model-toolbar .model-search {
    width: 100% !important;
  }
  .filter-tabs {
    flex-wrap: wrap;
  }
  .model-list-page { padding: 0; }
  .model-card { padding: 16px; }
}
</style>
