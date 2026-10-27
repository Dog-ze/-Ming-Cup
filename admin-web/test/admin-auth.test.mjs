import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (file) => readFile(new URL(`../${file}`, import.meta.url), 'utf8')

test('管理员认证 API 使用独立的后台登录、会话校验和退出路径', async () => {
  const source = await read('src/api/admin-auth.js')

  assert.match(source, /\/admin\/auth\/login/)
  assert.match(source, /\/admin\/auth\/me/)
  assert.match(source, /\/admin\/auth\/logout/)
})

test('管理员状态只持久化最小会话资料，并可清除会话', async () => {
  const source = await read('src/stores/admin.js')

  assert.match(source, /expiresAt/)
  assert.match(source, /clearSession\(\)/)
  assert.match(source, /localStorage\.removeItem\(TOKEN_KEY\)/)
  assert.match(source, /localStorage\.removeItem\(PROFILE_KEY\)/)
})

test('路由守卫通过当前管理员接口校验受保护会话', async () => {
  const source = await read('src/router/index.js')

  assert.match(source, /getCurrentAdmin/)
  assert.match(source, /store\.clearSession\(\)/)
  assert.match(source, /query:\s*\{\s*redirect:/)
})

test('登录页提交真实登录请求，成功后保存会话，失败后清空密码', async () => {
  const source = await read('src/views/login/LoginView.vue')

  assert.match(source, /import\s+\{\s*login\s*\}/)
  assert.match(source, /adminStore\.setSession\(result\.token, result\)/)
  assert.match(source, /form\.password\s*=\s*''/)
  assert.match(source, /:loading="loading"/)
})

test('顶部栏退出会调用管理员退出接口，并始终清除本地会话', async () => {
  const source = await read('src/layouts/AdminLayout.vue')

  assert.match(source, /logout\s+as\s+logoutRequest/)
  assert.match(source, /await logoutRequest\(\)/)
  assert.match(source, /adminStore\.clearSession\(\)/)
})

