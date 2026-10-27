import { request } from '@/utils/request.js'
import { BASE_URL } from '@/config/env.js'

export function register(data) {
  return request({ url: '/api/auth/register', method: 'POST', data })
}

export function login(data) {
  return request({ url: '/api/auth/login', method: 'POST', data })
}

export function getProfile() {
  return request({ url: '/api/user/profile' })
}

export function updateProfile(data) {
  return request({ url: '/api/user/profile', method: 'PUT', data })
}

export function uploadAvatar(filePath) {
  const token = uni.getStorageSync('token')

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE_URL}/api/user/avatar`,
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
          uni.showToast({ title: result.msg || '头像上传失败', icon: 'none' })
          reject(result)
        } catch (error) {
          uni.showToast({ title: '头像上传失败，请重试', icon: 'none' })
          reject(error)
        }
      },
      fail: error => {
        uni.showToast({ title: '头像上传失败，请检查网络', icon: 'none' })
        reject(error)
      }
    })
  })
}
