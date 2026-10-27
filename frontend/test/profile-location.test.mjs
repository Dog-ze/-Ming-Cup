import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('浏览器定位期间显示加载状态，并将反查出的城市填入资料表单', async () => {
  const source = await readFile(
    new URL('../src/pages/profile/profile.vue', import.meta.url),
    'utf8'
  )

  assert.match(source, /:loading="locating"/)
  assert.match(source, /const locating = ref\(false\)/)
  assert.match(source, /locating\.value = true/)
  assert.match(source, /await reverseGeocode\(location\.longitude, location\.latitude\)/)
  assert.match(source, /已定位为\$\{city\}，请保存资料/)
})
