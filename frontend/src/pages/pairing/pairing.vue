<template>
  <view class="pairing-page">
    <view class="page-header">
      <text class="eyebrow">茶点</text>
      <text class="page-title">让这一盏茶，配一口刚好的茶食</text>
    </view>

    <view v-if="!isLoggedIn" class="guest-card">
      <view class="guest-mark"><text>点</text></view>
      <text class="guest-title">登录后查看茶食搭配</text>
      <text class="guest-desc">按你选择的茶类，为这一盏茶配一份日常小食。</text>
      <button class="primary-button" @tap="goLogin">去登录</button>
    </view>

    <view v-else>
      <view class="category-card">
        <text class="section-label">选择茶类</text>
        <picker :range="teaCategories" :value="categoryIndex" @change="changeCategory">
          <view class="picker-value"><text>{{ teaCategory }}</text><text>›</text></view>
        </picker>
      </view>

      <view v-if="loading" class="state-card"><text>正在准备茶点搭配…</text></view>
      <view v-else-if="pairing" class="pairing-card">
        <text class="card-title">{{ pairing.teaCategory }}，可以这样搭配</text>
        <view class="food-grid">
          <view v-for="food in pairing.foods" :key="food.name" class="food-card" @tap="goNearbyStore(food.name)">
            <image :src="foodImage(food)" class="food-image" mode="aspectFill" />
            <view class="food-copy">
              <text class="food-name">{{ food.name }}</text>
              <text class="food-note">{{ foodNote(food.name) }}</text>
            </view>
          </view>
        </view>
        <text class="advice">{{ pairing.advice }}</text>
      </view>
      <view v-else class="state-card"><text>暂时无法准备茶点建议，请下拉重试。</text></view>

      <text class="disclaimer">本内容仅供日常饮食参考，不构成医疗建议；如有不适请咨询专业人士。</text>
    </view>
    <TeaTabBar />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getPairing } from '@/api/pairing.js'
import { useUserStore } from '@/store/user.js'
import TeaTabBar from '@/custom-tab-bar/index.vue'
import { resolveImageUrl } from '@/utils/image-url.js'
import { setActiveTabPath } from '@/utils/tab-bar-state.js'
import lightPastryImage from '@/static/images/pairing/light-pastry.png'
import fruitImage from '@/static/images/pairing/fruit.png'
import nutsBreadImage from '@/static/images/pairing/nuts-bread.png'
import dimSumImage from '@/static/images/pairing/dim-sum.png'
import savorySnackImage from '@/static/images/pairing/savory-snack.png'

const teaCategories = ['绿茶', '白茶', '黄茶', '青茶', '红茶', '黑茶', '花草']
const userStore = useUserStore()
const teaCategory = ref('绿茶')
const pairing = ref(null)
const loading = ref(false)

const isLoggedIn = computed(() => userStore.isLoggedIn)
const categoryIndex = computed(() => teaCategories.indexOf(teaCategory.value))

const foodImages = {
  '清淡糕点': lightPastryImage,
  '低糖饼干': lightPastryImage,
  '蒸点心': dimSumImage,
  '米糕': lightPastryImage,
  '广式茶点': dimSumImage,
  '水果': fruitImage,
  '坚果': nutsBreadImage,
  '全麦面包': nutsBreadImage,
  '虾仁': dimSumImage,
  '凤梨酥': savorySnackImage,
  '牛肉干': savorySnackImage,
  '牛奶饼干': lightPastryImage
}

const foodNotes = {
  '水果': '清爽解腻，适量即可',
  '坚果': '少量慢嚼，香气更足',
  '全麦面包': '朴素饱腹，不抢茶味',
  '虾仁': '鲜甜清淡，趁热享用',
  '牛肉干': '咸香有嚼劲，少量搭配'
}

onLoad(loadPairing)
onShow(() => {
  setActiveTabPath('/pages/pairing/pairing')
  loadPairing()
})
onPullDownRefresh(async () => {
  await loadPairing()
  uni.stopPullDownRefresh()
})

function changeCategory(event) {
  teaCategory.value = teaCategories[Number(event.detail.value)]
  loadPairing()
}

function normalizeFoods(foods) {
  if (!Array.isArray(foods)) return []
  return foods.map((food) => {
    if (typeof food === 'string') return { name: food, imageUrl: '' }
    return { name: food?.name || '', imageUrl: food?.imageUrl || '' }
  }).filter((food) => food.name)
}

function foodImage(food) {
  return resolveImageUrl(food.imageUrl) || foodImages[food.name] || lightPastryImage
}

function foodNote(food) {
  return foodNotes[food] || '一小份，刚好相配'
}

function goNearbyStore(food) {
  // 记录本次茶类，供「附近茶食」页在参数异常时回退，避免整页不可用。
  uni.setStorageSync('last_tea_category', teaCategory.value)
  uni.navigateTo({
    url: `/pages/pairing/nearby?teaCategory=${encodeURIComponent(teaCategory.value)}&foodName=${encodeURIComponent(food)}`
  })
}

async function loadPairing() {
  if (!isLoggedIn.value || loading.value) return
  loading.value = true
  try {
    const data = await getPairing({ teaCategory: teaCategory.value })
    pairing.value = { ...data, foods: normalizeFoods(data.foods) }
  } catch {
    pairing.value = null
  } finally {
    loading.value = false
  }
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login?redirect=%2Fpages%2Fpairing%2Fpairing' })
}
</script>

<style scoped lang="scss">
.pairing-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 48rpx 32rpx calc(208rpx + env(safe-area-inset-bottom));
  background: #faf8f2 url('@/static/images/app-background-tea.png') center top / cover fixed no-repeat;
  color: #26342d;
}

.eyebrow, .page-title, .guest-title, .guest-desc, .section-label, .card-title, .advice, .disclaimer, .food-name, .food-note { display: block; }
.eyebrow { color: #8daa98; font-size: 24rpx; letter-spacing: 2rpx; }
.page-title { max-width: 500rpx; margin-top: 12rpx; color: #175b43; font-size: 40rpx; font-weight: 600; line-height: 1.35; }
.guest-card, .category-card, .pairing-card, .state-card { box-sizing: border-box; margin-top: 32rpx; padding: 36rpx 32rpx; border-radius: 24rpx; background: #fff; box-shadow: 0 12rpx 32rpx rgba(38, 52, 45, .05); }
.guest-card { text-align: center; }
.guest-mark { display: flex; align-items: center; justify-content: center; width: 108rpx; height: 108rpx; margin: 0 auto 28rpx; border-radius: 50%; color: #fff; background: #175b43; font-size: 46rpx; }
.guest-title { font-size: 31rpx; font-weight: 600; }
.guest-desc { margin-top: 16rpx; color: #8daa98; font-size: 25rpx; line-height: 1.65; }
.primary-button { margin-top: 30rpx; border: 0; border-radius: 14rpx; color: #fff; background: #175b43; font-size: 27rpx; }
.primary-button::after { border: 0; }
.section-label { color: #52675a; font-size: 25rpx; }
.picker-value { display: flex; align-items: center; justify-content: space-between; height: 84rpx; margin-top: 16rpx; padding: 0 24rpx; border-radius: 14rpx; color: #26342d; background: #f4f7f3; font-size: 28rpx; }
.picker-value text:last-child { color: #8daa98; font-size: 40rpx; }
.state-card { color: #8daa98; font-size: 27rpx; text-align: center; }
.card-title { color: #175b43; font-size: 33rpx; font-weight: 600; }

.food-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
  margin-top: 28rpx;
}

.food-card {
  overflow: hidden;
  border: 1rpx solid #edf1eb;
  border-radius: 18rpx;
  background: #fffdfa;
}

.food-image { display: block; width: 100%; height: 176rpx; background: #edf1eb; }
.food-copy { padding: 18rpx 18rpx 20rpx; }
.food-name { overflow: hidden; color: #175b43; font-size: 28rpx; font-weight: 600; line-height: 1.3; text-overflow: ellipsis; white-space: nowrap; }
.food-note { overflow: hidden; margin-top: 8rpx; color: #8daa98; font-size: 21rpx; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }

.advice { margin-top: 30rpx; color: #52675a; font-size: 27rpx; line-height: 1.75; }
.disclaimer { margin: 28rpx 8rpx 0; color: #8daa98; font-size: 22rpx; line-height: 1.7; }
</style>
