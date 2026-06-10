<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import gsap from 'gsap'

const router = useRouter()

const records = ref([])
const currentPage = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const loading = ref(true)
const error = ref(null)

let listTl = null

async function fetchRecords(page = 0) {
  loading.value = true; error.value = null
  try {
    const res = await axios.get('/api/checkin/records', { params: { page, size: pageSize.value } })
    if (res.data.code === 200) {
      const data = res.data.data
      records.value = data.content || []
      totalPages.value = data.totalPages || 0
      totalElements.value = data.totalElements || 0
      currentPage.value = data.number ?? page
      await nextTick()
      animateList()
    } else { error.value = res.data.message || '获取记录失败' }
  } catch { error.value = '网络错误，请稍后重试' } finally { loading.value = false }
}

function goToPage(page) { if (page >= 0 && page < totalPages.value) fetchRecords(page) }
function prevPage() { if (currentPage.value > 0) goToPage(currentPage.value - 1) }
function nextPage() { if (currentPage.value < totalPages.value - 1) goToPage(currentPage.value + 1) }

function animateList() {
  if (listTl) listTl.kill()
  const items = document.querySelectorAll('.record-item')
  if (!items.length) return
  listTl = gsap.fromTo(items, { opacity: 0, y: 20 }, { opacity: 1, y: 0, duration: 0.35, stagger: 0.05, ease: 'power2.out', clearProps: 'transform' })
}

function formatDate(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function statusLabel(s) {
  const v = String(s || '').trim()
  if (v === '已签到' || v === '正常') return v
  if (v === 'success' || v === 'verified') return '正常'
  return '异常'
}

function isSuccess(s) {
  const v = String(s || '').trim()
  return v === '已签到' || v === '正常' || v === 'success' || v === 'verified'
}

onMounted(() => fetchRecords(0))

onUnmounted(() => {
  if (listTl) listTl.kill()
})
</script>

<template>
  <div class="records-page">
    <!-- Header -->
    <header class="page-header">
      <button class="back-btn cursor-pointer" @click="router.push('/')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
        返回
      </button>
      <h1 class="page-title">签到记录</h1>
      <span v-if="totalElements > 0" class="total-tag">共 {{ totalElements }} 条</span>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="glass-card state-box">
      <div class="spinner"></div><span>加载中...</span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="glass-card state-box">
      <span class="state-error">{{ error }}</span>
      <button class="btn-outline cursor-pointer" @click="fetchRecords(currentPage)">重试</button>
    </div>

    <!-- Empty -->
    <div v-else-if="records.length === 0" class="glass-card state-box">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" />
      </svg>
      <p class="state-title">暂无签到记录</p>
      <p class="state-hint">完成签到后，记录将在此显示</p>
      <button class="btn-primary cursor-pointer" @click="router.push('/checkin')">去签到</button>
    </div>

    <!-- Records -->
    <template v-else>
      <div class="records-list">
        <div v-for="(record, i) in records" :key="record.id || i" class="record-item glass-card">
          <div class="record-main">
            <div class="record-left">
              <div class="record-icon" :class="isSuccess(record.status) ? 'icon-success' : 'icon-error'">
                <svg v-if="isSuccess(record.status)" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              </div>
              <div class="record-info">
                <span class="record-time">{{ formatDate(record.checkTime) }}</span>
                <span v-if="record.dormBuilding || record.roomNumber" class="record-loc">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>
                  {{ record.dormBuilding || '' }}{{ record.roomNumber || '' }}
                </span>
                <span v-if="record.locationInfo" class="record-loc">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0Z" /><circle cx="12" cy="10" r="3" /></svg>
                  {{ record.locationInfo }}
                </span>
              </div>
            </div>
            <span class="status-tag" :class="isSuccess(record.status) ? 'tag-success' : 'tag-error'">
              {{ statusLabel(record.status) }}
            </span>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="pagination glass-card">
        <button class="page-btn cursor-pointer" :disabled="currentPage <= 0" @click="prevPage">上一页</button>
        <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button class="page-btn cursor-pointer" :disabled="currentPage >= totalPages - 1" @click="nextPage">下一页</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.records-page { max-width: 720px; margin: 0 auto; }

/* Header */
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.page-title { font-size: 1.25rem; font-weight: 700; color: var(--color-text); margin: 0; flex: 1; }
.total-tag { font-size: 0.75rem; color: var(--color-text-light); background: var(--color-primary-light); padding: 3px 10px; border-radius: var(--radius-full); }
.back-btn { display: flex; align-items: center; gap: 4px; padding: 8px 14px; border: 1px solid var(--color-border); border-radius: 8px; background: rgba(255, 255, 255, 0.7); color: var(--color-text-muted); font-size: 0.8125rem; font-weight: 500; transition: all 0.2s; min-height: 40px; font-family: inherit; cursor: pointer; }
.dark .back-btn { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.08); color: var(--color-text-light); }
.back-btn:hover { background: rgba(255, 255, 255, 0.9); color: var(--color-text-muted); }

/* States */
.state-box { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 48px 20px; color: var(--color-text-muted); }
.state-error { color: var(--color-error); font-size: 0.9rem; }
.state-title { font-size: 1rem; font-weight: 600; color: var(--color-text); margin: 8px 0 0; }
.state-hint { font-size: 0.8125rem; color: var(--color-text-light); margin: 0; }

.spinner { width: 28px; height: 28px; border: 3px solid #E2E8F0; border-top-color: #2563EB; border-radius: 50%; animation: spin 0.7s linear infinite; }
.dark .spinner { border-color: rgba(255, 255, 255, 0.1); border-top-color: #60A5FA; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Records */
.records-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
.record-item { padding: 16px 20px; }
.record-main { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.record-left { display: flex; align-items: center; gap: 14px; flex: 1; min-width: 0; }
.record-icon { width: 38px; height: 38px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.icon-success { background: var(--color-success-light); color: var(--color-success); }
.icon-error { background: var(--color-error-light); color: var(--color-error); }
.record-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.record-time { font-size: 0.875rem; font-weight: 600; color: var(--color-text); }
.record-loc { display: flex; align-items: center; gap: 4px; font-size: 0.75rem; color: var(--color-text-light); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.status-tag { padding: 3px 12px; border-radius: var(--radius-full); font-size: 0.75rem; font-weight: 600; flex-shrink: 0; }
.tag-success { background: var(--color-success-light); color: var(--color-success); }
.tag-error { background: var(--color-error-light); color: var(--color-error); }

/* Pagination */
.pagination { display: flex; align-items: center; justify-content: space-between; padding: 12px 20px; }
.page-btn { padding: 8px 18px; border: 1px solid var(--color-border); border-radius: 8px; background: #F8FAFC; color: var(--color-primary); font-size: 0.8125rem; font-weight: 500; transition: all 0.2s; min-height: 40px; font-family: inherit; cursor: pointer; }
.dark .page-btn { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.08); color: var(--color-secondary); }
.page-btn:hover:not(:disabled) { background: var(--color-primary-light); border-color: var(--color-primary); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 0.875rem; color: var(--color-text-muted); }

/* Shared buttons */
.btn-primary { display: inline-flex; align-items: center; gap: 6px; padding: 10px 24px; background: linear-gradient(135deg, #2563EB, #3B82F6); color: white; border: none; border-radius: 10px; font-size: 0.875rem; font-weight: 600; cursor: pointer; min-height: 44px; font-family: inherit; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3); }
.btn-primary:hover { transform: translateY(-1px); }
.btn-outline { padding: 8px 20px; background: transparent; color: #2563EB; border: 1px solid #2563EB; border-radius: 8px; font-size: 0.8125rem; cursor: pointer; min-height: 40px; font-family: inherit; transition: all 0.2s; }
.btn-outline:hover { background: rgba(37, 99, 235, 0.08); }

@media (max-width: 480px) { .record-item { padding: 12px 14px; } .record-icon { width: 32px; height: 32px; } }
</style>
