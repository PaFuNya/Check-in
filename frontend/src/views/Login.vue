<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import gsap from 'gsap'

const router = useRouter()
const authStore = useAuthStore()
const cardRef = ref(null)

const form = reactive({
  studentId: '',
  password: '',
  rememberMe: false,
})

const showPwd = ref(false)

async function handleLogin() {
  if (!form.studentId || !form.password) return
  authStore.clearError()
  const ok = await authStore.login(form.studentId, form.password, form.rememberMe)
  if (ok) {
    router.push('/')
  }
}

onMounted(() => {
  const saved = localStorage.getItem('rememberedStudentId')
  if (saved) {
    form.studentId = saved
    form.rememberMe = true
  }

  if (cardRef.value) {
    gsap.from(cardRef.value, { y: 60, opacity: 0, duration: 0.8, ease: 'power3.out' })
    gsap.from('.form-field', { y: 20, opacity: 0, duration: 0.45, stagger: 0.1, delay: 0.3, ease: 'power2.out' })
  }
})
</script>

<template>
  <div class="login-page">
    <!-- Decorative orbs -->
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>
    <div class="orb orb-3"></div>

    <div class="login-card glass-card" ref="cardRef">
      <div class="login-header">
        <div class="login-logo">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <h1 class="login-title">校园寝室签到</h1>
        <p class="login-subtitle">使用学号和密码登录系统</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-field">
          <label class="field-label">学号</label>
          <div class="input-wrapper">
            <span class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </span>
            <input
              v-model="form.studentId"
              type="text"
              class="field-input"
              placeholder="请输入学号"
              required
              autofocus
              autocomplete="username"
            />
          </div>
        </div>

        <div class="form-field">
          <label class="field-label">密码</label>
          <div class="input-wrapper">
            <span class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </span>
            <input
              v-model="form.password"
              :type="showPwd ? 'text' : 'password'"
              class="field-input"
              placeholder="请输入密码"
              required
              autocomplete="current-password"
              @keydown.enter="handleLogin"
            />
            <button type="button" class="pwd-toggle" @click="showPwd = !showPwd" tabindex="-1">
              <svg v-if="!showPwd" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
            </button>
          </div>
        </div>

        <div class="form-meta">
          <label class="form-check">
            <input type="checkbox" v-model="form.rememberMe" />
            <span class="check-box">
              <svg v-if="form.rememberMe" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
            </span>
            <span class="check-label">记住我</span>
          </label>
        </div>

        <div v-if="authStore.error" class="error-msg">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <span>{{ authStore.error }}</span>
        </div>

        <button type="submit" class="login-btn" :disabled="authStore.loading">
          <template v-if="authStore.loading">
            <span class="spinner"></span>
            <span>登录中...</span>
          </template>
          <span v-else>登录</span>
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #EFF6FF 0%, #E0E7FF 50%, #F0FDFA 100%);
  padding: 24px;
  position: relative;
  overflow: hidden;
}
.dark .login-page {
  background: linear-gradient(135deg, #0F172A 0%, #1E1B4B 50%, #0F172A 100%);
}

/* Decorative orbs */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  pointer-events: none;
}
.orb-1 { width: 300px; height: 300px; background: #818CF8; top: -80px; left: -60px; animation: float 8s ease-in-out infinite; }
.orb-2 { width: 250px; height: 250px; background: #38BDF8; top: 40%; right: -80px; animation: float 10s ease-in-out infinite 1s; }
.orb-3 { width: 200px; height: 200px; background: #34D399; bottom: -40px; left: 30%; animation: float 9s ease-in-out infinite 2s; }
.dark .orb { opacity: 0.25; }

@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-20px) scale(1.05); }
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 36px;
  border-radius: 20px;
  background: rgba(255,255,255,0.8);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255,255,255,0.4);
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
  position: relative;
  z-index: 1;
}
.dark .login-card {
  background: rgba(30,41,59,0.55);
  border: 1px solid rgba(255,255,255,0.08);
  box-shadow: 0 4px 24px rgba(0,0,0,0.25);
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}
.login-logo {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #2563EB, #3B82F6);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(37,99,235,0.3);
}
.login-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text);
  margin: 0 0 6px;
  letter-spacing: -0.02em;
}
.login-subtitle {
  font-size: 0.875rem;
  color: var(--color-text-light);
  margin: 0;
}

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
  color: var(--color-text-muted);
}
.input-wrapper {
  position: relative;
}
.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-light);
  display: flex;
  pointer-events: none;
}

.pwd-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--color-text-light);
  cursor: pointer;
  padding: 4px;
  display: flex;
}

.form-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-check {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  cursor: pointer;
}
.form-check input[type="checkbox"] {
  display: none;
}
.check-box {
  width: 18px;
  height: 18px;
  border: 1.5px solid var(--color-border);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}
.form-check input:checked + .check-box {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.check-label {
  font-size: 0.8125rem;
  color: var(--color-text-light);
}

.error-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--color-error-light);
  color: var(--color-error);
  border-radius: 10px;
  font-size: 0.8125rem;
  font-weight: 500;
}

.login-btn {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  min-height: 48px;
  background: linear-gradient(135deg, #2563EB, #3B82F6);
  color: #FFFFFF;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(37,99,235,0.3);
}
.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(37,99,235,0.4);
}
.login-btn:active:not(:disabled) {
  transform: translateY(0);
}
.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 480px) {
  .login-card {
    padding: 32px 24px;
  }
}
</style>
