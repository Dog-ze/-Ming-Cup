<template>
  <view class="chat-page">
    <view class="page-header">
      <text class="eyebrow">一盏茗 · 智能问茶</text>
      <text class="page-title">说说此刻的状态，我来为你选茶</text>
      <text class="page-note">建议会结合当地天气、你的饮茶偏好和茶叶库生成。</text>
      <text class="point-note">每次问茶消耗 10 积分</text>
    </view>

    <scroll-view class="message-list" :scroll-y="messages.length > 1" :scroll-into-view="scrollTarget">
      <view v-for="message in messages" :id="`message-${message.id}`" :key="message.id" class="message-row" :class="message.role">
        <view class="message-bubble">
          <text>{{ message.content || (message.role === 'assistant' && sending ? '正在思考…' : '') }}</text>
        </view>
      </view>
    </scroll-view>

    <view class="input-panel">
      <textarea
        v-model="draft"
        class="question-input"
        maxlength="1000"
        :disabled="sending"
        placeholder="例如：下午容易犯困，适合喝什么茶？"
        auto-height
      />
      <button class="send-button" :disabled="sending || !draft.trim()" @tap="sendQuestion">
        {{ sending ? '生成中' : '发送' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { onBackPress, onLoad, onUnload } from '@dcloudio/uni-app'
import { streamAiChat } from '@/api/ai-chat.js'
import { useLocationStore } from '@/store/location.js'
import { useUserStore } from '@/store/user.js'
import { formatBrewGuideText } from '@/utils/ai-text.mjs'
import { authToken } from '@/utils/auth-session.js'
import { readAiChatSession, writeAiChatSession, clearAiChatSession } from '@/utils/ai-chat-session.js'
import { createOperationId } from '@/utils/operation-id.mjs'

const locationStore = useLocationStore()
const userStore = useUserStore()
const draft = ref('')
const sending = ref(false)
const scrollTarget = ref('')
const initialMessage = { id: 1, role: 'assistant', content: '你好，我可以根据你的状态、天气和饮茶习惯推荐合适的茶。' }
const messages = ref([initialMessage])
let nextMessageId = 2
let activeRequest = null
let midnightTimer = null
let leaving = false
let leaveConfirmed = false

onLoad(() => {
  locationStore.restore()
  restoreChatSession()
  scheduleMidnightExpiry()
})

onUnload(() => {
  activeRequest?.abort()
  clearTimeout(midnightTimer)
})

onBackPress(() => {
  if (leaveConfirmed) return false
  requestExit()
  return true
})

async function sendQuestion() {
  const question = draft.value.trim()
  if (!question || sending.value) return
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/login/login?redirect=%2Fpages%2Fai-chat%2Fchat' })
    return
  }

  const userMessage = { id: nextMessageId++, role: 'user', content: question }
  const assistantMessage = { id: nextMessageId++, role: 'assistant', content: '' }
  messages.value.push(userMessage, assistantMessage)
  draft.value = ''
  sending.value = true
  scrollTo(assistantMessage.id)

  const requestData = {
    message: question,
    city: locationStore.city,
    requestId: createOperationId('chat')
  }
  if (locationStore.longitude !== null && locationStore.latitude !== null) {
    requestData.longitude = locationStore.longitude
    requestData.latitude = locationStore.latitude
  }

  activeRequest = streamAiChat(requestData, {
    onEvent(event) {
      if (event.event === 'delta') {
        assistantMessage.content += event.data?.content || ''
        scrollTo(assistantMessage.id)
      }
      if (event.event === 'done') {
        assistantMessage.content = formatBrewGuideText(assistantMessage.content)
        sending.value = false
        activeRequest = null
      }
      if (event.event === 'error') {
        assistantMessage.content = event.data?.message || 'AI 服务暂时不可用，请稍后重试'
        sending.value = false
        activeRequest = null
      }
    },
    onFinished() {
      if (sending.value) {
        assistantMessage.content = formatBrewGuideText(assistantMessage.content)
        sending.value = false
        activeRequest = null
      }
    },
    onError(message) {
      assistantMessage.content = message
      sending.value = false
      activeRequest = null
    }
  })
}

function scrollTo(messageId) {
  nextTick(() => {
    scrollTarget.value = `message-${messageId}`
  })
}

function restoreChatSession() {
  const savedMessages = readAiChatSession()
  if (!savedMessages.length) return

  messages.value = savedMessages
  nextMessageId = savedMessages.reduce((maxId, message) => Math.max(maxId, Number(message.id) || 0), 0) + 1
  scrollTo(savedMessages[savedMessages.length - 1].id)
}

function hasConversation() {
  return messages.value.some((message) => message.role === 'user')
}

function requestExit() {
  if (leaving) return

  activeRequest?.abort()
  activeRequest = null

  if (!hasConversation()) {
    clearAiChatSession()
    leavePage()
    return
  }

  leaving = true
  uni.showModal({
    title: '保留聊天记录？',
    content: '保留后可在今天内继续查看，明天零点自动清除。',
    confirmText: '保留',
    cancelText: '不保留',
    success({ confirm }) {
      if (confirm) {
        writeAiChatSession({ messages: messages.value, expectedToken: authToken.value })
      } else {
        clearAiChatSession()
      }
      leavePage()
    },
    fail() {
      leaving = false
    }
  })
}

function leavePage() {
  activeRequest?.abort()
  activeRequest = null
  leaveConfirmed = true
  uni.navigateBack()
}

function scheduleMidnightExpiry() {
  clearTimeout(midnightTimer)
  const now = new Date()
  const nextMidnight = new Date(now)
  nextMidnight.setHours(24, 0, 0, 0)
  midnightTimer = setTimeout(expireChatAtMidnight, nextMidnight.getTime() - now.getTime())
}

function expireChatAtMidnight() {
  activeRequest?.abort()
  activeRequest = null
  clearAiChatSession()
  messages.value = [{ ...initialMessage }]
  nextMessageId = 2
  scheduleMidnightExpiry()
}
</script>

<style scoped lang="scss">
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--window-top) - var(--window-bottom));
  overflow: hidden;
  box-sizing: border-box;
  padding: 48rpx 32rpx calc(220rpx + env(safe-area-inset-bottom));
  background: #faf8f2 url('@/static/images/app-background-tea.png') center top / cover fixed no-repeat;
  color: #26342d;
}

.eyebrow,
.page-title,
.page-note,
.message-bubble text { display: block; }
.eyebrow { color: #8daa98; font-size: 24rpx; letter-spacing: 2rpx; }
.page-title { margin-top: 12rpx; color: #175b43; font-size: 40rpx; font-weight: 600; line-height: 1.35; }
.page-note { margin-top: 14rpx; color: #718578; font-size: 24rpx; line-height: 1.65; }
.point-note { display:block; margin-top: 8rpx; color: #8f6944; font-size: 23rpx; }

.message-list {
  flex: 1;
  min-height: 0;
  margin-top: 28rpx;
}

.message-row { display: flex; margin-bottom: 20rpx; }
.message-row.assistant { justify-content: flex-start; }
.message-row.user { justify-content: flex-end; }
.message-bubble {
  max-width: 82%;
  padding: 22rpx 24rpx;
  border-radius: 20rpx;
  font-size: 27rpx;
  line-height: 1.75;
  white-space: pre-wrap;
}
.assistant .message-bubble { color: #405247; background: #ffffff; box-shadow: 0 8rpx 24rpx rgba(38, 52, 45, .06); }
.user .message-bubble { color: #ffffff; background: #175b43; }

.input-panel {
  position: fixed;
  right: 32rpx;
  bottom: calc(72rpx + env(safe-area-inset-bottom));
  left: 32rpx;
  z-index: 2;
  box-sizing: border-box;
  display: flex;
  align-items: flex-end;
  gap: 16rpx;
  padding: 18rpx;
  border: 1rpx solid #dce8de;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, .96);
  box-shadow: 0 8rpx 24rpx rgba(38, 52, 45, .08);
}
.question-input { flex: 1; min-height: 48rpx; max-height: 160rpx; color: #26342d; font-size: 27rpx; line-height: 1.55; }
.send-button { flex-shrink: 0; margin: 0; padding: 0 24rpx; border: 0; border-radius: 14rpx; color: #ffffff; background: #175b43; font-size: 25rpx; line-height: 62rpx; }
.send-button::after { border: 0; }
.send-button[disabled] { color: #8daa98; background: #e8eee8; }
</style>
