import { request } from '@/utils/request.js'

export function getQuestionnaire() {
  return request({ url: '/api/questionnaire', method: 'GET' })
}

export function submitQuestionnaire(data) {
  return request({ url: '/api/questionnaire', method: 'POST', data })
}
