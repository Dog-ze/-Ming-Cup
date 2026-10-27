import request from '@/utils/request'

export function getDashboard(userTrendDays = 30) {
  return request.get('/admin/dashboard', { params: { userTrendDays } })
}
