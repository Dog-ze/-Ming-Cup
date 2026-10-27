import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8')

/**
 * 源码用 `@/` 别名导入配置，node 无法直接解析。
 * 这里把 `@/config/env.js` 替换成内联的 data URL 模块，再整体以 data URL 导入。
 */
const toDataUrl = (source) => 'data:text/javascript;base64,' + Buffer.from(source).toString('base64')

async function loadImageUrlModule() {
  const envSource = 'export const BASE_URL = "http://172.19.43.153:8080"'
  const envUrl = toDataUrl(envSource)
  const aliasPattern = /from\s+['"]@\/config\/env\.js['"]/g

  const moduleSource = (await read('src/utils/image-url.js')).replace(
    aliasPattern,
    `from '${envUrl}'`
  )

  return import(toDataUrl(moduleSource))
}

const { resolveImageUrl, toStoredImageUrl } = await loadImageUrlModule()

test('后端保存的相对地址会拼上当前后端地址', () => {
  assert.equal(
    resolveImageUrl('/uploads/admin/tea/longjing.png'),
    'http://172.19.43.153:8080/uploads/admin/tea/longjing.png'
  )
  assert.equal(
    resolveImageUrl('/uploads/avatars/avatar.png'),
    'http://172.19.43.153:8080/uploads/avatars/avatar.png'
  )
})

test('历史遗留的 localhost 绝对地址会改写成当前后端地址', () => {
  // 这正是“后端能显示、手机显示不出来”的根因。
  assert.equal(
    resolveImageUrl('http://localhost:8080/uploads/admin/tea/longjing.png'),
    'http://172.19.43.153:8080/uploads/admin/tea/longjing.png'
  )
  assert.equal(
    resolveImageUrl('http://127.0.0.1:8080/uploads/admin/tea/longjing.png'),
    'http://172.19.43.153:8080/uploads/admin/tea/longjing.png'
  )
  assert.equal(
    resolveImageUrl('http://localhost/uploads/stores/tea-house.webp'),
    'http://172.19.43.153:8080/uploads/stores/tea-house.webp'
  )
})

test('外部绝对地址（如高德地图图片）保持原样', () => {
  const amap = 'https://restapi.amap.com/v3/staticmap?location=1,2'
  assert.equal(resolveImageUrl(amap), amap)
})

test('本地资源与空值不会被拼上后端地址', () => {
  assert.equal(resolveImageUrl(''), '')
  assert.equal(resolveImageUrl(null), '')
  assert.equal(resolveImageUrl(undefined), '')
  assert.equal(resolveImageUrl('/static/images/pairing/fruit.png'), '/static/images/pairing/fruit.png')
  assert.equal(resolveImageUrl('data:image/png;base64,AAAA'), 'data:image/png;base64,AAAA')
  assert.equal(resolveImageUrl('blob:http://localhost/abc'), 'blob:http://localhost/abc')
})

test('写回数据库前会剥掉域名，只保留相对地址', () => {
  assert.equal(
    toStoredImageUrl('http://172.19.43.153:8080/uploads/records/a.png'),
    '/uploads/records/a.png'
  )
  assert.equal(
    toStoredImageUrl('http://localhost:8080/uploads/admin/tea/a.png'),
    '/uploads/admin/tea/a.png'
  )
  assert.equal(toStoredImageUrl('/uploads/records/a.png'), '/uploads/records/a.png')
  assert.equal(toStoredImageUrl(''), '')
})

test('生产/发行构建的后端地址回退不能是 127.0.0.1 或 localhost', async () => {
  const envSource = await read('src/config/env.js')

  // .env.development 只在开发模式生效；发行构建会走这里的回退值。
  // 手机上的 127.0.0.1 指手机自己，回退成它会让接口和图片全部访问不到。
  const fallback = /VITE_API_BASE_URL\s*\|\|\s*'([^']+)'/.exec(envSource)
  assert.ok(fallback, 'env.js 必须提供 VITE_API_BASE_URL 的回退地址')
  assert.doesNotMatch(fallback[1], /(?:localhost|127\.0\.0\.1)/)
  assert.match(fallback[1], /^https?:\/\/\d{1,3}(?:\.\d{1,3}){3}(?::\d+)?$/)
})
