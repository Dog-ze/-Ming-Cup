import { BASE_URL } from '@/config/env.js'
import { isAuthExpiredError } from '@/utils/api-error.js'
import { authSessionVersion, authToken, clearAuthToken } from '@/utils/auth-session.js'
import { clearRecordSession } from '@/utils/record-session.js'
import { clearAiChatSession } from '@/utils/ai-chat-session.js'

/**
 * 统一调用后端接口。
 * 游客请求不会携带 Authorization；登录后自动携带 JWT。
 *
 * 失败时 reject 的对象带有明确标记，便于调用方区分两类失败：
 * - 业务失败：{ ...result, code: Number, isApiError: true }
 * - 网络失败：{ code: 'NETWORK_ERROR', isNetworkError: true, error }
 * 调用方可用 utils/api-error.js 的 isAuthExpiredError() 判断是否需要退出登录。
 */
export function request({ url, method = 'GET', data, responseType }) {
  // 读响应式 token：登录成功后立刻生效，不依赖本地存储的读取时机。
  const token = authToken.value
  const sessionVersion = authSessionVersion.value

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      responseType,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: ({ data: result }) => {
        if (result.code === 0) {
          resolve(result.data)
          return
        }

        // 统一转成数值型业务码并标记来源，保留 msg/data 方便既有代码继续读取；
        // 转成 Number 后 isAuthExpiredError() 才能稳定识别 401/40101。
        const errorObj = { ...result, code: Number(result.code), isApiError: true }

        // 只有响应仍属于发起请求时的当前会话，才有资格清理登录态。
        // 这样旧账号晚到的 401 不会误把后来登录的新账号踢下线。
        const isAuthError = isAuthExpiredError(errorObj)
        const belongsToCurrentSession = Boolean(token)
          && token === authToken.value
          && sessionVersion === authSessionVersion.value

        if (isAuthError && !belongsToCurrentSession) {
          errorObj.isStaleSessionError = true
        }

        if (isAuthError && belongsToCurrentSession) {
          // Token 失效后保留游客浏览能力，不在这里强制跳转登录页。
          // 通过 auth-session 清空，响应式 token 与本地存储会同步更新，
          // 各页面的 isLoggedIn 立即变为 false（不会残留已登录界面）。
          clearAuthToken()
          uni.removeStorageSync('user')
          // 连同按账号缓存的茶记列表一起清掉，避免登录页回来后还能看到旧账号的数据。
          clearRecordSession()
          clearAiChatSession()
          const isDisabled = errorObj.code === 40103
          uni.showToast({
            title: isDisabled ? '账号已停用，请联系管理员' : '登录已失效，请重新登录',
            icon: 'none',
          })
        } else if (!isAuthError) {
          uni.showToast({ title: result.msg || '请求失败', icon: 'none' })
        }

        reject(errorObj)
      },
      fail: (error) => {
        // 网络失败不携带 401/40101，避免调用方把短暂断网误判为登录失效。
        uni.showToast({ title: '网络连接失败，请检查网络或后端服务', icon: 'none' })
        reject({ code: 'NETWORK_ERROR', isNetworkError: true, error })
      }
    })
  })
}
