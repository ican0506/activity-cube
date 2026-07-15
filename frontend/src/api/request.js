import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('activity_cube_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && typeof data.code !== 'undefined') {
      if (data.code !== 200) {
        ElMessage.error(data.message || '请求失败')
        if (data.code === 401) {
          localStorage.removeItem('activity_cube_token')
          localStorage.removeItem('activity_cube_user')
          if (location.pathname !== '/login') {
            location.href = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`
          }
        }
        return Promise.reject(new Error(data.message || '请求失败'))
      }
      return data.data
    }
    return data
  },
  (error) => {
    ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      localStorage.removeItem('activity_cube_token')
      localStorage.removeItem('activity_cube_user')
      if (location.pathname !== '/login') {
        location.href = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`
      }
    }
    return Promise.reject(error)
  }
)

export default request
