<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import gsap from 'gsap'

const router = useRouter()
const authStore = useAuthStore()

const studentId = ref('')
const password = ref('')
const cardRef = ref(null)
const formRef = ref(null)

async function handleLogin() {
  if (!studentId.value || !password.value) {
    authStore.error = '请输入学号和密码'
    return
  }
  const success = await authStore.login(studentId.value, password.value, false)
  if (success) {
    router.push({ name: 'Home' })
  }
}

onMounted(() => {
  // GSAP fadeInUp entrance animation
  const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })

  tl.from(cardRef.value, {
    y: 60,
    opacity: 0,
    duration: 0.8,
  })

  tl.from(formRef.value?.querySelectorAll('.form-group, .error-message, button[type="submit"]'), {
    y: 30,
    opacity: 0,
    duration: 0.5,
    stagger: 0.1,
  }, '-=0.3')
})
</script>

<template>
  <div class="login-page">
    <!-- Background glow orbs -->
    <div class="glow glow-1"></div>
    <div class="glow glow-2"></div>
    <div class="glow glow-3"></div>

    <div ref="cardRef" class="login-card">
      <div class="card-header">
        <h1>校园寝室签到系统</h1>
        <p class="subtitle">请使用学号和密码登录</p>
      </div>

      <form ref="formRef" @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="studentId">学号</label>
          <div class="input-wrapper">
            <span class="input-icon">👤</span>
            <input
              id="studentId"
              v-model="studentId"
              type="text"
              placeholder="请输入学号"
              autocomplete="username"
            />
          </div>
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <div class="input-wrapper">
            <span class="input-icon">🔒</span>
            <input
              id="password"
              v-model="password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
          </div>
        </div>

        <div v-if="authStore.error" class="error-message">
          {{ authStore.error }}
        </div>

        <button type="submit" :disabled="authStore.loading">
          <span v-if="authStore.loading" class="spinner"></span>
          {{ authStore.loading ? '登录中...' : '登 录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
  position: relative;
  overflow: hidden;
}

/* Background glow orbs */
.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  pointer-events: none;
  animation: float 8s ease-in-out infinite;
}

.glow-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #667eea, #764ba2);
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.glow-2 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, #f093fb, #f5576c);
  bottom: -80px;
  right: -80px;
  animation-delay: -3s;
}

.glow-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #4facfe, #00f2fe);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -5s;
  opacity: 0.3;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(10px, 10px) scale(1.02); }
}

.glow-3 {
  animation-name: float-center;
}

@keyframes float-center {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  25% { transform: translate(-45%, -55%) scale(1.05); }
  50% { transform: translate(-55%, -45%) scale(0.95); }
  75% { transform: translate(-48%, -52%) scale(1.02); }
}

/* Glassmorphism card */
.login-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 20px;
  padding: 48px 40px;
  width: 100%;
  max-width: 420px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 1;
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

h1 {
  font-size: 1.75rem;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.subtitle {
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  font-size: 0.9rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

label {
  font-size: 0.85rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  font-size: 1rem;
  pointer-events: none;
  z-index: 1;
}

input[type="text"],
input[type="password"] {
  width: 100%;
  padding: 14px 14px 14px 44px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  font-size: 0.95rem;
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

input::placeholder {
  color: rgba(255, 255, 255, 0.35);
}

input:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.6);
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

.error-message {
  color: #ff6b6b;
  font-size: 0.85rem;
  padding: 10px 14px;
  background: rgba(255, 107, 107, 0.1);
  border: 1px solid rgba(255, 107, 107, 0.2);
  border-radius: 10px;
  text-align: center;
}

button[type="submit"] {
  padding: 14px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 4px;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

button[type="submit"]:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

button[type="submit"]:active:not(:disabled) {
  transform: translateY(0);
}

button[type="submit"]:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
