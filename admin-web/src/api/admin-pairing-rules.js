import request from '@/utils/request'

export const getPairingRulePage = (params) => request.get('/admin/pairing-rules', { params })
export const getPairingRule = (id) => request.get(`/admin/pairing-rules/${id}`)
export const createPairingRule = (data) => request.post('/admin/pairing-rules', data)
export const updatePairingRule = (id, data) => request.put(`/admin/pairing-rules/${id}`, data)
export const deletePairingRule = (id) => request.delete(`/admin/pairing-rules/${id}`)
