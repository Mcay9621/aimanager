<template>
  <div class="cloud-resources">
    <!-- 顶部工具栏：云厂商筛选 + 地域选择 + 搜索 -->
    <div class="top-bar">
      <div class="top-bar-left">
        <el-select v-model="filters.provider" placeholder="全部厂商" clearable style="width:140px" @change="fetchResources">
          <el-option label="全部厂商" value="" />
          <el-option label="阿里云" value="aliyun" />
          <el-option label="腾讯云" value="tencent" />
        </el-select>
        <el-select v-model="filters.region" placeholder="全部地域" clearable style="width:160px" @change="fetchResources">
          <el-option label="全部地域" value="" />
          <el-option v-for="r in regions" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
        <el-select v-model="filterMasterId" placeholder="全部主账号" clearable style="width:160px" @change="fetchResources">
          <el-option label="全部主账号" value="" />
          <el-option v-for="m in masterAccounts" :key="m.id" :label="m.aliasName" :value="m.id" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="搜索名称或 IP..." clearable style="width:200px" @clear="fetchResources" @keyup.enter="fetchResources">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="top-bar-right">
        <el-button @click="fetchResources" :icon="Refresh">刷新</el-button>
      </div>
    </div>

    <!-- 状态筛选 Tab + 统计 -->
    <div class="status-bar">
      <div class="status-tabs">
        <el-radio-group v-model="filters.status" @change="fetchResources" size="small">
          <el-radio-button label="">全部 <el-tag size="small" round>{{ stats.total }}</el-tag></el-radio-button>
          <el-radio-button label="running">运行中 <el-tag size="small" type="success" round>{{ stats.running }}</el-tag></el-radio-button>
          <el-radio-button label="stopped">已停止 <el-tag size="small" type="info" round>{{ stats.stopped }}</el-tag></el-radio-button>
          <el-radio-button label="error">异常 <el-tag size="small" type="danger" round>{{ stats.error }}</el-tag></el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-bar" v-if="selectedResources.length > 0">
      <span class="selected-count">已选 {{ selectedResources.length }} 项</span>
      <el-button size="small" type="primary" @click="batchAction('start')">启动</el-button>
      <el-button size="small" type="warning" @click="batchAction('stop')">停止</el-button>
      <el-button size="small" type="danger" @click="batchAction('restart')">重启</el-button>
    </div>

    <!-- 资源列表 -->
    <el-table :data="paginatedData" border stripe v-loading="loading" element-loading-background="rgba(10,10,15,0.8)" @selection-change="handleSelectionChange"
              :default-sort="{ prop: 'name', order: 'ascending' }">
      <el-table-column type="selection" width="40" />
      <el-table-column label="资源名称" min-width="160">
        <template #default="{ row }">
          <div class="resource-name">
            <span class="name-text" @click="showDetail(row)">{{ row.name }}</span>
            <el-tag :type="providerTag(row.provider)" size="small" class="provider-tag">
              {{ providerLabel(row.provider) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <span>{{ typeLabel(row.type) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small" effect="dark" round>
            <span class="status-dot" :class="row.status"></span>
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="配置" width="110">
        <template #default="{ row }">
          <span v-if="row.cpu && row.cpu !== '-'">{{ row.cpu }}C{{ row.memory }}G</span>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="公网 IP" width="130" sortable>
        <template #default="{ row }">
          <span :class="{ 'text-muted': !row.publicIp }">{{ row.publicIp || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="地域" width="120" sortable>
        <template #default="{ row }">{{ regionLabel(row.region) }}</template>
      </el-table-column>
      <el-table-column label="所属账号" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.masterAlias" class="hierarchy-account">{{ row.masterAlias }} / {{ row.accountAlias }}</span>
          <span v-else>{{ row.accountAlias }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" :disabled="row.status !== 'stopped'" @click="handleAction(row.id, 'start')">启动</el-button>
          <el-button size="small" type="warning" :disabled="row.status !== 'running'" @click="handleAction(row.id, 'stop')">停止</el-button>
          <el-dropdown trigger="click" @command="(cmd) => handleAction(row.id, cmd)">
            <el-button size="small" @click.prevent>
              更多<el-icon><ArrowDown /></el-icon>
            </el-button>
            <el-dropdown-menu>
              <el-dropdown-item command="restart" :disabled="row.status !== 'running'">重启</el-dropdown-item>
              <el-dropdown-item command="detail" divided>查看详情</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="resources.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="resources.length"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- 资源详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="detailData.name || '资源详情'" size="500px">
      <template v-if="detailData.id">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="资源名称">{{ detailData.name }}</el-descriptions-item>
          <el-descriptions-item label="ID">{{ detailData.id }}</el-descriptions-item>
          <el-descriptions-item label="云厂商">{{ providerLabel(detailData.provider) }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ typeLabel(detailData.type) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detailData.status)" size="small" effect="dark" round>{{ statusLabel(detailData.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="配置" v-if="detailData.cpu && detailData.cpu !== '-'">
            {{ detailData.cpu }} 核 CPU / {{ detailData.memory }} GB 内存
          </el-descriptions-item>
          <el-descriptions-item label="公网 IP">{{ detailData.publicIp || '无' }}</el-descriptions-item>
          <el-descriptions-item label="内网 IP">{{ detailData.privateIp || '无' }}</el-descriptions-item>
          <el-descriptions-item label="地域">{{ regionLabel(detailData.region) }}</el-descriptions-item>
          <el-descriptions-item label="操作系统" v-if="detailData.osName">{{ detailData.osName }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailData.createTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="到期时间">{{ detailData.expireTime || '—' }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <div v-else class="loading-detail">加载中...</div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, ArrowDown } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const resources = ref([])
const selectedResources = ref([])
const detailVisible = ref(false)
const detailData = ref({})
const currentPage = ref(1)
const pageSize = ref(10)
const regions = ref([])
const masterAccounts = ref([])
const filterMasterId = ref('')

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return resources.value.slice(start, start + pageSize.value)
})

const filters = reactive({
  provider: '',
  region: '',
  status: '',
  keyword: ''
})

const stats = reactive({
  total: 0,
  running: 0,
  stopped: 0,
  error: 0
})

const fetchRegions = async () => {
  try {
    const [aliyunItems, tencentItems] = await Promise.all([
      request.get('/dict/items/aliyun_region'),
      request.get('/dict/items/tencent_region')
    ])
    regions.value = [
      ...aliyunItems.map(r => ({ label: '【阿里云】' + r.itemValue, value: r.itemKey })),
      ...tencentItems.map(r => ({ label: '【腾讯云】' + r.itemValue, value: r.itemKey }))
    ]
  } catch (e) {
    console.error('获取区域数据失败', e)
  }
}

const fetchMasterAccounts = async () => {
  try {
    masterAccounts.value = await request.get('/admin/cloud/accounts', { params: { type: 'master' } })
  } catch (e) {
    console.error('获取主账号列表失败', e)
  }
}

const providerLabel = (p) => ({ aliyun: '阿里云', tencent: '腾讯云' }[p] || p)
const providerTag = (p) => ({ aliyun: 'danger', tencent: 'primary' }[p] || 'info')
const regionLabel = (r) => regions.value.find(x => x.value === r)?.label?.replace(/^【[^】]+】\s*/, '') || r || '未知'
const statusLabel = (s) => ({ running: '运行中', stopped: '已停止', error: '异常', active: '正常' }[s] || s)
const statusTag = (s) => ({ running: 'success', stopped: 'info', error: 'danger', active: 'success' }[s] || 'info')
const typeLabel = (t) => ({ ecs: 'ECS', bcc: 'BCC', oss: 'OSS', bos: 'BOS' }[t] || t)

const fetchResources = async () => {
  currentPage.value = 1
  loading.value = true
  try {
    const res = await request.get('/admin/cloud/resources', {
      params: {
        provider: filters.provider || undefined,
        region: filters.region || undefined,
        status: filters.status || undefined,
        keyword: filters.keyword || undefined,
        masterAccountId: filterMasterId.value || undefined
      }
    })
    resources.value = res.resources || []
    if (res.stats) {
      Object.assign(stats, res.stats)
    }
  } catch (e) {
    ElMessage.error('获取资源列表失败')
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (val) => {
  selectedResources.value = val
}

const handleAction = async (id, action) => {
  const actionMap = {
    start: { label: '启动', method: 'start' },
    stop: { label: '停止', method: 'stop' },
    restart: { label: '重启', method: 'restart' }
  }

  if (!actionMap[action]) {
    if (action === 'detail') {
      showDetail(resources.value.find(r => r.id === id))
    }
    return
  }

  try {
    await request.post(`/admin/cloud/resources/${id}/${actionMap[action].method}`)
    ElMessage.success(`${actionMap[action].label}指令已发送`)
    fetchResources()
  } catch (e) {
    ElMessage.error(`${actionMap[action].label}失败`)
  }
}

const batchAction = async (action) => {
  const actionLabel = { start: '启动', stop: '停止', restart: '重启' }[action]
  for (const res of selectedResources.value) {
    try {
      await request.post(`/admin/cloud/resources/${res.id}/${action}`)
    } catch (e) {
      // continue with next
    }
  }
  ElMessage.success(`批量${actionLabel}指令已发送`)
  fetchResources()
}

const showDetail = async (row) => {
  detailVisible.value = true
  detailData.value = {}
  try {
    const data = await request.get(`/admin/cloud/resources/${row.id}`)
    detailData.value = data
  } catch (e) {
    detailData.value = { name: row.name, id: row.id, provider: row.provider, type: row.type, status: row.status, ...row }
  }
}

onMounted(() => { fetchRegions(); fetchMasterAccounts(); fetchResources() })
</script>

<style scoped>
.cloud-resources {
  background: transparent;
  border-radius: 12px;
  padding: 20px 24px;
}
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.top-bar-left {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.top-bar-right {
  display: flex;
  gap: 8px;
}
.status-bar {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--app-border-color);
}
.status-tabs :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  gap: 6px;
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: rgba(74, 111, 165, 0.08);
  border-radius: 8px;
  margin-bottom: 12px;
}
.selected-count {
  font-size: 13px;
  color: var(--app-accent-light);
  font-weight: 500;
  margin-right: 8px;
}
.resource-name {
  display: flex;
  align-items: center;
  gap: 8px;
}
.name-text {
  cursor: pointer;
  color: var(--app-accent-light);
  font-weight: 500;
}
.name-text:hover {
  color: var(--app-accent);
}
.provider-tag {
  flex-shrink: 0;
}
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 4px;
}
.status-dot.running { background: #67c23a; }
.status-dot.stopped { background: var(--app-text-muted); }
.status-dot.error { background: #f56c6c; }
.text-muted { color: var(--app-text-muted); }
.hierarchy-account { font-size: 13px; }
.hierarchy-account::before { content: ''; }
.loading-detail {
  text-align: center;
  padding: 60px 0;
  color: var(--app-text-muted);
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
