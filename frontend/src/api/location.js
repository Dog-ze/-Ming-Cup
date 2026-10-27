import { request } from '@/utils/request.js'

export function reverseGeocode(longitude, latitude) {
  return request({
    url: '/api/location/reverse',
    method: 'GET',
    data: { longitude, latitude }
  })
}
