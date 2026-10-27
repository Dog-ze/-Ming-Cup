import { ref } from 'vue'

/**
 * 登录态的唯一真源。
 *
 * 背景（真实故障）：store 里的 isLoggedIn 原来是
 *   `getters: { isLoggedIn: () => Boolean(uni.getStorageSync('token')) }`
 * Pinia 会用 computed 包装 getter，而 `uni.getStorageSync` 是非响应式的，
 * 于是这个 getter 的依赖集合为空：**首次求值后就被缓存**，之后再写 token
 * 也不会让它失效。表现就是「点登录 → 提示登录成功 → 个人页仍是未登录卡片」，
 * 只能重启 App 才恢复。
 *
 * 所以 token 必须放在响应式状态里：内存里的 ref 负责触发更新，
 * 本地存储负责冷启动恢复。
 */
export const authToken = ref(readStoredToken())
// 每次登录或退出递增，用于识别请求发出后已经被替换的旧会话。
export const authSessionVersion = ref(0)

function readStoredToken() {
  try {
    return String(uni.getStorageSync('token') || '')
  } catch {
    return ''
  }
}

/** 是否已登录。读的是响应式 ref，token 一变所有页面立即同步。 */
export function hasAuthToken() {
  return Boolean(authToken.value)
}

/**
 * 写入登录态。登录成功后必须调用本方法，而不是直接 uni.setStorageSync。
 */
export function setAuthToken(token) {
  authToken.value = String(token || '')
  authSessionVersion.value += 1
  try {
    uni.setStorageSync('token', authToken.value)
  } catch {
    // 存储失败不影响本次运行期内的登录态（内存里仍然是已登录）。
  }
}

/**
 * 清除登录态（退出登录 / 登录失效）。
 */
export function clearAuthToken() {
  authToken.value = ''
  authSessionVersion.value += 1
  try {
    uni.removeStorageSync('token')
  } catch {
    // 忽略：本来就没有 token 时无需处理。
  }
}
