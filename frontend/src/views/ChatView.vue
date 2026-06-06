<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import gsap from 'gsap'

const router = useRouter()
const authStore = useAuthStore()

// ---- Message list ----
const messages = ref([])
const inputText = ref('')
const isStreaming = ref(false)
const isLoadingHistory = ref(false)
const eventSourceRef = ref(null)
const messagesContainer = ref(null)
const inputRef = ref(null)

// ---- Typewriter state (per AI message being streamed) ----
let typewriterTimeline = null
const currentStreamedText = ref('')

// ---- Auto-scroll ----
async function scrollToBottom() {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// ---- Load chat history ----
async function loadChatHistory() {
  isLoadingHistory.value = true
  try {
    const res = await fetch('/api/ai/chat-history?page=0&size=50&sort=id,asc', {
      credentials: 'include'
    })
    const data = await res.json()
    if (data.code === 200 && data.data?.content) {
      const history = data.data.content.map(item => ({
        role: item.role === '0' ? 'user' : 'ai',
        content: item.content,
        id: item.id || Date.now() + Math.random(),
        animated: true // history items don't need animation
      }))
      messages.value = history
      await nextTick()
      scrollToBottom()
    }
  } catch {
    // Silent fail — start with empty history
  } finally {
    isLoadingHistory.value = false
  }
}

// ---- Clear chat history ----
async function clearHistory() {
  const userId = authStore.studentId
  if (!userId) return
  try {
    const res = await fetch(`/api/ai/clear-chat-history/${userId}`, {
      method: 'POST',
      credentials: 'include'
    })
    const data = await res.json()
    if (data.code === 200) {
      messages.value = []
    }
  } catch {
    // silent
  }
}

// ---- GSAP typewriter effect ----
function typewriterEffect(element, text, onComplete) {
  if (typewriterTimeline) {
    typewriterTimeline.kill()
  }

  const chars = text.split('')
  const proxy = { charIndex: 0 }

  typewriterTimeline = gsap.timeline({
    onComplete: () => {
      typewriterTimeline = null
      if (onComplete) onComplete()
    }
  })

  typewriterTimeline.to(proxy, {
    charIndex: chars.length,
    duration: Math.min(chars.length * 0.03, 4), // cap at 4s
    ease: 'none',
    onUpdate: () => {
      const idx = Math.floor(proxy.charIndex)
      const visibleText = chars.slice(0, idx).join('')
      const cursor = idx < chars.length ? '<span class="typing-cursor">|</span>' : ''
      if (element) {
        element.innerHTML = formatMarkdown(visibleText) + cursor
      }
      scrollToBottom()
    }
  })
}

// ---- Simple markdown formatting ----
function formatMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}

// ---- Send message via SSE ----
function sendMessage() {
  const text = inputText.value.trim()
  if (!text || isStreaming.value) return

  // Add user message
  messages.value.push({
    role: 'user',
    content: text,
    id: Date.now(),
    animated: true
  })
  inputText.value = ''
  scrollToBottom()

  // Add placeholder AI message
  const aiMsgId = Date.now() + 1
  messages.value.push({
    role: 'ai',
    content: '',
    id: aiMsgId,
    animated: false,
    _streaming: true
  })
  isStreaming.value = true
  scrollToBottom()

  // Open SSE connection
  const userId = authStore.studentId || ''
  const params = new URLSearchParams({ message: text, userId })
  const url = `/api/ai/chat-stream?${params.toString()}`

  const es = new EventSource(url)
  eventSourceRef.value = es

  let fullContent = ''

  es.onmessage = (event) => {
    const rawData = event.data

    // Check for [DONE] marker
    if (rawData === '[DONE]') {
      finishStreaming(aiMsgId, fullContent)
      return
    }

    // Check for error JSON
    try {
      const parsed = JSON.parse(rawData)
      if (parsed.error) {
        fullContent += `\n[错误: ${parsed.message || '未知错误'}]`
        finishStreaming(aiMsgId, fullContent)
        return
      }
    } catch {
      // Not JSON, treat as normal text chunk
    }

    // Append chunk
    fullContent += rawData

    // Update the message content
    const msg = messages.value.find(m => m.id === aiMsgId)
    if (msg) {
      msg.content = fullContent
    }

    // GSAP typewriter: animate the latest AI bubble
    nextTick(() => {
      const bubbleEl = document.querySelector(`[data-msg-id="${aiMsgId}"] .bubble-text`)
      if (bubbleEl) {
        typewriterEffect(bubbleEl, fullContent)
      }
    })

    scrollToBottom()
  }

  es.onerror = (err) => {
    // EventSource auto-reconnects, but if we have content, treat as done
    if (fullContent) {
      finishStreaming(aiMsgId, fullContent)
    } else {
      const msg = messages.value.find(m => m.id === aiMsgId)
      if (msg) {
        msg.content = '连接中断，请重试'
        msg._streaming = false
      }
      isStreaming.value = false
    }
    es.close()
    eventSourceRef.value = null
  }
}

function finishStreaming(msgId, content) {
  // Kill typewriter timeline
  if (typewriterTimeline) {
    typewriterTimeline.kill()
    typewriterTimeline = null
  }

  const msg = messages.value.find(m => m.id === msgId)
  if (msg) {
    msg.content = content
    msg.animated = true
    msg._streaming = false

    // Ensure final text is fully rendered
    nextTick(() => {
      const bubbleEl = document.querySelector(`[data-msg-id="${msgId}"] .bubble-text`)
      if (bubbleEl) {
        bubbleEl.innerHTML = formatMarkdown(content)
        // Final glow animation
        gsap.fromTo(bubbleEl.parentElement,
          { boxShadow: '0 0 0px rgba(102, 126, 234, 0)' },
          {
            boxShadow: '0 0 20px rgba(102, 126, 234, 0.3)',
            duration: 0.5,
            yoyo: true,
            repeat: 1,
            ease: 'power2.out'
          }
        )
      }
    })
  }

  if (eventSourceRef.value) {
    eventSourceRef.value.close()
    eventSourceRef.value = null
  }
  isStreaming.value = false
  scrollToBottom()
}

// ---- Stop streaming ----
function stopStreaming() {
  if (eventSourceRef.value) {
    eventSourceRef.value.close()
    eventSourceRef.value = null
  }
  if (typewriterTimeline) {
    typewriterTimeline.kill()
    typewriterTimeline = null
  }

  const lastAi = [...messages.value].reverse().find(m => m.role === 'ai' && m._streaming)
  if (lastAi) {
    lastAi.animated = true
    lastAi._streaming = false
    nextTick(() => {
      const bubbleEl = document.querySelector(`[data-msg-id="${lastAi.id}"] .bubble-text`)
      if (bubbleEl) {
        bubbleEl.innerHTML = formatMarkdown(lastAi.content)
      }
    })
  }
  isStreaming.value = false
}

// ---- GSAP entrance animation for new messages ----
watch(() => messages.value.length, async (newLen, oldLen) => {
  if (newLen <= oldLen) return
  await nextTick()
  const container = messagesContainer.value
  if (!container) return
  const allBubbles = container.querySelectorAll('.message-row')
  const lastBubble = allBubbles[allBubbles.length - 1]
  if (lastBubble && !lastBubble.dataset.gsapInited) {
    lastBubble.dataset.gsapInited = 'true'
    gsap.from(lastBubble, {
      opacity: 0,
      y: 20,
      scale: 0.95,
      duration: 0.4,
      ease: 'back.out(1.4)'
    })
  }
})

// ---- Keyboard handler ----
function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// ---- Lifecycle ----
onMounted(() => {
  loadChatHistory()

  // Entrance animation for the chat container
  gsap.from('.chat-container', {
    opacity: 0,
    y: 30,
    duration: 0.6,
    ease: 'power3.out'
  })
})

onUnmounted(() => {
  if (eventSourceRef.value) {
    eventSourceRef.value.close()
    eventSourceRef.value = null
  }
  if (typewriterTimeline) {
    typewriterTimeline.kill()
    typewriterTimeline = null
  }
})
</script>

<template>
  <div class="chat-page">
    <!-- Background decorations -->
    <div class="glow glow-1"></div>
    <div class="glow glow-2"></div>
    <div class="glow glow-3"></div>

    <div class="chat-container">
      <!-- Header -->
      <header class="chat-header">
        <div class="header-left">
          <button class="back-btn" @click="router.push('/')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5"/>
              <polyline points="12 19 5 12 12 5"/>
            </svg>
          </button>
          <div class="header-info">
            <h1 class="header-title">AI 签到助手</h1>
            <span class="header-subtitle">
              <span class="status-dot" :class="{ 'status-dot-active': !isStreaming }"></span>
              {{ isStreaming ? '思考中...' : '在线' }}
            </span>
          </div>
        </div>
        <div class="header-actions">
          <button
            class="action-btn"
            title="清除聊天记录"
            @click="clearHistory"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
          </button>
        </div>
      </header>

      <!-- Messages area -->
      <div ref="messagesContainer" class="messages-area">
        <!-- Welcome message -->
        <div v-if="!isLoadingHistory && messages.length === 0" class="welcome-section">
          <div class="welcome-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4Z"/>
              <path d="M6 10a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1a8 8 0 0 1-12 0v-1Z"/>
              <path d="M9 18h6"/>
              <path d="M10 22h4"/>
            </svg>
          </div>
          <h2 class="welcome-title">欢迎使用 AI 签到助手</h2>
          <p class="welcome-desc">我可以帮你完成寝室签到、请假报备、考勤查询等操作</p>
          <div class="quick-prompts">
            <button class="prompt-chip" @click="inputText = '今天我已经签到了吗？'">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
              今天我已经签到了吗？
            </button>
            <button class="prompt-chip" @click="inputText = '我要请假'">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><polyline points="14 2 14 8 20 8"/></svg>
              我要请假
            </button>
            <button class="prompt-chip" @click="inputText = '寝室管理条例有哪些？'">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
              寝室管理条例有哪些？
            </button>
          </div>
        </div>

        <!-- Loading history -->
        <div v-if="isLoadingHistory" class="loading-indicator">
          <div class="loading-spinner"></div>
          <span>加载聊天记录中...</span>
        </div>

        <!-- Message bubbles -->
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-row"
          :class="{ 'message-user': msg.role === 'user', 'message-ai': msg.role === 'ai' }"
          :data-msg-id="msg.id"
        >
          <!-- AI avatar -->
          <div v-if="msg.role === 'ai'" class="avatar avatar-ai">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4Z"/>
              <path d="M6 10a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1a8 8 0 0 1-12 0v-1Z"/>
              <path d="M9 18h6"/>
            </svg>
          </div>

          <div class="bubble" :class="{ 'bubble-user': msg.role === 'user', 'bubble-ai': msg.role === 'ai' }">
            <div
              v-if="msg.role === 'ai' && !msg.animated"
              class="bubble-text"
            ></div>
            <div
              v-else-if="msg.role === 'ai'"
              class="bubble-text"
              v-html="formatMarkdown(msg.content)"
            ></div>
            <div v-else class="bubble-text bubble-text-user">{{ msg.content }}</div>

            <!-- Streaming indicator -->
            <div v-if="msg._streaming && !msg.content" class="streaming-dots">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>

          <!-- User avatar -->
          <div v-if="msg.role === 'user'" class="avatar avatar-user">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          </div>
        </div>
      </div>

      <!-- Input area -->
      <div class="input-area">
        <div class="input-wrapper">
          <textarea
            ref="inputRef"
            v-model="inputText"
            class="chat-input"
            placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
            rows="1"
            @keydown="handleKeydown"
            @input="(e) => { e.target.style.height = 'auto'; e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px' }"
          ></textarea>
          <div class="input-actions">
            <button
              v-if="isStreaming"
              class="stop-btn"
              @click="stopStreaming"
              title="停止生成"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <rect x="6" y="6" width="12" height="12" rx="2"/>
              </svg>
            </button>
            <button
              v-else
              class="send-btn"
              :disabled="!inputText.trim()"
              @click="sendMessage"
              title="发送"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="22" y1="2" x2="11" y2="13"/>
                <polygon points="22 2 15 22 11 13 2 9 22 2"/>
              </svg>
            </button>
          </div>
        </div>
        <p class="input-hint">AI 签到助手可以帮助你完成签到、请假、查询等操作</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ---- Page layout ---- */
.chat-page {
  position: relative;
  min-height: calc(100vh - 80px);
  overflow: hidden;
}

/* ---- Background glow orbs ---- */
.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.15;
  pointer-events: none;
  animation: glow-float 20s ease-in-out infinite;
  z-index: 0;
}

.glow-1 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #667eea, #764ba2);
  top: -80px;
  right: -80px;
}

.glow-2 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, #f093fb, #f5576c);
  bottom: 10%;
  left: -60px;
  animation-delay: -7s;
}

.glow-3 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, #4facfe, #00f2fe);
  top: 40%;
  right: 10%;
  animation-delay: -14s;
}

@keyframes glow-float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(20px, -20px) scale(1.05); }
  50% { transform: translate(-15px, 15px) scale(0.95); }
  75% { transform: translate(10px, 10px) scale(1.02); }
}

/* ---- Chat container ---- */
.chat-container {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
  padding: 0;
}

/* ---- Header ---- */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px 20px 0 0;
  flex-shrink: 0;
}

:root.dark .chat-header,
.dark .chat-header {
  background: rgba(15, 23, 42, 0.7);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.6);
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

:root.dark .back-btn,
.dark .back-btn {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.1);
  color: #cbd5e1;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateX(-2px);
}

:root.dark .back-btn:hover,
.dark .back-btn:hover {
  background: rgba(30, 41, 59, 0.9);
}

.header-info {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.3;
}

:root.dark .header-title,
.dark .header-title {
  color: #f1f5f9;
}

.header-subtitle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  color: #6b7280;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fbbf24;
  transition: background 0.3s;
}

.status-dot-active {
  background: #10b981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.4);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.5);
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

:root.dark .action-btn,
.dark .action-btn {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
  color: #94a3b8;
}

.action-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.2);
}

/* ---- Messages area ---- */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  scrollbar-width: thin;
  scrollbar-color: rgba(0,0,0,0.1) transparent;
}

:root.dark .messages-area,
.dark .messages-area {
  background: rgba(15, 23, 42, 0.4);
}

.messages-area::-webkit-scrollbar {
  width: 6px;
}

.messages-area::-webkit-scrollbar-track {
  background: transparent;
}

.messages-area::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

:root.dark .messages-area::-webkit-scrollbar-thumb,
.dark .messages-area::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
}

/* ---- Welcome section ---- */
.welcome-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  flex: 1;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15), rgba(118, 75, 162, 0.15));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
  margin-bottom: 20px;
  animation: float-gentle 4s ease-in-out infinite;
}

:root.dark .welcome-icon,
.dark .welcome-icon {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.25), rgba(118, 75, 162, 0.25));
  color: #a5b4fc;
}

@keyframes float-gentle {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.welcome-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
}

:root.dark .welcome-title,
.dark .welcome-title {
  color: #f1f5f9;
}

.welcome-desc {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0 0 24px;
  max-width: 360px;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.prompt-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid rgba(102, 126, 234, 0.2);
  background: rgba(102, 126, 234, 0.08);
  color: #4f46e5;
  font-size: 0.8125rem;
  cursor: pointer;
  transition: all 0.2s;
}

:root.dark .prompt-chip,
.dark .prompt-chip {
  border-color: rgba(129, 140, 248, 0.25);
  background: rgba(129, 140, 248, 0.1);
  color: #a5b4fc;
}

.prompt-chip:hover {
  background: rgba(102, 126, 234, 0.15);
  border-color: rgba(102, 126, 234, 0.3);
  transform: translateY(-1px);
}

:root.dark .prompt-chip:hover,
.dark .prompt-chip:hover {
  background: rgba(129, 140, 248, 0.2);
  border-color: rgba(129, 140, 248, 0.4);
}

/* ---- Loading indicator ---- */
.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px;
  color: #6b7280;
  font-size: 0.875rem;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(102, 126, 234, 0.2);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ---- Message rows ---- */
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 85%;
}

.message-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-ai {
  align-self: flex-start;
}

/* ---- Avatars ---- */
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-ai {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.avatar-user {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

/* ---- Bubbles ---- */
.bubble {
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.6;
  font-size: 0.9375rem;
  position: relative;
  word-break: break-word;
}

.bubble-ai {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #1a1a1a;
  border-radius: 4px 16px 16px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  min-width: 48px;
}

:root.dark .bubble-ai,
.dark .bubble-ai {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
}

.bubble-user {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-radius: 16px 4px 16px 16px;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.25);
}

.bubble-text {
  line-height: 1.65;
}

.bubble-text :deep(strong) {
  font-weight: 600;
}

.bubble-text :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 0.875em;
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
}

:root.dark .bubble-text :deep(code),
.dark .bubble-text :deep(code) {
  background: rgba(255, 255, 255, 0.1);
}

/* ---- Typing cursor ---- */
:deep(.typing-cursor) {
  display: inline-block;
  color: #667eea;
  font-weight: 300;
  animation: blink-cursor 0.8s step-end infinite;
  margin-left: 1px;
}

@keyframes blink-cursor {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* ---- Streaming dots ---- */
.streaming-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #667eea;
  animation: dot-bounce 1.4s ease-in-out infinite;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dot-bounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* ---- Input area ---- */
.input-area {
  padding: 16px 20px 12px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border-top: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 0 0 20px 20px;
  flex-shrink: 0;
}

:root.dark .input-area,
.dark .input-area {
  background: rgba(15, 23, 42, 0.7);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  padding: 8px 8px 8px 16px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

:root.dark .input-wrapper,
.dark .input-wrapper {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.1);
}

.input-wrapper:focus-within {
  border-color: rgba(102, 126, 234, 0.5);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  resize: none;
  font-size: 0.9375rem;
  line-height: 1.5;
  color: #1a1a1a;
  min-height: 24px;
  max-height: 120px;
  padding: 4px 0;
  font-family: inherit;
}

:root.dark .chat-input,
.dark .chat-input {
  color: #e2e8f0;
}

.chat-input::placeholder {
  color: #9ca3af;
}

:root.dark .chat-input::placeholder,
.dark .chat-input::placeholder {
  color: rgba(255, 255, 255, 0.35);
}

.input-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  padding-bottom: 2px;
}

.send-btn,
.stop-btn {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.send-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  box-shadow: none;
}

.stop-btn {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.stop-btn:hover {
  background: rgba(239, 68, 68, 0.2);
}

.input-hint {
  text-align: center;
  font-size: 0.75rem;
  color: #9ca3af;
  margin: 8px 0 0;
  padding: 0;
}

:root.dark .input-hint,
.dark .input-hint {
  color: rgba(255, 255, 255, 0.3);
}

/* ---- Responsive ---- */
@media (max-width: 640px) {
  .chat-container {
    height: calc(100vh - 100px);
    border-radius: 0;
  }

  .chat-header {
    border-radius: 0;
    padding: 10px 14px;
  }

  .input-area {
    border-radius: 0;
    padding: 12px 14px 8px;
  }

  .messages-area {
    padding: 14px;
  }

  .message-row {
    max-width: 92%;
  }

  .welcome-section {
    padding: 20px 10px;
  }

  .welcome-icon {
    width: 64px;
    height: 64px;
  }

  .welcome-icon svg {
    width: 36px;
    height: 36px;
  }

  .quick-prompts {
    flex-direction: column;
  }
}
</style>
