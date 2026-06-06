<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from 'axios'
import gsap from 'gsap'

const router = useRouter()
const authStore = useAuthStore()

// --- Check-in status ---
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
  } catch {
    // silent
  } finally {
    statusLoading.value = false
  }
}

const statusText = computed(() => {
  if (statusLoading.value) return '加载中...'
  return checkedIn.value ? '今日已签到' : '今日未签到'
})

const statusClass = computed(() => {
  if (statusLoading.value) return 'status-loading'
  return checkedIn.value ? 'status-done' : 'status-pending'
})

// --- Greeting ---
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

// --- Date ---
const today = computed(() => {
  const d = new Date()
  const weekday = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${weekday}`
})

// --- GSAP breathe animation ---
const btnRef = ref(null)
let breatheTl = null

onMounted(() => {
  fetchStatus()

  // Breathe animation on the check-in button
  if (btnRef.value) {
    breatheTl = gsap.timeline({ repeat: -1, yoyo: true })
    breatheTl.to(btnRef.value, {
      scale: 1.08,
      boxShadow: '0 0 28px rgba(37, 99, 235, 0.35)',
      duration: 1.4,
      ease: 'sine.inOut',
    })
    breatheTl.to(btnRef.value, {
      scale: 1.0,
      boxShadow: '0 0 8px rgba(37, 99, 235, 0.1)',
      duration: 1.4,
      ease: 'sine.inOut',
    })
  }
})

onUnmounted(() => {
  if (breatheTl) {
    breatheTl.kill()
  }
})

// --- Quick actions ---
function goCheckIn() {
  router.push('/checkin')
}

function goRecords() {
  router.push('/records')
}

function goChat() {
  router.push('/chat')
}

function goProfile() {
  router.push('/profile')
}
</script>

<template>
  <div class="home-page">
    <!-- Header: greeting + date -->
    <section class="greeting-section">
      <h1 class="greeting-title">{{ greeting }}，{{ authStore.displayName }}</h1>
      <p class="greeting-date">{{ today }}</p>
    </section>

    <!-- Check-in Status Card -->
    <section class="status-card">
      <div class="status-header">
        <span class="status-label">签到状态</span>
        <span class="status-badge" :class="statusClass">
          {{ statusText }}
        </span>
      </div>
      <div class="status-body">
        <div class="status-info">
          <div class="status-count">
            <span class="count-number">{{ statusLoading ? '--' : checkInCount }}</span>
            <span class="count-label">今日签到次数</span>
          </div>
          <div v-if="checkedIn" class="status-hint">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1Zm3.22 5.28-3.5 3.5a.75.75 0 0 1-1.06 0l-1.5-1.5a.75.75 0 1 1 1.06-1.06L7.19 9.19l2.97-2.97a.75.75 0 1 1 1.06 1.06Z" fill="currentColor"/>
            </svg>
            <span>签到完成，好好休息</span>
          </div>
          <div v-else class="status-hint">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1Zm0 3a.75.75 0 0 1 .75.75v3.5l2.1 1.26a.75.75 0 1 1-.76 1.3L7.63 9.38A.75.75 0 0 1 7.25 8.75v-4A.75.75 0 0 1 8 4Z" fill="currentColor"/>
            </svg>
            <span>记得及时签到哦</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Circular Check-in Button with GSAP breathe -->
    <section class="checkin-action">
      <button
        ref="btnRef"
        class="checkin-btn"
        :class="{ 'checkin-btn-done': checkedIn }"
        @click="goCheckIn"
      >
        <svg class="checkin-icon" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
          <polyline points="22 4 12 14.01 9 11.01"/>
        </svg>
        <span class="checkin-label">{{ checkedIn ? '已签到' : '签到' }}</span>
      </button>
      <p class="checkin-hint">点击进行寝室签到</p>
    </section>

    <!-- Quick Actions Grid -->
    <section class="quick-actions">
      <h2 class="section-title">快捷操作</h2>
      <div class="actions-grid">
        <button class="action-card" @click="goCheckIn">
          <div class="action-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <span class="action-label">签到打卡</span>
          <span class="action-desc">人脸识别+GPS定位</span>
        </button>

        <button class="action-card" @click="goRecords">
          <div class="action-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
          </div>
          <span class="action-label">签到记录</span>
          <span class="action-desc">查看历史签到</span>
        </button>

        <button class="action-card" @click="goChat">
          <div class="action-icon action-icon-ai">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4Z"/>
              <path d="M6 10a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1a8 8 0 0 1-12 0v-1Z"/>
              <path d="M9 18h6"/>
              <path d="M10 22h4"/>
            </svg>
          </div>
          <span class="action-label">AI 助手</span>
          <span class="action-desc">智能问答与帮助</span>
        </button>

        <button class="action-card" @click="goProfile">
          <div class="action-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          </div>
          <span class="action-label">个人信息</span>
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
}

/* ---- Greeting ---- */
.greeting-section {
  margin-bottom: 20px;
}

.greeting-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a1a);
  margin: 0;
  line-height: 1.4;
}

.greeting-date {
  color: var(--text-secondary, #6b7280);
  font-size: 0.875rem;
  margin: 4px 0 0;
}

/* ---- Status Card ---- */
.status-card {
  background: var(--bg-surface, #fff);
  border: 1px solid var(--border, #E5E5E3);
  border-radius: var(--radius-lg, 14px);
  padding: 20px 24px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.status-label {
  font-size: 0.875rem;
  color: var(--text-secondary, #6b7280);
  font-weight: 500;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 12px;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.status-done {
  background: var(--success-light, #ECFDF5);
  color: var(--success, #059669);
}

.status-pending {
  background: var(--error-light, #FEF2F2);
  color: var(--error, #DC2626);
}

.status-loading {
  background: var(--bg-elevated, #F9F9F8);
  color: var(--text-tertiary, #9ca3af);
}

.status-body {
  display: flex;
  align-items: center;
}

.status-info {
  width: 100%;
}

.status-count {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 4px;
}

.count-number {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a1a);
  line-height: 1;
}

.count-label {
  font-size: 0.8125rem;
  color: var(--text-tertiary, #9ca3af);
}

.status-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8125rem;
  color: var(--text-secondary, #6b7280);
  margin-top: 4px;
}

.status-hint svg {
  flex-shrink: 0;
  color: var(--text-tertiary, #9ca3af);
}

/* ---- Circular Check-in Button ---- */
.checkin-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

.checkin-btn {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: none;
  background: var(--accent, #2563EB);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 0 8px rgba(37, 99, 235, 0.1);
  transition: background 0.2s;
  outline: none;
}

.checkin-btn:hover {
  background: var(--accent-hover, #1d4ed8);
}

.checkin-btn:active {
  transform: scale(0.96);
}

.checkin-btn-done {
  background: var(--success, #059669);
  box-shadow: 0 0 8px rgba(5, 150, 105, 0.1);
}

.checkin-btn-done:hover {
  background: #047857;
}

.checkin-icon {
  width: 32px;
  height: 32px;
}

.checkin-label {
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.checkin-hint {
  margin-top: 12px;
  font-size: 0.8125rem;
  color: var(--text-tertiary, #9ca3af);
}

/* ---- Quick Actions ---- */
.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a1a);
  margin: 0 0 12px;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.action-card {
  background: var(--bg-surface, #fff);
  border: 1px solid var(--border, #E5E5E3);
  border-radius: var(--radius-md, 10px);
  padding: 20px 16px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s, border-color 0.2s;
  text-align: left;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  outline: none;
}

.action-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: var(--accent, #2563EB);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm, 6px);
  background: var(--accent-light, #EFF6FF);
  color: var(--accent, #2563EB);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.action-icon-ai {
  background: #F0FDF4;
  color: #059669;
}

.action-label {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a1a);
}

.action-desc {
  font-size: 0.75rem;
  color: var(--text-secondary, #6b7280);
}

/* ---- Responsive ---- */
@media (max-width: 480px) {
  .actions-grid {
    grid-template-columns: 1fr;
  }

  .checkin-btn {
    width: 100px;
    height: 100px;
  }

  .checkin-icon {
    width: 28px;
    height: 28px;
  }

  .checkin-label {
    font-size: 0.875rem;
  }
}
</style>
