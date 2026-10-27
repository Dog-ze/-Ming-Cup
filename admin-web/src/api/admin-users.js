import request from '@/utils/request'

export const getUsers = (params) => request.get('/admin/users', { params })
export const getUserDetail = (id) => request.get(`/admin/users/${id}`)
export const updateUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, { status })
export const grantUserPoints = (id, data) => request.post(`/admin/users/${id}/points/grant`, data)
export const getUserPointConsumptions = (id, params) => request.get(`/admin/users/${id}/points/consumptions`, { params })
