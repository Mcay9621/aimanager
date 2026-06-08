<template>
  <div class="page-container">
    <div class="table-card">
      <div class="table-header">
        <h3>审计日志</h3>
        <div class="filters">
          <el-select v-model="filterAction" placeholder="操作类型" clearable @change="fetchLogs">
            <el-option label="全部" value="" />
            <el-option label="创建" value="CREATE" />
            <el-option label="更新" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="启动" value="START" />
            <el-option label="停止" value="STOP" />
            <el-option label="重启" value="RESTART" />
          </el-select>
          <el-select v-model="filterTarget" placeholder="对象类型" clearable @change="fetchLogs">
            <el-option label="全部" value="" />
            <el-option label="用户" value="User" />
            <el-option label="角色" value="Role" />
            <el-option label="模型" value="Model" />
            <el-option label="认证" value="Auth" />
            <el-option label="云实例" value="CloudInstance" />
            <el-option label="云账号" value="CloudAccount" />
          </el-select>
          <el-button @click="fetchLogs" type="primary">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>
      <el-table :data="paginatedData" border stripe v-loading="loading" max-height="calc(100vh - 300px)" element-loading-background="rgba(10,10,15,0.8)">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="action" label="操作" width="100">
          <template #default="{ row }">
            <el-tag :type="actionType(row.action)" size="small">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="target" label="对象" width="80" />
        <el-table-column prop="targetId" label="对象ID" width="80" />
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="createTime" label="时间" width="180" />
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
import request from '../../utils/request'

const logs = ref([])
const loading = ref(false)
const filterAction = ref('')
const filterTarget = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
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

const actionLabel = (action) => ({
  CREATE: '创建',
  UPDATE: '更新',
  DELETE: '删除',
  LOGIN: '登录',
  START: '启动',
  STOP: '停止',
  RESTART: '重启'
}[action] || action)

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
  background: linear-gradient(180deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%);
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
</style>
