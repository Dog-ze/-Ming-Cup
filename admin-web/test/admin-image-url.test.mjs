import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8')

/**
 * 源码用了 Vite 专属的 import.meta.env，node 直接 import 会失败，
 * 这里替换成固定值后再以 data URL 导入。
 */
const toDataUrl = (source) => 'data:text/javascript;base64,' + Buffer.from(source).toString('base64')

async function loadImageUrlModule() {
  const source = (await read('src/utils/image-url.js')).replace(
    /import\.meta\.env\.VITE_API_BASE_URL/g,
    'undefined'
  )
  return import(toDataUrl(source))
}

const { resolveAdminImageUrl } = await loadImageUrlModule()

test('后台相对图片地址按同源使用，跨域配置时拼上后端主机', () => {
  // 未配置 VITE_API_BASE_URL 时，基址回退为 /api（同源），相对地址保持相对。
  assert.equal(
    resolveAdminImageUrl('/uploads/admin/tea/x.png'),
    '/uploads/admin/tea/x.png'
  )
})

test('历史遗留的 localhost 绝对地址会被改写成相对地址', () => {
  assert.equal(
    resolveAdminImageUrl('http://localhost:8080/uploads/admin/tea/x.png'),
    '/uploads/admin/tea/x.png'
  )
  assert.equal(
    resolveAdminImageUrl('http://127.0.0.1:8080/uploads/admin/tea/x.png'),
    '/uploads/admin/tea/x.png'
  )
})

test('其它绝对地址与空值原样返回', () => {
  assert.equal(resolveAdminImageUrl('https://cdn.example.com/a.png'), 'https://cdn.example.com/a.png')
  assert.equal(resolveAdminImageUrl(''), '')
  assert.equal(resolveAdminImageUrl(null), '')
})

test('两个后台编辑页的图片预览都经过地址解析', async () => {
  const tea = await read('src/views/tea/TeaEditor.vue')
  assert.match(tea, /:src="resolveAdminImageUrl\(form\.imageUrl\)"/)
  assert.match(tea, /import \{ resolveAdminImageUrl \} from '@\/utils\/image-url'/)

  const pairing = await read('src/views/pairing/PairingRuleView.vue')
  assert.match(pairing, /:src="resolveAdminImageUrl\(food\.imageUrl\)"/)
  assert.match(pairing, /import \{ resolveAdminImageUrl \} from '@\/utils\/image-url'/)
})

test('开发服务器把 /uploads 一并代理到后端，图片预览才不依赖绝对地址', async () => {
  const viteConfig = await read('vite.config.js')
  assert.match(viteConfig, /'\/uploads': \{ target: 'http:\/\/127\.0\.0\.1:8080'/)
})
