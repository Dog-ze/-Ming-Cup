import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('编辑资料弹窗有可滚动内容区和固定的保存操作区', async () => {
  const source = await readFile(
    new URL('../src/pages/profile/profile.vue', import.meta.url),
    'utf8'
  )

  assert.match(source, /<view class="dialog-actions">/)
  assert.match(source, /\.dialog-mask\s*\{[\s\S]*?overflow-y:\s*auto/)
  assert.match(source, /\.edit-dialog\s*\{[\s\S]*?max-height:\s*calc\(100vh - 40rpx\)/)
  assert.match(source, /\.dialog-actions\s*\{[\s\S]*?position:\s*sticky[\s\S]*?bottom:\s*0/)
})

test('个人页只显示用户名，编辑资料不再包含昵称', async () => {
  const source = await readFile(
    new URL('../src/pages/profile/profile.vue', import.meta.url),
    'utf8'
  )

  assert.match(source, /profile\.username\?\.slice\(0, 1\)/)
  assert.match(source, /class="username">\{\{ profile\.username \|\| '茶友' \}\}<\/text>/)
  assert.doesNotMatch(source, /editForm\.nickname/)
  assert.doesNotMatch(source, /form-label">昵称/)
})

test('个人页支持上传和显示用户头像', async () => {
  const source = await readFile(
    new URL('../src/pages/profile/profile.vue', import.meta.url),
    'utf8'
  )

  assert.match(source, /v-if="profile\.avatar"/)
  assert.match(source, /uploadAvatar/)
  assert.match(source, /uni\.chooseImage/)
  assert.match(source, /editForm\.avatar/)
})
