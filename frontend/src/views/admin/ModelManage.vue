<template>
  <div class="page-container">
    <div class="table-card">
      <div class="table-header">
        <h3>模型列表</h3>
        <div>
          <el-button @click="handleRefreshStatus" :loading="refreshing">检测连通性</el-button>
          <el-button @click="handleSync">同步模型</el-button>
          <el-button type="primary" @click="handleAdd">添加模型</el-button>
        </div>
      </div>
      <div class="filter-bar">
        <el-input v-model="filterName" placeholder="模型名称" clearable size="small" style="width:140px" />
        <el-select v-model="filterType" placeholder="类型" clearable size="small" style="width:110px">
          <el-option v-for="t in modelTypes" :key="t.key" :label="t.label" :value="t.key" />
        </el-select>
        <el-input v-model="filterModelName" placeholder="模型标识" clearable size="small" style="width:130px" />
        <el-input v-model="filterEndpoint" placeholder="API地址" clearable size="small" style="width:160px" />
        <el-select v-model="filterEnabled" placeholder="状态" clearable size="small" style="width:90px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-select v-model="filterAvailable" placeholder="可用性" clearable size="small" style="width:100px">
          <el-option label="在线" value="online" />
          <el-option label="离线" value="offline" />
          <el-option label="未检测" value="unknown" />
        </el-select>
      </div>
      <el-table :data="paginatedData" border stripe element-loading-background="var(--app-loading-bg)">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="模型名称" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <span class="type-badge" :style="{ background: getTypeBg(row.type), color: getTypeColor(row.type) }">
              {{ getTypeLabel(row.type) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="模型标识" />
        <el-table-column prop="endpoint" label="API地址" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可用性" width="90">
          <template #default="{ row }">
            <el-tag v-if="row._available === true" type="success" size="small">在线</el-tag>
            <el-tag v-else-if="row._available === false" type="danger" size="small">离线</el-tag>
            <el-tag v-else type="info" size="small">未检测</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" @click="handleTest(row)" :loading="testingId === row.id">测试</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="filteredModels.length > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredModels.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option v-for="t in modelTypes" :key="t.key" :label="t.label" :value="t.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="API地址" prop="endpoint">
          <el-input v-model="form.endpoint" />
        </el-form-item>
        <el-form-item label="API密钥" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password />
        </el-form-item>
        <el-form-item label="模型标识" prop="modelName">
          <el-input v-model="form.modelName" />
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 同步模型对话框 -->
    <el-dialog v-model="syncVisible" title="同步模型列表" width="640px">
      <el-form :model="syncForm" label-width="100px">
        <el-form-item label="API地址" prop="endpoint">
          <el-input v-model="syncForm.endpoint" placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1" />
        </el-form-item>
        <el-form-item label="API密钥" prop="apiKey">
          <el-input v-model="syncForm.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="模型类型">
          <el-select v-model="syncForm.modelType">
            <el-option v-for="t in modelTypes" :key="t.key" :label="t.label" :value="t.key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFetch" :loading="syncing">获取列表</el-button>
        </el-form-item>
      </el-form>

      <div v-if="syncModels.length > 0" class="sync-result">
        <div class="sync-count">
          <el-checkbox v-model="syncAllChecked" :indeterminate="syncIndeterminate" @change="handleSyncAllChange" />
          共 {{ syncModels.length }} 个模型，已选 {{ syncSelected.length }} 个
        </div>
        <div class="sync-list">
          <div
            v-for="m in syncModels"
            :key="m.id"
            class="sync-item"
            :class="{ 'sync-item-selected': syncSelected.includes(m.id) }"
            @click="toggleSyncItem(m.id)"
          >
            <el-checkbox :checked="syncSelected.includes(m.id)" @click.stop />
            <span class="sync-item-id">{{ m.id }}</span>
          </div>
        </div>
      </div>

      <div v-if="syncError" class="sync-error">{{ syncError }}</div>

      <template #footer>
        <el-button @click="syncVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="syncSelected.length === 0" :loading="importing" @click="handleImport">
          导入选中 ({{ syncSelected.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { getModelTypes } from '../../utils/modelTypes'

const models = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加模型')
const formRef = ref()
const isEdit = ref(false)
const testingId = ref(null)
const refreshing = ref(false)

// 筛选
const filterName = ref('')
const filterType = ref('')
const filterModelName = ref('')
const filterEndpoint = ref('')
const filterEnabled = ref('')
const filterAvailable = ref('')

// 动态模型类型
const modelTypes = ref([])
const typeMap = ref({})
const typeColorMap = ref({})

const currentPage = ref(1)
const pageSize = ref(10)

// 筛选后的数据
const filteredModels = computed(() => {
  let list = models.value
  const q = filterName.value.toLowerCase().trim()
  if (q) list = list.filter(m => (m.name || '').toLowerCase().includes(q))
  if (filterType.value) list = list.filter(m => m.type === filterType.value)
  if (filterModelName.value) {
    const qn = filterModelName.value.toLowerCase().trim()
    list = list.filter(m => (m.modelName || '').toLowerCase().includes(qn))
  }
  if (filterEndpoint.value) {
    const qe = filterEndpoint.value.toLowerCase().trim()
    list = list.filter(m => (m.endpoint || '').toLowerCase().includes(qe))
  }
  if (filterEnabled.value !== '' && filterEnabled.value !== undefined) {
    list = list.filter(m => m.enabled === filterEnabled.value)
  }
  if (filterAvailable.value === 'online') list = list.filter(m => m._available === true)
  else if (filterAvailable.value === 'offline') list = list.filter(m => m._available === false)
  else if (filterAvailable.value === 'unknown') list = list.filter(m => m._available == null)
  return list
})

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredModels.value.slice(start, start + pageSize.value)
})

// 重置页数当筛选变化
watch([filterName, filterType, filterModelName, filterEndpoint, filterEnabled, filterAvailable], () => { currentPage.value = 1 })

// 同步模型
const syncVisible = ref(false)
const syncing = ref(false)
const syncModels = ref([])
const syncError = ref('')
const syncForm = reactive({
  endpoint: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  apiKey: '',
  modelType: 'ali'
})
const syncSelected = ref([])
const syncAllChecked = ref(false)
const syncIndeterminate = ref(false)
const importing = ref(false)

const handleSync = () => {
  syncModels.value = []
  syncError.value = ''
  syncSelected.value = []
  syncAllChecked.value = false
  syncIndeterminate.value = false
  syncForm.apiKey = ''
  syncVisible.value = true
}

const handleFetch = async () => {
  if (!syncForm.endpoint || !syncForm.apiKey) {
    ElMessage.warning('请输入 API 地址和密钥')
    return
  }
  syncing.value = true
  syncError.value = ''
  syncModels.value = []
  syncSelected.value = []
  syncAllChecked.value = false
  syncIndeterminate.value = false
  try {
    const data = await request.post('/admin/models/fetch', {
      endpoint: syncForm.endpoint,
      apiKey: syncForm.apiKey
    })
    syncModels.value = data || []
    if (syncModels.value.length === 0) {
      ElMessage.info('未获取到模型')
    }
  } catch (e) {
    syncError.value = e.message || '获取失败'
  } finally {
    syncing.value = false
  }
}

const toggleSyncItem = (id) => {
  const idx = syncSelected.value.indexOf(id)
  if (idx > -1) {
    syncSelected.value.splice(idx, 1)
  } else {
    syncSelected.value.push(id)
  }
  syncAllChecked.value = syncSelected.value.length === syncModels.value.length
  syncIndeterminate.value = syncSelected.value.length > 0 && syncSelected.value.length < syncModels.value.length
}

const handleSyncAllChange = (checked) => {
  if (checked) {
    syncSelected.value = syncModels.value.map(m => m.id)
  } else {
    syncSelected.value = []
  }
  syncIndeterminate.value = false
}

const handleImport = async () => {
  if (syncSelected.value.length === 0) return
  importing.value = true
  let success = 0
  let fail = 0

  for (const modelId of syncSelected.value) {
    try {
      // Generate a display name from model ID
      const displayName = modelId
        .replace(/-(\d{8}|\d{6})/g, '')  // remove date suffixes
        .replace(/^(qwen|deepseek|glm|kimi)/, match => match)
        .replace(/^vanchin\//, '')
        .replace(/^siliconflow\//, '')
      await request.post('/admin/models', {
        name: displayName || modelId,
        type: syncForm.modelType,
        endpoint: syncForm.endpoint,
        apiKey: syncForm.apiKey,
        modelName: modelId,
        enabled: 1
      })
      success++
    } catch {
      fail++
    }
  }

  importing.value = false
  ElMessage.success(`导入完成: ${success} 成功${fail ? `, ${fail} 失败` : ''}`)
  if (fail === 0) {
    syncVisible.value = false
    fetchModels()
  }
}

const form = reactive({
  id: null,
  name: '',
  type: '',
  endpoint: '',
  apiKey: '',
  modelName: '',
  enabled: 1
})

const rules = {
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  modelName: [{ required: true, message: '请输入模型标识', trigger: 'blur' }]
}

// 类型辅助函数
const getTypeLabel = (key) => typeMap.value[key] || key
const getTypeColor = (key) => typeColorMap.value[key] || '#888'
const getTypeBg = (key) => getTypeColor(key) + '26'

const fetchModels = async () => {
  try {
    const data = await request.get('/admin/models')
    models.value = (data || []).map(m => ({
      ...m,
      _available: m._available,
      _latency: m._latency
    }))
  } catch (e) {
    ElMessage.error('获取模型列表失败: ' + (e.message || '网络错误'))
    return
  }

  // 静默刷新 — 缓存过期时不阻塞 UI
  const needsRefresh = models.value.some(m => m._available == null)
  if (needsRefresh) {
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
      // 静默处理
    }
  }
}

const handleRefreshStatus = async () => {
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
    ElMessage.success('连通性检测完成')
  } catch {
    ElMessage.warning('检测失败')
  } finally {
    refreshing.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '添加模型'
  Object.assign(form, {
    id: null, name: '', type: '', endpoint: '', apiKey: '', modelName: '', enabled: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑模型'
  Object.assign(form, { ...row, apiKey: row.apiKey ? '••••••••' : '' })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  const submitData = { ...form }
  if (submitData.apiKey === '••••••••') {
    delete submitData.apiKey
  }
  const api = isEdit.value ? `/admin/models/${form.id}` : '/admin/models'
  const method = isEdit.value ? 'put' : 'post'
  await request[method](api, submitData)
  ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
  dialogVisible.value = false
  fetchModels()
}

const handleTest = async (row) => {
  testingId.value = row.id
  try {
    const res = await request.post(`/admin/models/${row.id}/test`)
    if (res.connected) {
      ElMessage.success(`${row.name} 连接成功`)
    } else {
      ElMessage.warning(res.message || `${row.name} 连接失败`)
    }
  } catch (e) {
    ElMessage.error('测试请求失败')
  } finally {
    testingId.value = null
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该模型？', '提示', { type: 'warning' })
  await request.delete(`/admin/models/${id}`)
  ElMessage.success('删除成功')
  fetchModels()
}

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
.table-card {
  background: var(--app-bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
  backdrop-filter: blur(12px);
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.table-header h3 {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.5px;
  margin: 0;
}
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.type-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
.sync-result {
  margin-top: 16px;
  border-top: 1px solid var(--border-color);
  padding-top: 16px;
}
.sync-count {
  color: var(--text-muted);
  font-size: 13px;
  margin-bottom: 12px;
}
.sync-list {
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.sync-item {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
  color: var(--text-primary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;
  user-select: none;
}
.sync-item:hover {
  border-color: var(--el-color-primary);
  background: rgba(64,158,255,0.08);
}
.sync-item-selected {
  border-color: var(--el-color-primary);
  background: rgba(64,158,255,0.12);
}
.sync-error {
  margin-top: 12px;
  color: #f56c6c;
  font-size: 13px;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .table-card { padding: 14px; overflow-x: auto; }
  .table-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  .filter-bar { flex-direction: column; }
  .filter-bar .el-input,
  .filter-bar .el-select { width: 100% !important; }
  :deep(.el-table) { min-width: 1000px; }
  :deep(.el-pagination .el-pagination__sizes) { display: none; }
}
</style>
