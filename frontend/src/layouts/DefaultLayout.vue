<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import gsap from 'gsap'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// Clock
const clock = ref('')
let clockTimer = null

function updateClock() {
  const now = new Date()
  clock.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

// Theme
const isDark = ref(false)

function toggleTheme() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

// Logout
async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}

// Nav items
const navItems = [
  {
    path: '/',
    label: '首页',
    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`
  },
  {
    path: '/checkin',
    label: '签到',
    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>`
  },
  {
    path: '/records',
    label: '记录',
    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>`
  },
  {
    path: '/chat',
    label: 'AI 助手',
    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4Z"/><path d="M6 10a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1a8 8 0 0 1-12 0v-1Z"/><path d="M9 18h6"/><path d="M10 22h4"/></svg>`
  },
]

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)

  const saved = localStorage.getItem('theme')
  if (saved === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }

  // Entrance animation
  gsap.from('.nav-bar', { y: -20, opacity: 0, duration: 0.5, ease: 'power3.out' })
  gsap.from('.main-content', { y: 20, opacity: 0, duration: 0.5, delay: 0.1, ease: 'power3.out' })
})

onUnmounted(() => {
  clearInterval(clockTimer)
})
</script>

<template>
  <div class="layout-root">
    <!-- Navbar -->
    <nav class="nav-bar glass">
      <div class="nav-inner">
        <!-- Brand -->
        <div class="nav-brand">
          <div class="brand-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </div>
          <span class="brand-text">寝室签到</span>
        </div>

        <!-- Desktop nav -->
        <div class="nav-links">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link cursor-pointer"
            :class="{ 'nav-link-active': route.path === item.path }"
          >
            <span class="nav-link-icon" v-html="item.icon"></span>
            <span class="nav-link-text">{{ item.label }}</span>
          </router-link>
        </div>

        <!-- Right actions -->
        <div class="nav-actions">
          <span class="nav-clock">{{ clock }}</span>

          <button
            @click="toggleTheme"
            class="action-btn cursor-pointer"
            :title="isDark ? '切换亮色' : '切换暗色'"
            :aria-label="isDark ? '切换亮色模式' : '切换暗色模式'"
          >
            <svg v-if="isDark" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5" /><line x1="12" y1="1" x2="12" y2="3" /><line x1="12" y1="21" x2="12" y2="23" /><line x1="4.22" y1="4.22" x2="5.64" y2="5.64" /><line x1="18.36" y1="18.36" x2="19.78" y2="19.78" /><line x1="1" y1="12" x2="3" y2="12" /><line x1="21" y1="12" x2="23" y2="12" /><line x1="4.22" y1="19.78" x2="5.64" y2="18.36" /><line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
            </svg>
          </button>

          <button
            @click="handleLogout"
            class="action-btn action-btn-logout cursor-pointer"
            aria-label="退出登录"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Mobile nav -->
      <div class="nav-mobile">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-mobile-link cursor-pointer"
          :class="{ 'nav-mobile-active': route.path === item.path }"
        >
          <span v-html="item.icon"></span>
          <span class="nav-mobile-label">{{ item.label }}</span>
        </router-link>
      </div>
    </nav>

    <!-- Main -->
    <main class="main-content">
      <router-view />
    </main>
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

/* ── Navbar ── */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 50;
  border-bottom: 1px solid rgba(255, 255, 255, 0.4);
}

:root.dark .nav-bar {
  background: rgba(15, 23, 42, 0.75);
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

.nav-inner {
  max-width: 1024px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}

/* Brand */
.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2563EB, #3B82F6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-text {
  font-size: 1rem;
  font-weight: 700;
  color: #1E293B;
  letter-spacing: -0.02em;
}

:root.dark .brand-text { color: #F1F5F9; }

/* Desktop nav links */
.nav-links {
  display: none;
  align-items: center;
  gap: 4px;
}

@media (min-width: 768px) {
  .nav-links { display: flex; }
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 500;
  color: #64748B;
  text-decoration: none;
  transition: all 0.2s;
  min-height: 44px;
}

.nav-link:hover {
  background: rgba(37, 99, 235, 0.08);
  color: #2563EB;
}

:root.dark .nav-link { color: #94A3B8; }
:root.dark .nav-link:hover { background: rgba(37, 99, 235, 0.12); color: #60A5FA; }

.nav-link-active {
  background: rgba(37, 99, 235, 0.1);
  color: #2563EB;
  font-weight: 600;
}

:root.dark .nav-link-active {
  background: rgba(37, 99, 235, 0.15);
  color: #60A5FA;
}

.nav-link-icon {
  display: flex;
  align-items: center;
}

/* Right actions */
.nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-clock {
  font-size: 0.75rem;
  font-family: ui-monospace, monospace;
  color: #94A3B8;
  display: none;
}

@media (min-width: 640px) {
  .nav-clock { display: inline; }
}

:root.dark .nav-clock { color: #64748B; }

.action-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  border: none;
  background: rgba(241, 245, 249, 0.8);
  color: #64748B;
  transition: all 0.2s;
}

.action-btn:hover {
  background: rgba(226, 232, 240, 0.9);
  color: #334155;
}

:root.dark .action-btn {
  background: rgba(30, 41, 59, 0.6);
  color: #94A3B8;
}

:root.dark .action-btn:hover {
  background: rgba(30, 41, 59, 0.8);
  color: #CBD5E1;
}

.action-btn-logout:hover {
  background: rgba(220, 38, 38, 0.1);
  color: #DC2626;
}

:root.dark .action-btn-logout:hover {
  background: rgba(220, 38, 38, 0.15);
}

/* ── Mobile nav ── */
.nav-mobile {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 4px 16px 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.3);
}

:root.dark .nav-mobile {
  border-top-color: rgba(255, 255, 255, 0.05);
}

@media (min-width: 768px) {
  .nav-mobile { display: none; }
}

.nav-mobile-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 12px;
  border-radius: 10px;
  font-size: 0.6875rem;
  font-weight: 500;
  color: #94A3B8;
  text-decoration: none;
  transition: all 0.2s;
  min-height: 44px;
  min-width: 56px;
  justify-content: center;
}

.nav-mobile-link:hover,
.nav-mobile-link:active {
  background: rgba(37, 99, 235, 0.08);
}

.nav-mobile-active {
  color: #2563EB;
  background: rgba(37, 99, 235, 0.1);
}

:root.dark .nav-mobile-active {
  color: #60A5FA;
  background: rgba(37, 99, 235, 0.15);
}

.nav-mobile-label {
  line-height: 1;
}

/* ── Main Content ── */
.main-content {
  padding-top: 80px;
  padding-bottom: 32px;
  padding-left: 16px;
  padding-right: 16px;
  max-width: 1024px;
  margin: 0 auto;
}

@media (min-width: 768px) {
  .main-content { padding-top: 84px; }
}

@media (max-width: 767px) {
  .main-content { padding-top: 120px; }
}
</style>
