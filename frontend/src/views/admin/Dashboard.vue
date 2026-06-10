<template>
  <div class="dashboard">
    <!-- 欢迎头部 -->
    <div class="dash-header">
      <div class="dash-header-text">
        <h2>管理控制台</h2>
        <p>{{ greeting }}，{{ userStore.userInfo?.username || '管理员' }}</p>
      </div>
      <div class="dash-header-badge">
        <el-tag size="small" effect="dark" type="info">总览</el-tag>
      </div>
    </div>
    <!-- 统计卡片 -->
    <div class="stats-grid" v-loading="loading" element-loading-background="rgba(10,10,15,0.8)">
      <div class="stat-card stat-users">
        <div class="stat-glow"></div>
        <div class="stat-icon-wrap"><el-icon :size="24"><User /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.userCount }}</span>
          <span class="stat-label">用户总数</span>
        </div>
      </div>
      <div class="stat-card stat-models">
        <div class="stat-glow"></div>
        <div class="stat-icon-wrap"><el-icon :size="24"><Grid /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.modelCount }}</span>
          <span class="stat-label">模型总数</span>
          <span class="stat-sub">启用 {{ stats.enabledModelCount }}</span>
        </div>
      </div>
      <div class="stat-card stat-chat">
        <div class="stat-glow"></div>
        <div class="stat-icon-wrap"><el-icon :size="24"><ChatDotSquare /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.sessionCount || 0 }}</span>
          <span class="stat-label">对话会话</span>
          <span class="stat-sub">{{ stats.messageCount || 0 }} 条消息</span>
        </div>
      </div>
      <div class="stat-card stat-cloud">
        <div class="stat-glow"></div>
        <div class="stat-icon-wrap"><el-icon :size="24"><Cloudy /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.cloudAccountCount || 0 }}</span>
          <span class="stat-label">云账号</span>
          <span class="stat-sub">启用 {{ stats.enabledCloudAccountCount || 0 }}</span>
        </div>
      </div>
      <div class="stat-card stat-logs">
        <div class="stat-glow"></div>
        <div class="stat-icon-wrap"><el-icon :size="24"><Document /></el-icon></div>
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
        <div class="link-card link-users" @click="$router.push('/admin/users')">
          <div class="link-icon"><el-icon :size="22"><User /></el-icon></div>
          <span>用户管理</span>
        </div>
        <div class="link-card link-roles" @click="$router.push('/admin/roles')">
          <div class="link-icon"><el-icon :size="22"><Avatar /></el-icon></div>
          <span>角色管理</span>
        </div>
        <div class="link-card link-models" @click="$router.push('/admin/models')">
          <div class="link-icon"><el-icon :size="22"><Grid /></el-icon></div>
          <span>模型管理</span>
        </div>
        <div class="link-card link-cloud" @click="$router.push('/admin/cloud/resources')">
          <div class="link-icon"><el-icon :size="22"><Cloudy /></el-icon></div>
          <span>云资源总览</span>
        </div>
        <div class="link-card link-logs" @click="$router.push('/admin/audit-logs')">
          <div class="link-icon"><el-icon :size="22"><Document /></el-icon></div>
          <span>审计日志</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'
import * as echarts from 'echarts'

const userStore = useUserStore()
const loading = ref(false)
const chartLoading = ref(false)
const stats = ref({
  userCount: 0, modelCount: 0, enabledModelCount: 0,
  sessionCount: 0, messageCount: 0,
  auditLogCount: 0, todayLogCount: 0,
  cloudAccountCount: 0, enabledCloudAccountCount: 0
})
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
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
.dashboard {
  padding-top: 4px;
}

/* ===== 欢迎头部 ===== */
.dash-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 28px;
}
.dash-header-text h2 {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 400;
  color: var(--app-text-primary);
  letter-spacing: 1px;
}
.dash-header-text p {
  margin: 0;
  font-size: 13px;
  color: var(--app-text-muted);
  font-weight: 300;
}

/* ===== 统计卡片 ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  position: relative;
  border-radius: 16px;
  padding: 22px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  backdrop-filter: blur(12px);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 16px;
  border: 1px solid;
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  padding: 1px;
  pointer-events: none;
}
.stat-glow {
  position: absolute;
  top: -60%;
  right: -20%;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  filter: blur(50px);
  opacity: 0.15;
  transition: opacity 0.35s ease;
  pointer-events: none;
}
.stat-card:hover .stat-glow {
  opacity: 0.3;
}
.stat-card:hover {
  transform: translateY(-3px);
}

/* 各卡片独立配色 */
.stat-users {
  background: linear-gradient(135deg, rgba(99,102,241,0.08), rgba(99,102,241,0.02));
  border-color: rgba(99,102,241,0.15);
}
.stat-users::before { border-color: rgba(99,102,241,0.15); }
.stat-users .stat-glow { background: #6366f1; }
.stat-users .stat-icon-wrap { background: linear-gradient(135deg, #6366f1, #818cf8); }
.stat-users:hover { border-color: rgba(99,102,241,0.3);
  box-shadow: 0 8px 32px rgba(0,0,0,0.3), 0 0 40px rgba(99,102,241,0.06); }

.stat-models {
  background: linear-gradient(135deg, rgba(16,185,129,0.08), rgba(16,185,129,0.02));
  border-color: rgba(16,185,129,0.15);
}
.stat-models::before { border-color: rgba(16,185,129,0.15); }
.stat-models .stat-glow { background: #10b981; }
.stat-models .stat-icon-wrap { background: linear-gradient(135deg, #10b981, #34d399); }
.stat-models:hover { border-color: rgba(16,185,129,0.3);
  box-shadow: 0 8px 32px rgba(0,0,0,0.3), 0 0 40px rgba(16,185,129,0.06); }

.stat-chat {
  background: linear-gradient(135deg, rgba(139,92,246,0.08), rgba(139,92,246,0.02));
  border-color: rgba(139,92,246,0.15);
}
.stat-chat::before { border-color: rgba(139,92,246,0.15); }
.stat-chat .stat-glow { background: #8b5cf6; }
.stat-chat .stat-icon-wrap { background: linear-gradient(135deg, #8b5cf6, #a78bfa); }
.stat-chat:hover { border-color: rgba(139,92,246,0.3);
  box-shadow: 0 8px 32px rgba(0,0,0,0.3), 0 0 40px rgba(139,92,246,0.06); }

.stat-cloud {
  background: linear-gradient(135deg, rgba(6,182,212,0.08), rgba(6,182,212,0.02));
  border-color: rgba(6,182,212,0.15);
}
.stat-cloud::before { border-color: rgba(6,182,212,0.15); }
.stat-cloud .stat-glow { background: #06b6d4; }
.stat-cloud .stat-icon-wrap { background: linear-gradient(135deg, #06b6d4, #22d3ee); }
.stat-cloud:hover { border-color: rgba(6,182,212,0.3);
  box-shadow: 0 8px 32px rgba(0,0,0,0.3), 0 0 40px rgba(6,182,212,0.06); }

.stat-logs {
  background: linear-gradient(135deg, rgba(245,158,11,0.08), rgba(245,158,11,0.02));
  border-color: rgba(245,158,11,0.15);
}
.stat-logs::before { border-color: rgba(245,158,11,0.15); }
.stat-logs .stat-glow { background: #f59e0b; }
.stat-logs .stat-icon-wrap { background: linear-gradient(135deg, #f59e0b, #fbbf24); }
.stat-logs:hover { border-color: rgba(245,158,11,0.3);
  box-shadow: 0 8px 32px rgba(0,0,0,0.3), 0 0 40px rgba(245,158,11,0.06); }

.stat-icon-wrap {
  width: 48px; height: 48px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  color: #0a0a0f;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}
.stat-info { display: flex; flex-direction: column; min-width: 0; position: relative; z-index: 1; }
.stat-value { font-size: 28px; font-weight: 700; color: var(--app-text-primary); line-height: 1.2; letter-spacing: 1px; }
.stat-label { font-size: 13px; color: var(--app-text-muted); margin-top: 2px; }
.stat-sub { font-size: 12px; color: rgba(255,255,255,0.2); }

/* ===== 图表卡片 ===== */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}
.chart-card-wrapper {
  background: linear-gradient(180deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%);
  border: 1px solid var(--app-border-color);
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(12px);
  transition: all 0.3s ease;
  position: relative;
}
.chart-card-wrapper::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--app-accent), transparent);
  opacity: 0.3;
}
.chart-card-wrapper:hover {
  border-color: var(--app-border-color-hover);
  box-shadow: 0 8px 32px rgba(0,0,0,0.3), 0 0 40px rgba(74, 111, 165, 0.03);
}
.chart-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--app-border-color);
  color: var(--app-accent-light);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.5px;
}
.chart-card-header .el-icon {
  color: var(--app-accent);
}
.chart-card-body {
  padding: 4px;
}
.chart-container { width: 100%; height: 260px; }

/* ===== 快捷入口 ===== */
.quick-links {
  margin-top: 8px;
}
.quick-links h3 {
  margin-bottom: 14px;
  color: var(--app-text-primary);
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
  border-radius: 14px;
  padding: 22px 16px;
  cursor: pointer;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid;
  position: relative;
  overflow: hidden;
}
.link-card::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 14px;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.link-card:hover::after {
  opacity: 1;
}
.link-card:hover {
  transform: translateY(-3px);
}
.link-icon {
  width: 44px; height: 44px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.3s ease;
}
.link-card:hover .link-icon {
  transform: scale(1.1);
}

.link-users {
  background: linear-gradient(135deg, rgba(99,102,241,0.06), rgba(99,102,241,0.01));
  border-color: rgba(99,102,241,0.1);
  color: var(--app-text-muted);
}
.link-users::after { background: linear-gradient(135deg, rgba(99,102,241,0.06), transparent); }
.link-users .link-icon { background: linear-gradient(135deg, #6366f1, #818cf8); color: #0a0a0f; }
.link-users:hover { border-color: rgba(99,102,241,0.25); color: #818cf8;
  box-shadow: 0 8px 24px rgba(99,102,241,0.08); }

.link-roles {
  background: linear-gradient(135deg, rgba(16,185,129,0.06), rgba(16,185,129,0.01));
  border-color: rgba(16,185,129,0.1);
  color: var(--app-text-muted);
}
.link-roles::after { background: linear-gradient(135deg, rgba(16,185,129,0.06), transparent); }
.link-roles .link-icon { background: linear-gradient(135deg, #10b981, #34d399); color: #0a0a0f; }
.link-roles:hover { border-color: rgba(16,185,129,0.25); color: #34d399;
  box-shadow: 0 8px 24px rgba(16,185,129,0.08); }

.link-models {
  background: linear-gradient(135deg, rgba(139,92,246,0.06), rgba(139,92,246,0.01));
  border-color: rgba(139,92,246,0.1);
  color: var(--app-text-muted);
}
.link-models::after { background: linear-gradient(135deg, rgba(139,92,246,0.06), transparent); }
.link-models .link-icon { background: linear-gradient(135deg, #8b5cf6, #a78bfa); color: #0a0a0f; }
.link-models:hover { border-color: rgba(139,92,246,0.25); color: #a78bfa;
  box-shadow: 0 8px 24px rgba(139,92,246,0.08); }

.link-cloud {
  background: linear-gradient(135deg, rgba(6,182,212,0.06), rgba(6,182,212,0.01));
  border-color: rgba(6,182,212,0.1);
  color: var(--app-text-muted);
}
.link-cloud::after { background: linear-gradient(135deg, rgba(6,182,212,0.06), transparent); }
.link-cloud .link-icon { background: linear-gradient(135deg, #06b6d4, #22d3ee); color: #0a0a0f; }
.link-cloud:hover { border-color: rgba(6,182,212,0.25); color: #22d3ee;
  box-shadow: 0 8px 24px rgba(6,182,212,0.08); }

.link-logs {
  background: linear-gradient(135deg, rgba(245,158,11,0.06), rgba(245,158,11,0.01));
  border-color: rgba(245,158,11,0.1);
  color: var(--app-text-muted);
}
.link-logs::after { background: linear-gradient(135deg, rgba(245,158,11,0.06), transparent); }
.link-logs .link-icon { background: linear-gradient(135deg, #f59e0b, #fbbf24); color: #0a0a0f; }
.link-logs:hover { border-color: rgba(245,158,11,0.25); color: #fbbf24;
  box-shadow: 0 8px 24px rgba(245,158,11,0.08); }
</style>
