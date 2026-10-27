<template>
  <view class="login-page">
    <view class="brand-area">
      <view class="brand-mark">茗</view>
      <text class="brand-name">一盏茗</text>
      <text class="brand-slogan">以一盏茶，安放每一天</text>
    </view>

    <view class="auth-card">
      <view class="mode-switch">
        <text class="mode-item" :class="{ active: !isRegister }" @tap="mode = 'login'">登录</text>
        <text class="mode-item" :class="{ active: isRegister }" @tap="mode = 'register'">注册</text>
      </view>

      <view v-if="!isRegister" class="login-type-switch">
        <text class="login-type-item" :class="{ active: loginType === 'phone' }" @tap="changeLoginType('phone')">手机号登录</text>
        <text class="login-type-item" :class="{ active: loginType === 'email' }" @tap="changeLoginType('email')">邮箱登录</text>
      </view>

      <view v-if="isRegister" class="form-item">
        <text class="form-label">用户名</text>
        <input v-model="form.username" class="form-input" placeholder="1～10 个字符，可用中文、字母、数字或下划线" maxlength="10" />
      </view>

      <view v-if="isRegister" class="form-item">
        <text class="form-label">手机号</text>
        <input v-model="form.phone" class="form-input" type="number" placeholder="请输入 11 位手机号" maxlength="11" />
      </view>

      <view v-if="isRegister" class="form-item">
        <text class="form-label">邮箱</text>
        <input v-model="form.email" class="form-input" placeholder="请输入常用邮箱" maxlength="128" />
      </view>

      <view v-if="!isRegister" class="form-item">
        <text class="form-label">{{ loginType === 'phone' ? '手机号' : '邮箱' }}</text>
        <input v-model="form.account" class="form-input" :type="loginType === 'phone' ? 'number' : 'text'" :placeholder="loginType === 'phone' ? '请输入 11 位手机号' : '请输入邮箱'" maxlength="128" />
      </view>

      <view class="form-item">
        <text class="form-label">密码</text>
        <input v-model="form.password" class="form-input" password placeholder="6～32 位密码" maxlength="32" />
      </view>

      <view v-if="isRegister" class="form-item">
        <text class="form-label">确认密码</text>
        <input v-model="form.confirmPassword" class="form-input" password placeholder="请再次输入密码" maxlength="32" />
      </view>

      <button class="submit-button" :loading="loading" @tap="submit">
        {{ isRegister ? '注册并进入一盏茗' : '登录' }}
      </button>

      <text class="switch-tip">
        {{ isRegister ? '已有账号？' : '还没有账号？' }}
        <text class="switch-link" @tap="mode = isRegister ? 'login' : 'register'">
          {{ isRegister ? '去登录' : '去注册' }}
        </text>
      </text>
    </view>

    <text class="guest-link" @tap="uni.switchTab({ url: '/pages/home/home' })">暂不登录，先去看看</text>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getProfile, login, register } from '@/api/auth.js'
import { clearRecordSession } from '@/utils/record-session.js'
import { clearAiChatSession } from '@/utils/ai-chat-session.js'
import { hasAuthToken, setAuthToken } from '@/utils/auth-session.js'
import { useUserStore } from '@/store/user.js'

const userStore = useUserStore()
const mode = ref('login')
const loginType = ref('phone')
const loading = ref(false)
const redirect = ref('/pages/profile/profile')

const form = reactive({ username: '', phone: '', email: '', account: '', password: '', confirmPassword: '' })
const isRegister = computed(() => mode.value === 'register')

onLoad((options) => {
  if (options.redirect) redirect.value = decodeURIComponent(options.redirect)
})

function changeLoginType(type) {
  if (loginType.value === type) return

  loginType.value = type
  form.account = ''
  form.password = ''
}

function validate() {
  if (isRegister.value && !/^[\u4E00-\u9FFFA-Za-z0-9_]{1,10}$/.test(form.username.trim())) {
    uni.showToast({ title: '用户名为 1～10 个字符，可用中文、字母、数字或下划线', icon: 'none' })
    return false
  }
  if (form.password.length < 6 || form.password.length > 32) {
    uni.showToast({ title: '密码长度应为 6～32 位', icon: 'none' })
    return false
  }
  if (isRegister.value && !/^1[3-9]\d{9}$/.test(form.phone.trim())) {
    uni.showToast({ title: '请输入正确的 11 位手机号', icon: 'none' })
    return false
  }
  if (isRegister.value && !/^\S+@\S+\.\S+$/.test(form.email.trim())) {
    uni.showToast({ title: '请输入正确的邮箱地址', icon: 'none' })
    return false
  }
  if (!isRegister.value && loginType.value === 'phone' && !/^1[3-9]\d{9}$/.test(form.account.trim())) {
    uni.showToast({ title: '请输入正确的 11 位手机号', icon: 'none' })
    return false
  }
  if (!isRegister.value && loginType.value === 'email' && !/^\S+@\S+\.\S+$/.test(form.account.trim())) {
    uni.showToast({ title: '请输入正确的邮箱地址', icon: 'none' })
    return false
  }
  if (isRegister.value && form.password !== form.confirmPassword) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return false
  }
  return true
}

async function submit() {
  if (!validate() || loading.value) return
  loading.value = true
  try {
    const auth = isRegister.value
      ? await register({
          username: form.username.trim(), phone: form.phone.trim(),
          email: form.email.trim(), password: form.password
        })
      : await login({ loginType: loginType.value, account: form.account.trim(), password: form.password })

    // 换账号登录前先清掉上一个账号的茶记缓存，避免短暂显示别人的数据。
    clearRecordSession()
    clearAiChatSession()
    // 必须走 setAuthToken：它会同时更新响应式 token 与本地存储。
    // 直接 uni.setStorageSync 不会触发 isLoggedIn 更新（会被 Pinia 缓存），
    // 结果就是「提示登录成功，但个人页仍显示未登录」。
    setAuthToken(auth?.token)
    if (!hasAuthToken()) {
      uni.showToast({ title: '登录状态保存失败，请重试', icon: 'none' })
      return
    }
    userStore.setProfile(await getProfile())
    uni.showToast({ title: isRegister.value ? '注册成功，欢迎使用' : '登录成功', icon: 'success' })

    setTimeout(() => uni.switchTab({ url: redirect.value }), 500)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 104rpx 48rpx 64rpx;
  background: #faf8f2 url('@/static/images/app-background-tea.png') center top / cover fixed no-repeat;
}

.brand-area {
  text-align: center;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120rpx;
  height: 120rpx;
  margin: 0 auto 24rpx;
  border-radius: 50%;
  color: #ffffff;
  background: #175b43;
  font-size: 52rpx;
}

.brand-name,
.brand-slogan {
  display: block;
}

.brand-name {
  color: #175b43;
  font-size: 48rpx;
  font-weight: 600;
  letter-spacing: 8rpx;
}

.brand-slogan {
  margin-top: 16rpx;
  color: #8daa98;
  font-size: 25rpx;
}

.auth-card {
  box-sizing: border-box;
  margin-top: 72rpx;
  padding: 40rpx 36rpx;
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 16rpx 40rpx rgba(38, 52, 45, 0.06);
}

.mode-switch {
  display: flex;
  margin-bottom: 40rpx;
  border-bottom: 1rpx solid #edf0eb;
}

.login-type-switch {
  display: flex;
  gap: 16rpx;
  margin-top: -12rpx;
}

.login-type-item {
  flex: 1;
  padding: 16rpx 0;
  border-radius: 12rpx;
  color: #718578;
  background: #f4f7f3;
  font-size: 25rpx;
  text-align: center;
}

.login-type-item.active {
  color: #175b43;
  background: #e1efe8;
  font-weight: 600;
}

.mode-item {
  position: relative;
  flex: 1;
  padding-bottom: 20rpx;
  color: #8daa98;
  font-size: 31rpx;
  text-align: center;
}

.mode-item.active {
  color: #175b43;
  font-weight: 600;
}

.mode-item.active::after {
  position: absolute;
  right: 32rpx;
  bottom: -1rpx;
  left: 32rpx;
  height: 5rpx;
  border-radius: 6rpx;
  background: #175b43;
  content: '';
}

.form-item {
  margin-top: 28rpx;
}

.form-label {
  display: block;
  margin-bottom: 14rpx;
  color: #52675a;
  font-size: 25rpx;
}

.form-input {
  box-sizing: border-box;
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  background: #f4f7f3;
  color: #26342d;
  font-size: 28rpx;
}

.submit-button {
  margin-top: 48rpx;
  border: 0;
  border-radius: 14rpx;
  color: #ffffff;
  background: #175b43;
  font-size: 29rpx;
}

.submit-button::after {
  border: 0;
}

.switch-tip {
  display: block;
  margin-top: 28rpx;
  color: #8daa98;
  font-size: 25rpx;
  text-align: center;
}

.switch-link {
  margin-left: 8rpx;
  color: #175b43;
}

.guest-link {
  display: block;
  margin-top: 44rpx;
  color: #718578;
  font-size: 26rpx;
  text-align: center;
}
</style>
