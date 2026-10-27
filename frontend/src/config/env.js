/**
 * 后端接口地址。
 *
 * 优先读取 .env.[mode] 中的 VITE_API_BASE_URL；
 * 没有配置时回退到开发机的局域网地址，这样「发行/生产构建」不会意外打包成
 * 127.0.0.1（手机上的 127.0.0.1 指手机自己，会导致接口和图片都访问不到）。
 *
 * 换电脑或换网络时，只需修改 .env.development（真机调试）和这里或 .env.production 的地址。
 */
export const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://172.19.43.153:8080'
