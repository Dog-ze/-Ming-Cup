import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = async (path) => readFile(new URL(path, import.meta.url), 'utf8')

test('首页提供问茶入口，聊天页接收 delta 并在 done 后结束生成状态', async () => {
  const home = await source('../src/pages/home/home.vue')
  const pages = await source('../src/pages.json')
  const chat = await source('../src/pages/ai-chat/chat.vue')
  const api = await source('../src/api/ai-chat.js')

  assert.match(home, /@tap="goAiChat"/)
  assert.match(home, /function goAiChat\(\)/)
  assert.match(pages, /pages\/ai-chat\/chat/)
  assert.match(api, /\/api\/ai\/chat\/stream/)
  assert.match(chat, /streamAiChat\(/)
  assert.match(chat, /event === 'delta'/)
  assert.match(chat, /event === 'done'/)
})

test('问茶入口位于今日茶建议标题行，输入框固定在页面底部', async () => {
  const home = await source('../src/pages/home/home.vue')
  const chat = await source('../src/pages/ai-chat/chat.vue')

  assert.match(home, /<view class="recommend-header">[\s\S]*?今日茶建议[\s\S]*?@tap="goAiChat"/)
  assert.match(chat, /\.input-panel\s*\{[\s\S]*?position: fixed;/)
  assert.match(chat, /\.input-panel\s*\{[\s\S]*?bottom: calc\(72rpx \+ env\(safe-area-inset-bottom\)\);/)
  assert.match(chat, /padding: 48rpx 32rpx calc\(220rpx \+ env\(safe-area-inset-bottom\)\)/)
})

test('聊天消息只能在中间区域滚动，不会把底部输入框推出屏幕', async () => {
  const chat = await source('../src/pages/ai-chat/chat.vue')

  assert.match(chat, /\.chat-page\s*\{[\s\S]*?height: calc\(100vh - var\(--window-top\) - var\(--window-bottom\)\);/)
  assert.doesNotMatch(chat, /\.chat-page\s*\{[\s\S]*?min-height: 100vh;/)
  assert.match(chat, /\.message-list\s*\{[\s\S]*?flex: 1;[\s\S]*?min-height: 0;/)
})

test('初始欢迎语不启用滚动，发送问题后才允许消息区滚动', async () => {
  const chat = await source('../src/pages/ai-chat/chat.vue')

  assert.match(chat, /:scroll-y="messages\.length > 1"/)
  assert.match(chat, /messages\.value\.push\(userMessage, assistantMessage\)/)
})

test('离开有对话的问茶页时询问是否保存，并在当天恢复记录', async () => {
  const chat = await source('../src/pages/ai-chat/chat.vue')

  assert.match(chat, /import \{ onBackPress, onLoad, onUnload \} from '@dcloudio\/uni-app'/)
  assert.match(chat, /readAiChatSession, writeAiChatSession, clearAiChatSession/)
  assert.match(chat, /onBackPress\(\(\) => \{[\s\S]*?requestExit\(\)[\s\S]*?return true/)
  assert.match(chat, /uni\.showModal\([\s\S]*?confirmText: '保留'/)
  assert.match(chat, /writeAiChatSession\(\{ messages: messages\.value/)
  assert.match(chat, /setTimeout\(expireChatAtMidnight/)
  const exitFunction = chat.slice(chat.indexOf('function requestExit'), chat.indexOf('function leavePage'))
  const abortIndex = exitFunction.indexOf('activeRequest?.abort()')
  const modalIndex = exitFunction.indexOf('uni.showModal(')
  assert.ok(abortIndex > -1 && abortIndex < modalIndex)
})

test('聊天页在生成期间禁止重复发送，并在页面离开时中断请求', async () => {
  const chat = await source('../src/pages/ai-chat/chat.vue')

  assert.match(chat, /:disabled="sending \|\| !draft\.trim\(\)"/)
  assert.match(chat, /onUnload\(\(\) => \{[\s\S]*?activeRequest\?\.abort\(\)/)
})
