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
const statusError = ref(false)
const showFaceBanner = ref(false)

async function fetchStatus() {
  statusLoading.value = true
  statusError.value = false
  try {
    const res = await axios.get('/api/checkin/status')
    if (res.data.code === 200) {
      checkedIn.value = res.data.data.checkedIn
      checkInCount.value = res.data.data.count
    } else {
      statusError.value = true
    }
  } catch {
    statusError.value = true
  } finally {
    statusLoading.value = false
  }
}

const statusText = computed(() => {
  if (statusLoading.value) return '加载中'
  if (statusError.value) return '获取失败'
  return checkedIn.value ? '已签到' : '未签到'
})

const statusClass = computed(() => {
  if (statusLoading.value || statusError.value) return 'badge-muted'
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
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${weekdays[d.getDay()]}`
})

// GSAP breathe
const btnRef = ref(null)
let breatheTl = null

onMounted(async () => {
  fetchStatus()

  // 检查人脸注册状态
  try {
    const { data } = await axios.get('/api/auth/profile')
    if (data.code === 200 && !data.data.faceRegistered && data.data.faceImageUrl !== 'registered') {
      showFaceBanner.value = true
    }
  } catch { /* silent */ }

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

    <!-- Face registration banner -->
    <section v-if="showFaceBanner" class="home-section face-banner" @click="router.push('/face-register')">
      <div class="face-banner-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
        </svg>
      </div>
      <div class="face-banner-text">
        <span class="face-banner-title">请先注册人脸</span>
        <span class="face-banner-desc">注册后才能使用人脸签到功能</span>
      </div>
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
    </section>

    <!-- Status Card -->
    <section class="home-section glass-card status-card">
      <div class="status-top">
        <span class="status-label">签到状态</span>
        <span class="status-badge" :class="statusClass">{{ statusText }}</span>
      </div>
      <div class="status-body">
        <!-- Skeleton loading -->
        <template v-if="statusLoading">
          <div class="status-count">
            <span class="skeleton skeleton-num"></span>
            <span class="count-unit">次</span>
          </div>
          <div class="skeleton skeleton-hint"></div>
        </template>
        <!-- Error state -->
        <template v-else-if="statusError">
          <div class="status-count">
            <span class="count-num">--</span>
            <span class="count-unit">次</span>
          </div>
          <button class="retry-link" @click="fetchStatus">重新获取</button>
        </template>
        <!-- Normal -->
        <template v-else>
          <div class="status-count">
            <span class="count-num">{{ checkInCount }}</span>
            <span class="count-unit">次</span>
          </div>
          <div class="status-hint">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" />
            </svg>
            <span>{{ checkedIn ? '今日签到完成' : '记得及时签到' }}</span>
          </div>
        </template>
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
  color: var(--color-text);
  margin: 0 0 2px;
  letter-spacing: -0.02em;
}

.greeting-date {
  color: var(--color-text-muted);
  font-size: 0.875rem;
  margin: 0;
}

/* Face registration banner */
.face-banner {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; border-radius: 14px;
  background: linear-gradient(135deg, #EFF6FF, #DBEAFE);
  border: 1px solid rgba(37,99,235,0.15);
  cursor: pointer; transition: all 0.2s;
}
.face-banner:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(37,99,235,0.15); }
:root.dark .face-banner { background: linear-gradient(135deg, rgba(37,99,235,0.1), rgba(37,99,235,0.05)); border-color: rgba(37,99,235,0.2); }
.face-banner-icon { width: 40px; height: 40px; border-radius: 10px; background: rgba(37,99,235,0.1); color: #2563EB; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.face-banner-text { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.face-banner-title { font-size: 0.875rem; font-weight: 600; color: #1E40AF; }
:root.dark .face-banner-title { color: #60A5FA; }
.face-banner-desc { font-size: 0.75rem; color: #3B82F6; }
.face-banner svg:last-child { color: #3B82F6; flex-shrink: 0; }

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
  color: var(--color-text-muted);
  font-weight: 500;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 12px;
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 600;
}

.badge-success { background: var(--color-success-light); color: var(--color-success); }
.badge-error { background: var(--color-error-light); color: var(--color-error); }
.badge-muted { background: #F1F5F9; color: var(--color-text-light); }
.dark .badge-muted { background: rgba(100, 116, 139, 0.15); }

.status-body { display: flex; align-items: baseline; justify-content: space-between; }

.status-count { display: flex; align-items: baseline; gap: 4px; }

.count-num {
  font-size: 2.25rem;
  font-weight: 800;
  color: var(--color-text);
  line-height: 1;
  letter-spacing: -0.03em;
}

.count-unit { font-size: 0.8125rem; color: var(--color-text-light); }

.status-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8125rem;
  color: var(--color-text-light);
}

.retry-link {
  background: none;
  border: none;
  color: var(--color-primary);
  font-size: 0.8125rem;
  cursor: pointer;
  font-family: inherit;
  padding: 0;
}

.retry-link:hover {
  text-decoration: underline;
}

/* Skeleton loading */
.skeleton {
  background: linear-gradient(90deg, #E2E8F0 25%, #F1F5F9 50%, #E2E8F0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
}
.dark .skeleton {
  background: linear-gradient(90deg, rgba(255,255,255,0.06) 25%, rgba(255,255,255,0.1) 50%, rgba(255,255,255,0.06) 75%);
  background-size: 200% 100%;
}

.skeleton-num {
  width: 60px;
  height: 36px;
}

.skeleton-hint {
  width: 120px;
  height: 16px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
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
  background: linear-gradient(135deg, var(--color-success), #10B981);
  box-shadow: 0 0 8px rgba(5, 150, 105, 0.15);
}

.checkin-done:hover { background: linear-gradient(135deg, #047857, var(--color-success)); }

.checkin-text { font-size: 1rem; font-weight: 700; letter-spacing: 0.02em; }

.checkin-hint {
  margin-top: 12px;
  font-size: 0.8125rem;
  color: var(--color-text-light);
}

/* Section title */
.section-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 12px;
}

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

.dark .action-card:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.action-icon-ai {
  background: var(--color-success-light);
  color: var(--color-success);
}

.action-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
}

.action-desc {
  font-size: 0.75rem;
  color: var(--color-text-light);
}

@media (max-width: 480px) {
  .actions-grid { grid-template-columns: 1fr; }
  .checkin-btn { width: 108px; height: 108px; }
}
</style>
