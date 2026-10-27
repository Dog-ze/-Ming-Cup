<template>
  <view class="record-page">
    <view v-if="!isLoggedIn" class="guest-content">
      <text class="eyebrow">茶记</text>
      <text class="page-title">留住这一杯茶的味道</text>
      <view class="guest-card">
        <view class="guest-mark">记</view>
        <text class="guest-title">登录后，记录每一盏茶的味道与心情</text>
        <text class="guest-description">茶名、评分、评价与来源都会只保存在你的茶记中。</text>
        <button class="primary-button" @tap="goLogin">登录 / 注册</button>
      </view>
    </view>

    <view v-else class="logged-content">
      <view class="page-header">
        <view>
          <text class="eyebrow">茶记</text>
          <text class="page-title">每一杯，都值得记下</text>
        </view>
        <button class="add-button" @tap="goCreate">＋ 记一杯</button>
      </view>

      <view v-if="loading && !records.length" class="state-card">
        <text>正在翻阅你的茶记…</text>
      </view>

      <view v-else-if="!records.length" class="state-card empty-card">
        <view class="empty-mark">茶</view>
        <text class="empty-title">还没有品茶记录</text>
        <text class="empty-description">从今天喝的这一杯开始，留下属于你的茶味记忆。</text>
        <button class="primary-button" @tap="goCreate">去记一杯吧</button>
      </view>

      <view v-else class="record-list">
        <view v-for="record in records" :key="record.id" class="record-card" @tap="goEdit(record.id)">
          <view class="record-top">
            <view>
              <text class="tea-name">{{ record.teaName }}</text>
              <text class="tea-meta">{{ record.category }}{{ record.brand ? ` · ${record.brand}` : '' }}</text>
            </view>
            <text class="rating">{{ stars(record.rating) }}</text>
          </view>
          <image v-if="record.images?.length" class="record-photo" :src="resolveImageUrl(record.images[0])" mode="aspectFill" />
          <text v-if="record.comment" class="comment">{{ record.comment }}</text>
          <view class="record-bottom">
            <text>{{ formatDate(record.createdAt) }}</text>
            <text class="delete-link" @tap.stop="confirmDelete(record.id)">删除</text>
          </view>
        </view>
        <text class="load-tip">{{ hasMore ? (loading ? '正在加载…' : '继续向上滑动加载更多') : '已经到底了' }}</text>
      </view>
    </view>
    <TeaTabBar />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { deleteRecord, listRecords } from '@/api/records.js'
import { clearRecordSession, readRecordSession, writeRecordSession } from '@/utils/record-session.js'
import { authSessionVersion, authToken } from '@/utils/auth-session.js'
import { useUserStore } from '@/store/user.js'
import TeaTabBar from '@/custom-tab-bar/index.vue'
import { resolveImageUrl } from '@/utils/image-url.js'
import { setActiveTabPath } from '@/utils/tab-bar-state.js'

const PAGE_SIZE = 10
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const records = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const loadingSessionVersion = ref(null)
const recordRequestVersion = ref(0)
const hasMore = computed(() => records.value.length < total.value)

onShow(() => {
  setActiveTabPath('/pages/record/list')

  if (!isLoggedIn.value) {
    // 退出登录（或登录失效）后，tab 页实例仍然存在，
    // 必须主动清空列表，否则会继续显示上一个账号的茶记。
    resetLocalState()
    return
  }

  // 已登录时先恢复本账号的缓存，让列表立刻可见，再向服务端刷新。
  restoreLocalState()
  loadFirstPage()
})

onPullDownRefresh(async () => {
  if (isLoggedIn.value) await loadFirstPage()
  uni.stopPullDownRefresh()
})

onReachBottom(() => loadNextPage())

/**
 * 退出登录后彻底清空页面与本地缓存中的茶记。
 */
function resetLocalState() {
  records.value = []
  page.value = 1
  total.value = 0
  clearRecordSession()
}

/**
 * 只恢复「同一个登录账号」的缓存；换账号时 readRecordSession 会返回空会话。
 */
function restoreLocalState() {
  const session = readRecordSession()
  records.value = session.records
  page.value = session.page
  total.value = session.total
}

function isCurrentRecordSession(token, sessionVersion) {
  return Boolean(token)
    && isLoggedIn.value
    && token === authToken.value
    && sessionVersion === authSessionVersion.value
}

function finishRecordRequest(requestVersion) {
  if (requestVersion !== recordRequestVersion.value) return
  loading.value = false
  loadingSessionVersion.value = null
}

async function loadFirstPage() {
  const token = authToken.value
  const sessionVersion = authSessionVersion.value
  if (!token || (loading.value && loadingSessionVersion.value === sessionVersion)) return

  const requestVersion = ++recordRequestVersion.value
  loading.value = true
  loadingSessionVersion.value = sessionVersion
  try {
    const result = await listRecords(1, PAGE_SIZE)
    if (!isCurrentRecordSession(token, sessionVersion) || requestVersion !== recordRequestVersion.value) return
    records.value = result.records || []
    page.value = result.page || 1
    total.value = result.total || 0
    persistCache(token)
  } finally {
    finishRecordRequest(requestVersion)
  }
}

async function loadNextPage() {
  const token = authToken.value
  const sessionVersion = authSessionVersion.value
  if (!isLoggedIn.value || !token || !hasMore.value || (loading.value && loadingSessionVersion.value === sessionVersion)) return

  const requestVersion = ++recordRequestVersion.value
  const nextPage = page.value + 1
  loading.value = true
  loadingSessionVersion.value = sessionVersion
  try {
    const result = await listRecords(nextPage, PAGE_SIZE)
    if (!isCurrentRecordSession(token, sessionVersion) || requestVersion !== recordRequestVersion.value) return
    records.value.push(...(result.records || []))
    page.value = result.page || nextPage
    total.value = result.total || total.value
    persistCache(token)
  } finally {
    finishRecordRequest(requestVersion)
  }
}

/**
 * 把当前列表写进本账号的缓存，供下次进入页面秒开；
 * 退出登录或换账号后该缓存会自动失效。
 */
function persistCache(expectedToken = authToken.value) {
  writeRecordSession({
    records: records.value,
    page: page.value,
    total: total.value,
    expectedToken
  })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login?redirect=%2Fpages%2Frecord%2Flist' })
}

function goCreate() {
  uni.navigateTo({ url: '/pages/record/edit' })
}

function goEdit(id) {
  uni.navigateTo({ url: `/pages/record/edit?id=${id}` })
}

function confirmDelete(id) {
  uni.showModal({
    title: '删除茶记',
    content: '删除后无法恢复，确定删除这条记录吗？',
    confirmColor: '#b45745',
    success: async ({ confirm }) => {
      if (!confirm) return
      const token = authToken.value
      const sessionVersion = authSessionVersion.value
      try {
        await deleteRecord(id)
        if (!isCurrentRecordSession(token, sessionVersion)) return
        records.value = records.value.filter(record => record.id !== id)
        total.value = Math.max(0, total.value - 1)
        persistCache(token)
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch {
        // request.js 已显示后端错误信息，保留原卡片。
      }
    }
  })
}

function stars(rating) {
  return '★'.repeat(Number(rating) || 0) + '☆'.repeat(Math.max(0, 5 - (Number(rating) || 0)))
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : ''
}
</script>

<style scoped lang="scss">
.record-page { min-height: 100vh; box-sizing: border-box; padding: 48rpx 32rpx calc(208rpx + env(safe-area-inset-bottom)); background: #faf8f2 url('@/static/images/app-background-tea.png') center top / cover fixed no-repeat; color: #26342d; }
.eyebrow,.page-title,.guest-title,.guest-description,.tea-name,.tea-meta,.comment,.load-tip { display: block; }
.eyebrow { color: #8daa98; font-size: 24rpx; letter-spacing: 2rpx; }.page-title { margin-top: 12rpx; color: #175b43; font-size: 42rpx; font-weight: 600; }
.page-header,.record-top,.record-bottom { display: flex; align-items: center; justify-content: space-between; }.page-header { margin-bottom: 36rpx; }
.guest-card,.state-card,.record-card { box-sizing: border-box; border-radius: 24rpx; background: #fff; box-shadow: 0 12rpx 32rpx rgba(38,52,45,.05); }.guest-card { margin-top: 44rpx; padding: 64rpx 40rpx 48rpx; text-align: center; }.guest-mark,.empty-mark { display:flex; align-items:center; justify-content:center; width:112rpx; height:112rpx; margin:0 auto 32rpx; border-radius:50%; color:#fff; background:#175b43; font-size:48rpx; }.guest-title,.empty-title { font-size:32rpx; font-weight:600; }.guest-description,.empty-description { margin-top:20rpx; color:#8daa98; font-size:26rpx; line-height:1.7; }
.primary-button,.add-button { border:0; border-radius:14rpx; color:#fff; background:#175b43; font-size:27rpx; }.primary-button { margin-top:34rpx; }.primary-button::after,.add-button::after { border:0; }.add-button { margin:0; padding:0 22rpx; line-height:68rpx; font-size:24rpx; }
.state-card { padding:52rpx 34rpx; color:#8daa98; text-align:center; }.empty-card { padding:64rpx 34rpx; }.record-list { padding-bottom:20rpx; }.record-card { padding:30rpx; margin-bottom:22rpx; }.tea-name { color:#26342d; font-size:32rpx; font-weight:600; }.tea-meta { margin-top:10rpx; color:#8daa98; font-size:24rpx; }.rating { color:#c18c43; font-size:27rpx; letter-spacing:2rpx; }.record-photo { width:100%; height:300rpx; margin-top:24rpx; border-radius:16rpx; background:#edf2ed; }.comment { margin-top:24rpx; color:#52675a; font-size:26rpx; line-height:1.65; }.record-bottom { margin-top:24rpx; padding-top:20rpx; border-top:1rpx solid #edf0eb; color:#8daa98; font-size:23rpx; }.delete-link { color:#b45745; padding:8rpx; }.load-tip { padding:16rpx 0 30rpx; color:#8daa98; font-size:23rpx; text-align:center; }
</style>
