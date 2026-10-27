import { request } from '@/utils/request.js'

export const getPointAccount = () => request({ url: '/api/points/account' })
export const claimDailyPoints = () => request({ url: '/api/points/daily-claim', method: 'POST' })
export const getPointTransactions = (page = 1, pageSize = 20) => request({
  url: '/api/points/transactions',
  data: { page, pageSize }
})
