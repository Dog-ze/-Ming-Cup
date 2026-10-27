/**
 * 后台图片地址解析。
 *
 * 后端现在只返回相对地址（如 /uploads/admin/tea/x.png），不再把 host 写进数据库。
 * 后台与后端同源部署（生产由 nginx 同时代理 /api 和 /uploads），因此相对地址直接可用；
 * 但历史数据里仍可能存着 http://localhost:8080/... 这类「电脑本机地址」，
 * 那种地址在别的机器上会指向错误的主机，这里统一改写成相对地址，交给浏览器按同源解析。
 */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// 只保留 API 基址里的协议与主机部分（例如 http://172.19.43.153:8080/api → http://172.19.43.153:8080）。
const BACKEND_ORIGIN = /^https?:\/\//i.test(API_BASE_URL)
  ? API_BASE_URL.replace(/\/api\/?$/i, '').replace(/\/+$/, '')
  : ''

const LOCAL_ORIGIN_PATTERN = /^https?:\/\/(?:localhost|127\.0\.0\.1)(?::\d+)?(?=\/|$)/i

/**
 * 把后端返回的图片地址转换成浏览器可直接访问的地址。
 *
 * - 相对地址（/uploads/...）→ 同源使用；若配置了跨域 API 基址，则拼上后端主机
 * - 历史遗留的 localhost / 127.0.0.1 绝对地址 → 丢掉域名后按上面规则处理
 * - 其它 http(s) 绝对地址与本地资源 → 原样返回
 *
 * @param {string} url 后端返回的图片地址
 * @returns {string} 可直接用于 <img :src> 的地址
 */
export function resolveAdminImageUrl(url) {
  if (!url || typeof url !== 'string') return ''

  const value = url.trim()
  if (!value) return ''

  let path = value

  const localMatch = LOCAL_ORIGIN_PATTERN.exec(value)
  if (localMatch) {
    path = value.slice(localMatch[0].length)
    if (!path.startsWith('/')) path = `/${path}`
  }

  if (/^https?:\/\//i.test(path) || path.startsWith('//')) return path
  if (/^(?:data|blob|file):/i.test(path)) return path
  if (!path.startsWith('/')) return path

  return `${BACKEND_ORIGIN}${path}`
}
