import { defineStore } from 'pinia'
import { getProfile } from '@/api/auth.js'
import { clearAuthToken, hasAuthToken } from '@/utils/auth-session.js'
import { clearRecordSession } from '@/utils/record-session.js'
import { clearAiChatSession } from '@/utils/ai-chat-session.js'

export const useUserStore = defineStore('user', {
  state: () => ({
    profile: null,
    questionnaireCompleted: false
  }),

  getters: {
    // 判断依据必须是 utils/auth-session.js 里的响应式 token：
    // 直接读 uni.getStorageSync 会被 Pinia 缓存成「首读锁存」，
    // 导致登录成功后页面仍判定为未登录。
    isLoggedIn: () => hasAuthToken()
  },

  actions: {
    setProfile(profile) {
      this.profile = profile
      this.questionnaireCompleted = Boolean(profile?.questionnaireCompleted)
      uni.setStorageSync('user', profile)
    },

    restoreProfile() {
      const profile = uni.getStorageSync('user')
      if (profile) this.setProfile(profile)
    },

    /*
     * 刷新个人资料。
     * 失败时直接把 getProfile() 的错误抛给调用方：由页面通过
     * utils/api-error.js 判断是否真的是登录失效，再决定要不要退出登录。
     * 这里不做吞异常也不清登录态；且只有请求成功才会覆盖 this.profile，
     * 因此刷新失败时资料保持原值，不会被置空。
     */
    async refreshProfile() {
      if (!this.isLoggedIn) return null
      const profile = await getProfile()
      this.setProfile(profile)
      return profile
    },

    clearLoginState() {
      this.profile = null
      this.questionnaireCompleted = false
      // 走统一入口，保证响应式 token 与本地存储同步清空。
      clearAuthToken()
      uni.removeStorageSync('user')
      // 茶记等按账号缓存的数据必须一起清掉，
      // 否则退出登录后常驻的 tab 页仍会显示上一个账号的内容。
      clearRecordSession()
      clearAiChatSession()
    }
  }
})
