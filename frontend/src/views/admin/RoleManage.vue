<template>
  <div class="page-container">
    <div class="table-card">
      <div class="table-header">
        <h3>角色列表</h3>
        <el-button type="primary" @click="handleAdd">添加角色</el-button>
      </div>
      <el-table :data="paginatedData" border stripe element-loading-background="var(--app-loading-bg)">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="角色名称" />
        <el-table-column prop="code" label="角色编码" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="roles.length > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="roles.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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

const roles = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加角色')
const formRef = ref()
const isEdit = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return roles.value.slice(start, start + pageSize.value)
})

const form = reactive({
  id: null, name: '', code: '', description: ''
})

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const fetchRoles = async () => {
  const data = await request.get('/admin/roles')
  roles.value = data
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '添加角色'
  Object.assign(form, { id: null, name: '', code: '', description: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  const api = isEdit.value ? `/admin/roles/${form.id}` : '/admin/roles'
  const method = isEdit.value ? 'put' : 'post'
  await request[method](api, form)
  ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
  dialogVisible.value = false
  fetchRoles()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该角色？', '提示', { type: 'warning' })
  await request.delete(`/admin/roles/${id}`)
  ElMessage.success('删除成功')
  fetchRoles()
}

onMounted(() => { fetchRoles() })
</script>

<style scoped>
.table-card {
  background: var(--app-bg-glass);
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
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .table-card { padding: 14px; overflow-x: auto; }
  .table-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  :deep(.el-table) { min-width: 600px; }
}
</style>
