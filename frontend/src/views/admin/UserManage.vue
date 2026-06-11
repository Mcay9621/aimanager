<template>
  <div class="page-container">
    <div class="table-card">
      <el-table :data="paginatedData" border stripe v-loading="loading" element-loading-background="var(--app-loading-bg)">
        <el-table-column prop="id" :label="$t('admin.audit.id')" width="60" />
        <el-table-column prop="username" :label="$t('admin.users.username')" min-width="120" />
        <el-table-column prop="email" :label="$t('admin.users.email')" min-width="160" />
        <el-table-column prop="phone" :label="$t('admin.users.phone')" width="130" />
        <el-table-column :label="$t('admin.users.masterAccount')" width="150">
          <template #default="{ row }">
            <span>{{ masterName(row.masterAccountId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('admin.users.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? $t('admin.users.statusNormal') : $t('admin.users.statusDisabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('admin.users.createTime')" width="170" />
        <el-table-column :label="$t('common.action')" width="280">
          <template #default="{ row }">
            <el-button size="small" @click="handleAssignMaster(row)">{{ $t('admin.users.bindMaster') }}</el-button>
            <el-button type="primary" size="small" @click="handleAssignRole(row)">{{ $t('admin.users.assignRole') }}</el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? $t('admin.users.statusDisabled') : $t('admin.users.statusNormal') }}
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

    <el-dialog v-model="roleDialogVisible" :title="$t('admin.users.assignRole')" width="400px">
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="role in roles" :key="role.id" :label="role.id" border>
          {{ role.name }} ({{ role.code }})
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSaveRoles" :loading="saving">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="masterDialogVisible" :title="$t('admin.users.bindMaster')" width="400px">
      <el-form label-width="100px">
        <el-form-item :label="$t('admin.users.userLabel')">
          <span style="color: var(--text-secondary)">{{ masterUser?.username }}</span>
        </el-form-item>
        <el-form-item :label="$t('admin.users.masterLabel')">
          <el-select v-model="selectedMasterId" :placeholder="$t('admin.users.bindMaster')" clearable style="width:100%">
            <el-option v-for="m in masters" :key="m.id" :label="m.aliasName" :value="m.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="masterDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSaveMaster" :loading="saving">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import request from '../../utils/request'

const { t } = useI18n()

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
  return m ? m.aliasName : t('admin.users.masterUnknown')
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
    ElMessage.success(t('admin.users.masterSaveSuccess'))
    masterDialogVisible.value = false
  } catch (e) {
    ElMessage.error(t('admin.users.masterSaveFailed'))
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
    ElMessage.success(t('admin.users.roleSaveSuccess'))
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
    ElMessage.success(t('admin.users.statusUpdateSuccess'))
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
  background: var(--app-bg-glass);
  border: 1px solid var(--border-color);
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
  color: var(--text-secondary);
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
.el-dialog :deep(.el-form-item__label) {
  color: var(--text-secondary);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .table-card { padding: 14px; overflow-x: auto; }
  :deep(.el-table) { min-width: 1200px; }
  :deep(.el-pagination .el-pagination__sizes) { display: none; }
}
</style>
