<template>
  <div class="dashboard">
    <div class="header">
      <h2>仪表盘</h2>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid" v-loading="loading">
      <div class="stat-card">
        <div class="stat-icon user-icon"><el-icon :size="32"><User /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.userCount }}</span>
          <span class="stat-label">用户总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon model-icon"><el-icon :size="32"><Grid /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.modelCount }}</span>
          <span class="stat-label">模型总数</span>
          <span class="stat-sub">启用 {{ stats.enabledModelCount }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon chat-icon"><el-icon :size="32"><ChatDotSquare /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.sessionCount || 0 }}</span>
          <span class="stat-label">对话会话</span>
          <span class="stat-sub">{{ stats.messageCount || 0 }} 条消息</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon cloud-icon"><el-icon :size="32"><Cloudy /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.cloudAccountCount || 0 }}</span>
          <span class="stat-label">云账号</span>
          <span class="stat-sub">启用 {{ stats.enabledCloudAccountCount || 0 }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon log-icon"><el-icon :size="32"><Document /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.auditLogCount }}</span>
          <span class="stat-label">操作日志</span>
          <span class="stat-sub">今日 {{ stats.todayLogCount }}</span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row" v-if="!loading">
      <el-card class="chart-card" shadow="hover">
        <template #header>
          <span class="chart-title">模型使用分布</span>
        </template>
        <div ref="pieChartRef" class="chart-container" v-loading="chartLoading"></div>
        <el-empty v-if="!chartLoading && modelUsage.length === 0" description="暂无对话数据" :image-size="50" />
      </el-card>
      <el-card class="chart-card" shadow="hover">
        <template #header>
          <span class="chart-title">最近7天消息趋势</span>
        </template>
        <div ref="lineChartRef" class="chart-container" v-loading="chartLoading"></div>
        <el-empty v-if="!chartLoading && activityData.length === 0" description="暂无消息数据" :image-size="50" />
      </el-card>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-links">
      <h3>快捷入口</h3>
      <div class="links-grid">
        <el-card class="link-card" @click="$router.push('/admin/users')" shadow="hover">
          <el-icon :size="24"><User /></el-icon>
          <span>用户管理</span>
        </el-card>
        <el-card class="link-card" @click="$router.push('/admin/roles')" shadow="hover">
          <el-icon :size="24"><Avatar /></el-icon>
          <span>角色管理</span>
        </el-card>
        <el-card class="link-card" @click="$router.push('/admin/models')" shadow="hover">
          <el-icon :size="24"><Grid /></el-icon>
          <span>模型管理</span>
        </el-card>
        <el-card class="link-card" @click="$router.push('/admin/cloud/resources')" shadow="hover">
          <el-icon :size="24"><Cloudy /></el-icon>
          <span>云资源总览</span>
        </el-card>
        <el-card class="link-card" @click="$router.push('/admin/audit-logs')" shadow="hover">
          <el-icon :size="24"><Document /></el-icon>
          <span>审计日志</span>
        </el-card>
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
    pieChart = echarts.init(pieChartRef.value)
    const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9b59b6']
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}' },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: modelUsage.value.map((item, idx) => ({
          ...item,
          itemStyle: { color: colors[idx % colors.length] }
        }))
      }]
    })
  }

  if (activityData.value.length > 0 && lineChartRef.value) {
    lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: {
        type: 'category',
        data: activityData.value.map(d => d.date),
        axisLabel: { fontSize: 12 }
      },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        type: 'line',
        smooth: true,
        data: activityData.value.map(d => d.count),
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.3)' },
            { offset: 1, color: 'rgba(64,158,255,0.05)' }
          ])
        },
        lineStyle: { color: '#409eff', width: 3 },
        itemStyle: { color: '#409eff' }
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
  gap: 20px;
  margin-bottom: 24px;
}
.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 20px rgba(0,0,0,0.08); }
.stat-icon {
  width: 56px; height: 56px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.user-icon { background: linear-gradient(135deg, #409eff, #337ecc); }
.model-icon { background: linear-gradient(135deg, #67c23a, #529b2e); }
.log-icon { background: linear-gradient(135deg, #e6a23c, #cf9236); }
.chat-icon { background: linear-gradient(135deg, #9b59b6, #8e44ad); }
.cloud-icon { background: linear-gradient(135deg, #00bcd4, #0097a7); }
.stat-info { display: flex; flex-direction: column; min-width: 0; }
.stat-value { font-size: 30px; font-weight: 700; color: #1a1a2e; line-height: 1.2; }
.stat-label { font-size: 13px; color: #909399; margin-top: 2px; }
.stat-sub { font-size: 12px; color: #c0c4cc; }
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}
.chart-card :deep(.el-card__body) { padding: 0; }
.chart-title { font-weight: 600; font-size: 15px; color: #1a1a2e; }
.chart-container { width: 100%; height: 260px; }
.quick-links h3 { margin-bottom: 14px; color: #1a1a2e; font-size: 16px; }
.links-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.link-card { cursor: pointer; text-align: center; }
.link-card :deep(.el-card__body) {
  display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 20px;
}
</style>
