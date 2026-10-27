import { request } from '@/utils/request.js'

export function getWeather(data = {}) {
  return request({
    url: '/api/weather',
    method: 'GET',
    data
  })
}