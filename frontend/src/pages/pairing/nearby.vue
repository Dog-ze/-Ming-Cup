<template>
  <view class="nearby-page">
    <view class="page-header">
      <text class="eyebrow">附近茶食</text>
      <text class="page-title">为这一盏茶，寻一口相配的小食</text>
    </view>

    <view class="tea-context">
      <view class="tea-seal">食</view>
      <view>
        <text class="context-label">当前推荐</text>
        <text class="food-title">{{ foodName || '茶食' }}</text>
        <text class="context-copy">适合搭配 {{ teaCategory || '好茶' }}</text>
      </view>
    </view>

    <view class="tab-switch">
      <view class="tab-item" :class="{ active: activeTab === 'local' }" @tap="changeTab('local')">
        <text>本地茶点</text>
      </view>
      <view class="tab-item" :class="{ active: activeTab === 'online' }" @tap="changeTab('online')">
        <text>网上茶点</text>
      </view>
    </view>

    <view v-if="activeTab === 'local'">
      <view class="location-card">
        <view>
          <text class="location-label">附近店铺</text>
          <text class="location-value">{{ locationText }}</text>
        </view>
        <button class="location-button" :loading="locationLoading" @tap="useCurrentLocation">使用当前位置</button>
      </view>

      <view v-if="storesLoading" class="empty-card">
        <text class="empty-mark">茗</text>
        <text class="empty-title">正在查找附近茶食</text>
        <text class="empty-copy">正在结合当前位置与当前茶食建议查询店铺。</text>
      </view>

      <view v-else-if="stores.length" class="store-list">
        <view v-for="store in stores" :key="store.id || store.storeName" class="store-card">
          <image v-if="store.coverImage" class="store-image" :src="resolveImageUrl(store.coverImage)" mode="aspectFill" />
          <view v-else class="store-image-placeholder">食</view>

          <view class="store-copy">
            <view class="store-name-row">
              <text class="store-name">{{ store.storeName }}</text>
              <text v-if="store.promotion" class="promotion-tag">推广</text>
            </view>
            <text class="store-description">{{ store.description || '为这盏茶准备一口相配的小食' }}</text>
            <view class="store-meta">
              <text>{{ formatDistance(store.distanceMeters) }}</text>
              <text>{{ store.address || '地址信息暂缺' }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty-card">
        <text class="empty-mark">茗</text>
        <text class="empty-title">{{ storeLoadFailed ? '附近店铺暂不可用' : '暂未找到附近店铺' }}</text>
        <text class="empty-copy">
          {{ storeLoadFailed ? '请稍后重试，或重新获取当前位置。' : `暂未找到售卖“${foodName || '相关茶食'}”的附近店铺。` }}
        </text>
      </view>

      <text class="map-note">附近店铺来自高德地图数据；合作推广店会按距离和有效推广规则排序。</text>
    </view>

    <view v-else class="ad-card">
      <text class="ad-mark">茶食</text>
      <text class="ad-title">广告位招租</text>
      <text class="ad-copy">后续可在这里展示茶点商品、团购链接或品牌推广内容。</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCurrentLocation, getLocationErrorMessage } from '@/utils/location.js'
import { useLocationStore } from '@/store/location.js'
import { getNearbyStores } from '@/api/stores.js'
import { resolveImageUrl } from '@/utils/image-url.js'

const locationStore = useLocationStore()
// 与茶点页、后端 StoreService.TEA_CATEGORIES 保持一致。
const teaCategories = ['绿茶', '白茶', '黄茶', '青茶', '红茶', '黑茶', '花草']
const DEFAULT_TEA_CATEGORY = '绿茶'
const activeTab = ref('local')
const teaCategory = ref('')
const foodName = ref('')
const locationLoading = ref(false)
const storesLoading = ref(false)
const stores = ref([])
const storeLoadFailed = ref(false)

const locationText = computed(() => locationStore.displayLabel || locationStore.city || '暂未选择位置')

onLoad(async (options) => {
  // uni-app 官方说明：H5 会自动解码 onLoad 的 options，App/小程序端不会。
  // 这里统一再解一次（失败就保留原值），避免中文变成 %E4%BD%8E%E7%B3%96... 这种乱码，
  // 否则页面显示乱码，后端也会因拿不到合法茶类而返回「请选择正确的茶类」。
  teaCategory.value = pickTeaCategory(decodeOption(options.teaCategory))
  foodName.value = decodeOption(options.foodName)
  await refreshCurrentLocation(true)
})

/**
 * 取一个非空值并解码；H5 已解码的参数再解码一次是无变化的，解码失败则原样返回。
 */
function decodeOption(value) {
  if (value === null || value === undefined) return ''
  const text = String(value)
  if (!/%[0-9a-fA-F]{2}/.test(text)) return text

  try {
    return decodeURIComponent(text)
  } catch {
    return text
  }
}

/**
 * 只接受后端认可的茶类；参数缺失或非法时回退到默认茶类，
 * 避免因为一个坏参数让整个页面显示「附近店铺暂不可用」。
 */
function pickTeaCategory(value) {
  if (teaCategories.includes(value)) return value

  const lastUsed = uni.getStorageSync('last_tea_category')
  return teaCategories.includes(lastUsed) ? lastUsed : DEFAULT_TEA_CATEGORY
}

function changeTab(tab) {
  activeTab.value = tab
}

function formatDistance(distanceMeters) {
  if (distanceMeters === null || distanceMeters === undefined) return '距离暂缺'
  if (distanceMeters < 1000) return `${Math.round(distanceMeters)} 米`
  return `${(distanceMeters / 1000).toFixed(1)} 公里`
}

async function refreshCurrentLocation(silent = false) {
  if (locationLoading.value) return
  locationLoading.value = true

  try {
    const location = await getCurrentLocation()
    locationStore.setLocation({
      city: location.city || locationStore.city || '北京市',
      displayLabel: location.displayLabel || locationStore.city || '北京市',
      longitude: location.longitude,
      latitude: location.latitude,
      coordinateSystem: location.coordinateSystem
    })
    const storesLoaded = await loadStores(!silent)
    if (!silent && storesLoaded) {
      uni.showToast({ title: '已更新当前位置', icon: 'success' })
    }
  } catch (error) {
    // 自动定位失败时仍按上一次位置查询，避免首次进入页面被定位弹窗打断。
    await loadStores(!silent)
    if (!silent) {
      uni.showToast({ title: getLocationErrorMessage(error), icon: 'none', duration: 2500 })
    }
  } finally {
    locationLoading.value = false
  }
}

async function useCurrentLocation() {
  await refreshCurrentLocation(false)
}

/**
 * 根据当前推荐食品与当前保存的位置，加载后端统一排序后的店铺。
 */
async function loadStores(showError = false) {
  // 缺少茶类或茶食时不发请求，但要明确标记失败，
  // 否则页面会停在「暂未找到附近店铺」这种容易误解的文案上。
  if (!teaCategory.value || !foodName.value) {
    stores.value = []
    storeLoadFailed.value = true
    return false
  }
  if (storesLoading.value) return false
  storesLoading.value = true

  try {
    const result = await getNearbyStores({
      teaCategory: teaCategory.value,
      foodName: foodName.value,
      longitude: locationStore.longitude ?? undefined,
      latitude: locationStore.latitude ?? undefined,
      coordinateSystem: locationStore.coordinateSystem || 'gcj02',
      radius: 3000
    })
    stores.value = result.stores ?? []
    storeLoadFailed.value = false
    return true
  } catch (error) {
    stores.value = []
    storeLoadFailed.value = true
    if (showError) {
      uni.showToast({ title: '附近店铺暂不可用，请稍后重试', icon: 'none', duration: 2500 })
    }
    return false
  } finally {
    storesLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.nearby-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 48rpx 32rpx calc(64rpx + env(safe-area-inset-bottom));
  background: #faf8f2 url('@/static/images/app-background-tea.png') center top / cover fixed no-repeat;
  color: #26342d;
}

.eyebrow,
.page-title,
.context-label,
.food-title,
.context-copy,
.location-label,
.location-value,
.store-name,
.store-description,
.store-meta,
.empty-title,
.empty-copy,
.map-note,
.ad-title,
.ad-copy { display: block; }

.eyebrow { color: #8daa98; font-size: 24rpx; letter-spacing: 2rpx; }
.page-title { max-width: 520rpx; margin-top: 12rpx; color: #175b43; font-size: 40rpx; font-weight: 600; line-height: 1.35; }

.tea-context {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-top: 32rpx;
  padding: 30rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #e4f0e6, #fffdfa);
}

.tea-seal,
.store-image-placeholder,
.empty-mark,
.ad-mark {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #ffffff;
  background: #175b43;
}

.tea-seal { width: 88rpx; height: 88rpx; font-size: 34rpx; }
.context-label { color: #8daa98; font-size: 22rpx; }
.food-title { margin-top: 4rpx; color: #175b43; font-size: 32rpx; font-weight: 600; }
.context-copy { margin-top: 6rpx; color: #52675a; font-size: 24rpx; }

.tab-switch {
  display: flex;
  margin-top: 28rpx;
  padding: 8rpx;
  border-radius: 16rpx;
  background: #edf2ed;
}

.tab-item {
  flex: 1;
  padding: 18rpx 0;
  border-radius: 11rpx;
  color: #748178;
  font-size: 27rpx;
  text-align: center;
}

.tab-item.active { color: #ffffff; background: #175b43; font-weight: 600; }

.location-card,
.store-card,
.empty-card,
.ad-card {
  box-sizing: border-box;
  margin-top: 28rpx;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 12rpx 32rpx rgba(38, 52, 45, 0.05);
}

.location-card { display: flex; align-items: center; justify-content: space-between; padding: 28rpx; }
.location-label { color: #8daa98; font-size: 22rpx; }
.location-value { margin-top: 7rpx; color: #26342d; font-size: 29rpx; font-weight: 600; }
.location-button { margin: 0; border: 0; border-radius: 12rpx; color: #175b43; background: #e5f1e7; font-size: 24rpx; }
.location-button::after { border: 0; }

.store-list { margin-top: 28rpx; }

.store-card {
  display: flex;
  gap: 22rpx;
  margin-top: 20rpx;
  padding: 20rpx;
}

.store-card:first-child { margin-top: 0; }
.store-image,
.store-image-placeholder {
  display: block;
  flex-shrink: 0;
  width: 154rpx;
  height: 154rpx;
  border-radius: 16rpx;
}
.store-image { background: #edf1eb; }
.store-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: #8daa98;
  font-size: 42rpx;
}
.store-copy { min-width: 0; flex: 1; padding: 4rpx 0; }
.store-name-row { display: flex; align-items: center; gap: 12rpx; }
.store-name { overflow: hidden; color: #175b43; font-size: 29rpx; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.promotion-tag { flex-shrink: 0; padding: 4rpx 10rpx; border-radius: 8rpx; color: #8f6944; background: #fff1d8; font-size: 20rpx; }
.store-description { overflow: hidden; margin-top: 10rpx; color: #748178; font-size: 23rpx; line-height: 1.45; text-overflow: ellipsis; white-space: nowrap; }
.store-meta { display: flex; gap: 14rpx; overflow: hidden; margin-top: 16rpx; color: #8daa98; font-size: 21rpx; white-space: nowrap; }
.store-meta text:last-child { overflow: hidden; text-overflow: ellipsis; }

.empty-card,
.ad-card { padding: 58rpx 38rpx; text-align: center; }
.empty-mark,
.ad-mark { width: 96rpx; height: 96rpx; margin: 0 auto 24rpx; font-size: 38rpx; }
.ad-mark { border-radius: 18rpx; background: #8f6944; }
.empty-title,
.ad-title { color: #175b43; font-size: 31rpx; font-weight: 600; }
.ad-title { color: #6c4b2d; }
.empty-copy,
.ad-copy { margin-top: 16rpx; color: #8daa98; font-size: 25rpx; line-height: 1.7; }
.map-note { margin: 24rpx 8rpx 0; color: #8daa98; font-size: 22rpx; line-height: 1.6; text-align: center; }
</style>
