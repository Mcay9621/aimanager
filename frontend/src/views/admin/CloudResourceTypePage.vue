<template>
  <div class="cloud-type-page">
    <div class="top-bar">
      <div class="top-bar-left">
        <el-select v-model="filters.provider" placeholder="全部厂商" clearable style="width:130px" @change="fetchData">
          <el-option label="全部厂商" value="" />
          <el-option label="阿里云" value="aliyun" />
          <el-option label="腾讯云" value="tencent" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:120px" @change="fetchData">
          <el-option label="全部状态" value="" />
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="搜索名称..." clearable style="width:180px"
          @clear="fetchData" @keyup.enter="fetchData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="top-bar-right">
        <el-button @click="fetchData" :icon="Refresh">刷新</el-button>
      </div>
    </div>

    <div class="stat-bar">
      <span>共 <b>{{ total }}</b> 条</span>
    </div>

    <el-table :data="list" border stripe v-loading="loading" element-loading-background="rgba(10,10,15,0.8)" style="width:100%"
      :default-sort="{ prop: 'name', order: 'ascending' }">
      <el-table-column type="index" width="50" label="#" />
      <el-table-column v-for="col in allColumns" :key="col" :label="fieldLabels[col] || col" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <!-- 名称列：可点击 -->
          <span v-if="col === 'name'" class="link" @click="showDetail(row)">{{ row.name }}</span>
          <!-- 状态列：带颜色标签 -->
          <el-tag v-else-if="col === 'status'"
            :type="row.status === 'Running' || row.status === 'Available' || row.status === 'Active' ? 'success' : 'info'"
            size="small" effect="dark" round>{{ row.status }}</el-tag>
          <!-- 账号列：层级显示 -->
          <span v-else-if="col === 'accountAlias'" class="text-muted">
            <template v-if="row.masterAlias">{{ row.masterAlias }} / {{ row.accountAlias }}</template>
            <template v-else>{{ row.accountAlias }}</template>
          </span>
          <!-- 创建时间/更新时间/到期时间 -->
          <span v-else-if="col === 'expireTime' || col === 'createTime' || col === 'updateTime'">{{ row[col] || '—' }}</span>
          <!-- IP 列 -->
          <span v-else-if="col === 'publicIp' || col === 'privateIp'">{{ row[col] || '—' }}</span>
          <!-- CPU/内存等数字 -->
          <span v-else-if="col === 'cpu'">{{ row.cpu }}C</span>
          <span v-else-if="col === 'memory'">{{ row.memory }}G</span>
          <span v-else-if="col === 'memoryMb'">{{ row.memoryMb }}MB</span>
          <span v-else-if="col === 'sizeGb' || col === 'storageGb' || col === 'maxStorageGb'">{{ row[col] }}GB</span>
          <span v-else-if="col === 'bandwidth'">{{ row.bandwidth }}Mbps</span>
          <span v-else-if="col === 'timeoutSec'">{{ row.timeoutSec }}s</span>
          <span v-else-if="col === 'codeSize'">{{ row.codeSize }}KB</span>
          <span v-else-if="col === 'nodeCount' && row.nodeCount != null && (row.cpuTotal != null || row.memoryTotal != null)">
            {{ row.nodeCount }}节点
          </span>
          <!-- 布尔字段 -->
          <span v-else-if="col === 'rotationEnabled' || col === 'isDefault' || col === 'publicAccess'">
            <el-tag :type="row[col] === 1 || row[col] === true ? 'success' : 'info'" size="small">
              {{ row[col] === 1 || row[col] === true ? '是' : '否' }}
            </el-tag>
          </span>
          <!-- 默认渲染 -->
          <span v-else>{{ row[col] ?? '—' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <el-drawer v-model="detailVisible" :title="detail?.name || '详情'" size="450px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item v-for="(val, key) in detail" :key="key" :label="fieldLabels[key] || key">
            {{ val ?? '—' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import request from '../../utils/request'

const props = defineProps({ resourceType: { type: String, required: true } })

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const detailVisible = ref(false)
const detail = ref(null)

const filters = reactive({ provider: '', status: '', keyword: '' })

const statusOptions = [
  { value: 'Running', label: '运行中' },
  { value: 'Stopped', label: '已停止' },
  { value: 'Available', label: '可用' },
  { value: 'Active', label: '活动' },
  { value: 'InUse', label: '使用中' },
]

const fieldLabels = {
  name: '名称', provider: '云厂商', region: '地域',
  status: '状态', accountAlias: '所属账号', masterAlias: '主账号',
  createTime: '创建时间', updateTime: '更新时间',
  cpu: 'CPU(核)', memory: '内存(GB)', publicIp: '公网IP', privateIp: '内网IP',
  instanceId: '实例ID', vpcId: 'VPC', subnetId: '子网',
  osName: '操作系统', imageId: '镜像ID', expireTime: '到期时间',
  sizeGb: '容量(GB)', diskType: '磁盘类型', zone: '可用区',
  diskId: '磁盘ID', fileSystemId: '文件系统ID', protocol: '协议',
  storageType: '存储类型', mountPoint: '挂载点', bucketName: '存储桶',
  storageClass: '存储类型', objectCount: '对象数',
  snapshotId: '快照ID', diskSizeGb: '磁盘容量(GB)', engineVersion: '版本',
  spec: '规格', storageGb: '存储(GB)', networkType: '网络类型',
  cidr: 'CIDR', isDefault: '默认VPC',
  peerAddress: '对端地址', publicIps: '公网IP列表',
  bandwidth: '带宽(Mbps)', address: '地址', type: '类型',
  domain: '域名', cname: 'CNAME',
  availableIpCount: '可用IP数',
  securityGroupId: '安全组ID', description: '描述',
  routeTableId: '路由表ID', dcId: '专线ID', circuitCode: '电路编码',
  providerName: '运营商', ldcId: '逻辑专线ID', vlan: 'VLAN',
  dctId: '通道ID', pcId: '对等连接ID', peerVpcId: '对端VPC',
  peerRegion: '对端地域', eniId: '网卡ID', privateIps: '内网IP',
  mac: 'MAC地址', dcgId: '专线网关ID', bastionId: '堡垒机ID',
  licenseCount: '授权数', usedGb: '已用(GB)',
  zoneId: 'Zone ID', recordCount: '记录数', dnsType: 'DNS类型',
  certId: '证书ID', issuer: '颁发机构', algorithm: '算法',
  mode: '模式', domainCount: '防护域名数', ruleCount: '规则数',
  protectionCount: '防护IP数', clusterId: '集群ID', version: '版本',
  nodeCount: '节点数', cpuTotal: 'CPU总量', memoryTotal: '内存总量',
  registryId: '实例ID', registryType: '类型', repoCount: '仓库数',
  scalingGroupId: '伸缩组ID', minSize: '最小实例', maxSize: '最大实例',
  desiredSize: '期望实例', functionId: '函数ID', runtime: '运行时',
  memoryMb: '内存(MB)', timeoutSec: '超时(s)', triggerCount: '触发器数',
  codeSize: '代码(KB)', queueType: '队列类型', topicCount: 'Topic数',
  maxStorageGb: '最大存储(GB)', logsetId: '日志集ID', logsetName: '日志集',
  logTopicCount: '日志主题', retentionDays: '保留天数',
  alarmId: '告警ID', metric: '指标', threshold: '阈值',
  statPeriod: '统计周期(s)', acceleratorId: '加速器ID',
  concurrentConnections: '并发连接', keyId: '密钥ID', keySpec: '密钥规格',
  rotationEnabled: '轮转', taskId: '任务ID', sourceType: '源类型',
  targetType: '目标类型', migrationType: '迁移类型',
  masterAccountId: '主账号ID', accountId: '账号ID',
}

const allColumns = computed(() => {
  if (!list.value.length) return []
  const exclude = ['id', 'resourceType']
  return Object.keys(list.value[0]).filter(k => !exclude.includes(k))
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/cloud/resources', {
      params: {
        type: props.resourceType,
        provider: filters.provider || undefined,
        status: filters.status || undefined,
        keyword: filters.keyword || undefined,
      }
    })
    list.value = res.resources || []
    total.value = list.value.length
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const showDetail = async (row) => {
  detailVisible.value = true
  detail.value = {}
  try {
    const data = await request.get(`/admin/cloud/resources/${row.id}`)
    detail.value = data
  } catch {
    detail.value = row
  }
}

onMounted(fetchData)
watch(() => props.resourceType, fetchData)
</script>

<style scoped>
.cloud-type-page { background:transparent; border-radius:12px; padding:20px 24px; }
.top-bar { display:flex; justify-content:space-between; margin-bottom:16px; flex-wrap:wrap; gap:10px; }
.top-bar-left { display:flex; gap:10px; align-items:center; flex-wrap:wrap; }
.stat-bar { margin-bottom:12px; font-size:13px; color:var(--app-text-muted); }
.stat-bar b { color:var(--app-text-primary); }
.link { cursor:pointer; color:var(--app-accent-light); font-weight:500; }
.link:hover { color:var(--app-accent); }
.text-muted { color:var(--app-text-muted); font-size:12px; }
.pagination-wrapper { display:flex; justify-content:center; margin-top:20px; }
</style>
