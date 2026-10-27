import { request } from '@/utils/request.js'

/** 请求后端生成某款推荐茶的完整泡法与注意事项。 */
export function getBrewGuide(data) {
  return request({ url: '/api/ai/brew-guides', method: 'POST', data })
}
