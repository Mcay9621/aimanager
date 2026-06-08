<template>
  <div class="cloud-topology">
    <div class="top-bar">
      <h2>资源拓扑图</h2>
      <div class="top-actions">
        <el-select v-model="masterId" placeholder="选择主账号" style="width:180px" @change="loadTopology">
          <el-option v-for="m in masters" :key="m.id" :label="m.aliasName" :value="m.id" />
        </el-select>
        <el-button @click="loadTopology" :icon="Refresh">刷新</el-button>
        <el-tag v-if="statsText" type="info" effect="plain">{{ statsText }}</el-tag>
      </div>
    </div>

    <div class="graph-wrapper" ref="wrapperRef">
      <div id="g6-container" ref="containerRef"></div>
      <div v-if="!loaded && !loading" class="empty-hint">请选择主账号查看拓扑</div>
      <div v-if="loading" class="loading-overlay">
        <el-icon class="loading-icon" :size="32"><Loading /></el-icon>
        <span>生成拓扑中...</span>
      </div>
      <div v-show="tooltip.visible" class="topo-tooltip" :style="tooltip.style">
        <div class="tip-title">{{ tooltip.title }}</div>
        <div class="tip-row" v-for="(v, k) in tooltip.rows" :key="k">{{ k }}: {{ v }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Loading } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { Graph } from '@antv/g6'

const router = useRouter()
const masters = ref([])
const masterId = ref('')
const containerRef = ref(null)
const wrapperRef = ref(null)
const loading = ref(false)
const loaded = ref(false)
const statsText = ref('')
let graph = null

const tooltip = reactive({
  visible: false,
  style: { left: '0px', top: '0px' },
  title: '',
  rows: [],
})

// ========== Icons ==========
function svgDataUri(paths) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">${paths}</svg>`
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
}

const iconSvgs = {
  server: svgDataUri('<rect x="4" y="4" width="16" height="14" rx="2"/><path d="M4 10h16"/><circle cx="7.5" cy="7.5" r="1"/><circle cx="10.5" cy="7.5" r="1"/>'),
  database: svgDataUri('<ellipse cx="12" cy="6" rx="7" ry="3"/><path d="M5 6v12c0 1.7 3.1 3 7 3s7-1.3 7-3V6"/>'),
  cloud: svgDataUri('<path d="M17.5 11a4.5 4.5 0 0 0-8.5-2.5A3.5 3.5 0 0 0 9 16h8.5a3 3 0 0 0 0-6z"/>'),
  network: svgDataUri('<rect x="6" y="4" width="12" height="4" rx="1"/><rect x="4" y="10" width="16" height="4" rx="1"/><rect x="6" y="16" width="12" height="4" rx="1"/><path d="M12 8v2m0 4v2"/>'),
  shield: svgDataUri('<path d="M12 2l7 3v6c0 3.4-2.9 6.7-7 9-4.1-2.3-7-5.6-7-9V5l7-3z"/><path d="M9 12l2 2 4-4"/>'),
  lock: svgDataUri('<rect x="8" y="11" width="8" height="7" rx="2"/><path d="M10 11V8a2 2 0 1 1 4 0v3"/>'),
  disk: svgDataUri('<circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="4"/>'),
  folder: svgDataUri('<path d="M3 8v9a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7l-2-2H5a2 2 0 0 0-2 2z"/>'),
  key: svgDataUri('<circle cx="9" cy="12" r="4"/><path d="M13 12h6v2h-2v2h-2"/><path d="M13 14h2"/>'),
  container: svgDataUri('<rect x="4" y="4" width="16" height="16" rx="2"/><path d="M9 9l6 6m0-6l-6 6"/>'),
  layers: svgDataUri('<path d="M12 2l8 4-8 4-8-4 8-4z"/><path d="M4 10l8 4 8-4"/><path d="M4 16l8 4 8-4"/>'),
  code: svgDataUri('<path d="M14 4l-4 16"/><path d="M17 8l3 4-3 4"/><path d="M7 8l-3 4 3 4"/>'),
  chat: svgDataUri('<path d="M21 13a2 2 0 0 1-2 2H8l-4 4V5a2 2 0 0 1 2-2h13a2 2 0 0 1 2 2z"/>'),
  doc: svgDataUri('<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M8 13h8"/><path d="M8 17h6"/>'),
  chart: svgDataUri('<path d="M18 20V9"/><path d="M12 20V4"/><path d="M6 20v-4"/>'),
  globe: svgDataUri('<circle cx="12" cy="12" r="9"/><path d="M2 12h20"/><path d="M12 2a15 15 0 0 1 0 20 15 15 0 0 1 0-20z"/>'),
  arrow: svgDataUri('<path d="M7 17l10-10m0 0v8m0-8H9"/>'),
  router: svgDataUri('<ellipse cx="12" cy="5" rx="6" ry="2.5"/><path d="M6 5v3c0 1.4 2.7 2.5 6 2.5s6-1.1 6-2.5V5"/><path d="M10 3v1.5M14 7v3"/>'),
  castle: svgDataUri('<rect x="4" y="15" width="16" height="5"/><rect x="7" y="10" width="10" height="5"/><path d="M7 10V6l5-2 5 2v4"/><circle cx="12" cy="13" r="1"/>'),
  camera: svgDataUri('<path d="M12 10a3 3 0 1 0 0 6 3 3 0 0 0 0-6z"/><rect x="3" y="7" width="18" height="11" rx="2"/><path d="M8 7l1-2h6l1 2"/>'),
  sign: svgDataUri('<path d="M12 2v20"/><path d="M5 7h14l-2 3H7L5 7z"/><path d="M7 14h10l-2 3H9l-2-3z"/>'),
  cable: svgDataUri('<path d="M8 5v2a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2V5"/><path d="M8 19v-2a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><rect x="6" y="7" width="12" height="10" rx="1"/>'),
  card: svgDataUri('<rect x="4" y="6" width="16" height="12" rx="2"/><path d="M7 10h3m-3 4h7"/>'),
  router2: svgDataUri('<circle cx="6" cy="12" r="3"/><circle cx="18" cy="12" r="3"/><path d="M9 12h6"/>'),
  pulse: svgDataUri('<path d="M22 12h-4l-3 9L9 3l-3 9H2"/>'),
}

const iconMap = {
  cvm: 'server', bms: 'server',
  mysql: 'database', oracle: 'database', redis: 'database',
  cos: 'cloud', cdn: 'cloud',
  vpc: 'network', subnet: 'network', vpn: 'network',
  eip: 'arrow', clb: 'router',
  sg: 'shield', waf: 'shield', ddos: 'shield',
  ssl: 'lock', kms: 'key',
  cbs: 'disk', snapshot: 'camera', cfs: 'folder',
  nat: 'router', bastion: 'castle', eni: 'card',
  route_table: 'sign', peering: 'router2',
  dc: 'cable', ldc: 'cable', dc_tunnel: 'cable', dc_gateway: 'cable',
  tke: 'container', image_registry: 'layers',
  as: 'pulse', function: 'code', mq: 'chat',
  log_service: 'doc', cloud_monitor: 'chart',
  global_acceleration: 'globe', dts: 'router2',
}

const typeLabels = {
  cvm: 'CVM', bms: 'BMS', eip: 'EIP', clb: 'CLB', cdn: 'CDN',
  vpc: 'VPC', subnet: '子网', cbs: 'CBS', cfs: 'CFS', sg: '安全组',
  nat: 'NAT', bastion: '堡垒机', eni: '网卡', snapshot: '快照',
  vpn: 'VPN', route_table: '路由表', peering: '对等连接',
  dc: '物理专线', ldc: '逻辑专线', dc_tunnel: '专线通道', dc_gateway: '专线网关',
  mysql: 'MySQL', oracle: 'Oracle', redis: 'Redis', cos: 'COS',
  dns: 'DNS', ssl: 'SSL', waf: 'WAF', ddos: 'DDoS', kms: 'KMS',
  tke: 'TKE', image_registry: '镜像仓库', as: '弹性伸缩',
  function: '函数计算', mq: '消息队列', log_service: '日志服务',
  cloud_monitor: '云监控', global_acceleration: '全球加速', dts: 'DTS',
}

const colorMap = {
  cvm: '#409eff', bms: '#409eff', eip: '#00d2d3', clb: '#9b59b6', cdn: '#f56c6c',
  vpc: '#5f27cd', subnet: '#5f27cd', cbs: '#ff9f43', cfs: '#54a0ff',
  sg: '#8395a7', nat: '#5f27cd', bastion: '#8395a7', eni: '#00d2d3',
  snapshot: '#8395a7', vpn: '#5f27cd', route_table: '#8395a7',
  peering: '#9b59b6', dc: '#1a1a2e', ldc: '#1a1a2e', dc_tunnel: '#1a1a2e',
  dc_gateway: '#1a1a2e', mysql: '#67c23a', oracle: '#67c23a', redis: '#e6a23c',
  cos: '#00d2d3', dns: '#67c23a', ssl: '#e6a23c', waf: '#f56c6c',
  ddos: '#e74c3c', kms: '#95a5a6', tke: '#409eff', image_registry: '#00d2d3',
  as: '#9b59b6', function: '#f39c12', mq: '#3498db', log_service: '#8395a7',
  cloud_monitor: '#2ecc71', global_acceleration: '#e67e22', dts: '#54a0ff',
}

const fieldLabels = {
  instanceId: '实例ID', cpu: 'CPU', memory: '内存', publicIp: '公网IP',
  privateIp: '内网IP', osName: '系统', region: '地域', status: '状态', vpcId: 'VPC',
  subnetId: '子网', bandwidth: '带宽', domain: '域名', address: '地址',
  diskType: '磁盘类型', sizeGb: '容量(G)', engineVersion: '版本', spec: '规格',
  storageGb: '存储(GB)', networkType: '网络类型', zone: '可用区',
  bucketName: '存储桶', storageClass: '存储类型', objectCount: '对象数',
  cidr: 'CIDR', description: '描述', circuitCode: '电路编码', providerName: '运营商',
  peerVpcId: '对端VPC', peerRegion: '对端地域', mac: 'MAC地址', licenseCount: '授权数',
  mode: '模式', domainCount: '防护域名数', ruleCount: '规则数',
  clusterId: '集群ID', nodeCount: '节点数', version: '版本',
  runtime: '运行时', memoryMb: '内存(MB)', timeoutSec: '超时(s)', codeSize: '代码(KB)',
  queueType: '队列类型', topicCount: 'Topic数', logsetName: '日志集',
  retentionDays: '保留天数', metric: '指标', threshold: '阈值',
  concurrentConnections: '并发连接', keySpec: '密钥规格',
  sourceType: '源类型', targetType: '目标类型', migrationType: '迁移类型',
}

const tooltipFields = [
  'instanceId', 'publicIp', 'privateIp', 'cpu', 'memory', 'osName',
  'region', 'status', 'vpcId', 'subnetId', 'bandwidth', 'diskType',
  'sizeGb', 'domain', 'address', 'engineVersion', 'spec', 'storageGb',
  'cidr', 'nodeCount', 'version', 'topicCount', 'logsetName',
  'metric', 'keySpec', 'sourceType',
]

// ========== Data loading ==========

const loadMasters = async () => {
  try {
    masters.value = await request.get('/admin/cloud/accounts', { params: { type: 'master' } })
    if (masters.value.length > 0 && !masterId.value) {
      masterId.value = masters.value[0].id
      loadTopology()
    }
  } catch {}
}

const loadTopology = async () => {
  if (!masterId.value) return
  loading.value = true
  loaded.value = false
  tooltip.visible = false
  try {
    const res = await request.get('/admin/cloud/resources', { params: { masterAccountId: masterId.value } })
    const resources = res.resources || []
    if (resources.length === 0) { loading.value = false; return }

    const cvmCount = resources.filter(r => r.resourceType === 'cvm').length
    const typeCount = new Set(resources.map(r => r.resourceType)).size
    statsText.value = `${cvmCount} 台 CVM，共 ${resources.length} 个资源，${typeCount} 类`

    await nextTick()
    buildGraph(resources)
    loaded.value = true
  } catch (e) { console.error(e) }
  loading.value = false
}

// ========== Graph building ==========

const buildGraph = (resources) => {
  const container = containerRef.value
  if (!container) return
  const width = container.clientWidth || 800
  const height = container.clientHeight || 500
  if (graph) { graph.destroy(); graph = null }

  // ---- Group by type — one node per type ----
  const typeGroups = {}
  resources.forEach(r => {
    const t = r.resourceType
    if (!typeGroups[t]) typeGroups[t] = []
    typeGroups[t].push(r)
  })

  const types = Object.keys(typeGroups)
  statsText.value = `${types.length} 类，共 ${resources.length} 个资源`

  // ---- Column layout (左/中/右) ----
  const colMap = {
    vpc: 0, subnet: 0, sg: 0, nat: 0, vpn: 0, route_table: 0,
    peering: 0, dc: 0, ldc: 0, dc_tunnel: 0, dc_gateway: 0,
    cvm: 1, bms: 1, clb: 1, tke: 1, as: 1, function: 1, mq: 1, image_registry: 1,
  }
  const colX = [150, 430, 710]
  const colCount = [0, 0, 0]

  const nodes = types.map(type => {
    const items = typeGroups[type]
    const count = items.length
    const typeLabel = typeLabels[type] || type
    const iconKey = iconMap[type] || 'server'
    const detailText = count === 1 ? (items[0].name || items[0].id) : `${count} 个`
    const c = colMap[type] !== undefined ? colMap[type] : 2
    const idx = colCount[c]++
    return {
      id: type,
      x: colX[c],
      y: 60 + idx * 80,
      type: 'rect',
      data: { resourceType: type, count, sample: items[0] },
      style: {
        fill: colorMap[type] || '#8395a7',
        size: [180, 56],
        radius: 8,
        stroke: '#fff',
        lineWidth: 2.5,
        iconSrc: iconSvgs[iconKey],
        iconWidth: 22,
        iconHeight: 22,
        iconX: -72,
        iconY: 0,
        labelText: `${typeLabel}\n${detailText}`,
        labelFill: '#fff',
        labelFontSize: 11,
        labelLineHeight: 16,
        labelTextAlign: 'left',
        labelTextBaseline: 'middle',
        labelPlacement: 'center',
        labelOffsetX: 6,
        labelOffsetY: 0,
        labelWordWrap: true,
        labelMaxWidth: 160,
        cursor: 'pointer',
      },
    }
  })

  // ---- Type-level edges (actual 1-to-1 / 1-to-N / N-to-M) ----
  const edgeSet = new Set()

  const has = t => typeGroups[t] && typeGroups[t].length > 0
  const relate = (tA, tB, fn) => {
    if (!has(tA) || !has(tB)) return
    if (typeGroups[tA].some(a => typeGroups[tB].some(b => fn(a, b)))) {
      edgeSet.add(`${tA}->${tB}`)
    }
  }

  relate('vpc', 'cvm',     (a, b) => !!(a.id && b.vpcId && (a.id === b.vpcId || a.vpcId === b.vpcId)))
  relate('subnet', 'cvm',   (a, b) => !!(a.subnetId && b.subnetId && a.subnetId === b.subnetId))
  relate('sg', 'cvm',       (a, b) => !!(a.vpcId && b.vpcId && a.vpcId === b.vpcId))
  relate('nat', 'cvm',      (a, b) => !!(a.vpcId && b.vpcId && a.vpcId === b.vpcId))
  relate('clb', 'cvm',      (a, b) => !!(a.vpcId && b.vpcId && a.vpcId === b.vpcId))
  relate('eip', 'cvm',      (a, b) => !!(a.instanceId && b.instanceId && a.instanceId === b.instanceId))
  relate('cbs', 'cvm',      (a, b) => !!(a.instanceId && b.instanceId && a.instanceId === b.instanceId))
  relate('eni', 'cvm',      (a, b) => !!(a.instanceId && b.instanceId && a.instanceId === b.instanceId))
  relate('bms', 'cvm',      (a, b) => !!(a.instanceId && b.instanceId && a.instanceId === b.instanceId))
  relate('clb', 'cvm',      (a, b) => !!(a.instanceId && b.instanceId && a.instanceId === b.instanceId))
  relate('cbs', 'snapshot', (a, b) => !!(a.diskId && b.diskId && a.diskId === b.diskId))

  const edges = Array.from(edgeSet).map(s => {
    const [source, target] = s.split('->')
    return { source, target }
  })

  if (nodes.length === 0) return

  // ---- Create graph ----
  graph = new Graph({
    container,
    width,
    height,
    node: { type: 'rect' },
    edge: {
      type: 'line',
      style: { stroke: '#bcc0c8', lineWidth: 1.5, endArrow: false, strokeOpacity: 0.6 },
    },
    behaviors: ['drag-element', 'drag-canvas', 'zoom-canvas'],
    animation: false,
  })

  graph.setData({ nodes, edges })
  graph.render()

  // Position nodes manually (setData x/y may be ignored without a layout)
  nodes.forEach(n => graph.translateElementTo(n.id, [n.x, n.y], false))

  requestAnimationFrame(() => graph.fitView({ padding: 24 }))

  // ---- Hover tooltip ----
  graph.on('node:pointermove', (e) => {
    const nodeId = e.target?.id
    if (!nodeId) return
    const d = graph.getNodeData(nodeId)?.data || {}
    const typeLabel = typeLabels[nodeId] || nodeId
    if (d.count > 1) {
      tooltip.title = `${typeLabel}（${d.count} 个资源）`
      tooltip.rows = [{ key: '数量', val: `${d.count}` }]
    } else if (d.sample) {
      const s = d.sample
      tooltip.title = `${typeLabel}: ${s.name || nodeId}`
      const rows = []
      tooltipFields.forEach(f => {
        if (s[f] && s[f] !== '' && s[f] !== '-') rows.push({ key: fieldLabels[f] || f, val: s[f] })
      })
      tooltip.rows = rows
    }
    const wrapper = wrapperRef.value
    if (wrapper) {
      const rect = wrapper.getBoundingClientRect()
      tooltip.style = { left: `${e.client.x - rect.left + 14}px`, top: `${e.client.y - rect.top - 10}px` }
    }
    tooltip.visible = true
  })

  graph.on('node:pointerleave', () => { tooltip.visible = false })

  // ---- Click → type list page ----
  graph.on('node:click', (e) => {
    const nodeId = e.target?.id
    if (!nodeId) return
    router.push(`/admin/cloud/resources/${nodeId}`)
  })
}

onMounted(loadMasters)
onBeforeUnmount(() => { if (graph) { graph.destroy(); graph = null } })
</script>

<style scoped>
.cloud-topology { height: 100%; display: flex; flex-direction: column; }
.top-bar { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; flex-wrap:wrap; gap:8px; }
.top-bar h2 { font-size:18px; color:var(--text-primary); flex-shrink:0; }
.top-actions { display:flex; gap:12px; align-items:center; flex-wrap:wrap; }
.graph-wrapper { flex:1; background:var(--bg-secondary); border-radius:12px; position:relative; overflow:hidden; min-height:500px; }
#g6-container { width:100%; height:100%; min-height:500px; }
.empty-hint { position:absolute; top:50%; left:50%; transform:translate(-50%,-50%); color:#c0c4cc; font-size:16px; }
.loading-overlay { position:absolute; top:0; left:0; right:0; bottom:0; background:rgba(10,10,15,0.8); display:flex; flex-direction:column; align-items:center; justify-content:center; gap:12px; color:var(--text-muted); }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { from { transform:rotate(0deg) } to { transform:rotate(360deg) } }
.topo-tooltip {
  position:absolute; z-index:999; pointer-events:none;
  background:rgba(26,26,46,0.92); color:#fff; border-radius:8px;
  padding:10px 14px; font-size:12px; line-height:1.6;
  min-width:140px; max-width:260px;
  backdrop-filter:blur(6px); box-shadow:0 4px 20px rgba(0,0,0,0.3);
}
.topo-tooltip .tip-title { font-weight:600; font-size:13px; margin-bottom:4px; padding-bottom:4px; border-bottom:1px solid rgba(255,255,255,0.15); }
.topo-tooltip .tip-row { color:rgba(255,255,255,0.8); font-size:11px; }
</style>
