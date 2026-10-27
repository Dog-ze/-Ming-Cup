<template>
  <view class="profile-page">
    <view v-if="isLoggedIn && profile" class="logged-in-content">
      <view class="page-header">
        <text class="page-title">个人</text>
        <text class="page-subtitle">一盏清茗，记录此刻</text>
      </view>

      <view class="member-card">
        <image v-if="profile.avatar" class="avatar-image" :src="resolveImageUrl(profile.avatar)" mode="aspectFill" />
        <view v-else class="avatar">{{ profile.username?.slice(0, 1) || '茗' }}</view>
        <view class="member-info">
          <text class="username">{{ profile.username || '茶友' }}</text>
        </view>
        <text class="edit-link" @tap="openEdit">编辑</text>
      </view>

      <view class="section-card">
        <view class="section-row">
          <text class="row-label">所在城市</text>
          <text class="row-value">{{ profile.city || '暂未填写' }}</text>
        </view>
        <view class="divider"></view>
        <view class="section-row" @tap="goQuestionnaire">
          <text class="row-label">品茶问卷</text>
          <text class="row-value">{{ profile.questionnaireCompleted ? '已完成，点击修改' : '待填写' }}</text>
        </view>
      </view>

      <view class="section-card menu-card">
        <view class="menu-row" @tap="goPoints"><text>积分中心</text><text class="arrow">›</text></view>
        <view class="divider"></view>
        <view class="menu-row" @tap="openEdit">
          <text>个人资料</text>
          <text class="arrow">›</text>
        </view>
        <view class="divider"></view>
        <view class="menu-row" @tap="logout">
          <text class="logout-text">退出登录</text>
          <text class="arrow">›</text>
        </view>
      </view>
    </view>

    <view v-else class="guest-content">
      <view class="page-header">
        <text class="page-title">个人</text>
        <text class="page-subtitle">从一盏茶开始，认识更好的自己</text>
      </view>

      <view class="guest-card">
        <view class="guest-mark">茗</view>
        <text class="guest-title">登录后，留下你的品茶故事</text>
        <text class="guest-description">保存茶记、完成品茶问卷，获得更贴合你的推荐。</text>
        <button class="login-button" @tap="goLogin">登录 / 注册</button>
      </view>

      <view class="guest-tip">
        <text>你仍可浏览「今日一品」的本地茶饮推荐。</text>
      </view>
    </view>

    <view v-if="editing" class="dialog-mask" @tap.self="closeEdit">
      <view class="edit-dialog">
        <view class="dialog-header">
          <text class="dialog-title">编辑个人资料</text>
          <text class="close-button" @tap="closeEdit">×</text>
        </view>

        <view class="form-item">
          <text class="form-label">头像</text>
          <view class="avatar-editor" @tap="chooseAvatar">
            <image v-if="editForm.avatar" class="avatar-preview" :src="resolveImageUrl(editForm.avatar)" mode="aspectFill" />
            <view v-else class="avatar-placeholder">{{ profile?.username?.slice(0, 1) || '茗' }}</view>
            <view class="avatar-editor-copy">
              <text>{{ uploadingAvatar ? '头像上传中…' : '点击选择头像' }}</text>
              <text class="avatar-editor-tip">JPG、PNG、WebP，最多 5MB</text>
            </view>
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">所在城市</text>
          <view class="location-row">
            <text class="city-value">{{ editForm.city || '暂未选择城市' }}</text>
            <text class="location-link" @tap="chooseCity">选择城市</text>
          </view>
        </view>
        <view class="form-item">
          <button class="location-button" :loading="locating" :disabled="locating" @tap="useCurrentLocation">
            {{ locating ? '定位中…' : '使用当前位置' }}
          </button>
          <text class="location-tip">使用定位后，将自动填写城市和坐标。</text>
        </view>

        <view class="dialog-actions">
          <button class="save-button" :loading="saving" @tap="saveProfile">保存资料</button>
        </view>
      </view>
    </view>

    <CityPicker
      v-model:visible="cityPickerVisible"
      :current-city="editForm.city"
      :target-form="editForm"
    />
    <TeaTabBar />
  </view>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { onHide, onShow } from '@dcloudio/uni-app'
import { updateProfile, uploadAvatar } from '@/api/auth.js'
import { reverseGeocode } from '@/api/location.js'
import { getCurrentLocation, getLocationErrorMessage } from '@/utils/location.js'
import { isAuthExpiredError } from '@/utils/api-error.js'
import CityPicker from '@/components/city-picker/city-picker.vue'
import TeaTabBar from '@/custom-tab-bar/index.vue'
import { resolveImageUrl, toStoredImageUrl } from '@/utils/image-url.js'
import { setActiveTabPath } from '@/utils/tab-bar-state.js'
import { useLocationStore } from '@/store/location.js'
import { useUserStore } from '@/store/user.js'
import { setTabBarVisibility } from '@/utils/tab-bar.js'

const userStore = useUserStore()
const locationStore = useLocationStore()
const editing = ref(false)
const saving = ref(false)
const uploadingAvatar = ref(false)
const locating = ref(false)
const profile = computed(() => userStore.profile)
const isLoggedIn = computed(() => userStore.isLoggedIn)
const editForm = reactive({ avatar: null, city: '', longitude: null, latitude: null })
const cityPickerVisible = ref(false)

watch(cityPickerVisible, visible => {
  // 城市选择框关闭时，它会恢复导航栏；资料编辑框仍打开则再次隐藏。
  if (!visible && editing.value) {
    nextTick(() => setTabBarVisibility(false))
  }
})

onShow(async () => {
  setActiveTabPath('/pages/profile/profile')
  if (!isLoggedIn.value) return
  try {
    await userStore.refreshProfile()
  } catch (error) {
    // 只有后端明确返回 401/40101 才退出登录；
    // 网络短暂失败时保留登录态，仅提示刷新失败，避免误清 token。
    if (isAuthExpiredError(error)) {
      // 失效提示已由 utils/request.js 统一弹出，这里不再叠加“刷新失败”，避免覆盖。
      userStore.clearLoginState()
    } else {
      uni.showToast({ title: '资料刷新失败，请稍后重试', icon: 'none' })
    }
  }
})

onHide(() => {
  // 切换到其他页面时恢复导航栏，避免影响其他页面。
  setTabBarVisibility(true)
})

onBeforeUnmount(() => {
  setTabBarVisibility(true)
})

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login?redirect=%2Fpages%2Fprofile%2Fprofile' })
}

function goQuestionnaire() {
  uni.navigateTo({ url: '/pages/questionnaire/questionnaire' })
}

function goPoints() { uni.navigateTo({ url: '/pages/points/points' }) }

function openEdit() {
  Object.assign(editForm, profile.value)
  editing.value = true
  setTabBarVisibility(false)
}

function closeEdit() {
  editing.value = false
  setTabBarVisibility(true)
}

function chooseCity() {
  cityPickerVisible.value = true
}

function chooseAvatar() {
  if (uploadingAvatar.value) return

  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async ({ tempFilePaths }) => {
      uploadingAvatar.value = true
      try {
        editForm.avatar = await uploadAvatar(tempFilePaths[0])
        uni.showToast({ title: '头像已上传，请保存资料', icon: 'success' })
      } catch {
        // uploadAvatar 已提示失败原因；这里确保界面会恢复可操作状态。
      } finally {
        uploadingAvatar.value = false
      }
    }
  })
}

async function useCurrentLocation() {
  if (locating.value) return
  locating.value = true
  try {
    const location = await getCurrentLocation()
    const city = location.city || await reverseGeocode(location.longitude, location.latitude)
    editForm.city = city
    editForm.longitude = location.longitude
    editForm.latitude = location.latitude
    uni.showToast({ title: `已定位为${city}，请保存资料`, icon: 'success' })
  } catch (error) {
    uni.showToast({ title: getLocationErrorMessage(error), icon: 'none', duration: 2500 })
  } finally {
    locating.value = false
  }
}

function optionalNumber(value) {
  if (value === '' || value === null || value === undefined) return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

async function saveProfile() {
  if (!editForm.city.trim()) {
    uni.showToast({ title: '请选择所在城市', icon: 'none' })
    return
  }
  if (uploadingAvatar.value) {
    uni.showToast({ title: '头像上传中，请稍候', icon: 'none' })
    return
  }

  saving.value = true
  try {
    const savedForm = {
      // 数据库只保存 /uploads/... 相对地址，绝对域名由 App 运行时再拼。
      avatar: toStoredImageUrl(editForm.avatar) || null,
      city: editForm.city.trim(),
      longitude: optionalNumber(editForm.longitude),
      latitude: optionalNumber(editForm.latitude)
    }
    const updated = await updateProfile(savedForm)

    // 立即用本次保存的内容更新页面状态，不依赖重新请求个人资料。
    const latestProfile = {
      ...userStore.profile,
      ...updated,
      ...savedForm
    }
    userStore.setProfile(latestProfile)
    locationStore.setLocation({
      city: savedForm.city,
      displayLabel: savedForm.city,
      longitude: savedForm.longitude,
      latitude: savedForm.latitude
    })
    closeEdit()
    uni.showToast({ title: '资料已保存', icon: 'success' })
  } finally {
    saving.value = false
  }
}

function logout() {
  uni.showModal({
    title: '退出登录',
    content: '退出后仍可浏览今日一品的基础推荐。',
    success: ({ confirm }) => {
      if (confirm) {
        userStore.clearLoginState()
        uni.showToast({ title: '已退出登录', icon: 'none' })
      }
    }
  })
}
</script>

<style scoped lang="scss">
.profile-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 48rpx 32rpx calc(208rpx + env(safe-area-inset-bottom));
  background: #faf8f2 url('@/static/images/app-background-tea.png') center top / cover fixed no-repeat;
  color: #26342d;
}

.page-header {
  margin-bottom: 40rpx;
}

.page-title {
  display: block;
  color: #175b43;
  font-size: 56rpx;
  font-weight: 600;
  letter-spacing: 4rpx;
}

.page-subtitle {
  display: block;
  margin-top: 12rpx;
  color: #8daa98;
  font-size: 26rpx;
}

.member-card,
.section-card,
.guest-card,
.guest-tip {
  box-sizing: border-box;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 12rpx 32rpx rgba(38, 52, 45, 0.05);
}

.member-card {
  display: flex;
  align-items: center;
  padding: 32rpx;
  margin-bottom: 24rpx;
  background: linear-gradient(135deg, #e6f1e8 0%, #ffffff 100%);
}

.avatar,
.avatar-image,
.guest-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #ffffff;
  background: #175b43;
  border-radius: 50%;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  font-size: 40rpx;
}

.avatar-image {
  width: 96rpx;
  height: 96rpx;
}

.member-info {
  flex: 1;
  min-width: 0;
  margin-left: 24rpx;
}

.username {
  display: block;
}

.username {
  font-size: 34rpx;
  font-weight: 600;
}

.edit-link {
  color: #175b43;
  font-size: 26rpx;
}

.section-card {
  padding: 0 28rpx;
  margin-bottom: 24rpx;
}

.section-row,
.menu-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 104rpx;
  font-size: 29rpx;
}

.row-label {
  color: #26342d;
}

.row-value {
  color: #8daa98;
  font-size: 26rpx;
}

.divider {
  height: 1rpx;
  background: #edf0eb;
}

.arrow {
  color: #8daa98;
  font-size: 42rpx;
  font-weight: 300;
}

.logout-text {
  color: #b45745;
}

.guest-card {
  padding: 64rpx 40rpx 48rpx;
  text-align: center;
}

.guest-mark {
  width: 112rpx;
  height: 112rpx;
  margin: 0 auto 32rpx;
  font-size: 48rpx;
}

.guest-title,
.guest-description {
  display: block;
}

.guest-title {
  color: #26342d;
  font-size: 32rpx;
  font-weight: 600;
}

.guest-description {
  margin-top: 20rpx;
  color: #8daa98;
  font-size: 26rpx;
  line-height: 1.7;
}

.login-button,
.save-button {
  margin-top: 40rpx;
  border: 0;
  border-radius: 14rpx;
  color: #ffffff;
  background: #175b43;
  font-size: 29rpx;
}

.login-button::after,
.save-button::after {
  border: 0;
}

.guest-tip {
  margin-top: 24rpx;
  padding: 28rpx 32rpx;
  color: #8daa98;
  font-size: 25rpx;
  line-height: 1.6;
}

.dialog-mask {
  position: fixed;
  z-index: 10;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: flex-end;
  background: rgba(22, 40, 31, 0.36);
  overflow-y: auto;
}

.edit-dialog {
  box-sizing: border-box;
  width: 100%;
  max-height: calc(100vh - 40rpx);
  overflow-y: auto;
  padding: 36rpx 32rpx calc(48rpx + env(safe-area-inset-bottom));
  border-radius: 32rpx 32rpx 0 0;
  background: #faf8f2;
}

.dialog-actions {
  position: sticky;
  bottom: 0;
  padding-top: 24rpx;
  padding-bottom: env(safe-area-inset-bottom);
  background: #faf8f2;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28rpx;
}

.dialog-title {
  color: #175b43;
  font-size: 36rpx;
  font-weight: 600;
}

.close-button {
  width: 56rpx;
  color: #8daa98;
  font-size: 48rpx;
  line-height: 48rpx;
  text-align: center;
}

.form-item {
  margin-top: 24rpx;
}

.avatar-editor {
  display: flex;
  align-items: center;
  min-height: 116rpx;
  padding: 16rpx 20rpx;
  border-radius: 14rpx;
  background: #ffffff;
}

.avatar-preview,
.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 84rpx;
  height: 84rpx;
  border-radius: 50%;
}

.avatar-preview {
  background: #edf2ed;
}

.avatar-placeholder {
  color: #ffffff;
  background: #175b43;
  font-size: 34rpx;
}

.avatar-editor-copy {
  display: flex;
  flex-direction: column;
  margin-left: 20rpx;
  color: #175b43;
  font-size: 27rpx;
}

.avatar-editor-tip {
  margin-top: 8rpx;
  color: #8daa98;
  font-size: 22rpx;
}

.form-label {
  display: block;
  margin-bottom: 12rpx;
  color: #52675a;
  font-size: 25rpx;
}

.form-input {
  box-sizing: border-box;
  width: 100%;
  height: 84rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  background: #ffffff;
  color: #26342d;
  font-size: 28rpx;
}

.location-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  min-height: 84rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  background: #ffffff;
}

.city-value {
  color: #26342d;
  font-size: 28rpx;
}

.location-link {
  color: #175b43;
  font-size: 26rpx;
}

.location-button {
  border: 0;
  border-radius: 14rpx;
  color: #175b43;
  background: #e6f1e8;
  font-size: 27rpx;
}

.location-button::after {
  border: 0;
}

.location-tip {
  display: block;
  margin-top: 12rpx;
  color: #8daa98;
  font-size: 23rpx;
}

.save-button {
  margin-top: 48rpx;
}

.dialog-actions .save-button {
  margin-top: 0;
}
</style>

