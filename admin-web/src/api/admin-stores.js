import request from '@/utils/request'

export const getStorePage = (params) => request.get('/admin/pairing-stores', { params })
export const getStoreDetail = (id) => request.get(`/admin/pairing-stores/${id}`)
export const createStore = (data) => request.post('/admin/pairing-stores', data)
export const updateStore = (id, data) => request.put(`/admin/pairing-stores/${id}`, data)
export const updateStoreStatus = (id, status) => request.put(`/admin/pairing-stores/${id}/status`, { status })
export const updateStorePromotion = (id, data) => request.put(`/admin/pairing-stores/${id}/promotion`, data)
export const deleteStore = (id) => request.delete(`/admin/pairing-stores/${id}`)

export function uploadStoreImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/pairing-stores/images', formData)
}
