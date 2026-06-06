<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import gsap from 'gsap'

const router = useRouter()
const authStore = useAuthStore()

const studentId = ref('')
const password = ref('')
const rememberMe = ref(false)
const cardRef = ref(null)
const formRef = ref(null)

async function handleLogin() {
  if (!studentId.value || !password.value) {
    authStore.error = '请输入学号和密码'
    return
  }
  const success = await authStore.login(studentId.value, password.value, rememberMe.value)
  if (success) {
    router.push({ name: 'Home' })
  }
}

onMounted(() => {
  // Restore saved studentId
  const saved = localStorage.getItem('rememberedStudentId')
  if (saved) {
    studentId.value = saved
    rememberMe.value = true
  }

  // GSAP entrance
  const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })
  tl.from(cardRef.value, { y: 60, opacity: 0, duration: 0.8 })
  tl.from(formRef.value?.querySelectorAll('.form-field, .form-check, .error-msg, .login-btn'), {
    y: 24, opacity: 0, duration: 0.45, stagger: 0.08
  }, '-=0.3')
})
</script>

<template>
  <div class="login-page">
    <!-- Decorative orbs -->
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>
    <div class="orb orb-3"></div>

    <div ref="cardRef" class="login-card glass-card">
      <!-- Header -->
      <div class="login-header">
        <div class="login-logo">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
            <polyline points="22 4 12 14.01 9 11.01" />
          </svg>
        </div>
        <h1 class="login-title">校园寝室签到</h1>
        <p class="login-subtitle">使用学号和密码登录系统</p>
      </div>

      <!-- Form -->
      <form ref="formRef" @submit.prevent="handleLogin" class="login-form">
        <!-- Student ID -->
        <div class="form-field">
          <label for="studentId" class="field-label">学号</label>
          <div class="input-wrapper">
            <span class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
            </span>
            <input
              id="studentId"
              v-model="studentId"
              type="text"
              class="field-input"
              placeholder="请输入学号"
              autocomplete="username"
            />
          </div>
        </div>

        <!-- Password -->
        <div class="form-field">
          <label for="password" class="field-label">密码</label>
          <div class="input-wrapper">
            <span class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            </span>
            <input
              id="password"
              v-model="password"
              type="password"
              class="field-input"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
          </div>
        </div>

        <!-- Remember me + error -->
        <div class="form-meta">
          <label class="form-check">
            <input v-model="rememberMe" type="checkbox" class="check-input" />
            <span class="check-box"></span>
            <span class="check-label">记住我</span>
          </label>
        </div>

        <!-- Error -->
        <div v-if="authStore.error" class="error-msg">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="15" y1="9" x2="9" y2="15" />
            <line x1="9" y1="9" x2="15" y2="15" />
          </svg>
          <span>{{ authStore.error }}</span>
        </div>

        <!-- Submit -->
        <button type="submit" class="login-btn" :disabled="authStore.loading">
          <span v-if="authStore.loading" class="spinner"></span>
          <span v-else>登录</span>
          <span v-if="authStore.loading">登录中...</span>
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
  background: linear-gradient(135deg, #EFF6FF 0%, #E0E7FF 50%, #F0FDFA 100%);
  position: relative;
  overflow: hidden;
  padding: 24px;
}

:root.dark .login-page {
  background: linear-gradient(135deg, #0F172A 0%, #1E1B4B 50%, #0F172A 100%);
}

/* Orbs */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  pointer-events: none;
  animation: orbFloat 10s ease-in-out infinite;
}
.orb-1 { width: 400px; height: 400px; background: #818CF8; top: -100px; left: -100px; }
.orb-2 { width: 300px; height: 300px; background: #38BDF8; bottom: -60px; right: -60px; animation-delay: -3s; }
.orb-3 { width: 250px; height: 250px; background: #34D399; top: 50%; left: 50%; transform: translate(-50%, -50%); animation-delay: -5s; opacity: 0.2; }

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(20px, -20px) scale(1.04); }
  66% { transform: translate(-15px, 15px) scale(0.96); }
}

/* Card */
.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 36px;
  position: relative;
  z-index: 1;
  border-radius: 20px;
}

:root.dark .login-card {
  background: rgba(15, 23, 42, 0.6);
  border-color: rgba(255, 255, 255, 0.1);
}

/* Header */
.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #2563EB, #3B82F6);
  color: white;
  margin-bottom: 16px;
}

.login-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0 0 4px;
  letter-spacing: -0.02em;
}

:root.dark .login-title { color: #F1F5F9; }

.login-subtitle {
  color: #64748B;
  font-size: 0.875rem;
  margin: 0;
}

/* Form */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}

:root.dark .field-label { color: #CBD5E1; }

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  color: #94A3B8;
  pointer-events: none;
  display: flex;
  align-items: center;
}

.field-input {
  width: 100%;
  padding: 12px 14px 12px 44px;
  border: 1.5px solid #E2E8F0;
  border-radius: 12px;
  font-size: 0.9375rem;
  background: #F8FAFC;
  color: #1E293B;
  transition: all 0.2s;
  outline: none;
  min-height: 44px;
  font-family: inherit;
}

:root.dark .field-input {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
  color: #E2E8F0;
}

.field-input:focus {
  border-color: #2563EB;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
  background: white;
}

:root.dark .field-input:focus {
  background: rgba(30, 41, 59, 0.8);
}

.field-input::placeholder {
  color: #94A3B8;
}

/* Checkbox */
.form-meta {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.form-check {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
  min-height: 44px;
}

.check-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.check-box {
  width: 18px;
  height: 18px;
  border: 1.5px solid #CBD5E1;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
  position: relative;
}

:root.dark .check-box { border-color: #475569; }

.check-input:checked + .check-box {
  background: #2563EB;
  border-color: #2563EB;
}

.check-input:checked + .check-box::after {
  content: '';
  width: 10px;
  height: 6px;
  border: 2px solid white;
  border-top: none;
  border-right: none;
  transform: rotate(-45deg);
  position: absolute;
  top: 2px;
}

.check-input:focus-visible + .check-box {
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.check-label {
  font-size: 0.8125rem;
  color: #64748B;
}

:root.dark .check-label { color: #94A3B8; }

/* Error */
.error-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #DC2626;
  font-size: 0.8125rem;
  padding: 10px 14px;
  background: #FEF2F2;
  border: 1px solid #FECACA;
  border-radius: 10px;
}

:root.dark .error-msg {
  background: rgba(220, 38, 38, 0.1);
  border-color: rgba(220, 38, 38, 0.2);
}

/* Submit */
.login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  min-height: 48px;
  background: linear-gradient(135deg, #2563EB, #3B82F6);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
  margin-top: 4px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.4);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 480px) {
  .login-card { padding: 32px 24px; }
}
</style>
