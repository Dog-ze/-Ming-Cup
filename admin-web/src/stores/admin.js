import { defineStore } from 'pinia'

const TOKEN_KEY = 'admin_token'
const PROFILE_KEY = 'admin_profile'

function readProfile() {
  try {
    return JSON.parse(localStorage.getItem(PROFILE_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAdminStore = defineStore('admin', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: readProfile(),
  }),
  getters: {
    hasSession: (state) => Boolean(state.token && state.profile),
    isExpired: (state) => !state.profile?.expiresAt || new Date(state.profile.expiresAt).getTime() <= Date.now(),
  },
  actions: {
    restore() {
      this.token = localStorage.getItem(TOKEN_KEY) || ''
      this.profile = readProfile()
    },
    setSession(token, profile) {
      const safeProfile = {
        id: profile.id,
        username: profile.username,
        displayName: profile.displayName,
        expiresAt: profile.expiresAt,
      }
      this.token = token
      this.profile = safeProfile
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(PROFILE_KEY, JSON.stringify(safeProfile))
    },
    clearSession() {
      this.token = ''
      this.profile = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(PROFILE_KEY)
    },
  },
})
