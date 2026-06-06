<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

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

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  // Load saved theme
  const saved = localStorage.getItem('theme')
  if (saved === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
})

onUnmounted(() => {
  clearInterval(clockTimer)
})

// Theme toggle
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
  { path: '/', label: '首页', icon: '🏠' },
  { path: '/checkin', label: '签到', icon: '✅' },
  { path: '/records', label: '记录', icon: '📋' },
  { path: '/chat', label: 'AI助手', icon: '🤖' },
]
</script>

<template>
  <div class="layout-root min-h-screen">
    <!-- Glassmorphism Navbar -->
    <nav class="navbar-glass fixed top-0 left-0 right-0 z-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <!-- Logo / Title -->
          <div class="flex items-center gap-3">
            <span class="text-xl font-bold bg-gradient-to-r from-blue-500 to-purple-600 bg-clip-text text-transparent">
              📌 寝室签到
            </span>
          </div>

          <!-- Nav Links -->
          <div class="hidden md:flex items-center gap-1">
            <router-link
              v-for="item in navItems"
              :key="item.path"
              :to="item.path"
              class="nav-link"
              :class="{ 'nav-link-active': route.path === item.path }"
            >
              <span class="mr-1">{{ item.icon }}</span>
              {{ item.label }}
            </router-link>
          </div>

          <!-- Right side: clock, theme, user, logout -->
          <div class="flex items-center gap-3">
            <!-- Clock -->
            <span class="text-sm font-mono text-gray-600 dark:text-gray-300 hidden sm:inline">
              🕐 {{ clock }}
            </span>

            <!-- Theme Toggle -->
            <button
              @click="toggleTheme"
              class="glass-btn p-2 rounded-full"
              :title="isDark ? '切换亮色' : '切换暗色'"
            >
              <span v-if="isDark">☀️</span>
              <span v-else>🌙</span>
            </button>

            <!-- User -->
            <span class="text-sm text-gray-700 dark:text-gray-200 hidden sm:inline">
              {{ authStore.displayName }}
            </span>

            <!-- Logout -->
            <button
              @click="handleLogout"
              class="glass-btn px-3 py-1.5 rounded-lg text-sm text-red-500 hover:text-red-600"
            >
              退出
            </button>
          </div>
        </div>
      </div>

      <!-- Mobile nav -->
      <div class="md:hidden flex justify-center gap-2 pb-2 px-4">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link text-xs"
          :class="{ 'nav-link-active': route.path === item.path }"
        >
          <span>{{ item.icon }}</span>
        </router-link>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="pt-20 pb-8 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.layout-root {
  background: linear-gradient(135deg, #e0e7ff 0%, #f0e6ff 50%, #dbeafe 100%);
  transition: background 0.3s;
}

:root.dark .layout-root,
.dark .layout-root {
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #0f172a 100%);
}

.navbar-glass {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.06);
}

:root.dark .navbar-glass,
.dark .navbar-glass {
  background: rgba(15, 23, 42, 0.7);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.3);
}

.nav-link {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 0.75rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  color: #475569;
  transition: all 0.2s;
}

.nav-link:hover {
  background: rgba(99, 102, 241, 0.1);
  color: #4f46e5;
}

:root.dark .nav-link,
.dark .nav-link {
  color: #cbd5e1;
}

:root.dark .nav-link:hover,
.dark .nav-link:hover {
  background: rgba(129, 140, 248, 0.15);
  color: #a5b4fc;
}

.nav-link-active {
  background: rgba(99, 102, 241, 0.15);
  color: #4f46e5;
  font-weight: 600;
}

:root.dark .nav-link-active,
.dark .nav-link-active {
  background: rgba(129, 140, 248, 0.2);
  color: #a5b4fc;
}

.glass-btn {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.2s;
}

.glass-btn:hover {
  background: rgba(255, 255, 255, 0.7);
}

:root.dark .glass-btn,
.dark .glass-btn {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.1);
}

:root.dark .glass-btn:hover,
.dark .glass-btn:hover {
  background: rgba(30, 41, 59, 0.8);
}
</style>
