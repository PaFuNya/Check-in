<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

async function handleLogout() {
  await authStore.logout()
  router.push({ name: 'Login' })
}
</script>

<template>
  <div class="home-page">
    <header class="page-header">
      <div>
        <h1>你好，{{ authStore.displayName }}</h1>
        <p class="subtitle">欢迎使用校园寝室签到系统</p>
      </div>
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </header>

    <div class="nav-cards">
      <div class="nav-card" @click="router.push('/checkin')">
        <div class="card-icon">📍</div>
        <h3>签到打卡</h3>
        <p>进行每日寝室签到</p>
      </div>
      <div class="nav-card" @click="router.push('/records')">
        <div class="card-icon">📋</div>
        <h3>签到记录</h3>
        <p>查看历史签到记录</p>
      </div>
      <div class="nav-card" @click="router.push('/chat')">
        <div class="card-icon">💬</div>
        <h3>AI 助手</h3>
        <p>智能问答与帮助</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a1a);
  margin: 0;
}

.subtitle {
  color: var(--text-secondary, #6b7280);
  margin: 4px 0 0;
  font-size: 0.9rem;
}

.logout-btn {
  padding: 8px 16px;
  border: 1px solid var(--border, #E5E5E3);
  border-radius: var(--radius-sm, 6px);
  background: var(--bg-surface, #fff);
  color: var(--text-secondary, #6b7280);
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
}

.logout-btn:hover {
  border-color: var(--error, #DC2626);
  color: var(--error, #DC2626);
}

.nav-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.nav-card {
  background: var(--bg-surface, #fff);
  border-radius: var(--radius-md, 10px);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0,0,0,0.04));
  padding: 24px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.nav-card:hover {
  box-shadow: var(--shadow-md, 0 2px 8px rgba(0,0,0,0.06));
  transform: translateY(-2px);
}

.card-icon {
  font-size: 2rem;
  margin-bottom: 12px;
}

.nav-card h3 {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a1a);
  margin: 0 0 4px;
}

.nav-card p {
  color: var(--text-secondary, #6b7280);
  font-size: 0.85rem;
  margin: 0;
}
</style>
