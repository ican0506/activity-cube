import axios from 'axios'
import { ElMessage } from 'element-plus'
import { resolveApiBaseUrl } from './requestConfig'
import { applyAuthHeader, buildLoginRedirect, clearAuthSession } from '../utils/authSession'

const request = axios.create({
  baseURL: resolveApiBaseUrl(import.meta.env),
  timeout: 10000
})

request.interceptors.request.use((config) => {
  return applyAuthHeader(config, localStorage)
})

request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && typeof data.code !== 'undefined') {
      if (data.code !== 200) {
        if (data.code === 401) {
          ElMessage.error('登录已过期，请重新登录')
          clearAuthSession(localStorage)
          if (location.pathname !== '/login') {
            location.href = buildLoginRedirect(location.pathname, location.search)
          }
        } else {
          ElMessage.error(data.message || '请求失败')
        }
        return Promise.reject(new Error(data.message || '请求失败'))
      }
      return data.data
    }
    return data
  },
  (error) => {
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      clearAuthSession(localStorage)
      if (location.pathname !== '/login') {
        location.href = buildLoginRedirect(location.pathname, location.search)
      }
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
