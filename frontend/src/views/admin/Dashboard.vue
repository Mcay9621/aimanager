<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stats-grid" v-loading="loading" element-loading-background="rgba(10,10,15,0.8)">
      <div class="stat-card">
        <div class="stat-icon user-icon"><el-icon :size="28"><User /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.userCount }}</span>
          <span class="stat-label">用户总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon model-icon"><el-icon :size="28"><Grid /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.modelCount }}</span>
          <span class="stat-label">模型总数</span>
          <span class="stat-sub">启用 {{ stats.enabledModelCount }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon chat-icon"><el-icon :size="28"><ChatDotSquare /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.sessionCount || 0 }}</span>
          <span class="stat-label">对话会话</span>
          <span class="stat-sub">{{ stats.messageCount || 0 }} 条消息</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon cloud-icon"><el-icon :size="28"><Cloudy /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.cloudAccountCount || 0 }}</span>
          <span class="stat-label">云账号</span>
          <span class="stat-sub">启用 {{ stats.enabledCloudAccountCount || 0 }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon log-icon"><el-icon :size="28"><Document /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.auditLogCount }}</span>
          <span class="stat-label">操作日志</span>
          <span class="stat-sub">今日 {{ stats.todayLogCount }}</span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row" v-if="!loading">
      <div class="chart-card-wrapper">
        <div class="chart-card-header">
          <el-icon><PieChart /></el-icon>
          <span>模型使用分布</span>
        </div>
        <div class="chart-card-body">
          <div ref="pieChartRef" class="chart-container" v-loading="chartLoading" element-loading-background="rgba(10,10,15,0.8)"></div>
          <el-empty v-if="!chartLoading && modelUsage.length === 0" description="暂无对话数据" :image-size="50" />
        </div>
      </div>
      <div class="chart-card-wrapper">
        <div class="chart-card-header">
          <el-icon><TrendCharts /></el-icon>
          <span>最近7天消息趋势</span>
        </div>
        <div class="chart-card-body">
          <div ref="lineChartRef" class="chart-container" v-loading="chartLoading" element-loading-background="rgba(10,10,15,0.8)"></div>
          <el-empty v-if="!chartLoading && activityData.length === 0" description="暂无消息数据" :image-size="50" />
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-links">
      <h3>快捷入口</h3>
      <div class="links-grid">
        <div class="link-card" @click="$router.push('/admin/users')">
          <el-icon :size="22"><User /></el-icon>
          <span>用户管理</span>
        </div>
        <div class="link-card" @click="$router.push('/admin/roles')">
          <el-icon :size="22"><Avatar /></el-icon>
          <span>角色管理</span>
        </div>
        <div class="link-card" @click="$router.push('/admin/models')">
          <el-icon :size="22"><Grid /></el-icon>
          <span>模型管理</span>
        </div>
        <div class="link-card" @click="$router.push('/admin/cloud/resources')">
          <el-icon :size="22"><Cloudy /></el-icon>
          <span>云资源总览</span>
        </div>
        <div class="link-card" @click="$router.push('/admin/audit-logs')">
          <el-icon :size="22"><Document /></el-icon>
          <span>审计日志</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import request from '../../utils/request'
import * as echarts from 'echarts'

const loading = ref(false)
const chartLoading = ref(false)
const stats = ref({
  userCount: 0, modelCount: 0, enabledModelCount: 0,
  sessionCount: 0, messageCount: 0,
  auditLogCount: 0, todayLogCount: 0,
  cloudAccountCount: 0, enabledCloudAccountCount: 0
})
const modelUsage = ref([])
const activityData = ref([])
const pieChartRef = ref(null)
const lineChartRef = ref(null)
let pieChart = null
let lineChart = null

const fetchStats = async () => {
  loading.value = true
  try {
    const [s, usage, activity] = await Promise.all([
      request.get('/admin/dashboard/stats'),
      request.get('/admin/dashboard/model-usage').catch(() => []),
      request.get('/admin/dashboard/activity').catch(() => [])
    ])
    stats.value = s
    modelUsage.value = usage
    activityData.value = activity
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const renderCharts = () => {
  if (modelUsage.value.length > 0 && pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value, null, { renderer: 'canvas' })
    const blueColors = ['#4a6fa5', '#6b8fc9', '#2d4a6e', '#4a80d4', '#6a9be0', '#3a9b9b']
    pieChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)',
        backgroundColor: 'rgba(18,18,26,0.95)',
        borderColor: 'rgba(74,111,165,0.2)',
        borderWidth: 1,
        textStyle: { color: '#fff', fontSize: 12 }
      },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: 'rgba(10,10,15,0.8)', borderWidth: 3 },
        label: {
          show: true,
          formatter: '{b}',
          color: 'rgba(255,255,255,0.8)',
          fontSize: 12
        },
        labelLine: {
          lineStyle: { color: 'rgba(74,111,165,0.2)' }
        },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' },
          itemStyle: { shadowBlur: 20, shadowColor: 'rgba(74,111,165,0.3)' }
        },
        data: modelUsage.value.map((item, idx) => ({
          ...item,
          itemStyle: { color: blueColors[idx % blueColors.length] }
        }))
      }]
    })
  }

  if (activityData.value.length > 0 && lineChartRef.value) {
    lineChart = echarts.init(lineChartRef.value, null, { renderer: 'canvas' })
    lineChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(18,18,26,0.95)',
        borderColor: 'rgba(74,111,165,0.2)',
        borderWidth: 1,
        textStyle: { color: '#fff', fontSize: 12 }
      },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: {
        type: 'category',
        data: activityData.value.map(d => d.date),
        axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 11 },
        axisLine: { lineStyle: { color: 'rgba(74,111,165,0.1)' } },
        axisTick: { lineStyle: { color: 'rgba(74,111,165,0.1)' } }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 11 },
        splitLine: { lineStyle: { color: 'rgba(74,111,165,0.06)', type: 'dashed' } }
      },
      series: [{
        type: 'line',
        smooth: true,
        data: activityData.value.map(d => d.count),
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(74,111,165,0.25)' },
            { offset: 1, color: 'rgba(74,111,165,0.02)' }
          ])
        },
        lineStyle: { color: '#4a6fa5', width: 2.5 },
        itemStyle: { color: '#4a6fa5' },
        symbol: 'circle',
        symbolSize: 6
      }]
    })
  }
}

const handleResize = () => {
  pieChart?.resize()
  lineChart?.resize()
}

onMounted(async () => {
  await fetchStats()
  await nextTick()
  renderCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  lineChart?.dispose()
})
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: linear-gradient(180deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 22px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  backdrop-filter: blur(12px);
  transition: all 0.3s ease;
}
.stat-card:hover {
  border-color: var(--border-color-hover);
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0,0,0,0.4), 0 0 40px rgba(74, 111, 165, 0.04);
}
.stat-icon {
  width: 52px; height: 52px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  color: #0a0a0f;
  flex-shrink: 0;
}
.user-icon { background: linear-gradient(135deg, #4a6fa5, #6b8fc9); }
.model-icon { background: linear-gradient(135deg, #4a80d4, #6a9be0); }
.log-icon { background: linear-gradient(135deg, #2d4a6e, #4a6fa5); }
.chat-icon { background: linear-gradient(135deg, #3a7a7a, #4a6fa5); }
.cloud-icon { background: linear-gradient(135deg, #4a80d4, #7caeff); }
.stat-info { display: flex; flex-direction: column; min-width: 0; }
.stat-value { font-size: 28px; font-weight: 700; color: var(--text-primary); line-height: 1.2; letter-spacing: 1px; }
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 2px; }
.stat-sub { font-size: 12px; color: rgba(255,255,255,0.2); }

/* 图表卡片 */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}
.chart-card-wrapper {
  background: linear-gradient(180deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(12px);
  transition: all 0.3s ease;
}
.chart-card-wrapper:hover {
  border-color: var(--border-color-hover);
  box-shadow: 0 8px 32px rgba(0,0,0,0.3), 0 0 40px rgba(74, 111, 165, 0.03);
}
.chart-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  color: var(--accent-light);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.5px;
}
.chart-card-header .el-icon {
  color: var(--accent);
}
.chart-card-body {
  padding: 4px;
}
.chart-container { width: 100%; height: 260px; }

/* 快捷入口 */
.quick-links h3 {
  margin-bottom: 14px;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 1px;
}
.links-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
.link-card {
  background: linear-gradient(180deg, rgba(255,255,255,0.03) 0%, rgba(255,255,255,0.01) 100%);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: var(--text-muted);
  font-size: 13px;
  transition: all 0.3s ease;
}
.link-card:hover {
  border-color: var(--border-color-hover);
  background: rgba(74, 111, 165, 0.04);
  color: var(--accent-light);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.3);
}
.link-card .el-icon {
  color: var(--text-tertiary);
  transition: color 0.3s;
}
.link-card:hover .el-icon {
  color: var(--accent);
}
</style>
