<template>
  <div class="login-page">
    <div class="bg-orb orb-a"></div>
    <div class="bg-orb orb-b"></div>
    <div class="bg-orb orb-c"></div>

    <div class="glass login-card" ref="cardRef">
      <h1 class="login-title">🎓 校园签到系统</h1>
      <p class="login-sub">请使用学号登录</p>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="field">
          <label>学号</label>
          <input v-model="studentId" class="input-field" placeholder="请输入学号" autocomplete="username" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="password" type="password" class="input-field" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <label class="remember-row">
          <input type="checkbox" v-model="remember" />
          <span>记住我</span>
        </label>
        <button type="submit" class="btn-primary login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
        <p v-if="error" class="error-msg">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import gsap from 'gsap'

const auth = useAuthStore()
const router = useRouter()
const cardRef = ref(null)

const studentId = ref(localStorage.getItem('remember_studentId') || '')
const password = ref('')
const remember = ref(!!localStorage.getItem('remember_studentId'))
const loading = ref(false)
const error = ref('')

onMounted(() => {
  gsap.from(cardRef.value, { y: 40, opacity: 0, duration: 0.8, ease: 'power3.out' })
})

async function handleLogin() {
  if (!studentId.value || !password.value) { error.value = '请填写完整信息'; return }
  loading.value = true; error.value = ''
  try {
    await auth.login(studentId.value, password.value, remember.value)
    router.push('/')
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-canvas); position: relative; overflow: hidden;
}
.bg-orb { position: fixed; border-radius: 50%; filter: blur(80px); opacity: 0.35; pointer-events: none; }
.orb-a { width: 350px; height: 350px; background: #6366f1; top: -80px; left: -80px; animation: fa 18s ease-in-out infinite; }
.orb-b { width: 280px; height: 280px; background: #ec4899; bottom: -60px; right: -60px; animation: fb 22s ease-in-out infinite; }
.orb-c { width: 200px; height: 200px; background: #06b6d4; top: 60%; left: 60%; animation: fc 15s ease-in-out infinite; }
@keyframes fa { 0%,100%{transform:translate(0,0)} 50%{transform:translate(80px,60px)} }
@keyframes fb { 0%,100%{transform:translate(0,0)} 50%{transform:translate(-60px,-80px)} }
@keyframes fc { 0%,100%{transform:translate(0,0)} 50%{transform:translate(-40px,40px)} }

.login-card {
  width: 100%; max-width: 400px; padding: 2.5rem 2rem; z-index: 1; margin: 1rem;
}
.login-title { text-align: center; font-size: 1.5rem; font-weight: 700; color: var(--accent); margin-bottom: 0.25rem; }
.login-sub { text-align: center; color: var(--text-secondary); font-size: 0.9rem; margin-bottom: 1.5rem; }
.login-form { display: flex; flex-direction: column; gap: 1rem; }
.field label { display: block; font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 0.25rem; font-weight: 500; }
.remember-row { display: flex; align-items: center; gap: 0.5rem; font-size: 0.85rem; color: var(--text-secondary); cursor: pointer; }
.remember-row input { accent-color: var(--accent); }
.login-btn { width: 100%; margin-top: 0.5rem; font-size: 1rem; }
.error-msg { color: var(--error); text-align: center; font-size: 0.85rem; }
</style>
