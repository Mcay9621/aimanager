import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const isAdmin = computed(() => {
    if (!userInfo.value) return false
    return userInfo.value.authorities?.some(a => a === 'ROLE_ADMIN' || a === 'ROLE_SUPER_ADMIN')
  })

  const isSuperAdmin = computed(() => {
    if (!userInfo.value) return false
    return userInfo.value.authorities?.some(a => a === 'ROLE_SUPER_ADMIN')
  })

  const userRole = computed(() => {
    if (!userInfo.value?.authorities) return ''
    if (userInfo.value.authorities.includes('ROLE_SUPER_ADMIN')) return 'SUPER_ADMIN'
    if (userInfo.value.authorities.includes('ROLE_ADMIN')) return 'ADMIN'
    if (userInfo.value.authorities.includes('ROLE_USER')) return 'USER'
    return ''
  })

  const isLoggedIn = computed(() => !!token.value)

  const masterAccountId = computed(() => userInfo.value?.masterAccountId || null)

  const hasCloudAccess = computed(() => isAdmin.value || masterAccountId.value)

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
    axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
  }

  function setUserInfo(info) {
    userInfo.value = info
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      const response = await axios.get('/api/v1/auth/info')
      const result = response.data
      const userData = result.data || result
      // 处理 authorities 可能是数组或对象数组的情况
      const authorities = userData.authorities?.map(a =>
        typeof a === 'object' ? a.authority : a
      ) || []
      setUserInfo({ ...userData, authorities })
    } catch (error) {
      logout()
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    delete axios.defaults.headers.common['Authorization']
  }

  axios.defaults.headers.common['Authorization'] = token.value ? `Bearer ${token.value}` : ''
  return {
    token,
    userInfo,
    isAdmin,
    isSuperAdmin,
    userRole,
    isLoggedIn,
    masterAccountId,
    hasCloudAccess,
    setToken,
    setUserInfo,
    fetchUserInfo,
    logout
  }
})
