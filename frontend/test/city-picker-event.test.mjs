import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('城市选择组件确认后直接更新传入的资料表单', async () => {
  const source = await readFile(
    new URL('../src/components/city-picker/city-picker.vue', import.meta.url),
    'utf8'
  )

  assert.match(source, /targetForm:/)
  assert.match(source, /props\.targetForm\.city = selectedCity\.value\.name/)
})
