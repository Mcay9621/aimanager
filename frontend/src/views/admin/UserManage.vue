<template>
  <div class="page-container">
    <div class="table-card">
      <el-table :data="paginatedData" border stripe v-loading="loading" element-loading-background="rgba(10,10,15,0.8)">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="所属主账号" width="150">
          <template #default="{ row }">
            <span>{{ masterName(row.masterAccountId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button size="small" @click="handleAssignMaster(row)">绑定主账号</el-button>
            <el-button type="primary" size="small" @click="handleAssignRole(row)">分配角色</el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="users.length > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="users.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="400px">
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="role in roles" :key="role.id" :label="role.id" border>
          {{ role.name }} ({{ role.code }})
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRoles" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="masterDialogVisible" title="绑定主账号" width="400px">
      <el-form label-width="100px">
        <el-form-item label="用户">
          <span style="color: var(--app-text-secondary)">{{ masterUser?.username }}</span>
        </el-form-item>
        <el-form-item label="主账号">
          <el-select v-model="selectedMasterId" placeholder="选择主账号" clearable style="width:100%">
            <el-option v-for="m in masters" :key="m.id" :label="m.aliasName" :value="m.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="masterDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveMaster" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const users = ref([])
const roles = ref([])
const masters = ref([])
const loading = ref(false)
const saving = ref(false)
const roleDialogVisible = ref(false)
const selectedRoleIds = ref([])
const currentUser = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)

const masterDialogVisible = ref(false)
const masterUser = ref(null)
const selectedMasterId = ref(null)
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return users.value.slice(start, start + pageSize.value)
})

const masterName = (id) => {
  if (!id) return '—'
  const m = masters.value.find(x => x.id === id)
  return m ? m.aliasName : '未知'
}

const fetchMasters = async () => {
  try {
    masters.value = await request.get('/admin/cloud/accounts', { params: { type: 'master' } })
  } catch (e) { console.error(e) }
}

const handleAssignMaster = async (row) => {
  masterUser.value = row
  selectedMasterId.value = row.masterAccountId || null
  masterDialogVisible.value = true
}

const handleSaveMaster = async () => {
  saving.value = true
  try {
    await request.put(`/admin/users/${masterUser.value.id}/master-account`, {
      masterAccountId: selectedMasterId.value
    })
    masterUser.value.masterAccountId = selectedMasterId.value
    ElMessage.success('主账号分配成功')
    masterDialogVisible.value = false
  } catch (e) {
    ElMessage.error('分配失败')
  } finally {
    saving.value = false
  }
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const data = await request.get('/admin/users')
    users.value = data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchRoles = async () => {
  try {
    roles.value = await request.get('/admin/roles')
  } catch (e) {
    console.error(e)
  }
}

const handleAssignRole = async (row) => {
  currentUser.value = row
  const res = await request.get(`/admin/users/${row.id}/roles`)
  selectedRoleIds.value = res.roleIds || []
  roleDialogVisible.value = true
}

const handleSaveRoles = async () => {
  saving.value = true
  try {
    await request.put(`/admin/users/${currentUser.value.id}/roles`, {
      roleIds: selectedRoleIds.value
    })
    ElMessage.success('角色分配更新成功')
    roleDialogVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

const handleToggleStatus = async (row) => {
  try {
    await request.put(`/admin/users/${row.id}/status`)
    ElMessage.success('状态更新成功')
    row.status = row.status === 1 ? 0 : 1
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
  fetchMasters()
})
</script>

<style scoped>
.table-card {
  background: linear-gradient(180deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%);
  border: 1px solid var(--app-border-color);
  border-radius: 16px;
  padding: 20px;
  backdrop-filter: blur(12px);
}
.el-checkbox {
  margin: 8px 0;
  display: flex;
  width: 100%;
}
.el-checkbox :deep(.el-checkbox__label) {
  color: var(--app-text-secondary);
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
.el-dialog :deep(.el-form-item__label) {
  color: var(--app-text-secondary);
}
</style>
