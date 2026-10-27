import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (file) => readFile(new URL(`../${file}`, import.meta.url), 'utf8')

test('用户管理 API 覆盖列表、详情和状态更新', async () => {
  const source = await read('src/api/admin-users.js')

  assert.match(source, /request\.get\('\/admin\/users'/)
  assert.match(source, /request\.get\(`\/admin\/users\/\$\{id\}`\)/)
  assert.match(source, /request\.put\(`\/admin\/users\/\$\{id\}\/status`, \{ status \}\)/)
  assert.match(source, /grantUserPoints/)
  assert.match(source, /getUserPointConsumptions/)
  assert.match(source, /\/admin\/users\/\$\{id\}\/points\/consumptions/)
})

test('用户管理页支持筛选、分页、详情和启停确认', async () => {
  const source = await read('src/views/users/UserListView.vue')

  assert.match(source, /关键词、手机号或邮箱/)
  assert.match(source, /el-pagination/)
  assert.match(source, /getUserDetail/)
  assert.match(source, /ElMessageBox\.confirm/)
  assert.match(source, /updateUserStatus/)
  assert.match(source, /pointsBalance/)
  assert.match(source, /积分/)
  assert.match(source, /发放积分/)
  assert.match(source, /消费明细/)
  assert.match(source, /showConsumptions/)
})

test('用户路由使用真实管理页面而不是占位页', async () => {
  const source = await read('src/router/index.js')

  assert.match(source, /path: '\/users'/)
  assert.match(source, /UserListView\.vue/)
})
