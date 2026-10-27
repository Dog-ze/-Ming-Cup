import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

function clearExpiredSession() {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_profile')
  if (window.location.pathname !== '/login') {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.assign(`/login?redirect=${redirect}`)
  }
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body?.code === 0) return body.data
    if ([40101, 40103].includes(body?.code)) clearExpiredSession()
    return Promise.reject(new Error(body?.msg || '服务器未返回有效数据'))
  },
  (error) => {
    if (error.response?.status === 401) clearExpiredSession()
    return Promise.reject(error)
  },
)

export default request
