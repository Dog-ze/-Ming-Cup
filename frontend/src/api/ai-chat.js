import { BASE_URL } from '@/config/env.js'
import { authToken, clearAuthToken } from '@/utils/auth-session.js'
import { clearRecordSession } from '@/utils/record-session.js'
import { clearAiChatSession } from '@/utils/ai-chat-session.js'
import { createSseParser } from '@/utils/sse.mjs'

/**
 * 向流式问茶接口发送问题。
 * H5 用 fetch 边读边显示；其它端使用 uni.request，在请求结束后解析完整事件流。
 */
export function streamAiChat(data, handlers) {
  // #ifdef H5
  return streamInBrowser(data, handlers)
  // #endif

  // #ifndef H5
  return streamInNativeApp(data, handlers)
  // #endif
}

function streamInBrowser(data, handlers) {
  const controller = new AbortController()
  void (async () => {
    try {
      const response = await fetch(`${BASE_URL}/api/ai/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
          ...(authToken.value ? { Authorization: `Bearer ${authToken.value}` } : {})
        },
        body: JSON.stringify(data),
        signal: controller.signal
      })
      if (!response.ok) {
        handleHttpError(response.status, handlers)
        return
      }
      if (!response.body) throw new Error('浏览器不支持流式响应')

      const parser = createSseParser((event) => handlers.onEvent?.(event))
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        parser.push(decoder.decode(value, { stream: true }))
      }
      parser.push(decoder.decode())
      parser.finish()
      handlers.onFinished?.()
    } catch (error) {
      if (error?.name !== 'AbortError') {
        handlers.onError?.('网络连接失败，请稍后重试')
      }
    }
  })()
  return { abort: () => controller.abort() }
}

function streamInNativeApp(data, handlers) {
  const parser = createSseParser((event) => handlers.onEvent?.(event))
  const task = uni.request({
    url: `${BASE_URL}/api/ai/chat/stream`,
    method: 'POST',
    data,
    dataType: 'text',
    header: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(authToken.value ? { Authorization: `Bearer ${authToken.value}` } : {})
    },
    success(response) {
      if (response.statusCode < 200 || response.statusCode >= 300) {
        handleHttpError(response.statusCode, handlers)
        return
      }
      parser.push(String(response.data || ''))
      parser.finish()
      handlers.onFinished?.()
    },
    fail() {
      handlers.onError?.('网络连接失败，请稍后重试')
    }
  })
  return { abort: () => task.abort() }
}

function handleHttpError(statusCode, handlers) {
  if (statusCode === 401) {
    clearAuthToken()
    uni.removeStorageSync('user')
    clearRecordSession()
    clearAiChatSession()
    handlers.onError?.('登录已失效，请重新登录')
    return
  }
  handlers.onError?.('AI 服务暂时不可用，请稍后重试')
}
