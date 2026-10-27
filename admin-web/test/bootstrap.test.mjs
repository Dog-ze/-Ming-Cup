import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (file) => readFile(new URL(`../${file}`, import.meta.url), 'utf8')

test('开发服务器固定使用后台管理端端口，并转发 API 请求', async () => {
  const source = await read('vite.config.js')

  assert.match(source, /port:\s*5174/)
  assert.match(source, /strictPort:\s*true/)
  assert.match(source, /['"]\/api['"]/)
  assert.match(source, /127\.0\.0\.1:8080/)
})

test('应用入口注册 Pinia 与路由', async () => {
  const source = await read('src/main.js')

  assert.match(source, /app\.use\(pinia\)/)
  assert.match(source, /app\.use\(router\)/)
})

test('路由包含登录页和受保护的后台功能页', async () => {
  const source = await read('src/router/index.js')

  assert.match(source, /path:\s*'\/login'/)
  assert.match(source, /path:\s*'\/dashboard'/)
  assert.match(source, /requiresAuth:\s*true/)
  assert.match(source, /\/users/)
  assert.match(source, /\/tea-records/)
  assert.match(source, /\/stores/)
})

test('管理员状态使用独立的本地存储键', async () => {
  const source = await read('src/stores/admin.js')

  assert.match(source, /admin_token/)
  assert.match(source, /admin_profile/)
})
