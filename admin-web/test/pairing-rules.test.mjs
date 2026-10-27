import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (file) => readFile(new URL(`../${file}`, import.meta.url), 'utf8')

test('茶食规则 API 覆盖查询、详情、保存和删除', async () => {
  const source = await read('src/api/admin-pairing-rules.js')

  assert.match(source, /request\.get\('\/admin\/pairing-rules', \{ params \}\)/)
  assert.match(source, /request\.get\(`\/admin\/pairing-rules\/\$\{id\}`\)/)
  assert.match(source, /request\.post\('\/admin\/pairing-rules', data\)/)
  assert.match(source, /request\.put\(`\/admin\/pairing-rules\/\$\{id\}`, data\)/)
  assert.match(source, /request\.delete\(`\/admin\/pairing-rules\/\$\{id\}`\)/)
})

test('茶食规则页面支持筛选、食品标签、保存和删除确认', async () => {
  const source = await read('src/views/pairing/PairingRuleView.vue')

  assert.match(source, /getPairingRulePage/)
  assert.match(source, /form\.foods\.push/)
  assert.match(source, /ElMessageBox\.confirm/)
  assert.match(source, /deletePairingRule/)
  assert.match(source, /el-pagination/)
})

test('茶食规则路由使用真实页面而不是占位页', async () => {
  const source = await read('src/router/index.js')

  assert.match(source, /path: '\/pairing-rules'/)
  assert.match(source, /PairingRuleView\.vue/)
})
