/**
 * 「茶记」列表的本地会话缓存。
 *
 * 四个主页面是常驻的 tab 页，退出登录时页面实例不会被销毁，
 * 列表数据如果只保存在页面内存里，就会跨账号残留（退出后仍能看到上一个账号的茶记）。
 * 因此这里把茶记缓存和「当前是谁登录」绑定：token 一变（退出登录 / 换账号登录），
 * 缓存立即失效，避免把上一个账号的数据展示给下一个人。
 */
const STORAGE_KEY = 'record_list_cache'

function emptySession() {
  return { token: '', records: [], page: 1, total: 0 }
}

function readToken() {
  try {
    return String(uni.getStorageSync('token') || '')
  } catch {
    return ''
  }
}

/**
 * 读取当前登录会话对应的茶记缓存。
 * 未登录、缓存不存在、或缓存属于其它账号时，返回空会话并顺手清掉过期缓存。
 */
export function readRecordSession() {
  const token = readToken()
  if (!token) {
    clearRecordSession()
    return emptySession()
  }

  let cached = null
  try {
    cached = uni.getStorageSync(STORAGE_KEY)
  } catch {
    cached = null
  }

  if (!cached || cached.token !== token || !Array.isArray(cached.records)) {
    // 缓存属于其它账号（或格式已过期）：直接清掉，避免残留别人的数据。
    if (cached && cached.token !== token) clearRecordSession()
    return emptySession()
  }

  return {
    token,
    records: cached.records,
    page: Number(cached.page) || 1,
    total: Number(cached.total) || 0
  }
}

export function writeRecordSession({ records = [], page = 1, total = 0, expectedToken = '' }) {
  const token = readToken()
  // 异步请求返回时，当前账号可能已经切换。此时不能把旧账号的数据
  // 用新账号的 token 写入缓存。
  if (!token || (expectedToken && expectedToken !== token)) return

  try {
    uni.setStorageSync(STORAGE_KEY, {
      token,
      records: Array.isArray(records) ? records : [],
      page: Number(page) || 1,
      total: Number(total) || 0
    })
  } catch {
    // 本地存储写失败只影响缓存，不影响页面已渲染的数据。
  }
}

/**
 * 清空茶记缓存。退出登录、登录成功（换账号）时必须调用。
 */
export function clearRecordSession() {
  try {
    uni.removeStorageSync(STORAGE_KEY)
  } catch {
    // 忽略：缓存本来就不存在时无需处理。
  }
}
