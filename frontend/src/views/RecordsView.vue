<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import gsap from 'gsap'

const router = useRouter()

// ---- Pagination state ----
const records = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = ref(10)
const loading = ref(true)
const error = ref('')

// ---- Fetch records ----
async function fetchRecords(page = 0) {
  loading.value = true
  error.value = ''
  try {
    const res = await axios.get('/api/checkin/records', {
      params: { page, size: pageSize.value }
    })
    if (res.data.code === 200) {
      const data = res.data.data
      records.value = data.content || []
      totalPages.value = data.totalPages || 0
      totalElements.value = data.totalElements || 0
      currentPage.value = data.currentPage ?? page

      // Trigger stagger animation after DOM updates
      await nextTick()
      animateList()
    } else {
      error.value = res.data.message || '获取记录失败'
    }
  } catch (err) {
    if (err.response?.data?.message) {
      error.value = err.response.data.message
    } else {
      error.value = '网络错误，请检查后端服务'
    }
  } finally {
    loading.value = false
  }
}

// ---- GSAP stagger animation ----
function animateList() {
  const items = document.querySelectorAll('.record-item')
  if (!items.length) return

  gsap.fromTo(
    items,
    { opacity: 0, y: 30, scale: 0.96 },
    {
      opacity: 1,
      y: 0,
      scale: 1,
      duration: 0.45,
      ease: 'power3.out',
      stagger: 0.08,
    }
  )
}

// ---- Pagination helpers ----
function goToPage(page) {
  if (page < 0 || page >= totalPages.value || page === currentPage.value) return
  fetchRecords(page)
  // Smooth scroll to top
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const pageNumbers = ref([])

function computePageNumbers() {
  const pages = []
  const total = totalPages.value
  const cur = currentPage.value

  if (total <= 7) {
    for (let i = 0; i < total; i++) pages.push(i)
  } else {
    pages.push(0)
    if (cur > 3) pages.push('...')
    const start = Math.max(1, cur - 1)
    const end = Math.min(total - 2, cur + 1)
    for (let i = start; i <= end; i++) pages.push(i)
    if (cur < total - 4) pages.push('...')
    pages.push(total - 1)
  }
  pageNumbers.value = pages
}

watch(totalPages, computePageNumbers)
watch(currentPage, computePageNumbers)

// ---- Format helpers ----
function formatTime(timeStr) {
  if (!timeStr) return '--'
  const d = new Date(timeStr)
  if (isNaN(d.getTime())) return timeStr
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${min}`
}

function formatDate(timeStr) {
  if (!timeStr) return '--'
  const d = new Date(timeStr)
  if (isNaN(d.getTime())) return '--'
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${month}/${day}`
}

function formatWeekday(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  if (isNaN(d.getTime())) return ''
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
}

function formatLocation(loc) {
  if (!loc) return '--'
  // loc is like "29.991316,122.179503"
  const parts = loc.split(',')
  if (parts.length === 2) {
    return `${parseFloat(parts[0]).toFixed(4)}°N, ${parseFloat(parts[1]).toFixed(4)}°E`
  }
  return loc
}

// ---- Entrance animation ----
onMounted(() => {
  fetchRecords()

  // Animate header entrance
  nextTick(() => {
    const header = document.querySelector('.page-header')
    if (header) {
      gsap.fromTo(header, { y: -20, opacity: 0 }, { y: 0, opacity: 1, duration: 0.5, ease: 'power3.out' })
    }
    const summaryBar = document.querySelector('.summary-bar')
    if (summaryBar) {
      gsap.fromTo(summaryBar, { y: 20, opacity: 0 }, { y: 0, opacity: 1, duration: 0.5, delay: 0.15, ease: 'power3.out' })
    }
  })
})
</script>

<template>
  <div class="records-page">
    <!-- Background glow orbs -->
    <div class="glow glow-1"></div>
    <div class="glow glow-2"></div>

    <!-- Top bar -->
    <div class="page-header">
      <button class="back-btn" @click="router.push('/')">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
        返回
      </button>
      <h1 class="page-title">签到记录</h1>
      <div style="width: 64px"></div>
    </div>

    <!-- Summary bar -->
    <div class="summary-bar" v-if="!loading && !error">
      <div class="summary-stat">
        <span class="stat-number">{{ totalElements }}</span>
        <span class="stat-label">总签到次数</span>
      </div>
      <div class="summary-divider"></div>
      <div class="summary-stat">
        <span class="stat-number">{{ totalPages }}</span>
        <span class="stat-label">页</span>
      </div>
    </div>

    <!-- Content container -->
    <div class="content-container">
      <!-- Loading state -->
      <div v-if="loading" class="state-card">
        <div class="spinner"></div>
        <p class="state-text">正在加载签到记录...</p>
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="state-card error-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/>
          <line x1="15" y1="9" x2="9" y2="15"/>
          <line x1="9" y1="9" x2="15" y2="15"/>
        </svg>
        <p class="state-text">{{ error }}</p>
        <button class="retry-btn" @click="fetchRecords(currentPage)">重试</button>
      </div>

      <!-- Empty state -->
      <div v-else-if="records.length === 0" class="state-card">
        <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
        </svg>
        <p class="state-text">暂无签到记录</p>
        <p class="state-hint">完成签到后，记录将在这里显示</p>
        <button class="primary-btn" @click="router.push('/checkin')">去签到</button>
      </div>

      <!-- Records list -->
      <div v-else class="records-list">
        <div
          v-for="(record, index) in records"
          :key="record.id || index"
          class="record-item"
        >
          <div class="record-left">
            <div class="record-date-badge">
              <span class="date-day">{{ formatDate(record.checkTime).split('/')[1] }}</span>
              <span class="date-month">{{ formatDate(record.checkTime).split('/')[0] }}月</span>
            </div>
          </div>

          <div class="record-center">
            <div class="record-top-row">
              <span class="record-time">{{ formatTime(record.checkTime) }}</span>
              <span class="record-weekday">{{ formatWeekday(record.checkTime) }}</span>
            </div>
            <div class="record-details">
              <span class="detail-chip" v-if="record.dormBuilding || record.roomNumber">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                  <polyline points="9 22 9 12 15 12 15 22"/>
                </svg>
                {{ record.dormBuilding || '' }}{{ record.roomNumber ? record.roomNumber + '室' : '' }}
              </span>
              <span class="detail-chip" v-if="record.locationInfo">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0Z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
                {{ formatLocation(record.locationInfo) }}
              </span>
            </div>
          </div>

          <div class="record-right">
            <span class="status-badge" :class="{ 'status-ok': record.status === '已签到' }">
              {{ record.status || '已签到' }}
            </span>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="!loading && !error && totalPages > 1" class="pagination">
        <button
          class="page-btn"
          :disabled="currentPage === 0"
          @click="goToPage(currentPage - 1)"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>

        <template v-for="(p, idx) in pageNumbers" :key="idx">
          <span v-if="p === '...'" class="page-ellipsis">…</span>
          <button
            v-else
            class="page-btn page-num"
            :class="{ active: p === currentPage }"
            @click="goToPage(p)"
          >
            {{ p + 1 }}
          </button>
        </template>

        <button
          class="page-btn"
          :disabled="currentPage === totalPages - 1"
          @click="goToPage(currentPage + 1)"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.records-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
  position: relative;
  overflow: hidden;
  padding: 0 0 40px;
}

/* ---- Background glows ---- */
.glow {
  position: fixed;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  pointer-events: none;
  animation: float 10s ease-in-out infinite;
}
.glow-1 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, #667eea, #764ba2);
  top: -80px;
  left: -80px;
}
.glow-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #4facfe, #00f2fe);
  bottom: -60px;
  right: -60px;
  animation-delay: -4s;
}
@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(25px, -25px) scale(1.05); }
  50% { transform: translate(-15px, 15px) scale(0.95); }
  75% { transform: translate(10px, 10px) scale(1.02); }
}

/* ---- Page header ---- */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  position: relative;
  z-index: 2;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  padding: 8px 14px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.875rem;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: all 0.2s;
}
.back-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}
.page-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #fff;
  margin: 0;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* ---- Summary bar ---- */
.summary-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin: 0 20px 20px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  position: relative;
  z-index: 2;
}
.summary-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.stat-number {
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}
.stat-label {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.45);
}
.summary-divider {
  width: 1px;
  height: 32px;
  background: rgba(255, 255, 255, 0.15);
}

/* ---- Content container ---- */
.content-container {
  max-width: 560px;
  margin: 0 auto;
  padding: 0 16px;
  position: relative;
  z-index: 2;
}

/* ---- State cards (loading / error / empty) ---- */
.state-card {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 48px 32px;
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
}
.state-card svg {
  margin-bottom: 16px;
  opacity: 0.5;
}
.state-text {
  font-size: 0.95rem;
  margin: 0 0 8px;
  color: rgba(255, 255, 255, 0.7);
}
.state-hint {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.4);
  margin: 0 0 20px;
}
.error-state svg {
  color: #ef4444;
  opacity: 0.7;
}

/* ---- Spinner ---- */
.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(255, 255, 255, 0.15);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  margin: 0 auto 16px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ---- Buttons ---- */
.retry-btn, .primary-btn {
  padding: 10px 22px;
  border: none;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}
.retry-btn {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
}
.retry-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}
.primary-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}
.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
}

/* ---- Records list ---- */
.records-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  transition: all 0.25s ease;
  /* Initial state for GSAP — will be overridden by animation */
  opacity: 0;
  transform: translateY(30px);
}
.record-item:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.18);
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.2);
}

/* ---- Record left: date badge ---- */
.record-left {
  flex-shrink: 0;
}
.record-date-badge {
  width: 48px;
  height: 52px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3), rgba(118, 75, 162, 0.3));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  gap: 1px;
}
.date-day {
  font-size: 1.25rem;
  font-weight: 700;
  color: #fff;
  line-height: 1.1;
}
.date-month {
  font-size: 0.625rem;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 500;
}

/* ---- Record center: info ---- */
.record-center {
  flex: 1;
  min-width: 0;
}
.record-top-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
}
.record-time {
  font-size: 0.9rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}
.record-weekday {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.4);
}
.record-details {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.detail-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.72rem;
  color: rgba(255, 255, 255, 0.45);
  background: rgba(255, 255, 255, 0.06);
  border-radius: 6px;
  padding: 2px 8px;
}
.detail-chip svg {
  flex-shrink: 0;
  opacity: 0.6;
}

/* ---- Record right: status ---- */
.record-right {
  flex-shrink: 0;
}
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 9999px;
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.status-badge.status-ok {
  background: rgba(5, 150, 105, 0.2);
  color: #34d399;
  border-color: rgba(5, 150, 105, 0.25);
}

/* ---- Pagination ---- */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 24px;
  padding: 16px 0;
}
.page-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  backdrop-filter: blur(10px);
}
.page-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.2);
}
.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.page-btn.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.35);
}
.page-ellipsis {
  color: rgba(255, 255, 255, 0.35);
  font-size: 0.85rem;
  padding: 0 4px;
}

/* ---- Responsive ---- */
@media (max-width: 480px) {
  .summary-bar {
    margin: 0 12px 16px;
    padding: 14px 20px;
    gap: 20px;
  }
  .content-container {
    padding: 0 10px;
  }
  .record-item {
    padding: 14px 14px;
    gap: 10px;
  }
  .record-date-badge {
    width: 42px;
    height: 46px;
  }
  .date-day {
    font-size: 1.1rem;
  }
  .record-time {
    font-size: 0.82rem;
  }
  .page-btn {
    min-width: 32px;
    height: 32px;
    font-size: 0.75rem;
  }
}
</style>
