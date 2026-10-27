import request from '@/utils/request'

export function uploadAdminImage(scene, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/images', formData, { params: { scene } })
}
