<template>
  <div class="home-page fade-in">
    <div class="glass welcome-card" ref="welcomeRef">
      <h1 class="greeting">{{ greeting }}，{{ auth.studentName || '同学' }} 👋</h1>
      <p class="date-text">{{ dateText }}</p>
    </div>

    <div class="checkin-area" ref="checkinRef">
      <div class="pulse-ring"></div>
      <div class="checkin-circle" :class="{ checked: checkedIn }" @click="goCheckin">
        <span class="checkin-icon">{{ checkedIn ? '✅' : '📍' }}</span>
        <span class="checkin-text">{{ checkedIn ? '已签到' : '立即签到' }}</span>
      </div>
    </div>

    <div class="quick-actions" ref="actionsRef">
      <button class="glass action-card" @click="$router.push('/records')">
        <span class="action-icon">📋</span>
        <span>签到记录</span>
      </button>
      <button class="glass action-card" @click="$router.push('/chat')">
        <span class="action-icon">💬</span>
        <span>AI咨询</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import axios from 'axios'
import gsap from 'gsap'

const auth = useAuthStore()
const router = useRouter()
const checkedIn = ref(false)
const welcomeRef = ref(null)
const checkinRef = ref(null)
const actionsRef = ref(null)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const dateText = computed(() => {
  return new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
})

function goCheckin() {
  if (!checkedIn.value) router.push('/checkin')
}

onMounted(async () => {
  gsap.from(welcomeRef.value, { y: 30, opacity: 0, duration: 0.6, ease: 'power3.out' })
  gsap.from(checkinRef.value, { scale: 0.8, opacity: 0, duration: 0.8, delay: 0.2, ease: 'back.out(1.5)' })
  gsap.from(actionsRef.value, { y: 20, opacity: 0, duration: 0.5, delay: 0.4 })
  try {
    const { data } = await axios.get('/api/checkin/status')
    if (data.code === 200) checkedIn.value = data.data.checkedIn
  } catch {}
})
</script>

<style scoped>
.home-page { display: flex; flex-direction: column; gap: 1.5rem; align-items: center; }
.welcome-card { width: 100%; padding: 1.5rem; text-align: center; }
.greeting { font-size: 1.3rem; font-weight: 700; }
.date-text { color: var(--text-secondary); font-size: 0.85rem; margin-top: 0.25rem; }

.checkin-area { position: relative; display: flex; align-items: center; justify-content: center; padding: 2rem 0; }
.pulse-ring {
  position: absolute; width: 140px; height: 140px; border-radius: 50%;
  border: 2px solid var(--accent); opacity: 0; animation: pulse 2s ease-out infinite;
}
@keyframes pulse { 0% { transform: scale(1); opacity: 0.6; } 100% { transform: scale(1.6); opacity: 0; } }

.checkin-circle {
  width: 120px; height: 120px; border-radius: 50%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; cursor: pointer;
  background: linear-gradient(135deg, var(--accent), var(--accent-light));
  color: #fff; box-shadow: 0 8px 32px rgba(99,102,241,0.3);
  transition: transform 0.3s, box-shadow 0.3s;
}
.checkin-circle:hover { transform: scale(1.05); box-shadow: 0 12px 40px rgba(99,102,241,0.5); }
.checkin-circle.checked { background: linear-gradient(135deg, var(--success), #4ade80); box-shadow: 0 8px 32px rgba(34,197,94,0.3); cursor: default; }
.checkin-circle.checked:hover { transform: none; box-shadow: 0 8px 32px rgba(34,197,94,0.3); }
.checkin-icon { font-size: 2rem; }
.checkin-text { font-size: 0.85rem; font-weight: 600; margin-top: 0.25rem; }

.quick-actions { display: flex; gap: 1rem; width: 100%; }
.action-card {
  flex: 1; padding: 1.2rem; display: flex; flex-direction: column; align-items: center; gap: 0.5rem;
  cursor: pointer; font-size: 0.9rem; font-weight: 600; color: var(--text-primary); transition: transform 0.2s;
}
.action-card:hover { transform: translateY(-2px); }
.action-icon { font-size: 1.8rem; }
</style>
