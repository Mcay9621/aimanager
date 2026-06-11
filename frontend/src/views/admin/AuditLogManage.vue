<template>
  <div class="page-container">
    <div class="table-card">
      <div class="table-header">
        <h3>{{ $t('admin.audit.title') }}</h3>
        <div class="filters">
          <el-select v-model="filterAction" :placeholder="$t('admin.audit.filterAction')" clearable @change="fetchLogs">
            <el-option :label="$t('common.all')" value="" />
            <el-option v-for="a in actionOptions" :key="a.value" :label="a.label" :value="a.value" />
          </el-select>
          <el-select v-model="filterTarget" :placeholder="$t('admin.audit.filterTarget')" clearable @change="fetchLogs">
            <el-option :label="$t('common.all')" value="" />
            <el-option v-for="t in targetOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-button @click="fetchLogs" type="primary">
            <el-icon><Refresh /></el-icon>
            {{ $t('common.refresh') }}
          </el-button>
        </div>
      </div>
      <el-table :data="paginatedData" border stripe v-loading="loading" max-height="calc(100vh - 300px)" element-loading-background="var(--app-loading-bg)">
        <el-table-column prop="id" :label="$t('admin.audit.id')" width="60" />
        <el-table-column prop="username" :label="$t('admin.audit.operator')" width="120" />
        <el-table-column :label="$t('admin.audit.action')" width="100">
          <template #default="{ row }">
            <el-tag :type="actionType(row.action)" size="small">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="target" :label="$t('admin.audit.target')" width="80" />
        <el-table-column prop="targetId" :label="$t('admin.audit.targetId')" width="80" />
        <el-table-column prop="detail" :label="$t('admin.audit.detail')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" :label="$t('admin.audit.ip')" width="140" />
        <el-table-column prop="createTime" :label="$t('admin.audit.time')" width="180" />
      </el-table>
      <div class="pagination-wrapper" v-if="logs.length > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="logs.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import request from '../../utils/request'

const { t } = useI18n()
const logs = ref([])
const loading = ref(false)
const filterAction = ref('')
const filterTarget = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const actionOptions = [
  { value: 'CREATE', label: t('admin.audit.actions.CREATE') },
  { value: 'UPDATE', label: t('admin.audit.actions.UPDATE') },
  { value: 'DELETE', label: t('admin.audit.actions.DELETE') },
  { value: 'LOGIN', label: t('admin.audit.actions.LOGIN') },
  { value: 'START', label: t('admin.audit.actions.START') },
  { value: 'STOP', label: t('admin.audit.actions.STOP') },
  { value: 'RESTART', label: t('admin.audit.actions.RESTART') },
]

const targetOptions = [
  { value: 'User', label: t('admin.audit.targets.User') },
  { value: 'Role', label: t('admin.audit.targets.Role') },
  { value: 'Model', label: t('admin.audit.targets.Model') },
  { value: 'Auth', label: t('admin.audit.targets.Auth') },
  { value: 'CloudInstance', label: t('admin.audit.targets.CloudInstance') },
  { value: 'CloudAccount', label: t('admin.audit.targets.CloudAccount') },
]

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return logs.value.slice(start, start + pageSize.value)
})

const actionType = (action) => ({
  CREATE: 'success',
  UPDATE: 'warning',
  DELETE: 'danger',
  LOGIN: 'primary',
  START: 'success',
  STOP: 'danger',
  RESTART: 'warning'
}[action] || 'info')

const actionLabel = (action) => {
  const key = `admin.audit.actions.${action}`
  const label = t(key)
  return label !== key ? label : action
}

const fetchLogs = async () => {
  loading.value = true
  try {
    const params = {}
    if (filterAction.value) params.action = filterAction.value
    if (filterTarget.value) params.target = filterTarget.value
    logs.value = await request.get('/admin/audit-logs', { params })
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchLogs)
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
  flex-wrap: wrap;
  gap: 12px;
}
.table-header h3 {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.5px;
  margin: 0;
}
.filters {
  display: flex;
  gap: 12px;
  align-items: center;
}
.filters .el-select { width: 140px; }
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .table-card { padding: 14px; overflow-x: auto; }
  .table-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  .filters { flex-wrap: wrap; width: 100%; }
  .filters .el-select { width: 100% !important; }
  :deep(.el-table) { min-width: 1000px; }
}
</style>
