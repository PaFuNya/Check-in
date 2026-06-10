<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import gsap from 'gsap'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isDark = ref(false)

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}

function toggleTheme() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

const navItems = [
  { path: '/', label: '首页', icon: 'home' },
  { path: '/checkin', label: '签到', icon: 'checkin' },
  { path: '/chat', label: 'AI助手', icon: 'chat' },
  { path: '/profile', label: '我的', icon: 'profile' },
]

onMounted(() => {
  const saved = localStorage.getItem('theme')
  if (saved === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
  gsap.from('.main-content', { y: 20, opacity: 0, duration: 0.5, delay: 0.1, ease: 'power3.out' })
})
</script>

<template>
  <div class="layout-root">
    <!-- Top bar -->
    <header class="top-bar glass">
      <div class="top-inner">
        <div class="top-brand">
          <div class="brand-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </div>
          <span class="brand-text">寝室签到</span>
        </div>
        <div class="top-actions">
          <button @click="toggleTheme" class="action-btn cursor-pointer" :title="isDark ? '亮色' : '暗色'">
            <svg v-if="isDark" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5" /><line x1="12" y1="1" x2="12" y2="3" /><line x1="12" y1="21" x2="12" y2="23" /><line x1="4.22" y1="4.22" x2="5.64" y2="5.64" /><line x1="18.36" y1="18.36" x2="19.78" y2="19.78" /><line x1="1" y1="12" x2="3" y2="12" /><line x1="21" y1="12" x2="23" y2="12" /><line x1="4.22" y1="19.78" x2="5.64" y2="18.36" /><line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
            </svg>
          </button>
          <button @click="handleLogout" class="action-btn action-btn-logout cursor-pointer" title="退出登录">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>
        </div>
      </div>
    </header>

    <!-- Main content -->
    <main class="main-content">
      <router-view />
    </main>

    <!-- Bottom tab bar -->
    <nav class="bottom-nav glass">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="tab-item cursor-pointer"
        :class="{ 'tab-active': route.path === item.path }"
      >
        <!-- Home -->
        <svg v-if="item.icon === 'home'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
        </svg>
        <!-- Checkin -->
        <svg v-if="item.icon === 'checkin'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
        </svg>
        <!-- Chat -->
        <svg v-if="item.icon === 'chat'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
        <!-- Profile -->
        <svg v-if="item.icon === 'profile'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
        </svg>
        <span class="tab-label">{{ item.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<style scoped>
.layout-root {
  min-height: 100vh;
  background: linear-gradient(135deg, #EFF6FF 0%, #E0E7FF 40%, #F0FDFA 100%);
  transition: background 0.3s;
}
:root.dark .layout-root {
  background: linear-gradient(135deg, #0F172A 0%, #1E1B4B 50%, #0F172A 100%);
}

/* Top bar */
.top-bar {
  position: fixed; top: 0; left: 0; right: 0; z-index: 50;
  border-bottom: 1px solid rgba(255,255,255,0.4);
}
:root.dark .top-bar { background: rgba(15,23,42,0.75); border-bottom-color: rgba(255,255,255,0.06); }
.top-inner { max-width: 1024px; margin: 0 auto; padding: 0 16px; display: flex; align-items: center; justify-content: space-between; height: 56px; }
.top-brand { display: flex; align-items: center; gap: 10px; }
.brand-icon { width: 32px; height: 32px; border-radius: 8px; background: linear-gradient(135deg, #2563EB, #3B82F6); display: flex; align-items: center; justify-content: center; }
.brand-text { font-size: 1rem; font-weight: 700; color: #1E293B; letter-spacing: -0.02em; }
:root.dark .brand-text { color: #F1F5F9; }
.top-actions { display: flex; gap: 8px; }
.action-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; border-radius: 10px; border: none; background: rgba(241,245,249,0.8); color: #64748B; transition: all 0.2s; }
.action-btn:hover { background: rgba(226,232,240,0.9); color: #334155; }
:root.dark .action-btn { background: rgba(30,41,59,0.6); color: #94A3B8; }
:root.dark .action-btn:hover { background: rgba(30,41,59,0.8); color: #CBD5E1; }
.action-btn-logout { color: #EF4444; }
.action-btn-logout:hover { background: rgba(239,68,68,0.1); color: #DC2626; }
:root.dark .action-btn-logout { color: #F87171; }
:root.dark .action-btn-logout:hover { background: rgba(248,113,113,0.15); color: #FCA5A5; }

/* Main content */
.main-content {
  padding: 72px 16px 80px;
  max-width: 1024px;
  margin: 0 auto;
}

/* Bottom nav */
.bottom-nav {
  position: fixed; bottom: 0; left: 0; right: 0; z-index: 50;
  display: flex; justify-content: space-around; align-items: center;
  height: 64px; padding: 0 8px;
  border-top: 1px solid rgba(255,255,255,0.4);
}
:root.dark .bottom-nav { background: rgba(15,23,42,0.85); border-top-color: rgba(255,255,255,0.06); }

.tab-item {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 3px; padding: 8px 16px; border-radius: 12px;
  font-size: 0.6875rem; font-weight: 500;
  color: #94A3B8; text-decoration: none; transition: all 0.2s;
  min-width: 60px; min-height: 52px;
}
.tab-item:hover, .tab-item:active { background: rgba(37,99,235,0.06); }
.tab-active { color: #2563EB; background: rgba(37,99,235,0.08); }
:root.dark .tab-active { color: #60A5FA; background: rgba(37,99,235,0.12); }
.tab-label { line-height: 1; }
</style>
