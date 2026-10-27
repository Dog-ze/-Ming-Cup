import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (file) => readFile(new URL(`../${file}`, import.meta.url), 'utf8')

test('操作日志 API 仅提供分页查询，不提供编辑或删除请求', async () => {
  const source = await read('src/api/admin-operation-logs.js')

  assert.match(source, /request\.get\('\/admin\/operation-logs', \{ params \}\)/)
  assert.doesNotMatch(source, /request\.(post|put|delete)\(/)
})

test('操作日志页面提供筛选、分页与只读摘要弹窗', async () => {
  const source = await read('src/views/logs/OperationLogView.vue')

  assert.match(source, /getOperationLogs/)
  assert.match(source, /adminId/)
  assert.match(source, /dateRange/)
  assert.match(source, /loadLogs/)
  assert.match(source, /formatDetail/)
  assert.match(source, /查看摘要/)
  assert.match(source, /el-pagination/)
  assert.match(source, /el-dialog/)
})

test('操作日志使用真实路由并显示在侧边栏中', async () => {
  const routerSource = await read('src/router/index.js')
  const layoutSource = await read('src/layouts/AdminLayout.vue')

  assert.match(routerSource, /path: '\/operation-logs'/)
  assert.match(routerSource, /OperationLogView\.vue/)
  assert.doesNotMatch(routerSource, /placeholder\('\/operation-logs', '操作日志'\)/)
  assert.match(layoutSource, /Tickets/)
  assert.match(layoutSource, /index="\/operation-logs"/)
  assert.match(layoutSource, /操作日志/)
})
