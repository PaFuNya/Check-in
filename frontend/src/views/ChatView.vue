<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import gsap from 'gsap'

const router = useRouter()
const authStore = useAuthStore()

const messages = ref([])
const inputText = ref('')
const isStreaming = ref(false)
const isLoadingHistory = ref(false)
const eventSourceRef = ref(null)
const messagesContainer = ref(null)

let typewriterTimeline = null

async function scrollToBottom() {
  await nextTick()
  if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
}

async function loadChatHistory() {
  isLoadingHistory.value = true
  try {
    const res = await fetch('/api/ai/chat-history?page=0&size=50&sort=id,asc', { credentials: 'include' })
    const data = await res.json()
    if (data.code === 200 && data.data?.content) {
      messages.value = data.data.content.map(item => ({
        role: item.role === '0' ? 'user' : 'ai',
        content: item.content,
        id: item.id || Date.now() + Math.random(),
        animated: true
      }))
      await nextTick(); scrollToBottom()
    }
  } catch { /* silent */ } finally { isLoadingHistory.value = false }
}

async function clearHistory() {
  const userId = authStore.studentId
  if (!userId) return
  try {
    const res = await fetch(`/api/ai/clear-chat-history/${userId}`, { method: 'POST', credentials: 'include' })
    const data = await res.json()
    if (data.code === 200) messages.value = []
  } catch { /* silent */ }
}

function typewriterEffect(element, text, onComplete) {
  if (typewriterTimeline) typewriterTimeline.kill()
  const chars = text.split('')
  const proxy = { charIndex: 0 }
  typewriterTimeline = gsap.timeline({
    onComplete: () => { typewriterTimeline = null; if (onComplete) onComplete() }
  })
  typewriterTimeline.to(proxy, {
    charIndex: chars.length,
    duration: Math.min(chars.length * 0.03, 4),
    ease: 'none',
    onUpdate: () => {
      const idx = Math.floor(proxy.charIndex)
      const visibleText = chars.slice(0, idx).join('')
      const cursor = idx < chars.length ? '<span class="typing-cursor">|</span>' : ''
      if (element) element.innerHTML = formatMarkdown(visibleText) + cursor
      scrollToBottom()
    }
  })
}

function formatMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || isStreaming.value) return

  messages.value.push({ role: 'user', content: text, id: Date.now(), animated: true })
  inputText.value = ''
  scrollToBottom()

  const aiMsgId = Date.now() + 1
  messages.value.push({ role: 'ai', content: '', id: aiMsgId, animated: false, _streaming: true })
  isStreaming.value = true
  scrollToBottom()

  const userId = authStore.studentId || ''
  const params = new URLSearchParams({ message: text, userId })
  const es = new EventSource(`/api/ai/chat-stream?${params}`)
  eventSourceRef.value = es

  let fullContent = ''

  es.onmessage = (event) => {
    const rawData = event.data
    if (rawData === '[DONE]') { finishStreaming(aiMsgId, fullContent); return }
    try { const p = JSON.parse(rawData); if (p.error) { fullContent += `\n[错误: ${p.message || '未知'}]`; finishStreaming(aiMsgId, fullContent); return } } catch { /* not JSON */ }
    fullContent += rawData
    const msg = messages.value.find(m => m.id === aiMsgId)
    if (msg) msg.content = fullContent
    nextTick(() => {
      const el = document.querySelector(`[data-msg-id="${aiMsgId}"] .bubble-text`)
      if (el) typewriterEffect(el, fullContent)
    })
    scrollToBottom()
  }

  es.onerror = () => {
    if (fullContent) finishStreaming(aiMsgId, fullContent)
    else { const msg = messages.value.find(m => m.id === aiMsgId); if (msg) { msg.content = '连接中断，请重试'; msg._streaming = false }; isStreaming.value = false }
    es.close(); eventSourceRef.value = null
  }
}

function finishStreaming(msgId, content) {
  if (typewriterTimeline) { typewriterTimeline.kill(); typewriterTimeline = null }
  const msg = messages.value.find(m => m.id === msgId)
  if (msg) {
    msg.content = content; msg.animated = true; msg._streaming = false
    nextTick(() => {
      const el = document.querySelector(`[data-msg-id="${msgId}"] .bubble-text`)
      if (el) el.innerHTML = formatMarkdown(content)
    })
  }
  if (eventSourceRef.value) { eventSourceRef.value.close(); eventSourceRef.value = null }
  isStreaming.value = false; scrollToBottom()
}

function stopStreaming() {
  if (eventSourceRef.value) { eventSourceRef.value.close(); eventSourceRef.value = null }
  if (typewriterTimeline) { typewriterTimeline.kill(); typewriterTimeline = null }
  const lastAi = [...messages.value].reverse().find(m => m.role === 'ai' && m._streaming)
  if (lastAi) {
    lastAi.animated = true; lastAi._streaming = false
    nextTick(() => { const el = document.querySelector(`[data-msg-id="${lastAi.id}"] .bubble-text`); if (el) el.innerHTML = formatMarkdown(lastAi.content) })
  }
  isStreaming.value = false
}

watch(() => messages.value.length, async (newLen, oldLen) => {
  if (newLen <= oldLen) return
  await nextTick()
  const container = messagesContainer.value; if (!container) return
  const all = container.querySelectorAll('.msg-row')
  const last = all[all.length - 1]
  if (last && !last.dataset.gsapInited) {
    last.dataset.gsapInited = 'true'
    gsap.from(last, { opacity: 0, y: 16, scale: 0.96, duration: 0.35, ease: 'back.out(1.3)' })
  }
})

function handleKeydown(e) { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage() } }

onMounted(() => {
  loadChatHistory()
  gsap.from('.chat-container', { opacity: 0, y: 20, duration: 0.5, ease: 'power3.out' })
})

onUnmounted(() => {
  if (eventSourceRef.value) { eventSourceRef.value.close(); eventSourceRef.value = null }
  if (typewriterTimeline) { typewriterTimeline.kill(); typewriterTimeline = null }
})
</script>

<template>
  <div class="chat-page">
    <div class="chat-container glass-card">
      <!-- Header -->
      <header class="chat-header">
        <div class="header-left">
          <button class="hdr-btn cursor-pointer" @click="router.push('/')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
          </button>
          <div>
            <h1 class="hdr-title">AI 签到助手</h1>
            <span class="hdr-status">
              <span class="status-dot" :class="{ active: !isStreaming }"></span>
              {{ isStreaming ? '思考中...' : '在线' }}
            </span>
          </div>
        </div>
        <button class="hdr-btn cursor-pointer" title="清除聊天" @click="clearHistory">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        </button>
      </header>

      <!-- Messages -->
      <div ref="messagesContainer" class="messages-area">
        <!-- Welcome -->
        <div v-if="!isLoadingHistory && messages.length === 0" class="welcome">
          <div class="welcome-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#2563EB" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4Z"/><path d="M6 10a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1a8 8 0 0 1-12 0v-1Z"/><path d="M9 18h6"/><path d="M10 22h4"/>
            </svg>
          </div>
          <h2 class="welcome-title">欢迎使用 AI 签到助手</h2>
          <p class="welcome-desc">我可以帮你完成寝室签到、请假报备、考勤查询等操作</p>
          <div class="quick-prompts">
            <button class="prompt-chip cursor-pointer" @click="inputText = '今天我已经签到了吗？'">今天签到了吗？</button>
            <button class="prompt-chip cursor-pointer" @click="inputText = '我要请假'">我要请假</button>
            <button class="prompt-chip cursor-pointer" @click="inputText = '寝室管理条例有哪些？'">寝室管理条例</button>
          </div>
        </div>

        <!-- Loading history -->
        <div v-if="isLoadingHistory" class="loading-msg">
          <div class="spinner"></div><span>加载聊天记录...</span>
        </div>

        <!-- Messages -->
        <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'" :data-msg-id="msg.id">
          <div v-if="msg.role === 'ai'" class="avatar avatar-ai">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4Z"/><path d="M6 10a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1a8 8 0 0 1-12 0v-1Z"/></svg>
          </div>
          <div class="bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
            <div v-if="msg.role === 'ai' && !msg.animated" class="bubble-text"></div>
            <div v-else-if="msg.role === 'ai'" class="bubble-text" v-html="formatMarkdown(msg.content)"></div>
            <div v-else class="bubble-text">{{ msg.content }}</div>
            <div v-if="msg._streaming && !msg.content" class="dots"><span></span><span></span><span></span></div>
          </div>
          <div v-if="msg.role === 'user'" class="avatar avatar-user">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </div>
        </div>
      </div>

      <!-- Input -->
      <div class="input-area">
        <div class="input-row">
          <textarea
            v-model="inputText"
            class="chat-input"
            placeholder="输入消息... (Enter 发送)"
            rows="1"
            @keydown="handleKeydown"
            @input="e => { e.target.style.height = 'auto'; e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px' }"
          ></textarea>
          <button v-if="isStreaming" class="send-btn stop-btn cursor-pointer" @click="stopStreaming" title="停止">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>
          </button>
          <button v-else class="send-btn cursor-pointer" :disabled="!inputText.trim()" @click="sendMessage" title="发送">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-page { max-width: 720px; margin: 0 auto; }
.chat-container { display: flex; flex-direction: column; height: calc(100vh - 140px); overflow: hidden; padding: 0; }

/* Header */
.chat-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid rgba(0, 0, 0, 0.06); flex-shrink: 0; }
:root.dark .chat-header { border-bottom-color: rgba(255, 255, 255, 0.06); }
.header-left { display: flex; align-items: center; gap: 12px; }
.hdr-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; border: none; border-radius: 8px; background: #F1F5F9; color: #64748B; transition: all 0.2s; }
:root.dark .hdr-btn { background: rgba(30, 41, 59, 0.5); color: #94A3B8; }
.hdr-btn:hover { background: #E2E8F0; color: #334155; }
.hdr-title { font-size: 1rem; font-weight: 700; color: #1E293B; margin: 0; }
:root.dark .hdr-title { color: #F1F5F9; }
.hdr-status { display: flex; align-items: center; gap: 6px; font-size: 0.75rem; color: #94A3B8; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; background: #D1D5DB; }
.status-dot.active { background: #059669; }

/* Messages */
.messages-area { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 12px; }

/* Welcome */
.welcome { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 40px 20px; gap: 12px; }
.welcome-icon { width: 72px; height: 72px; border-radius: 20px; background: #EFF6FF; display: flex; align-items: center; justify-content: center; }
:root.dark .welcome-icon { background: rgba(37, 99, 235, 0.12); }
.welcome-title { font-size: 1.125rem; font-weight: 700; color: #1E293B; margin: 0; }
:root.dark .welcome-title { color: #F1F5F9; }
.welcome-desc { font-size: 0.8125rem; color: #94A3B8; margin: 0; max-width: 300px; }
.quick-prompts { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-top: 8px; }
.prompt-chip { padding: 8px 16px; border: 1px solid #E2E8F0; border-radius: 9999px; background: white; color: #334155; font-size: 0.8125rem; transition: all 0.2s; min-height: 40px; font-family: inherit; }
:root.dark .prompt-chip { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.08); color: #CBD5E1; }
.prompt-chip:hover { border-color: #2563EB; color: #2563EB; background: #EFF6FF; }

/* Message rows */
.msg-row { display: flex; gap: 10px; max-width: 85%; }
.msg-user { align-self: flex-end; flex-direction: row-reverse; }
.msg-ai { align-self: flex-start; }

.avatar { width: 32px; height: 32px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.avatar-ai { background: #EFF6FF; color: #2563EB; }
.avatar-user { background: #F1F5F9; color: #64748B; }
:root.dark .avatar-ai { background: rgba(37, 99, 235, 0.12); }
:root.dark .avatar-user { background: rgba(30, 41, 59, 0.6); color: #94A3B8; }

.bubble { padding: 12px 16px; border-radius: 16px; font-size: 0.875rem; line-height: 1.6; }
.bubble-ai { background: white; color: #1E293B; border: 1px solid #E2E8F0; border-bottom-left-radius: 4px; }
:root.dark .bubble-ai { background: rgba(30, 41, 59, 0.6); border-color: rgba(255, 255, 255, 0.06); color: #E2E8F0; }
.bubble-user { background: linear-gradient(135deg, #2563EB, #3B82F6); color: white; border-bottom-right-radius: 4px; }
.bubble-text :deep(strong) { font-weight: 700; }
.bubble-text :deep(code) { background: rgba(0, 0, 0, 0.06); padding: 1px 4px; border-radius: 4px; font-size: 0.8125rem; }
.bubble-user .bubble-text :deep(code) { background: rgba(255, 255, 255, 0.2); }

.typing-cursor { animation: blink 0.8s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }

.dots { display: flex; gap: 4px; padding: 4px 0; }
.dots span { width: 6px; height: 6px; background: #94A3B8; border-radius: 50%; animation: dotBounce 1.4s infinite; }
.dots span:nth-child(2) { animation-delay: 0.2s; }
.dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotBounce { 0%, 80%, 100% { transform: translateY(0); } 40% { transform: translateY(-6px); } }

.loading-msg { display: flex; align-items: center; gap: 10px; justify-content: center; padding: 20px; color: #94A3B8; font-size: 0.875rem; }
.spinner { width: 20px; height: 20px; border: 2px solid #E2E8F0; border-top-color: #2563EB; border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Input */
.input-area { padding: 12px 16px; border-top: 1px solid rgba(0, 0, 0, 0.06); flex-shrink: 0; }
:root.dark .input-area { border-top-color: rgba(255, 255, 255, 0.06); }
.input-row { display: flex; align-items: flex-end; gap: 8px; }
.chat-input { flex: 1; padding: 10px 14px; border: 1.5px solid #E2E8F0; border-radius: 12px; font-size: 0.9375rem; background: #F8FAFC; color: #1E293B; resize: none; outline: none; font-family: inherit; line-height: 1.5; min-height: 44px; transition: border-color 0.2s; }
:root.dark .chat-input { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.08); color: #E2E8F0; }
.chat-input:focus { border-color: #2563EB; }
.chat-input::placeholder { color: #94A3B8; }
.send-btn { width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; border: none; border-radius: 12px; background: linear-gradient(135deg, #2563EB, #3B82F6); color: white; transition: all 0.2s; flex-shrink: 0; }
.send-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3); }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.stop-btn { background: #DC2626; }
.stop-btn:hover { background: #B91C1C; }
</style>
