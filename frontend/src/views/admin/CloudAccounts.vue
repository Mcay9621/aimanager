<template>
  <div class="cloud-accounts">
    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>{{ activeTab === 'master' ? '添加主账号' : '添加子账号' }}
        </el-button>
        <el-button v-if="activeTab === 'sub'" :loading="testing" @click="handleBatchTest">测试连通性</el-button>
        <el-button @click="fetchAll" :icon="Refresh">刷新</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="主账号管理" name="master">
        <el-table :data="masterPageData" border stripe v-loading="loading" element-loading-background="var(--app-loading-bg)">
          <el-table-column prop="aliasName" label="主账号名称" min-width="200" />
          <el-table-column label="子账号数量" width="120">
            <template #default="{ row }">
              <el-tag type="primary" size="small">{{ subCountMap[row.id] || 0 }} 个</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="viewSubs(row)">查看子账号</el-button>
              <el-button type="primary" size="small" plain @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrapper" v-if="masters.length > 0">
          <el-pagination
            v-model:current-page="masterPage"
            v-model:page-size="masterPageSize"
            :total="masters.length"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="子账号管理" name="sub">
        <div class="filter-bar">
          <el-select v-model="filterMasterId" placeholder="全部主账号" clearable style="width:200px" @change="subPage = 1">
            <el-option v-for="m in masters" :key="m.id" :label="m.aliasName" :value="m.id" />
          </el-select>
          <el-input v-model="searchKeyword" placeholder="搜索子账号..." clearable style="width:240px" @input="subPage = 1">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <el-table :data="subPageData" border stripe v-loading="loading" element-loading-background="var(--app-loading-bg)" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="aliasName" label="账号别名" min-width="140" />
          <el-table-column prop="provider" label="云厂商" width="110">
            <template #default="{ row }">
              <el-tag :type="providerTag(row.provider)" size="small">{{ providerLabel(row.provider) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="accessKey" label="AccessKey ID" min-width="180" show-overflow-tooltip />
          <el-table-column prop="region" label="默认地域" width="130">
            <template #default="{ row }">{{ regionLabel(row.region) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="handleTest(row)">测试</el-button>
              <el-button type="primary" size="small" plain @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrapper" v-if="filteredSubs.length > 0">
          <el-pagination
            v-model:current-page="subPage"
            v-model:page-size="subPageSize"
            :total="filteredSubs.length"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加/编辑 对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item v-if="isSubForm" label="所属主账号" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择主账号" style="width:100%">
            <el-option v-for="m in masters" :key="m.id" :label="m.aliasName" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isSubForm" label="云厂商" prop="provider">
          <el-select v-model="form.provider" placeholder="请选择云厂商" style="width:100%">
            <el-option label="阿里云" value="aliyun" />
            <el-option label="腾讯云" value="tencent" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号名称" prop="aliasName">
          <el-input v-model="form.aliasName" :placeholder="isSubForm ? '例如：生产环境-阿里云' : '例如：生产主账号'" />
        </el-form-item>
        <el-form-item v-if="isSubForm" label="AccessKey ID" prop="accessKey">
          <el-input v-model="form.accessKey" placeholder="例如：LTAI5t..." />
        </el-form-item>
        <el-form-item v-if="isSubForm" label="AccessKey Secret" prop="accessSecret">
          <el-input v-model="form.accessSecret" type="password" show-password
                     :placeholder="isEdit ? '不修改请留空' : '请输入 Secret'" />
        </el-form-item>
        <el-form-item v-if="isSubForm" label="默认地域" prop="region">
          <el-select v-model="form.region" placeholder="选择地域" style="width:100%" filterable>
            <el-option v-for="r in regions" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const testing = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const isEdit = ref(false)
const allAccounts = ref([])
const selectedAccounts = ref([])
const searchKeyword = ref('')
const activeTab = ref('master')
const filterMasterId = ref(null)
const masterPage = ref(1)
const masterPageSize = ref(10)
const subPage = ref(1)
const subPageSize = ref(10)

const form = reactive({
  id: null,
  type: 'sub',
  parentId: null,
  provider: '',
  aliasName: '',
  accessKey: '',
  accessSecret: '',
  region: ''
})

// ===== 派生数据 =====
const masters = computed(() => allAccounts.value.filter(a => a.type === 'master'))
const subs = computed(() => allAccounts.value.filter(a => a.type === 'sub'))

const subCountMap = computed(() => {
  const map = {}
  subs.value.forEach(s => {
    if (s.parentId) map[s.parentId] = (map[s.parentId] || 0) + 1
  })
  return map
})

const isSubForm = computed(() => form.type === 'sub')

const formRules = computed(() => {
  if (isSubForm.value) {
    return {
      parentId: [{ required: true, message: '请选择所属主账号', trigger: 'change' }],
      provider: [{ required: true, message: '请选择云厂商', trigger: 'change' }],
      aliasName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }],
      accessKey: [{ required: true, message: '请输入 AccessKey ID', trigger: 'blur' }],
      accessSecret: [{ required: true, message: '请输入 AccessKey Secret', trigger: 'blur' }]
    }
  }
  return {
    aliasName: [{ required: true, message: '请输入账号名称', trigger: 'blur' }]
  }
})

const filteredSubs = computed(() => {
  let result = subs.value
  if (filterMasterId.value) {
    result = result.filter(s => s.parentId === filterMasterId.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(s =>
      s.aliasName?.toLowerCase().includes(kw) ||
      s.accessKey?.toLowerCase().includes(kw) ||
      s.provider?.toLowerCase().includes(kw)
    )
  }
  return result
})

const masterPageData = computed(() => {
  const start = (masterPage.value - 1) * masterPageSize.value
  return masters.value.slice(start, start + masterPageSize.value)
})

const subPageData = computed(() => {
  const start = (subPage.value - 1) * subPageSize.value
  return filteredSubs.value.slice(start, start + subPageSize.value)
})

// ===== 区域数据 =====
const regions = ref([])

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

const providerLabel = (p) => ({ aliyun: '阿里云', tencent: '腾讯云', baidu: '百度云' }[p] || p)
const providerTag = (p) => ({ aliyun: 'danger', tencent: 'primary', baidu: 'warning' }[p] || 'info')
const regionLabel = (r) => regions.value.find(x => x.value === r)?.label?.replace(/^【[^】]+】\s*/, '') || r || '未设置'

// ===== 数据加载 =====
const fetchAll = async () => {
  loading.value = true
  try {
    allAccounts.value = await request.get('/admin/cloud/accounts')
  } catch (e) {
    ElMessage.error('获取账号列表失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  selectedAccounts.value = []
}

const viewSubs = (master) => {
  filterMasterId.value = master.id
  activeTab.value = 'sub'
  subPage.value = 1
}

// ===== CRUD =====
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = activeTab.value === 'master' ? '添加主账号' : '添加子账号'
  Object.assign(form, {
    id: null,
    type: activeTab.value === 'master' ? 'master' : 'sub',
    parentId: null,
    provider: '',
    aliasName: '',
    accessKey: '',
    accessSecret: '',
    region: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = row.type === 'master' ? '编辑主账号' : '编辑子账号'
  Object.assign(form, {
    id: row.id,
    type: row.type,
    parentId: row.parentId || null,
    provider: row.provider || '',
    aliasName: row.aliasName || '',
    accessKey: row.accessKey || '',
    accessSecret: '',
    region: row.region || ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }

  const submitData = { ...form }
  // 编辑时 secret 留空表示不修改
  if (isEdit.value && !submitData.accessSecret) {
    delete submitData.accessSecret
  }
  // 主账号不提交无关字段
  if (submitData.type === 'master') {
    delete submitData.parentId
    delete submitData.provider
    delete submitData.accessKey
    delete submitData.accessSecret
    delete submitData.region
  }

  submitting.value = true
  try {
    const api = isEdit.value ? `/admin/cloud/accounts/${form.id}` : '/admin/cloud/accounts'
    const method = isEdit.value ? 'put' : 'post'
    await request[method](api, submitData)
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    fetchAll()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该账号？此操作不可恢复。', '提示', { type: 'warning', confirmButtonText: '确定删除' })
  try {
    await request.delete(`/admin/cloud/accounts/${id}`)
    ElMessage.success('删除成功')
    fetchAll()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

// ===== 测试连通性 =====
const handleTest = async (row) => {
  try {
    const res = await request.post(`/admin/cloud/accounts/${row.id}/test`)
    if (res.connected) {
      ElMessage.success(`「${row.aliasName}」连接成功`)
    } else {
      ElMessage.error(`「${row.aliasName}」${res.message}`)
    }
  } catch (e) {
    ElMessage.error('测试失败：' + (e.response?.data?.message || e.message))
  }
}

const handleBatchTest = async () => {
  if (selectedAccounts.value.length === 0) {
    ElMessage.warning('请选择要测试的账号')
    return
  }
  testing.value = true
  for (const acc of selectedAccounts.value) {
    try {
      const res = await request.post(`/admin/cloud/accounts/${acc.id}/test`)
      if (res.connected) {
        ElMessage.success(`「${acc.aliasName}」连接成功`)
      } else {
        ElMessage.warning(`「${acc.aliasName}」${res.message}`)
      }
    } catch (e) {
      ElMessage.error(`「${acc.aliasName}」测试失败`)
    }
  }
  testing.value = false
}

const handleSelectionChange = (val) => {
  selectedAccounts.value = val
}

onMounted(() => { fetchRegions(); fetchAll() })
</script>

<style scoped>
.cloud-accounts {
  background: transparent;
  border-radius: 12px;
  padding: 24px;
}
.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-left {
  display: flex;
  gap: 8px;
  align-items: center;
}
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .cloud-accounts { padding: 14px; }
  .page-toolbar { flex-direction: column; align-items: flex-start; }
  .toolbar-left { flex-wrap: wrap; }
  .filter-bar { flex-direction: column; }
  .filter-bar .el-input,
  .filter-bar .el-select { width: 100% !important; }
  :deep(.el-table) { min-width: 700px; }
  :deep(.el-table__body-wrapper) { overflow-x: auto; }
}
</style>
