<template>
  <div class="layout-container">
    <!-- Background orbs -->
    <div class="bg-orb orb-1"></div>
    <div class="bg-orb orb-2"></div>
    <div class="bg-orb orb-3"></div>

    <!-- Navbar -->
    <nav class="glass navbar">
      <div class="nav-inner">
        <div class="nav-brand">📋 签到系统</div>
        <div class="nav-time" id="nav-clock">{{ timeStr }}</div>
        <div class="nav-actions">
          <button class="theme-btn" @click="cycleTheme" :title="themeLabel">
            {{ themeIcon }}
          </button>
          <span class="nav-user">{{ auth.studentName || auth.studentId }}</span>
          <button class="btn-logout" @click="handleLogout">退出</button>
        </div>
      </div>
      <!-- Nav links -->
      <div class="nav-links">
        <router-link to="/" class="nav-link" exact-active-class="active">🏠 首页</router-link>
        <router-link to="/checkin" class="nav-link" active-class="active">✅ 签到</router-link>
        <router-link to="/records" class="nav-link" active-class="active">📝 记录</router-link>
        <router-link to="/chat" class="nav-link" active-class="active">💬 AI咨询</router-link>
      </div>
    </nav>

    <!-- Main -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const timeStr = ref('')
let timer = null

const themes = ['light', 'dark', 'system']
const themeIcons = { light: '☀️', dark: '🌙', system: '💻' }
const themeLabels = { light: '浅色模式', dark: '深色模式', system: '跟随系统' }
const currentTheme = ref(localStorage.getItem('theme') || 'system')

const themeIcon = ref(themeIcons[currentTheme.value])
const themeLabel = ref(themeLabels[currentTheme.value])

function updateTime() {
  const now = new Date()
  timeStr.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function applyTheme(t) {
  if (t === 'dark' || (t === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    document.documentElement.setAttribute('data-theme', 'dark')
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}

function cycleTheme() {
  const idx = (themes.indexOf(currentTheme.value) + 1) % themes.length
  currentTheme.value = themes[idx]
  localStorage.setItem('theme', currentTheme.value)
  themeIcon.value = themeIcons[currentTheme.value]
  themeLabel.value = themeLabels[currentTheme.value]
  applyTheme(currentTheme.value)
}

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  applyTheme(currentTheme.value)
})

onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}
.bg-orb {
  position: fixed; border-radius: 50%; filter: blur(80px); opacity: 0.3; pointer-events: none; z-index: 0;
}
.orb-1 { width: 400px; height: 400px; background: var(--accent); top: -100px; left: -100px; animation: float1 20s ease-in-out infinite; }
.orb-2 { width: 300px; height: 300px; background: #ec4899; bottom: -50px; right: -50px; animation: float2 25s ease-in-out infinite; }
.orb-3 { width: 250px; height: 250px; background: #06b6d4; top: 50%; left: 50%; animation: float3 18s ease-in-out infinite; }
@keyframes float1 { 0%,100% { transform: translate(0,0); } 50% { transform: translate(60px,80px); } }
@keyframes float2 { 0%,100% { transform: translate(0,0); } 50% { transform: translate(-50px,-60px); } }
@keyframes float3 { 0%,100% { transform: translate(-50%,-50%); } 50% { transform: translate(-30%,-30%); } }

.navbar {
  position: sticky; top: 0; z-index: 100; padding: 0.5rem 1rem; border-radius: 0;
}
.nav-inner { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
.nav-brand { font-size: 1.1rem; font-weight: 700; color: var(--accent); white-space: nowrap; }
.nav-time { font-size: 0.9rem; color: var(--text-secondary); font-variant-numeric: tabular-nums; }
.nav-actions { display: flex; align-items: center; gap: 0.5rem; }
.theme-btn { background: none; border: none; font-size: 1.2rem; cursor: pointer; min-width: 44px; min-height: 44px; display: flex; align-items: center; justify-content: center; border-radius: 50%; transition: background 0.2s; }
.theme-btn:hover { background: var(--glass-bg); }
.nav-user { font-size: 0.85rem; color: var(--text-secondary); }
.btn-logout {
  background: var(--glass-bg); border: 1px solid var(--glass-border); color: var(--text-primary);
  padding: 0.4rem 0.8rem; border-radius: 0.5rem; cursor: pointer; font-size: 0.85rem; min-height: 44px;
}
.btn-logout:hover { background: var(--error); color: #fff; border-color: var(--error); }
.nav-links { display: flex; gap: 0.25rem; margin-top: 0.5rem; }
.nav-link {
  flex: 1; text-align: center; padding: 0.5rem; border-radius: 0.5rem; color: var(--text-secondary);
  text-decoration: none; font-size: 0.85rem; min-height: 44px; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.nav-link:hover, .nav-link.active { background: var(--glass-bg); color: var(--accent); font-weight: 600; }

.main-content { position: relative; z-index: 1; padding: 1.5rem 1rem; max-width: 640px; margin: 0 auto; }

.page-enter-active, .page-leave-active { transition: opacity 0.3s, transform 0.3s; }
.page-enter-from { opacity: 0; transform: translateY(12px); }
.page-leave-to { opacity: 0; transform: translateY(-12px); }
</style>
