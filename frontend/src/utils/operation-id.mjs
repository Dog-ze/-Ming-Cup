/**
 * 为一次用户操作生成可跨网络重试复用的标识。
 * 只使用字母、数字、下划线和连字符，能直接通过后端校验。
 */
export function createOperationId(prefix) {
  const randomPart = Math.random().toString(36).slice(2, 12)
  return `${prefix}_${Date.now().toString(36)}_${randomPart}`
}
