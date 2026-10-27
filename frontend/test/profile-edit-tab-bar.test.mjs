import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('编辑资料弹窗打开时隐藏底部导航，关闭时恢复底部导航', async () => {
  const source = await readFile(
    new URL('../src/pages/profile/profile.vue', import.meta.url),
    'utf8'
  )

  assert.match(source, /import \{ setTabBarVisibility \} from '@\/utils\/tab-bar\.js'/)
  assert.match(source, /function openEdit\(\) \{[\s\S]*?setTabBarVisibility\(false\)/)
  assert.match(source, /function closeEdit\(\) \{[\s\S]*?setTabBarVisibility\(true\)/)
  assert.match(source, /@tap\.self="closeEdit"/)
  assert.match(source, /@tap="closeEdit"/)
  assert.match(source, /closeEdit\(\)\s*\n\s*uni\.showToast\(\{ title: '资料已保存'/)
})
