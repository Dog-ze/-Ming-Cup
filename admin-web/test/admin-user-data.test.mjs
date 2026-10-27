import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (file) => readFile(new URL(`../${file}`, import.meta.url), 'utf8')

test('用户数据 API 覆盖汇总、品茶记录、问卷和统计查询', async () => {
  const source = await read('src/api/admin-user-data.js')

  assert.match(source, /request\.get\('\/admin\/user-data', \{ params \}\)/)
  assert.match(source, /request\.get\(`\/admin\/user-data\/\$\{userId\}\/tea-records`, \{ params \}\)/)
  assert.match(source, /request\.get\(`\/admin\/user-data\/\$\{userId\}\/questionnaire`\)/)
  assert.match(source, /request\.get\('\/admin\/user-data\/questionnaire-statistics', \{ params \}\)/)
})

test('用户数据页提供汇总表、两个查看弹窗和问卷统计抽屉', async () => {
  const source = await read('src/views/data/UserDataOverviewView.vue')

  assert.match(source, /品茶记录数/)
  assert.match(source, /是否填写问卷/)
  assert.match(source, /查看品茶记录/)
  assert.match(source, /查看问卷/)
  assert.match(source, /getUserTeaRecords/)
  assert.match(source, /getUserQuestionnaire/)
  assert.match(source, /getQuestionnaireStatistics/)
  assert.match(source, /el-dialog/)
  assert.match(source, /el-drawer/)
})

test('用户数据使用新路由，旧品茶记录和问卷地址会跳转到该页面', async () => {
  const source = await read('src/router/index.js')

  assert.match(source, /path: '\/user-data'/)
  assert.match(source, /UserDataOverviewView\.vue/)
  assert.match(source, /path: '\/tea-records', redirect: '\/user-data'/)
  assert.match(source, /path: '\/questionnaires', redirect: '\/user-data'/)
})

test('侧边栏将两个旧菜单合并为用户数据入口', async () => {
  const source = await read('src/layouts/AdminLayout.vue')

  assert.match(source, /index="\/user-data"/)
  assert.match(source, /用户数据/)
})
