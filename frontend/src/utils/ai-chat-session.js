/** 当天有效、且与登录账号绑定的 AI 问茶临时记录。 */
const STORAGE_KEY = 'ai_chat_session'

function readToken() {
  try {
    return String(uni.getStorageSync('token') || '')
  } catch {
    return ''
  }
}

function dateKey(now) {
  const date = now instanceof Date ? now : new Date(now)
  return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`
}

/** 仅恢复同一账号当天主动保存的聊天记录；其余情况立即清除。 */
export function readAiChatSession({ now = new Date() } = {}) {
  const token = readToken()
  let cached = null
  try {
    cached = uni.getStorageSync(STORAGE_KEY)
  } catch {
    return []
  }

  if (!token || !cached || cached.token !== token || cached.date !== dateKey(now) || !Array.isArray(cached.messages)) {
    if (cached) clearAiChatSession()
    return []
  }

  return cached.messages
}

/** 保存本次聊天到当天午夜前；账号切换或日期变化时读取端会自动失效。 */
export function writeAiChatSession({ messages = [], expectedToken = '', now = new Date() } = {}) {
  const token = readToken()
  if (!token || (expectedToken && expectedToken !== token) || !Array.isArray(messages)) return

  try {
    uni.setStorageSync(STORAGE_KEY, {
      token,
      date: dateKey(now),
      messages
    })
  } catch {
    // 缓存写入失败不影响当前页面中的聊天记录。
  }
}

export function clearAiChatSession() {
  try {
    uni.removeStorageSync(STORAGE_KEY)
  } catch {
    // 缓存不存在或本地存储不可用时无需处理。
  }
}
