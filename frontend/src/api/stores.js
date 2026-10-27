import { request } from '@/utils/request.js'

/**
 * 获取“附近茶食”统一店铺列表。游客和登录用户均可调用。
 */
export function getNearbyStores(params = {}) {
  return request({ url: '/api/pairing/stores/nearby', method: 'GET', data: params })
}
