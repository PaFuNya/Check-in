<template>
  <div class="records-page fade-in">
    <h1 class="page-title">📝 签到记录</h1>

    <div v-if="loading" class="loading-text">加载中...</div>

    <div v-else-if="records.length === 0" class="glass empty-card">
      <span class="empty-icon">📭</span>
      <p>暂无签到记录</p>
    </div>

    <div v-else class="records-list" ref="listRef">
      <div v-for="(r, i) in records" :key="r.id" class="glass record-card" :ref="el => { if (el) cardRefs[i] = el }">
        <div class="record-header">
          <span class="record-status" :class="r.status === '正常' ? 'normal' : 'abnormal'">
            {{ r.status === '正常' ? '✅ 正常' : '⚠️ ' + r.status }}
          </span>
          <span class="record-time">{{ formatTime(r.checkTime) }}</span>
        </div>
        <div class="record-detail">
          <span>🏠 {{ r.roomNumber || '-' }} {{ r.dormBuilding || '' }}</span>
          <span>📍 {{ r.locationInfo || '-' }}</span>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="page === 0" @click="goPage(page - 1)">上一页</button>
      <span class="page-info">{{ page + 1 }} / {{ totalPages }}</span>
      <button class="page-btn" :disabled="page >= totalPages - 1" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import axios from 'axios'
import gsap from 'gsap'

const records = ref([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const cardRefs = ref([])
const listRef = ref(null)

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

async function fetchRecords(p = 0) {
  loading.value = true
  try {
    const { data } = await axios.get('/api/checkin/records', { params: { page: p, size: 10 } })
    if (data.code === 200) {
      records.value = data.data.content || []
      totalPages.value = data.data.totalPages || 0
      page.value = p
      await nextTick()
      animateCards()
    }
  } catch {} finally { loading.value = false }
}

function animateCards() {
  const els = cardRefs.value.filter(Boolean)
  if (els.length) gsap.from(els, { y: 20, opacity: 0, duration: 0.4, stagger: 0.08, ease: 'power2.out' })
}

function goPage(p) { fetchRecords(p) }

onMounted(() => fetchRecords())
</script>

<style scoped>
.records-page { display: flex; flex-direction: column; gap: 1rem; }
.page-title { font-size: 1.3rem; font-weight: 700; }
.loading-text { text-align: center; color: var(--text-secondary); padding: 2rem; }

.empty-card { padding: 2rem; text-align: center; color: var(--text-secondary); }
.empty-icon { font-size: 2rem; display: block; margin-bottom: 0.5rem; }

.records-list { display: flex; flex-direction: column; gap: 0.75rem; }
.record-card { padding: 1rem; }
.record-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
.record-status { font-size: 0.85rem; font-weight: 600; padding: 0.15rem 0.5rem; border-radius: 0.5rem; }
.record-status.normal { background: rgba(34,197,94,0.15); color: var(--success); }
.record-status.abnormal { background: rgba(239,68,68,0.15); color: var(--error); }
.record-time { font-size: 0.8rem; color: var(--text-secondary); }
.record-detail { display: flex; flex-wrap: wrap; gap: 0.5rem 1rem; font-size: 0.85rem; color: var(--text-secondary); }

.pagination { display: flex; align-items: center; justify-content: center; gap: 1rem; padding: 0.5rem 0; }
.page-btn {
  background: var(--glass-bg); border: 1px solid var(--glass-border); color: var(--text-primary);
  padding: 0.4rem 1rem; border-radius: 0.5rem; cursor: pointer; min-height: 44px; font-size: 0.85rem;
}
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 0.85rem; color: var(--text-secondary); }
</style>
