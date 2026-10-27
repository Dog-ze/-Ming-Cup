<template>
  <view class="home-page">
    <view class="page-header">
      <view>
        <text class="eyebrow">今日一品</text>
        <text class="title">以今日天气，选一盏好茶</text>
      </view>

      <view class="location-actions">
        <text class="city-button" @tap="chooseCity">
          {{ displayCity }}⌄
        </text>
        <text class="location-button" @tap="useCurrentLocation">
          定位
        </text>
      </view>
    </view>

    <view class="weather-card">
      <view class="weather-main">
        <view>
          <text class="weather-label">此刻天气</text>
          <text class="temperature">
            {{ weather ? `${weather.temperature}°C` : '--' }}
          </text>
          <text class="weather-text">
            {{ weather?.weatherText || '正在获取天气' }}
          </text>
        </view>

        <view class="weather-mark">
          <image
            class="weather-logo"
            src="@/static/images/logo.png"
            mode="aspectFill"
          />
        </view>
      </view>

      <view class="weather-divider"></view>

      <view class="weather-details">
        <view class="weather-detail">
          <text class="detail-label">湿度</text>
          <text class="detail-value">
            {{ weather?.humidity ?? '--' }}%
          </text>
        </view>

        <view class="weather-detail">
          <text class="detail-label">气候</text>
          <text class="detail-value">
            {{ climateText }}
          </text>
        </view>

        <view class="weather-detail">
          <text class="detail-label">更新</text>
          <text class="detail-value">
            {{ updateText }}
          </text>
        </view>
      </view>

      <view v-if="weather?.source === 'stale_cache'" class="fallback-tip">
        <text>天气服务暂不可用，当前展示稍早获取的天气信息。</text>
      </view>
      <view v-else-if="weather?.source === 'fallback'" class="fallback-tip">
        <text>天气服务暂不可用，当前为默认天气信息。</text>
      </view>
    </view>

    <view class="recommend-placeholder">
      <view class="recommend-header">
        <text class="section-title">今日茶建议</text>
        <text class="ask-tea-button" @tap="goAiChat">问茶</text>
      </view>
      <text class="section-description">
        {{ recommendData?.personalized ? '已结合你的饮茶偏好，为你挑选今日茶。' : '根据当地天气挑选今日适饮茶。' }}
      </text>

      <view v-if="recommendLoading" class="tea-card">
        <view class="tea-icon">茶</view>
        <view class="tea-info">
          <text class="tea-name">正在准备今日茶建议</text>
          <text class="tea-reason">正在结合天气与饮茶偏好。</text>
        </view>
      </view>
      <view v-for="recommendation in recommendData?.recommendations || []" :key="recommendation.teaId" class="tea-card">
        <image
          v-if="recommendation.imageUrl"
          class="tea-image"
          :src="resolveImageUrl(recommendation.imageUrl)"
          mode="aspectFill"
        />
        <view v-else class="tea-icon tea-icon-fallback">茶</view>
        <view class="tea-info">
          <text class="tea-name">{{ recommendation.name }} · {{ recommendation.category }}</text>
          <text class="tea-reason">{{ recommendation.reason }}</text>
        </view>
        <button
          class="ai-brew-button"
          :loading="brewGuideLoadingTeaId === recommendation.teaId"
          :disabled="brewGuideLoadingTeaId !== null"
          @tap="generateBrewGuide(recommendation)"
        >
          {{ brewGuideLoadingTeaId === recommendation.teaId ? '生成中' : `AI 泡法（${brewGuideFreeAvailable ? '今日首次免费' : '5 积分'}）` }}
        </button>
      </view>
      <text v-if="!recommendLoading && recommendData && !recommendData.recommendations?.length" class="empty-tip">暂未找到合适茶叶。</text>
    </view>

    <view v-if="brewGuideVisible" class="brew-guide-mask" @tap="closeBrewGuide">
      <view class="brew-guide-dialog" @tap.stop>
        <view class="brew-guide-header">
          <text class="brew-guide-title">{{ brewGuide.teaName }} · AI 泡法</text>
          <text class="brew-guide-close" @tap="closeBrewGuide">×</text>
        </view>
        <scroll-view scroll-y class="brew-guide-content">
          <text>{{ formatBrewGuideText(brewGuide.content) }}</text>
        </scroll-view>
        <button class="brew-guide-confirm" @tap="closeBrewGuide">知道了</button>
      </view>
    </view>

    <view v-if="!isLoggedIn" class="guest-card">
      <text class="guest-title">登录后，获得更适合你的推荐</text>
      <text class="guest-description">
        完成饮茶偏好问卷后，推荐会结合你的口味与习惯。
      </text>
      <button class="login-button" @tap="goLogin">去登录</button>
    </view>
    <view v-else-if="recommendData && !recommendData.personalized" class="guest-card">
      <text class="guest-title">完成偏好问卷，获得更贴合的推荐</text>
      <button class="login-button" @tap="goQuestionnaire">填写问卷</button>
    </view>

    <CityPicker
      v-model:visible="cityPickerVisible"
      :current-city="locationStore.city"
    />
    <TeaTabBar />
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getWeather } from '@/api/weather.js'
import { getRecommend } from '@/api/recommend.js'
import { getBrewGuide } from '@/api/ai.js'
import { getPointAccount } from '@/api/points.js'
import { createOperationId } from '@/utils/operation-id.mjs'
import { formatBrewGuideText } from '@/utils/ai-text.mjs'
import { updateProfile } from '@/api/auth.js'
import { reverseGeocode } from '@/api/location.js'
import { getCurrentLocation, getLocationErrorMessage } from '@/utils/location.js'
import CityPicker from '@/components/city-picker/city-picker.vue'
import TeaTabBar from '@/custom-tab-bar/index.vue'
import { resolveImageUrl } from '@/utils/image-url.js'
import { setActiveTabPath } from '@/utils/tab-bar-state.js'
import { useLocationStore } from '@/store/location.js'
import { useUserStore } from '@/store/user.js'

const locationStore = useLocationStore()
const userStore = useUserStore()

// 当前天气数据。刚进入页面时没有数据，所以是 null。
const weather = ref(null)
const weatherLoading = ref(false)
// 只有最新城市的天气请求可以更新页面，避免旧请求晚到后覆盖新城市。
const weatherRequestVersion = ref(0)
const recommendData = ref(null)
const recommendLoading = ref(false)
// 每次切换城市都会增加编号，只有最新城市的推荐结果可以写入页面。
const recommendRequestVersion = ref(0)
// 正在生成泡法的茶叶编号；非空时禁用所有 AI 泡法按钮，避免重复请求。
const brewGuideLoadingTeaId = ref(null)
const brewGuide = ref(null)
const brewGuideVisible = ref(false)
const brewGuideFreeAvailable = ref(true)

// 首次资料与天气加载完成后，才允许 onShow 再次刷新天气。
const initialized = ref(false)

// 控制“省份 → 城市”选择弹窗。
const cityPickerVisible = ref(false)

// 页面上显示的城市：优先显示刚刚选择的城市，不需要等待天气接口返回。
const displayCity = computed(() => {
  return locationStore.displayLabel || locationStore.city || weather.value?.city || '北京市'
})

// 是否已登录。
const isLoggedIn = computed(() => userStore.isLoggedIn)

// 城市弹窗确认后会直接更新位置仓库；首页据此重新加载天气。
watch(
  () => [locationStore.city, locationStore.longitude, locationStore.latitude],
  () => {
    if (initialized.value) {
      loadWeather()
      loadRecommend()
    }
  }
)

// 将给推荐功能使用的英文气候标签，转换成用户能看懂的中文。
const climateText = computed(() => {
  const climateTag = weather.value?.climateTag

  const climateMap = {
    hot_humid: '闷热潮湿',
    hot: '炎热',
    cold: '寒冷',
    mild: '温和'
  }

  return climateMap[climateTag] || '--'
})

// 只显示“刚刚更新”；后续如果需要可改成具体时间。
const updateText = computed(() => {
  return weather.value?.updatedAt ? '刚刚' : '--'
})

/*
 * 页面第一次创建时：
 * 1. 读取游客上次手选的城市。
 * 2. 如果已经登录，则读取个人资料中的城市和坐标。
 */
onLoad(async () => {
  locationStore.restore()
  await syncProfileLocation()
  await loadWeather()
  await loadRecommend()
  await loadPointAccount()
  initialized.value = true
})

/*
 * 每次回到“今日一品”页面时刷新天气。
 * 例如用户在个人页修改城市，回来后天气会重新加载。
 */
onShow(() => {
  setActiveTabPath('/pages/home/home')

  if (initialized.value) {
    loadWeather()
    loadRecommend()
    loadPointAccount()
  }
})

/*
 * 用户下拉页面时刷新天气。
 * 无论成功或失败，都必须停止下拉动画。
 */
onPullDownRefresh(async () => {
  await loadWeather()
  await loadRecommend()
  uni.stopPullDownRefresh()
})

/*
 * 如果用户已经登录：
 * 从后端读取最新个人资料，
 * 并将其中的城市、经纬度作为首页默认地区。
 */
async function syncProfileLocation() {
  if (!userStore.isLoggedIn) return

  try {
    const profile = await userStore.refreshProfile()

    if (profile?.city) {
      locationStore.setLocation({
        city: profile.city,
        displayLabel: profile.city,
        longitude: profile.longitude,
        latitude: profile.latitude
      })
    }
  } catch {
    // 刷新失败不在这里退出登录：网络短暂失败时保留登录态；
    // 若后端返回 401/40101，由 request.js 统一清除本地登录信息。
    // 首页仍可继续按游客方式显示天气。
  }
}

/*
 * 根据当前城市或坐标请求天气。
 */
async function loadWeather() {
  const requestVersion = ++weatherRequestVersion.value
  weatherLoading.value = true

  try {
    let requestData

    // 有经纬度时优先按坐标查天气。
    if (
      locationStore.longitude !== null &&
      locationStore.latitude !== null
    ) {
      requestData = {
        city: locationStore.city,
        longitude: locationStore.longitude,
        latitude: locationStore.latitude
      }
    } else {
      // 没有坐标时按城市查询。
      requestData = {
        city: locationStore.city
      }
    }

    const result = await getWeather(requestData)

    // 切换城市后，旧请求即使最后返回也不能覆盖新城市天气。
    if (requestVersion === weatherRequestVersion.value) {
      weather.value = result
    }
  } catch {
    /*
     * 请求失败时不要把 weather 清空。
     * 这样网络暂时异常时，页面仍能保留上一次成功显示的天气。
     */
    // 仅最新请求才提示错误，避免旧城市请求失败干扰当前城市。
    if (requestVersion === weatherRequestVersion.value) {
      uni.showToast({
        title: '天气暂时无法更新',
        icon: 'none'
      })
    }
  } finally {
    if (requestVersion === weatherRequestVersion.value) {
      weatherLoading.value = false
    }
  }
}

async function loadRecommend() {
  // 不拦截新请求：用户切换城市后，必须立刻按新城市重新查询。
  const requestVersion = ++recommendRequestVersion.value
  recommendLoading.value = true

  // 在发请求前保存当前地点，避免等待期间城市又被切换。
  const requestData = locationStore.longitude !== null && locationStore.latitude !== null
    ? { city: locationStore.city, longitude: locationStore.longitude, latitude: locationStore.latitude }
    : { city: locationStore.city }

  try {
    const result = await getRecommend(requestData)

    // 旧城市的请求即使后返回，也不能覆盖新城市的推荐内容。
    if (requestVersion === recommendRequestVersion.value) {
      recommendData.value = result
    }
  } catch {
    // 推荐服务未完成或临时异常时，不影响天气卡正常显示。
  } finally {
    // 只有最后一次请求才能关闭加载状态。
    if (requestVersion === recommendRequestVersion.value) {
      recommendLoading.value = false
    }
  }
}

async function loadPointAccount() {
  if (!userStore.isLoggedIn) {
    brewGuideFreeAvailable.value = true
    return
  }
  try {
    brewGuideFreeAvailable.value = (await getPointAccount()).brewGuideFreeAvailable === true
  } catch {
    // 积分状态读取失败时保持当前文案；实际扣费仍以后端为准。
  }
}

/*
 * 对当前推荐茶生成完整泡法。该接口需要登录；地点与首页天气、推荐使用同一份位置数据。
 */
async function generateBrewGuide(recommendation) {
  if (!userStore.isLoggedIn) {
    uni.showToast({ title: '请先登录后使用 AI 泡法', icon: 'none' })
    goLogin()
    return
  }

  if (brewGuideLoadingTeaId.value !== null) return

  brewGuideLoadingTeaId.value = recommendation.teaId
  const requestData = {
    teaId: recommendation.teaId,
    city: locationStore.city,
    requestId: createOperationId('brew')
  }
  if (locationStore.longitude !== null && locationStore.latitude !== null) {
    requestData.longitude = locationStore.longitude
    requestData.latitude = locationStore.latitude
  }

  try {
    const result = await getBrewGuide(requestData)
    brewGuide.value = result
    brewGuideVisible.value = true
    await loadPointAccount()
  } catch {
    // request.js 已统一显示接口或网络错误提示。
  } finally {
    brewGuideLoadingTeaId.value = null
  }
}

function closeBrewGuide() {
  brewGuideVisible.value = false
}

/*
 * 打开“省份 → 城市”选择框。
 */
function chooseCity() {
  cityPickerVisible.value = true
}

/*
 * 用户确认手动选择的城市。
 */
async function selectCity(city) {
  cityPickerVisible.value = false

  locationStore.setLocation({
    city: city.name,
    displayLabel: city.name,
    longitude: city.longitude,
    latitude: city.latitude
  })

  /*
   * 已登录用户：把新城市保存到个人资料。
   * 游客：只保存在手机本地。
   */
  if (userStore.isLoggedIn && userStore.profile) {
    try {
      const updatedProfile = await updateProfile({
        city: city.name,
        longitude: city.longitude,
        latitude: city.latitude
      })

      userStore.setProfile(updatedProfile)
    } catch {
      uni.showToast({
        title: '城市保存失败，但仍可查看天气',
        icon: 'none'
      })
    }
  }

  await loadWeather()
}

/*
 * 用户主动点击“定位”时调用。
 */
async function useCurrentLocation() {
  try {
    // 向手机或浏览器请求定位权限，并取得坐标。
    const location = await getCurrentLocation()

    // “城市”保存真实行政区；“当前位置”只用作首页展示名称。
    // H5 只能取到坐标时，保留之前已保存的真实城市，绝不把“当前位置”写入后台。
    const city = location.city || await reverseGeocode(location.longitude, location.latitude)
    locationStore.setLocation({
      city,
      displayLabel: location.displayLabel || city,
      longitude: location.longitude,
      latitude: location.latitude
    })

    // 地点已变化，重新查询天气。
    await loadWeather()
    await loadRecommend()

    /*
     * 已登录用户把定位到的城市和坐标保存至资料。
     * 游客只保留本机缓存。
     */
    if (userStore.isLoggedIn && userStore.profile) {
      const updatedProfile = await updateProfile({
        city,
        longitude: location.longitude,
        latitude: location.latitude
      })

      userStore.setProfile(updatedProfile)
    }

  } catch (error) {
    /*
     * 用户拒绝权限、设备不支持定位、定位失败，
     * 都不会影响当前已有城市和天气。
     */
    uni.showToast({
      title: getLocationErrorMessage(error),
      icon: 'none'
    })
  }
}

/*
 * 游客点击后进入登录页。
 */
function goLogin() {
  uni.navigateTo({
    url: '/pages/login/login?redirect=%2Fpages%2Fhome%2Fhome'
  })
}

function goQuestionnaire() {
  // 游客不能填写问卷，先进入登录页；登录完成后会回到首页。
  if (!userStore.isLoggedIn) {
    goLogin()
    return
  }

  uni.navigateTo({ url: '/pages/questionnaire/questionnaire' })
}

function goAiChat() {
  uni.navigateTo({ url: '/pages/ai-chat/chat' })
}
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 48rpx 32rpx calc(208rpx + env(safe-area-inset-bottom));
  background: #faf8f2 url('@/static/images/app-background-tea.png') center top / cover fixed no-repeat;
  color: #26342d;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 36rpx;
}

.eyebrow,
.title {
  display: block;
}

.eyebrow {
  color: #8daa98;
  font-size: 24rpx;
  letter-spacing: 2rpx;
}

.title {
  max-width: 420rpx;
  margin-top: 12rpx;
  color: #175b43;
  font-size: 40rpx;
  font-weight: 600;
  line-height: 1.35;
}

.location-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 14rpx;
}

.city-button,
.location-button {
  color: #175b43;
  font-size: 25rpx;
}

.ask-tea-button {
  padding: 8rpx 18rpx;
  border-radius: 20rpx;
  color: #ffffff;
  background: #175b43;
  font-size: 25rpx;
}

.location-button {
  padding: 8rpx 18rpx;
  border-radius: 20rpx;
  background: #e6f1e8;
}

.weather-card,
.recommend-placeholder,
.guest-card {
  box-sizing: border-box;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 12rpx 32rpx rgba(38, 52, 45, 0.05);
}

.weather-card {
  padding: 36rpx 32rpx;
  background: linear-gradient(135deg, #e6f1e8 0%, #ffffff 100%);
}

.weather-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.weather-label,
.temperature,
.weather-text {
  display: block;
}

.weather-label {
  color: #718578;
  font-size: 25rpx;
}

.temperature {
  margin-top: 14rpx;
  color: #175b43;
  font-size: 72rpx;
  font-weight: 600;
  line-height: 1;
}

.weather-text {
  margin-top: 16rpx;
  color: #52675a;
  font-size: 28rpx;
}

.weather-mark,
.tea-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #ffffff;
  background: #175b43;
  border-radius: 50%;
}

.weather-mark {
  width: 108rpx;
  height: 108rpx;
  font-size: 46rpx;
  overflow: hidden;
}

.weather-logo {
  width: 100%;
  height: 100%;
}

.weather-divider {
  height: 1rpx;
  margin: 34rpx 0 24rpx;
  background: rgba(23, 91, 67, 0.12);
}

.weather-details {
  display: flex;
}

.weather-detail {
  flex: 1;
}

.detail-label,
.detail-value {
  display: block;
}

.detail-label {
  color: #8daa98;
  font-size: 23rpx;
}

.detail-value {
  margin-top: 10rpx;
  color: #26342d;
  font-size: 27rpx;
}

.fallback-tip {
  margin-top: 26rpx;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  color: #8f6944;
  background: #fff6e7;
  font-size: 23rpx;
}

.recommend-placeholder {
  padding: 36rpx 32rpx;
  margin-top: 28rpx;
}

.recommend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title,
.section-description {
  display: block;
}

.section-title {
  color: #175b43;
  font-size: 34rpx;
  font-weight: 600;
}

.section-description {
  margin-top: 14rpx;
  color: #8daa98;
  font-size: 25rpx;
  line-height: 1.65;
}

.tea-card {
  display: flex;
  align-items: center;
  padding: 26rpx 0 0;
  margin-top: 28rpx;
  border-top: 1rpx solid #edf0eb;
}

.tea-icon,
.tea-image {
  width: 72rpx;
  height: 72rpx;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
}

.tea-icon {
  font-size: 30rpx;
}

.tea-info {
  flex: 1;
  margin-left: 20rpx;
}

.tea-name,
.tea-reason {
  display: block;
}

.ai-brew-button {
  flex-shrink: 0;
  min-width: 132rpx;
  padding: 0 18rpx;
  margin: 0 0 0 16rpx;
  border: 1rpx solid #175b43;
  border-radius: 999rpx;
  color: #175b43;
  background: #f3f8f3;
  font-size: 23rpx;
  line-height: 58rpx;
}

.ai-brew-button::after,
.brew-guide-confirm::after {
  border: 0;
}

.ai-brew-button[disabled] {
  color: #8daa98;
  border-color: #dce8de;
  background: #f5f7f4;
}

.brew-guide-mask {
  position: fixed;
  z-index: 20;
  inset: 0;
  display: flex;
  align-items: flex-end;
  background: rgba(24, 37, 30, 0.45);
}

.brew-guide-dialog {
  box-sizing: border-box;
  width: 100%;
  max-height: 78vh;
  padding: 36rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));
  border-radius: 32rpx 32rpx 0 0;
  background: #ffffff;
}

.brew-guide-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brew-guide-title {
  color: #175b43;
  font-size: 32rpx;
  font-weight: 600;
}

.brew-guide-close {
  padding: 0 10rpx;
  color: #718578;
  font-size: 48rpx;
  line-height: 1;
}

.brew-guide-content {
  max-height: 760rpx;
  margin-top: 28rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  color: #405247;
  background: #f7faf6;
  font-size: 27rpx;
  line-height: 1.8;
  white-space: pre-wrap;
}

.brew-guide-confirm {
  margin-top: 28rpx;
  border: 0;
  border-radius: 14rpx;
  color: #ffffff;
  background: #175b43;
  font-size: 27rpx;
}

.tea-name {
  color: #26342d;
  font-size: 28rpx;
  font-weight: 500;
}

.tea-reason {
  margin-top: 10rpx;
  color: #8daa98;
  font-size: 23rpx;
}

.guest-card {
  padding: 32rpx;
  margin-top: 28rpx;
  text-align: center;
}

.guest-title,
.guest-description {
  display: block;
}

.guest-title {
  color: #26342d;
  font-size: 29rpx;
  font-weight: 600;
}

.guest-description {
  margin-top: 12rpx;
  color: #8daa98;
  font-size: 24rpx;
  line-height: 1.6;
}

.login-button {
  margin-top: 28rpx;
  border: 0;
  border-radius: 14rpx;
  color: #ffffff;
  background: #175b43;
  font-size: 27rpx;
}

.login-button::after {
  border: 0;
}
</style>
