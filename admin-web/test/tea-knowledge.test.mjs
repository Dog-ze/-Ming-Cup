import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const read = (file) => readFile(new URL(`../${file}`, import.meta.url), 'utf8')

test('茶叶知识库 API 覆盖列表、详情、保存、状态、删除和分类', async () => {
  const source = await read('src/api/admin-teas.js')

  assert.match(source, /request\.get\('\/admin\/teas', \{ params \}\)/)
  assert.match(source, /request\.get\(`\/admin\/teas\/\$\{id\}`\)/)
  assert.match(source, /request\.post\('\/admin\/teas', data\)/)
  assert.match(source, /request\.put\(`\/admin\/teas\/\$\{id\}`, data\)/)
  assert.match(source, /request\.put\(`\/admin\/teas\/\$\{id\}\/status`, \{ status \}\)/)
  assert.match(source, /request\.delete\(`\/admin\/teas\/\$\{id\}`\)/)
  assert.match(source, /request\.get\('\/admin\/teas\/categories'\)/)
})

test('茶叶知识库页面支持筛选、详情、新增编辑、上下架与删除确认', async () => {
  const source = await read('src/views/tea/TeaKnowledgeView.vue')

  assert.match(source, /名称、分类或产地/)
  assert.match(source, /getTeaPage/)
  assert.match(source, /getTeaDetail/)
  assert.match(source, /TeaEditor/)
  assert.match(source, /ElMessageBox\.confirm/)
  assert.match(source, /updateTeaStatus/)
  assert.match(source, /deleteTea/)
  assert.match(source, /确认删除茶叶/)
  assert.match(source, /el-pagination/)
})

test('茶叶编辑表单使用现有 tea 表字段', async () => {
  const source = await read('src/views/tea/TeaEditor.vue')

  assert.match(source, /flavorTags/)
  assert.match(source, /effectTags/)
  assert.match(source, /suitableStrength/)
  assert.match(source, /brewTemp/)
  assert.match(source, /enabled/)
})

test('茶叶知识库路由使用真实页面而非占位页', async () => {
  const source = await read('src/router/index.js')

  assert.match(source, /path: '\/tea-knowledge'/)
  assert.match(source, /TeaKnowledgeView\.vue/)
})
