import { request } from '@/utils/request.js'

/**
 * 根据茶类获取日常茶食搭配；不传城市、定位或天气数据。
 */
export function getPairing(params = {}) {
  return request({ url: '/api/pairing', method: 'GET', data: params })
}
