<template>
  <view class="points-page">
    <view class="balance-card">
      <text class="label">我的积分</text>
      <text class="balance">{{ balance }}</text>
      <text class="tip">AI 问茶每次 10 积分，AI 泡法每日首次免费，之后 5 积分。</text>
    </view>
    <view class="task-card">
      <view><text class="task-title">每日领取 20 积分</text><text class="task-tip">每天可领取一次</text></view>
      <button class="claim-button" :disabled="claiming || dailyClaimed" @tap="claim">{{ dailyClaimed ? '已领取' : '领取 20 积分' }}</button>
    </view>
    <view class="history-card">
      <text class="history-title">积分明细</text>
      <view v-if="!transactions.length" class="empty">暂无积分记录</view>
      <view v-for="item in transactions" :key="item.id" class="history-row">
        <view><text class="history-desc">{{ item.description }}</text><text class="history-time">{{ item.createdAt }}</text></view>
        <text :class="item.changeAmount >= 0 ? 'income' : 'expense'">{{ item.changeAmount >= 0 ? '+' : '' }}{{ item.changeAmount }}</text>
      </view>
      <button v-if="transactions.length < total" class="more-button" :loading="loadingMore" @tap="loadMore">加载更多</button>
      <text v-else-if="transactions.length" class="empty">没有更多了</text>
    </view>
  </view>
</template>

<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { claimDailyPoints, getPointAccount, getPointTransactions } from '@/api/points.js'
const balance = ref(0)
const claiming = ref(false)
const dailyClaimed = ref(false)
const transactions = ref([])
const page = ref(1)
const total = ref(0)
const loadingMore = ref(false)
async function load() { try { const account = await getPointAccount(); balance.value = account.balance ?? 0; dailyClaimed.value = account.dailyClaimed === true; const transactionPage = await getPointTransactions(1); transactions.value = transactionPage.records ?? []; total.value = transactionPage.total ?? 0; page.value = 1 } catch {} }
async function loadMore() { if (loadingMore.value || transactions.value.length >= total.value) return; loadingMore.value = true; try { const transactionPage = await getPointTransactions(page.value + 1); transactions.value.push(...(transactionPage.records ?? [])); page.value += 1 } finally { loadingMore.value = false } }
async function claim() { if (claiming.value || dailyClaimed.value) return; claiming.value = true; try { balance.value = (await claimDailyPoints()).balance; dailyClaimed.value = true; await load(); uni.showToast({ title: '已领取 20 积分', icon: 'success' }) } catch {} finally { claiming.value = false } }
onShow(load)
</script>

<style scoped lang="scss">
.points-page { min-height: 100vh; box-sizing: border-box; padding: 40rpx 32rpx; background: #faf8f2 url('@/static/images/app-background-tea.png') center / cover fixed; }
.balance-card,.task-card { border-radius: 24rpx; background: #fff; box-shadow: 0 12rpx 32rpx rgba(38,52,45,.05); }
.balance-card { padding: 42rpx 36rpx; background: linear-gradient(135deg,#e6f1e8,#fff); }.label,.tip,.task-tip { display:block; color:#8daa98; font-size:25rpx; }.balance { display:block; margin:12rpx 0; color:#175b43; font-size:76rpx; font-weight:600; }.tip { line-height:1.7; }
.task-card { display:flex; align-items:center; justify-content:space-between; padding:30rpx; margin-top:24rpx; }.task-title { display:block; color:#26342d; font-size:30rpx; font-weight:600; }.task-tip { margin-top:8rpx; }.claim-button { margin:0; border:0; border-radius:14rpx; color:#fff; background:#175b43; font-size:25rpx; }.claim-button::after { border:0; }
.claim-button[disabled] { color:#8daa98; background:#e8eee8; }
.history-card { margin-top:24rpx; padding:30rpx; border-radius:24rpx; background:#fff; }.history-title { display:block; color:#175b43; font-size:30rpx; font-weight:600; }.history-row { display:flex; align-items:center; justify-content:space-between; padding:24rpx 0; border-bottom:1rpx solid #edf0eb; }.history-desc,.history-time { display:block; }.history-desc { color:#405247; font-size:26rpx; }.history-time,.empty { margin-top:8rpx; color:#9aac9f; font-size:22rpx; }.income { color:#175b43; font-size:30rpx; font-weight:600; }.expense { color:#b45745; font-size:30rpx; font-weight:600; }
.more-button { margin-top:24rpx; border:0; border-radius:14rpx; color:#175b43; background:#edf5ef; font-size:24rpx; }.more-button::after { border:0; }
</style>
