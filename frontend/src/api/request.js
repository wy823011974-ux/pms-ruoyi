import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('pms_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  response => {
    // blob 类型响应直接透传（如 Excel 导出）
    if (response.config.responseType === 'blob' || response.data instanceof Blob) {
      return response
    }
    const res = response.data
    if (res.code === 200) return res
    if (res.code === 401) { localStorage.removeItem('pms_token'); window.location.href = '/login' }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  error => {
    if (error.response?.status === 401) { localStorage.removeItem('pms_token'); window.location.href = '/login' }
    const msg = error.response?.data?.msg || error.response?.data?.detail || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
export const get = (url, params) => request.get(url, { params })
export const post = (url, data) => request.post(url, data)
export const put = (url, data) => request.put(url, data)
export const del = (url, params) => request.delete(url, { params })
