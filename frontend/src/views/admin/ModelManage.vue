<template>
  <div class="model-manage">
    <div class="header">
      <h2>模型管理</h2>
      <el-button type="primary" @click="handleAdd">添加模型</el-button>
    </div>
    <el-table :data="paginatedData" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="模型名称" />
      <el-table-column prop="type" label="类型">
        <template #default="{ row }">
          {{ getTypeName(row.type) }}
        </template>
      </el-table-column>
      <el-table-column prop="modelName" label="模型标识" />
      <el-table-column prop="endpoint" label="API地址" show-overflow-tooltip />
      <el-table-column prop="enabled" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" @click="handleTest(row)" :loading="testingId === row.id">测试</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="models.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="models.length"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option label="OpenAI" value="openai" />
            <el-option label="Anthropic" value="anthropic" />
            <el-option label="阿里云" value="ali" />
            <el-option label="百度" value="baidu" />
            <el-option label="字节跳动" value="byte" />
            <el-option label="腾讯" value="tencent" />
          </el-select>
        </el-form-item>
        <el-form-item label="API地址" prop="endpoint">
          <el-input v-model="form.endpoint" />
        </el-form-item>
        <el-form-item label="API密钥" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password />
        </el-form-item>
        <el-form-item label="模型标识" prop="modelName">
          <el-input v-model="form.modelName" />
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const models = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加模型')
const formRef = ref()
const isEdit = ref(false)
const testingId = ref(null)

const currentPage = ref(1)
const pageSize = ref(10)
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return models.value.slice(start, start + pageSize.value)
})

const form = reactive({
  id: null,
  name: '',
  type: '',
  endpoint: '',
  apiKey: '',
  modelName: '',
  enabled: 1
})

const rules = {
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  modelName: [{ required: true, message: '请输入模型标识', trigger: 'blur' }]
}

const typeMap = {
  openai: 'OpenAI',
  anthropic: 'Anthropic',
  ali: '阿里云',
  baidu: '百度',
  byte: '字节跳动',
  tencent: '腾讯'
}

const getTypeName = (type) => typeMap[type] || type

const fetchModels = async () => {
  const data = await request.get('/admin/models')
  models.value = data
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '添加模型'
  Object.assign(form, {
    id: null,
    name: '',
    type: '',
    endpoint: '',
    apiKey: '',
    modelName: '',
    enabled: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑模型'
  Object.assign(form, {
    ...row,
    apiKey: row.apiKey ? '••••••••' : ''  // 脱敏显示
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()

  // 如果 API 密钥是脱敏字符串，不提交
  const submitData = { ...form }
  if (submitData.apiKey === '••••••••') {
    delete submitData.apiKey
  }

  const api = isEdit.value ? `/admin/models/${form.id}` : '/admin/models'
  const method = isEdit.value ? 'put' : 'post'
  await request[method](api, submitData)
  ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
  dialogVisible.value = false
  fetchModels()
}

const handleTest = async (row) => {
  testingId.value = row.id
  try {
    const res = await request.post(`/admin/models/${row.id}/test`)
    if (res.connected) {
      ElMessage.success(`${row.name} 连接成功`)
    } else {
      ElMessage.warning(res.message || `${row.name} 连接失败`)
    }
  } catch (e) {
    ElMessage.error('测试请求失败')
  } finally {
    testingId.value = null
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该模型？', '提示', { type: 'warning' })
  await request.delete(`/admin/models/${id}`)
  ElMessage.success('删除成功')
  fetchModels()
}

onMounted(() => {
  fetchModels()
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
