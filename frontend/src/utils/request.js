import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 是否正在刷新 token 中
let isRefreshing = false
// 等待刷新的请求队列
let refreshQueue = []

const refreshToken = async () => {
  const refreshTokenVal = localStorage.getItem('refreshToken')
  if (!refreshTokenVal) {
    throw new Error('no refresh token')
  }
  const response = await axios.post('/api/auth/refresh', {
    refreshToken: refreshTokenVal
  })
  const result = response.data
  const { token, refreshToken: newRefresh } = result.data || result
  localStorage.setItem('token', token)
  localStorage.setItem('refreshToken', newRefresh)
  return token
}

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    // 解包统一 Result<T> 响应格式
    const result = response.data
    if (result.code === 200) {
      return result.data ?? result
    }
    ElMessage.error(result.message || '请求失败')
    return Promise.reject(new Error(result.message))
  },
  async error => {
    const response = error.response

    // 401 尝试刷新 token
    if (response?.status === 401) {
      const originalRequest = error.config

      // 避免循环刷新
      if (!originalRequest._retry) {
        originalRequest._retry = true

        if (isRefreshing) {
          // 等待刷新完成
          return new Promise((resolve, reject) => {
            refreshQueue.push({ resolve, reject })
          }).then(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            return request(originalRequest)
          })
        }

        isRefreshing = true
        try {
          const newToken = await refreshToken()
          isRefreshing = false

          // 处理排队中的请求
          refreshQueue.forEach(p => p.resolve(newToken))
          refreshQueue = []

          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return request(originalRequest)
        } catch (refreshError) {
          isRefreshing = false
          refreshQueue.forEach(p => p.reject(refreshError))
          refreshQueue = []

          // 刷新失败，清除登录状态并跳转
          localStorage.removeItem('token')
          localStorage.removeItem('refreshToken')
          localStorage.removeItem('userInfo')
          ElMessage.error('登录已过期，请重新登录')

          const currentPath = window.location.pathname + window.location.hash
          if (currentPath.includes('/admin')) {
            router.push('/admin/login')
          } else {
            router.push('/login')
          }

          return Promise.reject(refreshError)
        }
      }
    }

    // 非 401 错误或刷新失败
    const message = response?.data?.message || error.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
