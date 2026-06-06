<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import gsap from 'gsap'

const router = useRouter()

// --- State ---
const records = ref([])
const currentPage = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const loading = ref(true)
const error = ref(null)

// --- Fetch records ---
async function fetchRecords(page = 0) {
  loading.value = true
  error.value = null
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
      // Trigger stagger animation after DOM update
      await nextTick()
      animateList()
    } else {
      error.value = res.data.message || '获取记录失败'
    }
  } catch (err) {
    error.value = '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

// --- Pagination ---
function goToPage(page) {
  if (page < 0 || page >= totalPages.value) return
  fetchRecords(page)
}

function prevPage() {
  if (currentPage.value > 0) goToPage(currentPage.value - 1)
}

function nextPage() {
  if (currentPage.value < totalPages.value - 1) goToPage(currentPage.value + 1)
}

// --- GSAP stagger animation ---
function animateList() {
  const items = document.querySelectorAll('.record-item')
  if (items.length === 0) return
  gsap.fromTo(
    items,
    { opacity: 0, y: 24, scale: 0.97 },
    {
      opacity: 1,
      y: 0,
      scale: 1,
      duration: 0.4,
      stagger: 0.06,
      ease: 'power2.out',
      clearProps: 'transform'
    }
  )
}

// --- Format date ---
function formatDate(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// --- Status display ---
function statusLabel(status) {
  if (!status) return '未知'
  const s = String(status).toLowerCase()
  if (s === 'success' || s === 'verified' || s === '正常') return '正常'
  if (s === 'failed' || s === 'fail') return '异常'
  return status
}

function statusClass(status) {
  const s = String(status || '').toLowerCase()
  if (s === 'success' || s === 'verified' || s === '正常') return 'status-success'
  return 'status-fail'
}

onMounted(() => {
  fetchRecords(0)
})
</script>

<template>
  <div class="records-page">
    <!-- Header -->
    <header class="page-header">
      <button class="back-btn" @click="router.push('/')">← 返回</button>
      <h1>📋 签到记录</h1>
      <span class="total-badge" v-if="totalElements > 0">共 {{ totalElements }} 条</span>
    </header>

    <!-- Loading state -->
    <div v-if="loading" class="glass-card loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="glass-card error-state">
      <p class="error-text">⚠️ {{ error }}</p>
      <button class="retry-btn" @click="fetchRecords(currentPage)">重试</button>
    </div>

    <!-- Empty state -->
    <div v-else-if="records.length === 0" class="glass-card empty-state">
      <div class="empty-icon">📭</div>
      <p class="empty-title">暂无签到记录</p>
      <p class="empty-hint">完成签到后，记录将在此显示</p>
      <button class="go-checkin-btn" @click="router.push('/checkin')">去签到</button>
    </div>

    <!-- Records list -->
    <template v-else>
      <div class="records-list">
        <div
          v-for="(record, index) in records"
          :key="record.id || index"
          class="record-item glass-card"
        >
          <div class="record-main">
            <div class="record-left">
              <div class="record-icon" :class="statusClass(record.status)">
                <svg v-if="statusClass(record.status) === 'status-success'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="15" y1="9" x2="9" y2="15"/>
                  <line x1="9" y1="9" x2="15" y2="15"/>
                </svg>
              </div>
              <div class="record-info">
                <span class="record-time">{{ formatDate(record.checkTime) }}</span>
                <span class="record-location" v-if="record.roomNumber || record.dormBuilding">
                  🏠 {{ record.dormBuilding || '' }}{{ record.roomNumber || '' }}
                </span>
                <span class="record-location" v-if="record.locationInfo">
                  📍 {{ record.locationInfo }}
                </span>
              </div>
            </div>
            <div class="record-right">
              <span class="status-tag" :class="statusClass(record.status)">
                {{ statusLabel(record.status) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="pagination glass-card">
        <button
          class="page-btn"
          :disabled="currentPage <= 0"
          @click="prevPage"
        >
          ← 上一页
        </button>
        <div class="page-info">
          <span class="page-current">{{ currentPage + 1 }}</span>
          <span class="page-sep">/</span>
          <span class="page-total">{{ totalPages }}</span>
        </div>
        <button
          class="page-btn"
          :disabled="currentPage >= totalPages - 1"
          @click="nextPage"
        >
          下一页 →
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.records-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 0 32px;
}

/* ---- Header ---- */
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a1a);
  margin: 0;
}

.total-badge {
  font-size: 0.75rem;
  color: var(--text-tertiary, #9ca3af);
  background: rgba(99, 102, 241, 0.08);
  padding: 2px 10px;
  border-radius: 9999px;
  margin-left: auto;
}

.back-btn {
  padding: 6px 14px;
  border: 1px solid var(--border, #E5E5E3);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12px);
  color: var(--text-secondary, #6b7280);
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-primary, #1a1a1a);
}

/* ---- Glass Card ---- */
.glass-card {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 14px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
}

:root.dark .glass-card,
.dark .glass-card {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
}

/* ---- Records List ---- */
.records-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.record-item {
  padding: 16px 20px;
  opacity: 0; /* GSAP will animate to 1 */
}

.record-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.record-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.record-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.record-icon.status-success {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}

.record-icon.status-fail {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.record-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.record-time {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a1a);
}

.record-location {
  font-size: 0.78rem;
  color: var(--text-secondary, #6b7280);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-right {
  flex-shrink: 0;
}

.status-tag {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
}

.status-tag.status-success {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}

.status-tag.status-fail {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

/* ---- Pagination ---- */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
}

.page-btn {
  padding: 8px 18px;
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 8px;
  background: rgba(99, 102, 241, 0.06);
  color: #4f46e5;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: rgba(99, 102, 241, 0.15);
  border-color: rgba(99, 102, 241, 0.3);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-size: 0.9rem;
}

.page-current {
  font-weight: 700;
  color: var(--text-primary, #1a1a1a);
}

.page-sep {
  color: var(--text-tertiary, #9ca3af);
}

.page-total {
  color: var(--text-secondary, #6b7280);
}

/* ---- Loading ---- */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  color: var(--text-secondary, #6b7280);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(99, 102, 241, 0.15);
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ---- Error ---- */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 12px;
}

.error-text {
  color: #dc2626;
  font-size: 0.9rem;
}

.retry-btn {
  padding: 8px 24px;
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 8px;
  background: rgba(99, 102, 241, 0.08);
  color: #4f46e5;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.retry-btn:hover {
  background: rgba(99, 102, 241, 0.15);
}

/* ---- Empty ---- */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 8px;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 8px;
}

.empty-title {
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a1a);
  margin: 0;
}

.empty-hint {
  font-size: 0.85rem;
  color: var(--text-secondary, #6b7280);
  margin: 0;
}

.go-checkin-btn {
  margin-top: 16px;
  padding: 10px 28px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.3);
}

.go-checkin-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* ---- Responsive ---- */
@media (max-width: 480px) {
  .record-item {
    padding: 12px 14px;
  }

  .record-icon {
    width: 34px;
    height: 34px;
  }

  .record-time {
    font-size: 0.82rem;
  }
}
</style>
