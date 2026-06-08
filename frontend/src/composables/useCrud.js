import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

export function useCrud(url, options = {}) {
  const {
    pageSize = 10,
    defaultForm = () => ({}),
    listField = null,
    idField = 'id'
  } = options

  const data = ref([])
  const loading = ref(false)
  const dialogVisible = ref(false)
  const isEdit = ref(false)
  const currentRow = ref(null)

  const pagination = reactive({
    pageNum: 1,
    pageSize,
    total: 0
  })

  const form = ref(defaultForm())

  async function fetchData() {
    loading.value = true
    try {
      const params = {
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      }
      const res = await request.get(url, { params })
      if (listField) {
        data.value = res[listField] || res
      } else {
        data.value = res.rows || res.list || res.records || res.data || res
      }
      if (res.total !== undefined) {
        pagination.total = res.total
      } else if (Array.isArray(data.value)) {
        pagination.total = data.value.length
      }
    } catch (e) {
      console.error(e)
    } finally {
      loading.value = false
    }
  }

  function openCreate() {
    isEdit.value = false
    currentRow.value = null
    form.value = defaultForm()
    dialogVisible.value = true
  }

  function openEdit(row) {
    isEdit.value = true
    currentRow.value = row
    form.value = { ...row }
    dialogVisible.value = true
  }

  async function submitForm(saveUrl) {
    try {
      if (isEdit.value) {
        await request.put(`${saveUrl || url}/${currentRow.value[idField]}`, form.value)
        ElMessage.success('更新成功')
      } else {
        await request.post(saveUrl || url, form.value)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      pagination.pageNum = 1
      await fetchData()
    } catch (e) {
      // error already handled by request interceptor
    }
  }

  async function deleteRow(row, deleteUrl) {
    try {
      await request.delete(`${deleteUrl || url}/${row[idField]}`)
      ElMessage.success('删除成功')
      await fetchData()
    } catch (e) {
      // error already handled
    }
  }

  return {
    data,
    loading,
    dialogVisible,
    isEdit,
    currentRow,
    pagination,
    form,
    fetchData,
    openCreate,
    openEdit,
    submitForm,
    deleteRow
  }
}
