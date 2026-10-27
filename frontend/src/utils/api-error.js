// 401：未授权；40101：旧版登录失效；40103：账号被停用
export const AUTH_ERROR_CODES = [401, 40101, 40103]

/**
 * 判断错误是否表示「登录态已失效」。
 * 仅当错误对象携带 AUTH_ERROR_CODES 中的业务码时返回 true；
 * 网络错误（如 { code: 'NETWORK_ERROR' }）、未定义的错误、
 * 以及普通 Error 实例一律返回 false，
 * 避免短暂的网络失败被误判成需要退出登录。
 */
export function isAuthExpiredError(error) {
  if (!error || typeof error !== 'object') return false
  if (error.isStaleSessionError) return false
  return AUTH_ERROR_CODES.includes(Number(error.code))
}
