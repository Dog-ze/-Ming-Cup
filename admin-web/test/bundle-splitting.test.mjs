import assert from 'node:assert/strict'
import { execFile } from 'node:child_process'
import { readFile, readdir } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { promisify } from 'node:util'
import test from 'node:test'

const execFileAsync = promisify(execFile)
const projectRoot = fileURLToPath(new URL('../', import.meta.url))

test('生产构建保留按需资源和确认弹窗样式', async () => {
  await execFileAsync(process.execPath, ['node_modules/vite/bin/vite.js', 'build'], {
    cwd: projectRoot,
  })

  const assetFiles = await readdir(new URL('../dist/assets/', import.meta.url))

  assert.ok(assetFiles.some((file) => file.startsWith('DashboardView-') && file.endsWith('.js')))
  assert.ok(assetFiles.some((file) => file.startsWith('echarts-') && file.endsWith('.js')))

  const stylesheets = await Promise.all(
    assetFiles
      .filter((file) => file.endsWith('.css'))
      .map((file) => readFile(new URL(`../dist/assets/${file}`, import.meta.url), 'utf8')),
  )
  assert.ok(
    stylesheets.some((stylesheet) => stylesheet.includes('.el-message-box{')),
    '确认弹窗必须携带 Element Plus 的布局样式',
  )
})
