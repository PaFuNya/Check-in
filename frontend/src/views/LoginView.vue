<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const studentId = ref('')
const password = ref('')
const rememberMe = ref(false)

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
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1>校园寝室签到系统</h1>
      <p class="subtitle">请使用学号和密码登录</p>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="studentId">学号</label>
          <input
            id="studentId"
            v-model="studentId"
            type="text"
            placeholder="请输入学号"
            autocomplete="username"
          />
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </div>

        <div class="form-group checkbox">
          <label>
            <input v-model="rememberMe" type="checkbox" />
            记住我
          </label>
        </div>

        <div v-if="authStore.error" class="error-message">
          {{ authStore.error }}
        </div>

        <button type="submit" :disabled="authStore.loading">
          {{ authStore.loading ? '登录中...' : '登录' }}
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
  background: var(--bg-canvas, #F7F6F3);
}

.login-card {
  background: var(--bg-surface, #fff);
  border-radius: var(--radius-lg, 14px);
  box-shadow: var(--shadow-md, 0 2px 8px rgba(0,0,0,0.06));
  padding: 40px;
  width: 100%;
  max-width: 400px;
}

h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a1a);
  margin: 0 0 4px;
}

.subtitle {
  color: var(--text-secondary, #6b7280);
  margin: 0 0 24px;
  font-size: 0.9rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group.checkbox {
  flex-direction: row;
  align-items: center;
}

.form-group.checkbox label {
  flex-direction: row;
  gap: 6px;
  cursor: pointer;
}

label {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-primary, #1a1a1a);
}

input[type="text"],
input[type="password"] {
  padding: 10px 12px;
  border: 1px solid var(--border, #E5E5E3);
  border-radius: var(--radius-sm, 6px);
  font-size: 0.95rem;
  background: var(--bg-surface, #fff);
  color: var(--text-primary, #1a1a1a);
  transition: border-color 0.2s;
}

input:focus {
  outline: none;
  border-color: var(--accent, #2563EB);
}

.error-message {
  color: var(--error, #DC2626);
  font-size: 0.85rem;
  padding: 8px 12px;
  background: var(--error-light, #FEF2F2);
  border-radius: var(--radius-sm, 6px);
}

button[type="submit"] {
  padding: 12px;
  background: var(--accent, #2563EB);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm, 6px);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

button[type="submit"]:hover:not(:disabled) {
  background: var(--accent-hover, #1d4ed8);
}

button[type="submit"]:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
