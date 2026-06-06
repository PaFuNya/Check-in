import { defineStore } from 'pinia'
import axios from 'axios'

const API_BASE = '/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    isLoggedIn: false,
    authChecked: false,
    loading: false,
    error: null
  }),

  getters: {
    studentId: (state) => state.user?.studentId || null,
    studentName: (state) => state.user?.studentName || null,
    displayName: (state) => state.user?.studentName || state.user?.studentId || '未登录'
  },

  actions: {
    /**
     * Login with student ID and password
     * @param {string} studentId
     * @param {string} password
     * @param {boolean} rememberMe
     * @returns {Promise<boolean>} success
     */
    async login(studentId, password, rememberMe = false) {
      this.loading = true
      this.error = null
      try {
        const res = await axios.post(`${API_BASE}/login`, {
          studentId,
          password,
          rememberMe
        })
        if (res.data.code === 200) {
          this.user = res.data.data
          this.isLoggedIn = true
          this.authChecked = true
          return true
        } else {
          this.error = res.data.message || '登录失败'
          return false
        }
      } catch (err) {
        if (err.response) {
          const data = err.response.data
          this.error = data?.message || `登录失败 (${err.response.status})`
        } else {
          this.error = '网络错误，请检查后端服务'
        }
        return false
      } finally {
        this.loading = false
      }
    },

    /**
     * Logout current user
     */
    async logout() {
      try {
        await axios.post(`${API_BASE}/logout`)
      } catch {
        // Ignore logout API errors — clear local state anyway
      }
      this.user = null
      this.isLoggedIn = false
      this.authChecked = true
      this.error = null
    },

    /**
     * Check if user is currently authenticated (session-based)
     * Called on app startup / route guard
     */
    async checkAuth() {
      this.loading = true
      try {
        const res = await axios.get(`${API_BASE}/check`)
        if (res.data.code === 200 && res.data.data?.loggedIn) {
          this.isLoggedIn = true
          this.user = {
            studentId: res.data.data.studentId,
            studentName: res.data.data.studentName,
            className: res.data.data.className,
            phoneNumber: res.data.data.phoneNumber,
            avatarUrl: res.data.data.avatarUrl
          }
        } else {
          this.isLoggedIn = false
          this.user = null
        }
      } catch {
        this.isLoggedIn = false
        this.user = null
      } finally {
        this.authChecked = true
        this.loading = false
      }
    },

    /**
     * Fetch full profile data (with dorm info)
     */
    async fetchProfile() {
      try {
        const res = await axios.get(`${API_BASE}/profile`)
        if (res.data.code === 200) {
          this.user = res.data.data
        }
      } catch {
        // Silently fail — user data remains from login/check
      }
    },

    /**
     * Clear error message
     */
    clearError() {
      this.error = null
    }
  }
})
