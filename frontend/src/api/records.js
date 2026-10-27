import { request } from '@/utils/request.js'
import { BASE_URL } from '@/config/env.js'

export function listRecords(page = 1, pageSize = 10) {
  return request({ url: '/api/records', method: 'GET', data: { page, pageSize } })
}

export function getRecord(id) {
  return request({ url: `/api/records/${id}`, method: 'GET' })
}

export function createRecord(data) {
  return request({ url: '/api/records', method: 'POST', data })
}

export function updateRecord(id, data) {
  return request({ url: `/api/records/${id}`, method: 'PUT', data })
}

export function deleteRecord(id) {
  return request({ url: `/api/records/${id}`, method: 'DELETE' })
}

/**
 * 上传一张品茶照片，成功后返回后端可访问的完整图片地址。
 */
export function uploadRecordImage(filePath) {
  const token = uni.getStorageSync('token')

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE_URL}/api/records/images`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: ({ data }) => {
        try {
          const result = JSON.parse(data)
          if (result.code === 0) {
            resolve(result.data.url)
            return
          }
          uni.showToast({ title: result.msg || '图片上传失败', icon: 'none' })
          reject(result)
        } catch (error) {
          uni.showToast({ title: '图片上传失败，请重试', icon: 'none' })
          reject(error)
        }
      },
      fail: error => {
        uni.showToast({ title: '图片上传失败，请检查网络', icon: 'none' })
        reject(error)
      }
    })
  })
}
