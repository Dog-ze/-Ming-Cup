import { BASE_URL } from '@/config/env.js'

const REQUEST_BASE_URL = (BASE_URL || '').replace(/\/+$/, '')

// 历史上后台把「电脑本机地址」写进了数据库：
// 电脑浏览器里 localhost 指电脑本身，后台能显示；手机里 localhost 指手机自己，图片一定加载失败。
// 这里丢掉这类地址的域名部分，只保留路径，再由 App 拼上当前后端地址。
const LOCAL_ORIGIN_PATTERN = /^https?:\/\/(?:localhost|127\.0\.0\.1)(?::\d+)?(?=\/|$)/i

// 本地打包资源与浏览器内部地址，不需要拼后端域名。
const LOCAL_ASSET_PREFIXES = ['/static/', 'static/', '/assets/', 'assets/']

/**
 * 把后端返回的图片地址转换成当前设备可访问的完整地址。
 *
 * - 后端保存的相对地址（/uploads/admin/tea/xxx.png）→ 拼上当前 BASE_URL
 * - 历史遗留的 localhost / 127.0.0.1 绝对地址 → 丢掉域名后拼上当前 BASE_URL
 * - 其它 http(s) 绝对地址（如高德地图图片）→ 原样返回
 * - 本地资源（/static、data:、blob:、file:）或空值 → 原样返回
 *
 * @param {string} url 后端返回或本地 import 得到的图片地址
 * @returns {string} 可直接用于 <image :src> 的地址
 */
export function resolveImageUrl(url) {
  if (!url || typeof url !== 'string') return ''

  const value = url.trim()
  if (!value) return ''

  // 本机地址（含历史遗留的 localhost 绝对地址）：一律按当前后端地址重新拼。
  const localMatch = LOCAL_ORIGIN_PATTERN.exec(value)
  if (localMatch) {
    const relativePath = value.slice(localMatch[0].length)
    return `${REQUEST_BASE_URL}${relativePath.startsWith('/') ? relativePath : `/${relativePath}`}`
  }

  // 已经是 http(s) 绝对地址：不再改动。
  if (/^https?:\/\//i.test(value) || value.startsWith('//')) return value

  // 浏览器/客户端内部地址与本地打包资源：原样使用。
  if (/^(?:data|blob|file|content):/i.test(value)) return value
  if (LOCAL_ASSET_PREFIXES.some((prefix) => value.startsWith(prefix))) return value

  // 其余以 / 开头的都是后端相对地址：拼上当前后端地址。
  if (value.startsWith('/')) return `${REQUEST_BASE_URL}${value}`

  return value
}

/**
 * 把图片地址还原成「只含路径」的相对地址，用于写回数据库。
 *
 * 数据库里只应该保存 /uploads/... 这类相对地址；即使来源是历史遗留的
 * localhost 绝对地址，或当前后端地址的绝对地址，写回前也要剥掉域名。
 * 本地打包资源（/static 等）不属于后端图片，原样返回。
 *
 * @param {string} url 图片地址
 * @returns {string} 相对地址；无法识别的值原样返回
 */
export function toStoredImageUrl(url) {
  if (!url || typeof url !== 'string') return url || ''

  const value = url.trim()
  if (!value) return ''

  const localMatch = LOCAL_ORIGIN_PATTERN.exec(value)
  if (localMatch) {
    const relativePath = value.slice(localMatch[0].length)
    return relativePath.startsWith('/') ? relativePath : `/${relativePath}`
  }

  // 当前后端地址的绝对地址：剥掉域名，只留路径。
  if (REQUEST_BASE_URL && value.startsWith(`${REQUEST_BASE_URL}/`)) {
    return value.slice(REQUEST_BASE_URL.length)
  }

  return value
}
