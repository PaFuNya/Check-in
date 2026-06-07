<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from 'axios'
import gsap from 'gsap'

const router = useRouter()
const authStore = useAuthStore()

const checkedIn = ref(false)
const checkInCount = ref(0)
const statusLoading = ref(true)

async function fetchStatus() {
  statusLoading.value = true
  try {
    const res = await axios.get('/api/checkin/status')
    if (res.data.code === 200) {
      checkedIn.value = res.data.data.checkedIn
      checkInCount.value = res.data.data.count
    }
  } catch { /* silent */ } finally {
    statusLoading.value = false
  }
}

const statusText = computed(() => {
  if (statusLoading.value) return '加载中'
  return checkedIn.value ? '已签到' : '未签到'
})

const statusClass = computed(() => {
  if (statusLoading.value) return 'badge-muted'
  return checkedIn.value ? 'badge-success' : 'badge-error'
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  if (h < 22) return '晚上好'
  return '夜深了'
})

const today = computed(() => {
  const d = new Date()
  const weekday = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${weekday}`
})

// GSAP breathe
const btnRef = ref(null)
let breatheTl = null

onMounted(() => {
  fetchStatus()

  // Entrance
  gsap.from('.home-section', { y: 30, opacity: 0, duration: 0.6, stagger: 0.1, ease: 'power3.out' })

  if (btnRef.value) {
    breatheTl = gsap.timeline({ repeat: -1, yoyo: true })
    breatheTl.to(btnRef.value, {
      scale: 1.06,
      boxShadow: '0 0 32px rgba(37, 99, 235, 0.35)',
      duration: 1.5,
      ease: 'sine.inOut',
    })
    breatheTl.to(btnRef.value, {
      scale: 1.0,
      boxShadow: '0 0 8px rgba(37, 99, 235, 0.1)',
      duration: 1.5,
      ease: 'sine.inOut',
    })
  }
})

onUnmounted(() => {
  if (breatheTl) breatheTl.kill()
})
</script>

<template>
  <div class="home-page">
    <!-- Greeting -->
    <section class="home-section greeting-section">
      <h1 class="greeting-title">{{ greeting }}，{{ authStore.displayName }}</h1>
      <p class="greeting-date">{{ today }}</p>
    </section>

    <!-- Status Card -->
    <section class="home-section glass-card status-card">
      <div class="status-top">
        <span class="status-label">签到状态</span>
        <span class="status-badge" :class="statusClass">{{ statusText }}</span>
      </div>
      <div class="status-body">
        <div class="status-count">
          <span class="count-num">{{ statusLoading ? '--' : checkInCount }}</span>
          <span class="count-unit">次</span>
        </div>
        <div class="status-hint">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" />
          </svg>
          <span>{{ checkedIn ? '今日签到完成' : '记得及时签到' }}</span>
        </div>
      </div>
    </section>

    <!-- Check-in Button -->
    <section class="home-section checkin-action">
      <button ref="btnRef" class="checkin-btn cursor-pointer" :class="{ 'checkin-done': checkedIn }" @click="router.push('/checkin')">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
          <polyline points="22 4 12 14.01 9 11.01" />
        </svg>
        <span class="checkin-text">{{ checkedIn ? '已签到' : '签到' }}</span>
      </button>
      <p class="checkin-hint">点击进行寝室签到</p>
    </section>

    <!-- Quick Actions -->
    <section class="home-section">
      <h2 class="section-title">快捷操作</h2>
      <div class="actions-grid">
        <button class="action-card glass-card cursor-pointer" @click="router.push('/checkin')">
          <div class="action-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </div>
          <span class="action-name">签到打卡</span>
          <span class="action-desc">人脸识别 + GPS 定位</span>
        </button>

        <button class="action-card glass-card cursor-pointer" @click="router.push('/records')">
          <div class="action-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" />
            </svg>
          </div>
          <span class="action-name">签到记录</span>
          <span class="action-desc">查看历史签到</span>
        </button>

        <button class="action-card glass-card cursor-pointer" @click="router.push('/chat')">
          <div class="action-icon action-icon-ai">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4Z" /><path d="M6 10a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1a8 8 0 0 1-12 0v-1Z" /><path d="M9 18h6" /><path d="M10 22h4" />
            </svg>
          </div>
          <span class="action-name">AI 助手</span>
          <span class="action-desc">智能问答与帮助</span>
        </button>

        <button class="action-card glass-card cursor-pointer" @click="router.push('/profile')">
          <div class="action-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
            </svg>
          </div>
          <span class="action-name">个人信息</span>
          <span class="action-desc">查看与编辑资料</span>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 0 0 32px;
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.home-section {
  margin-bottom: 20px;
}

.home-section:last-child {
  margin-top: auto;
  margin-bottom: 0;
}

/* Greeting */
.greeting-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: #1E293B;
  margin: 0 0 2px;
  letter-spacing: -0.02em;
}

:root.dark .greeting-title { color: #F1F5F9; }

.greeting-date {
  color: #64748B;
  font-size: 0.8125rem;
  margin: 0;
}

/* Status Card */
.status-card {
  padding: 20px 24px;
}

.status-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.status-label {
  font-size: 0.8125rem;
  color: #64748B;
  font-weight: 500;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 12px;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.badge-success { background: #ECFDF5; color: #059669; }
.badge-error { background: #FEF2F2; color: #DC2626; }
.badge-muted { background: #F1F5F9; color: #94A3B8; }

:root.dark .badge-success { background: rgba(5, 150, 105, 0.15); }
:root.dark .badge-error { background: rgba(220, 38, 38, 0.15); }
:root.dark .badge-muted { background: rgba(100, 116, 139, 0.15); }

.status-body { display: flex; align-items: baseline; justify-content: space-between; }

.status-count { display: flex; align-items: baseline; gap: 4px; }

.count-num {
  font-size: 2.25rem;
  font-weight: 800;
  color: #1E293B;
  line-height: 1;
  letter-spacing: -0.03em;
}

:root.dark .count-num { color: #F1F5F9; }

.count-unit { font-size: 0.8125rem; color: #94A3B8; }

.status-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8125rem;
  color: #94A3B8;
}

/* Check-in Button */
.checkin-action { text-align: center; }

.checkin-btn {
  width: 128px;
  height: 128px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #2563EB, #3B82F6);
  color: white;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 0 8px rgba(37, 99, 235, 0.15);
  transition: background 0.2s;
  outline: none;
}

.checkin-btn:hover { background: linear-gradient(135deg, #1D4ED8, #2563EB); }
.checkin-btn:active { transform: scale(0.97); }

.checkin-done {
  background: linear-gradient(135deg, #059669, #10B981);
  box-shadow: 0 0 8px rgba(5, 150, 105, 0.15);
}

.checkin-done:hover { background: linear-gradient(135deg, #047857, #059669); }

.checkin-text { font-size: 1rem; font-weight: 700; letter-spacing: 0.02em; }

.checkin-hint {
  margin-top: 12px;
  font-size: 0.8125rem;
  color: #94A3B8;
}

/* Section title */
.section-title {
  font-size: 1rem;
  font-weight: 700;
  color: #1E293B;
  margin: 0 0 12px;
}

:root.dark .section-title { color: #E2E8F0; }

/* Actions grid */
.actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.action-card {
  padding: 20px 16px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border: none;
  font-family: inherit;
  outline: none;
}

.action-card:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

:root.dark .action-card:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #EFF6FF;
  color: #2563EB;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.action-icon-ai {
  background: #ECFDF5;
  color: #059669;
}

:root.dark .action-icon { background: rgba(37, 99, 235, 0.12); }
:root.dark .action-icon-ai { background: rgba(5, 150, 105, 0.12); }

.action-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #1E293B;
}

:root.dark .action-name { color: #E2E8F0; }

.action-desc {
  font-size: 0.75rem;
  color: #94A3B8;
}

@media (max-width: 480px) {
  .actions-grid { grid-template-columns: 1fr; }
  .checkin-btn { width: 108px; height: 108px; }
}
</style>
