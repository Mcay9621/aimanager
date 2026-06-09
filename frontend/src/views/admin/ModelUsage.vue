<template>
  <div class="usage-page">
    <!-- 余额/状态概览 -->
    <div class="table-card">
      <div class="table-header">
        <h3>模型余额与状态</h3>
        <div class="table-actions">
          <el-tag v-if="lastRefresh" type="info" effect="plain" size="small">
            上次刷新: {{ lastRefresh }}
          </el-tag>
          <el-button size="small" @click="fetchBalances" :loading="loading">刷新状态</el-button>
        </div>
      </div>
      <div class="filter-bar">
        <el-input v-model="searchQuery" placeholder="搜索模型名称..." clearable size="small" style="width:200px" />
        <el-select v-model="typeFilter" placeholder="厂商" clearable size="small" style="width:120px">
          <el-option v-for="t in modelTypes" :key="t.key" :label="t.label" :value="t.key" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="状态" clearable size="small" style="width:110px">
          <el-option label="在线" value="online" />
          <el-option label="离线" value="offline" />
        </el-select>
      </div>
      <el-table :data="filteredBalances" border stripe v-loading="loading" element-loading-background="rgba(10,10,15,0.8)">
        <el-table-column prop="name" label="模型名称" width="140" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <span class="type-badge" :style="{ background: getTypeBg(row.type), color: getTypeColor(row.type) }">
              {{ getTypeLabel(row.type) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.available === true" type="success" size="small">{{ row.latency }}ms</el-tag>
            <el-tag v-else-if="row.available === false" type="danger" size="small">离线</el-tag>
            <el-tag v-else type="warning" size="small">检测中</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sessionCount" label="会话数" width="80" align="center" />
        <el-table-column prop="messageCount" label="消息数" width="80" align="center" />
        <el-table-column label="总 Tokens" width="110" align="center">
          <template #default="{ row }">{{ formatTokens(row.totalTokens) }}</template>
        </el-table-column>
        <el-table-column label="最后使用" width="160">
          <template #default="{ row }">{{ row.lastUsed ? formatTime(row.lastUsed) : '-' }}</template>
        </el-table-column>
        <el-table-column label="余额" min-width="160">
          <template #default="{ row }">
            <div v-if="row.balance?.supported" class="balance-cell">
              <span class="balance-amount" :class="balanceClass(row.balance?.total)">
                ¥{{ row.balance?.total || '0.00' }}
              </span>
            </div>
            <div v-else class="balance-cell">
              <span class="balance-hint">{{ row.balance?.message || '-' }}</span>
              <el-link type="primary" :href="getConsoleUrl(row.type)" target="_blank" :underline="false" size="small">
                前往控制台
              </el-link>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper" v-if="filteredBalances.length > 0">
        <el-pagination
          v-model:current-page="pageNo"
          v-model:page-size="pageSize"
          :total="filteredCount"
          :page-sizes="[15, 30, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          small
        />
      </div>
    </div>

    <!-- 用量统计 -->
    <div class="stats-section">
      <div class="section-header">
        <h3>用量分析</h3>
        <el-radio-group v-model="usageDays" size="small" @change="fetchUsage">
          <el-radio-button :value="7">7 天</el-radio-button>
          <el-radio-button :value="30">30 天</el-radio-button>
          <el-radio-button :value="90">90 天</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-value">{{ formatTokens(summary.totalTokens) }}</div>
            <div class="stat-label">总 Tokens</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-value">{{ summary.totalMessages }}</div>
            <div class="stat-label">总消息数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-value">{{ summary.totalSessions }}</div>
            <div class="stat-label">总会话数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-value">{{ summary.activeModels }}</div>
            <div class="stat-label">活跃模型数</div>
          </div>
        </el-col>
      </el-row>

      <!-- Token 趋势 + Top 模型 -->
      <el-row :gutter="16">
        <el-col :span="16">
          <div class="chart-card">
            <h4>Token 使用趋势</h4>
            <div ref="trendChartRef" class="chart-container"></div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="chart-card">
            <h4>模型 Top 排行榜</h4>
            <div ref="topChartRef" class="chart-container"></div>
          </div>
        </el-col>
      </el-row>

      <!-- DeepSeek 用量 -->
      <div v-if="deepseekUsage.hasDeepSeek" class="chart-card deepseek-card">
        <h4>DeepSeek 用量
          <span v-if="deepseekUsage.source === 'api'" class="subtitle">(来自 DeepSeek 官方 API)</span>
          <span v-else class="subtitle">(本地数据库统计)</span>
        </h4>
        <div v-if="deepseekUsage.note" class="ds-note">{{ deepseekUsage.note }}</div>
        <div>
          <el-row :gutter="16" class="ds-summary">
            <el-col :span="8" v-if="deepseekUsage.totalCost != null">
              <div class="ds-stat">
                <span class="ds-stat-value">¥{{ deepseekUsage.totalCost }}</span>
                <span class="ds-stat-label">总消费金额</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="ds-stat">
                <span class="ds-stat-value">{{ deepseekUsage.totalApiCalls }}</span>
                <span class="ds-stat-label">API 调用次数</span>
              </div>
            </el-col>
            <el-col :span="deepseekUsage.totalCost != null ? 8 : 16">
              <div class="ds-stat">
                <span class="ds-stat-value">{{ formatTokens(deepseekUsage.totalTokens) }}</span>
                <span class="ds-stat-label">总 Tokens</span>
              </div>
            </el-col>
          </el-row>
          <div ref="dsChartRef" class="chart-container" style="height:240px"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'
import { getModelTypes } from '../../utils/modelTypes'

const loading = ref(false)
const balances = ref([])
const lastRefresh = ref('')
const usageDays = ref(7)
const summary = ref({ totalTokens: 0, totalMessages: 0, totalSessions: 0, activeModels: 0 })
const dailyTrend = ref([])
const topModels = ref([])
const deepseekUsage = ref({ hasDeepSeek: false })
const usageLoading = ref(false)

// 筛选
const searchQuery = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const pageNo = ref(1)
const pageSize = ref(15)

// 动态模型类型
const modelTypes = ref([])
const typeMap = ref({})
const typeColorMap = ref({})

const trendChartRef = ref(null)
const topChartRef = ref(null)
const dsChartRef = ref(null)

let trendChart = null
let topChart = null
let dsChart = null

// 从动态类型构建查询映射
function buildTypeMaps(types) {
  const km = {}, cm = {}
  types.forEach(t => { km[t.key] = t.label; cm[t.key] = t.color })
  typeMap.value = km
  typeColorMap.value = cm
}

// 模板中使用的类型辅助函数（已绑定 modelTypes）
const getTypeLabel = (key) => typeMap.value[key] || key
const getTypeColor = (key) => typeColorMap.value[key] || '#888'
const getTypeBg = (key) => getTypeColor(key) + '26'
const getConsoleUrl = (key) => { const t = modelTypes.value.find(t => t.key === key); return t ? t.consoleUrl : '#' }

// 筛选+分页
const filteredList = computed(() => {
  let list = balances.value
  const q = searchQuery.value.toLowerCase().trim()
  if (q) list = list.filter(m => (m.name || '').toLowerCase().includes(q))
  if (typeFilter.value) list = list.filter(m => m.type === typeFilter.value)
  if (statusFilter.value === 'online') list = list.filter(m => m.available === true)
  else if (statusFilter.value === 'offline') list = list.filter(m => m.available === false)
  return list
})
const filteredCount = computed(() => filteredList.value.length)
const filteredBalances = computed(() => {
  const start = (pageNo.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

// 重置页码当筛选变化时
watch([searchQuery, typeFilter, statusFilter], () => { pageNo.value = 1 })

const formatTokens = (val) => {
  if (!val && val !== 0) return '0'
  const n = Number(val)
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
  return n.toLocaleString()
}

const formatTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  if (y === now.getFullYear()) return m + '-' + day
  return y + '-' + m + '-' + day
}

const balanceClass = (total) => {
  if (!total) return 'balance-empty'
  const n = parseFloat(total)
  if (n <= 0) return 'balance-empty'
  if (n < 10) return 'balance-low'
  return 'balance-ok'
}

const fetchBalances = async () => {
  loading.value = true
  try {
    const data = await request.get('/admin/models/balances')
    balances.value = data || []
    lastRefresh.value = new Date().toLocaleTimeString()
  } catch { /* ignore */ }
  loading.value = false
}

const fetchUsage = async () => {
  usageLoading.value = true
  try {
    const data = await request.get('/admin/models/usage?days=' + usageDays.value)
    if (data) {
      summary.value = data.summary || summary.value
      dailyTrend.value = data.dailyTrend || []
      topModels.value = data.topModels || []
    }
  } catch { /* ignore */ }
  usageLoading.value = false
  await nextTick()
  renderCharts()
}

const fetchDeepSeekUsage = async () => {
  try {
    const data = await request.get('/admin/models/usage/deepseek?days=' + usageDays.value)
    deepseekUsage.value = data || { hasDeepSeek: false }
  } catch {
    deepseekUsage.value = { hasDeepSeek: false }
  }
  await nextTick()
  renderDeepSeekChart()
}

const renderCharts = () => {
  // Token 趋势折线图
  if (trendChartRef.value) {
    if (!trendChart) {
      trendChart = echarts.init(trendChartRef.value)
    }
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['Prompt Tokens', 'Completion Tokens', '总 Tokens'], textStyle: { color: '#888' } },
      grid: { left: 50, right: 20, top: 35, bottom: 25 },
      xAxis: {
        type: 'category',
        data: dailyTrend.value.map(d => d.date),
        axisLabel: { color: '#666', fontSize: 11 },
        axisLine: { lineStyle: { color: '#333' } }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#666', fontSize: 11, formatter: v => v >= 1000 ? (v / 1000).toFixed(0) + 'K' : v },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } }
      },
      series: [
        {
          name: 'Prompt Tokens', type: 'line', smooth: true,
          data: dailyTrend.value.map(d => d.prompt_tokens || 0),
          lineStyle: { color: '#4a6fa5', width: 2 },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(74,111,165,0.3)' }, { offset: 1, color: 'rgba(74,111,165,0)' }
          ])},
          symbol: 'none'
        },
        {
          name: 'Completion Tokens', type: 'line', smooth: true,
          data: dailyTrend.value.map(d => d.completion_tokens || 0),
          lineStyle: { color: '#6b8fc9', width: 2 },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(107,143,201,0.3)' }, { offset: 1, color: 'rgba(107,143,201,0)' }
          ])},
          symbol: 'none'
        },
        {
          name: '总 Tokens', type: 'line', smooth: true,
          data: dailyTrend.value.map(d => d.total_tokens || 0),
          lineStyle: { color: '#10b981', width: 1.5, type: 'dashed' },
          symbol: 'none'
        }
      ]
    }, true)
    trendChart.resize()
  }

  // Top 模型条形图
  if (topChartRef.value) {
    if (!topChart) {
      topChart = echarts.init(topChartRef.value)
    }
    const top10 = topModels.value.slice(0, 10)
    topChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 10, right: 60, top: 10, bottom: 5 },
      xAxis: { type: 'value', axisLabel: { color: '#666', fontSize: 10, formatter: v => v >= 1000 ? (v/1000).toFixed(0)+'K' : v }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } } },
      yAxis: {
        type: 'category',
        data: top10.map(m => m.modelName || '未知').reverse(),
        axisLabel: { color: '#ccc', fontSize: 10 },
        axisLine: { show: false },
        axisTick: { show: false }
      },
      series: [{
        type: 'bar',
        data: top10.map(m => m.totalTokens || 0).reverse(),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#4a6fa5' }, { offset: 1, color: '#6b8fc9' }
          ]),
          borderRadius: [0, 4, 4, 0]
        },
        barMaxWidth: 20
      }]
    }, true)
    topChart.resize()
  }
}

const renderDeepSeekChart = () => {
  if (!dsChartRef.value || !deepseekUsage.value.dailyBreakdown) return
  if (!dsChart) {
    dsChart = echarts.init(dsChartRef.value)
  }
  const data = (deepseekUsage.value.dailyBreakdown || []).reverse()
  dsChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['调用次数', '消费金额'], textStyle: { color: '#888' } },
    grid: { left: 50, right: 30, top: 35, bottom: 25 },
    xAxis: {
      type: 'category', data: data.map(d => d.date),
      axisLabel: { color: '#666', fontSize: 11 },
      axisLine: { lineStyle: { color: '#333' } }
    },
    yAxis: [
      { type: 'value', name: '调用次数', axisLabel: { color: '#666' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } } },
      { type: 'value', name: '金额 (¥)', axisLabel: { color: '#666' }, splitLine: { show: false } }
    ],
    series: [
      { name: '调用次数', type: 'bar', data: data.map(d => d.apiCalls || 0), itemStyle: { color: 'rgba(74,111,165,0.7)', borderRadius: [4,4,0,0] }, barMaxWidth: 24 },
      { name: '消费金额', type: 'line', yAxisIndex: 1, data: data.map(d => parseFloat(d.cost) || 0), smooth: true, lineStyle: { color: '#10b981', width: 2 }, symbol: 'none' }
    ]
  }, true)
  dsChart.resize()
}

watch(usageDays, () => {
  fetchUsage()
  fetchDeepSeekUsage()
})

// 自适应
const handleResize = () => {
  [trendChart, topChart, dsChart].forEach(c => c?.resize())
}

onMounted(async () => {
  // 加载动态模型类型
  const types = await getModelTypes()
  modelTypes.value = types
  buildTypeMaps(types)

  await fetchBalances()
  await fetchUsage()
  await fetchDeepSeekUsage()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  [trendChart, topChart, dsChart].forEach(c => c?.dispose())
})
</script>

<style scoped>
.usage-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
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
}
.table-header h3 {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.5px;
  margin: 0;
}
.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.type-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}
.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 14px;
}

.balance-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.balance-amount {
  font-weight: 600;
  font-family: 'SF Mono', Monaco, monospace;
}
.balance-ok { color: var(--el-color-success); }
.balance-low { color: var(--el-color-warning); }
.balance-empty { color: var(--el-color-danger); }
.balance-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.stats-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.section-header h3 {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.5px;
  margin: 0;
}

.stat-cards {
  margin-bottom: 4px;
}
.stat-card {
  background: linear-gradient(180deg, rgba(255,255,255,0.04), rgba(255,255,255,0.01));
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  transition: all 0.3s;
}
.stat-card:hover {
  border-color: rgba(74,111,165,0.2);
  transform: translateY(-1px);
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--accent-light);
  font-family: 'SF Mono', Monaco, monospace;
  margin-bottom: 4px;
}
.stat-label {
  font-size: 13px;
  color: var(--text-muted);
}

.chart-card {
  background: linear-gradient(180deg, rgba(255,255,255,0.04), rgba(255,255,255,0.01));
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}
.chart-card h4 {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  margin: 0 0 12px;
}
.chart-card h4 .subtitle {
  font-weight: 300;
  font-size: 12px;
  color: var(--text-muted);
}
.chart-container {
  width: 100%;
  height: 260px;
}

.deepseek-card {
  margin-top: 0;
}
.ds-error {
  color: var(--el-color-danger);
  font-size: 13px;
  padding: 8px;
}
.ds-note {
  color: var(--text-muted);
  font-size: 12px;
  padding: 0 0 12px;
}
.ds-summary {
  margin-bottom: 16px;
}
.ds-stat {
  text-align: center;
  padding: 12px;
  background: rgba(0,150,255,0.04);
  border-radius: 10px;
  border: 1px solid rgba(0,150,255,0.08);
}
.ds-stat-value {
  display: block;
  font-size: 22px;
  font-weight: 600;
  color: #0096ff;
  font-family: 'SF Mono', Monaco, monospace;
}
.ds-stat-label {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
