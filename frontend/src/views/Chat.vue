<template>
  <div class="chat-page fade-in">
    <h1 class="page-title">💬 AI 智能咨询</h1>

    <div class="glass chat-messages" ref="messagesRef">
      <div v-if="messages.length === 0" class="empty-chat">
        <span>🤖</span>
        <p>你好！我是AI助手，有什么可以帮您？</p>
      </div>
      <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
        <div class="msg-bubble glass" :class="m.role">
          <div class="msg-content" v-html="formatMsg(m.content)"></div>
        </div>
      </div>
      <div v-if="streaming" class="msg-row assistant">
        <div class="msg-bubble glass assistant">
          <div class="msg-content">{{ streamingText }}<span class="cursor">▊</span></div>
        </div>
      </div>
    </div>

    <div class="chat-input-area glass">
      <input
        v-model="inputMsg"
        class="input-field"
        placeholder="输入你的问题..."
        @keydown.enter="sendMessage"
        :disabled="streaming"
      />
      <button class="btn-primary send-btn" @click="sendMessage" :disabled="!inputMsg.trim() || streaming">
        {{ streaming ? '...' : '发送' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import gsap from 'gsap'

const auth = useAuthStore()
const messages = ref([])
const inputMsg = ref('')
const streaming = ref(false)
const streamingText = ref('')
const messagesRef = ref(null)

function formatMsg(text) {
  if (!text) return ''
  return text.replace(/\n/g, '<br>')
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

async function sendMessage() {
  const msg = inputMsg.value.trim()
  if (!msg || streaming.value) return

  messages.value.push({ role: 'user', content: msg })
  inputMsg.value = ''
  scrollToBottom()

  streaming.value = true
  streamingText.value = ''

  try {
    const url = `/api/ai/chat-stream?message=${encodeURIComponent(msg)}&userId=${encodeURIComponent(auth.studentId)}`
    const es = new EventSource(url)
    let fullText = ''

    es.onmessage = (event) => {
      const data = event.data
      if (data === '[DONE]') {
        es.close()
        messages.value.push({ role: 'assistant', content: fullText })
        streaming.value = false
        streamingText.value = ''
        scrollToBottom()
        return
      }
      fullText += data
      streamingText.value = fullText
      scrollToBottom()
    }

    es.onerror = () => {
      es.close()
      if (streaming.value) {
        if (fullText) {
          messages.value.push({ role: 'assistant', content: fullText })
        } else {
          messages.value.push({ role: 'assistant', content: '⚠️ 连接中断，请重试' })
        }
        streaming.value = false
        streamingText.value = ''
        scrollToBottom()
      }
    }
  } catch {
    messages.value.push({ role: 'assistant', content: '⚠️ 请求失败，请重试' })
    streaming.value = false
  }
}

onMounted(() => {
  gsap.from('.chat-messages', { y: 20, opacity: 0, duration: 0.5 })
})
</script>

<style scoped>
.chat-page { display: flex; flex-direction: column; gap: 1rem; height: calc(100vh - 160px); }
.page-title { font-size: 1.3rem; font-weight: 700; flex-shrink: 0; }

.chat-messages {
  flex: 1; overflow-y: auto; padding: 1rem; display: flex; flex-direction: column; gap: 0.75rem;
  min-height: 0;
}

.empty-chat { text-align: center; color: var(--text-secondary); padding: 3rem 1rem; }
.empty-chat span { font-size: 2.5rem; display: block; margin-bottom: 0.5rem; }

.msg-row { display: flex; }
.msg-row.user { justify-content: flex-end; }
.msg-row.assistant { justify-content: flex-start; }

.msg-bubble {
  max-width: 80%; padding: 0.75rem 1rem; border-radius: 1rem; font-size: 0.9rem; line-height: 1.6;
  word-break: break-word;
}
.msg-bubble.user {
  background: linear-gradient(135deg, var(--accent), var(--accent-light)); color: #fff;
  border-bottom-right-radius: 0.25rem; border: none;
}
.msg-bubble.assistant {
  background: var(--glass-bg); color: var(--text-primary); border: 1px solid var(--glass-border);
  border-bottom-left-radius: 0.25rem;
}

.cursor { animation: blink 0.8s step-end infinite; }
@keyframes blink { 0%,100% { opacity: 1; } 50% { opacity: 0; } }

.chat-input-area {
  display: flex; gap: 0.5rem; padding: 0.75rem; flex-shrink: 0; border-radius: 1rem;
}
.chat-input-area .input-field { flex: 1; border-radius: 0.75rem; }
.send-btn { border-radius: 0.75rem; padding: 0 1.25rem; flex-shrink: 0; }
</style>
