import { request } from '@/utils/request.js'

export function getRecommend(data = {}) {
  return request({ url: '/api/recommend', method: 'GET', data })
}
