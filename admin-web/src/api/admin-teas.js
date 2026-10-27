import request from '@/utils/request'

export const getTeaPage = (params) => request.get('/admin/teas', { params })
export const getTeaDetail = (id) => request.get(`/admin/teas/${id}`)
export const createTea = (data) => request.post('/admin/teas', data)
export const updateTea = (id, data) => request.put(`/admin/teas/${id}`, data)
export const updateTeaStatus = (id, status) => request.put(`/admin/teas/${id}/status`, { status })
export const deleteTea = (id) => request.delete(`/admin/teas/${id}`)
export const getTeaCategories = () => request.get('/admin/teas/categories')
