<template>
  <div class="page-container">
    <div class="table-card">
      <div class="table-header">
        <h3>{{ $t('admin.models.title') }}</h3>
        <div>
          <el-button @click="handleRefreshStatus" :loading="refreshing">{{ $t('admin.models.refreshStatus') }}</el-button>
          <el-button @click="handleSync">{{ $t('admin.models.sync') }}</el-button>
          <el-button type="primary" @click="handleAdd">{{ $t('admin.models.add') }}</el-button>
        </div>
      </div>
      <div class="filter-bar">
        <el-input v-model="filterName" :placeholder="$t('admin.models.filterName')" clearable size="small" style="width:140px" />
        <el-select v-model="filterType" :placeholder="$t('admin.models.filterType')" clearable size="small" style="width:110px">
          <el-option v-for="t in modelTypes" :key="t.key" :label="t.label" :value="t.key" />
        </el-select>
        <el-input v-model="filterModelName" :placeholder="$t('admin.models.filterModelName')" clearable size="small" style="width:130px" />
        <el-input v-model="filterEndpoint" :placeholder="$t('admin.models.filterEndpoint')" clearable size="small" style="width:160px" />
        <el-select v-model="filterEnabled" :placeholder="$t('admin.models.filterStatus')" clearable size="small" style="width:90px">
          <el-option :label="$t('common.enable')" :value="1" />
          <el-option :label="$t('common.disable')" :value="0" />
        </el-select>
        <el-select v-model="filterAvailable" :placeholder="$t('admin.models.filterAvailability')" clearable size="small" style="width:100px">
          <el-option :label="$t('common.online')" value="online" />
          <el-option :label="$t('common.offline')" value="offline" />
          <el-option :label="$t('common.unknown')" value="unknown" />
        </el-select>
      </div>
      <el-table :data="paginatedData" border stripe element-loading-background="var(--app-loading-bg)">
        <el-table-column prop="id" :label="$t('admin.audit.id')" width="60" />
        <el-table-column prop="name" :label="$t('admin.models.name')" />
        <el-table-column prop="type" :label="$t('admin.models.type')" width="100">
          <template #default="{ row }">
            <span class="type-badge" :style="{ background: getTypeBg(row.type), color: getTypeColor(row.type) }">
              {{ getTypeLabel(row.type) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" :label="$t('admin.models.modelName')" />
        <el-table-column prop="endpoint" :label="$t('admin.models.endpoint')" show-overflow-tooltip />
        <el-table-column prop="enabled" :label="$t('admin.models.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? $t('common.enable') : $t('common.disable') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('admin.models.availability')" width="90">
          <template #default="{ row }">
            <el-tag v-if="row._available === true" type="success" size="small">{{ $t('common.online') }}</el-tag>
            <el-tag v-else-if="row._available === false" type="danger" size="small">{{ $t('common.offline') }}</el-tag>
            <el-tag v-else type="info" size="small">{{ $t('common.unknown') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.action')" width="280">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button size="small" @click="handleTest(row)" :loading="testingId === row.id">{{ $t('admin.models.test') }}</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">{{ $t('common.delete') }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <!-- 快速预设（仅添加模式显示） -->
      <div v-if="!isEdit" class="preset-section">
        <div class="preset-header" @click="presetOpen = !presetOpen">
          <span class="preset-title">{{ $t('admin.models.presets') }}</span>
          <el-icon :class="{ 'preset-arrow-open': presetOpen }"><ArrowRight /></el-icon>
        </div>
        <transition name="preset-collapse">
          <div v-show="presetOpen" class="preset-body">
            <div
              v-for="group in presetGroups"
              :key="group.type"
              class="preset-group"
            >
              <div class="preset-group-label">{{ getTypeLabel(group.type) }}</div>
              <div class="preset-list">
                <div
                  v-for="p in group.items"
                  :key="p.modelName"
                  class="preset-item"
                  :class="{ 'preset-item-active': form.modelName === p.modelName }"
                  @click="applyPreset(p)"
                >
                  <span class="preset-item-name">{{ p.name }}</span>
                  <span class="preset-item-model">{{ p.modelName }}</span>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item :label="$t('admin.models.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="$t('admin.models.type')" prop="type">
          <el-select v-model="form.type" :placeholder="$t('admin.models.typeRequired')">
            <el-option v-for="t in modelTypes" :key="t.key" :label="t.label" :value="t.key" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('admin.models.endpoint')" prop="endpoint">
          <el-input v-model="form.endpoint" />
        </el-form-item>
        <el-form-item :label="$t('admin.models.apiKey')" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password />
        </el-form-item>
        <el-form-item :label="$t('admin.models.modelName')" prop="modelName">
          <el-input v-model="form.modelName" />
        </el-form-item>
        <el-form-item :label="$t('admin.models.status')" prop="enabled">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 同步模型对话框 -->
    <el-dialog v-model="syncVisible" :title="$t('admin.models.syncTitle')" width="640px">
      <el-form :model="syncForm" label-width="100px">
        <el-form-item :label="$t('admin.models.endpoint')" prop="endpoint">
          <el-input v-model="syncForm.endpoint" placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1" />
        </el-form-item>
        <el-form-item :label="$t('admin.models.apiKey')" prop="apiKey">
          <el-input v-model="syncForm.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item :label="$t('admin.models.type')">
          <el-select v-model="syncForm.modelType">
            <el-option v-for="t in modelTypes" :key="t.key" :label="t.label" :value="t.key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFetch" :loading="syncing">{{ $t('admin.models.syncFetch') }}</el-button>
        </el-form-item>
      </el-form>

      <div v-if="syncModels.length > 0" class="sync-result">
        <div class="sync-count">
          <el-checkbox v-model="syncAllChecked" :indeterminate="syncIndeterminate" @change="handleSyncAllChange" />
          {{ $t('admin.models.selectTotal', { total: syncModels.length, selected: syncSelected.length }) }}
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
        <el-button @click="syncVisible = false">{{ $t('admin.models.syncClose') }}</el-button>
        <el-button type="primary" :disabled="syncSelected.length === 0" :loading="importing" @click="handleImport">
          {{ $t('admin.models.syncImport', { count: syncSelected.length }) }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import request from '../../utils/request'
import { getModelTypes } from '../../utils/modelTypes'
import { modelPresets } from '../../utils/modelPresets'

const { t } = useI18n()

const models = ref([])
const dialogVisible = ref(false)

// 快速预设
const presetOpen = ref(false)
const presetGroups = computed(() => {
  const map = {}
  modelPresets.forEach(p => {
    if (!map[p.type]) map[p.type] = []
    map[p.type].push(p)
  })
  return Object.entries(map).map(([type, items]) => ({ type, items }))
})
const applyPreset = (p) => {
  Object.assign(form, {
    name: p.name,
    type: p.type,
    endpoint: p.endpoint,
    modelName: p.modelName,
    apiKey: form.apiKey || '',
    enabled: p.enabled ?? 1,
  })
}
const dialogTitle = ref(t('admin.models.add'))
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
    ElMessage.warning(t('admin.models.inputApiKey'))
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
      ElMessage.info(t('admin.models.noFetchResult'))
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
  const extra = fail ? (', ' + fail + ' 失败') : ''
  ElMessage.success(t('admin.models.importResult', { success, extra }))
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
  name: [{ required: true, message: t('admin.models.nameRequired'), trigger: 'blur' }],
  type: [{ required: true, message: t('admin.models.typeRequired'), trigger: 'change' }],
  modelName: [{ required: true, message: t('admin.models.modelNameRequired'), trigger: 'blur' }]
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
    ElMessage.error(t('admin.models.fetchModelsFailed') + ': ' + (e.message || '网络错误'))
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
    ElMessage.success(t('admin.models.refreshSuccess'))
  } catch {
    ElMessage.warning(t('admin.models.refreshWarning'))
  } finally {
    refreshing.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = t('admin.models.add')
  presetOpen.value = false
  Object.assign(form, {
    id: null, name: '', type: '', endpoint: '', apiKey: '', modelName: '', enabled: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = t('admin.models.edit')
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
  ElMessage.success(isEdit.value ? t('admin.models.updateSuccess') : t('admin.models.addSuccess'))
  dialogVisible.value = false
  fetchModels()
}

const handleTest = async (row) => {
  testingId.value = row.id
  try {
    const res = await request.post(`/admin/models/${row.id}/test`)
    if (res.connected) {
      ElMessage.success(t('admin.models.connectSuccess', { name: row.name }))
    } else {
      ElMessage.warning(res.message || t('admin.models.connectFailed', { name: row.name }))
    }
  } catch (e) {
    ElMessage.error(t('admin.models.testRequestFailed'))
  } finally {
    testingId.value = null
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm(t('admin.models.confirmDelete'), t('chat.confirmTitle'), { type: 'warning' })
  await request.delete(`/admin/models/${id}`)
  ElMessage.success(t('common.deleteSuccess'))
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

/* ===== 模型预设 ===== */
.preset-section {
  margin-bottom: 16px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
}
.preset-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
  color: var(--text-secondary);
  font-size: 13px;
}
.preset-header:hover {
  background: var(--bg-card);
}
.preset-title {
  font-weight: 500;
}
.preset-header .el-icon {
  transition: transform 0.25s;
  font-size: 14px;
}
.preset-arrow-open {
  transform: rotate(90deg);
}
.preset-body {
  border-top: 1px solid var(--border-color);
  padding: 12px 14px;
  max-height: 380px;
  overflow-y: auto;
}
.preset-group {
  margin-bottom: 12px;
}
.preset-group:last-child {
  margin-bottom: 0;
}
.preset-group-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.preset-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.preset-item {
  background: var(--bg-card);
  border: 1px solid var(--border-color-light);
  border-radius: 8px;
  padding: 5px 11px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 90px;
}
.preset-item:hover {
  border-color: var(--el-color-primary);
  background: rgba(64,158,255,0.06);
  transform: translateY(-1px);
}
.preset-item-active {
  border-color: var(--el-color-primary) !important;
  background: rgba(64,158,255,0.1) !important;
}
.preset-item-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-primary);
}
.preset-item-model {
  font-size: 10px;
  color: var(--text-muted);
  font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
  word-break: break-all;
}
.preset-collapse-enter-active,
.preset-collapse-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}
.preset-collapse-enter-from,
.preset-collapse-leave-to {
  opacity: 0;
  max-height: 0 !important;
  padding-top: 0;
  padding-bottom: 0;
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
