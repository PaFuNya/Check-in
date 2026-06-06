import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'

export const useAuthStore = defineStore('auth', () => {
  const studentId = ref('')
  const studentName = ref('')
  const loggedIn = ref(false)

  async function checkAuth() {
    try {
      const { data } = await axios.get('/api/auth/check')
      if (data.code === 200 && data.data?.loggedIn) {
        studentId.value = data.data.studentId
        studentName.value = data.data.studentName
        loggedIn.value = true
        return true
      }
    } catch {}
    loggedIn.value = false
    return false
  }

  async function login(id, password, remember) {
    const { data } = await axios.post('/api/auth/login', { studentId: id, password })
    if (data.code === 200) {
      studentId.value = data.data.studentId
      studentName.value = data.data.studentName
      loggedIn.value = true
      if (remember) localStorage.setItem('remember_studentId', id)
      else localStorage.removeItem('remember_studentId')
      return true
    }
    throw new Error(data.message || '登录失败')
  }

  async function logout() {
    try { await axios.post('/api/auth/logout') } catch {}
    loggedIn.value = false
    studentId.value = ''
    studentName.value = ''
  }

  return { studentId, studentName, loggedIn, checkAuth, login, logout }
})
