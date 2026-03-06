<template>
  <div class="page chat-page">
    <aside class="chat-sidebar glass-card">
      <div class="glass-content">
        <h3>应用入口</h3>
        <div class="app-list">
          <AppPill
            title="LoveApp"
            subtitle="恋爱顾问（支持图片）"
            :active="store.activeApp === 'love'"
            @click="store.setActiveApp('love')"
          />
          <AppPill
            title="Manus"
            subtitle="通用 Agent（SSE）"
            :active="store.activeApp === 'manus'"
            @click="store.setActiveApp('manus')"
          />
          <AppPill
            title="ExerciseApp"
            subtitle="预留扩展"
            :active="store.activeApp === 'exercise'"
            :disabled="true"
            @click="store.setActiveApp('exercise')"
          />
        </div>

        <div class="mode-box">
          <label>回复模式</label>
          <select v-model="store.chatMode">
            <option v-for="mode in modeOptions" :key="mode.value" :value="mode.value">{{ mode.label }}</option>
          </select>
        </div>

        <button class="btn-outline" type="button" @click="newChat">新建 chatId</button>
      </div>
    </aside>

    <section class="chat-main glass-card">
      <div class="chat-topbar">
        <div>
          <h3>会话中心</h3>
          <p>app: {{ store.activeApp }} | chatId: {{ store.currentChatId }}</p>
        </div>
        <div class="top-actions">
          <select v-model="selectedChatId" @change="switchChat">
            <option v-for="id in store.chatIds" :key="id" :value="id">{{ id }}</option>
          </select>
          <button class="btn-mini" type="button" @click="copyText(store.currentChatId)">复制 chatId</button>
        </div>
      </div>

      <div ref="messageBox" class="message-list">
        <ChatMessage
          v-for="msg in messages"
          :key="msg.id"
          :message="msg"
          @copy="copyText(msg.content)"
          @retry="retryMessage"
        />
        <div v-if="messages.length === 0" class="empty-tip">开始输入你的问题，支持图文一起发送给 AI。</div>
      </div>

      <div class="chat-input-wrap">
        <div v-if="pendingAttachment" class="pending-attachment">
          <img :src="pendingAttachment.previewUrl" :alt="pendingAttachment.fileName" />
          <div class="pending-meta">
            <strong>{{ pendingAttachment.fileName }}</strong>
            <span>{{ pendingAttachment.relativePath || pendingAttachment.filePath }}</span>
          </div>
          <button class="btn-mini" type="button" :disabled="sending || uploading" @click="clearPendingAttachment">移除</button>
        </div>

        <textarea
          v-model="input"
          rows="4"
          placeholder="有问题尽管问。支持图片 + 文本一起发送（Enter 发送，Shift+Enter 换行）"
          @keydown="handleInputKeydown"
        />

        <div class="send-bar">
          <span>{{ sending ? 'AI 正在回复...' : uploading ? '图片上传中...' : 'Ready' }}</span>
          <div class="send-actions">
            <input ref="fileInput" type="file" accept="image/*" class="file-input" @change="handleFileChange" />
            <button class="btn-outline" type="button" :disabled="sending || uploading" @click="triggerUpload">上传图片</button>
            <button class="btn" type="button" :disabled="sending || uploading || (!input.trim() && !pendingAttachment)" @click="send">发送</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppPill from '../components/AppPill.vue'
import ChatMessage from '../components/ChatMessage.vue'
import { loveChatSync, uploadLoveImage } from '../api/modules/ai'
import { openSSE } from '../api/sse'
import { useAppStore } from '../stores/app'
import { uid } from '../utils/uid'

const store = useAppStore()
const route = useRoute()
const input = ref('')
const sending = ref(false)
const uploading = ref(false)
const pendingAttachment = ref(null)
const selectedChatId = ref(store.currentChatId)
const messageBox = ref(null)
const messages = ref([])
const fileInput = ref(null)
let currentSSE = null

const modeOptions = computed(() => {
  if (store.activeApp === 'manus') {
    return [{ value: 'sse', label: '流式 sse（Manus）' }]
  }
  return [
    { value: 'sync', label: '同步 sync' },
    { value: 'sse', label: '流式 sse' },
    { value: 'sse_emitter', label: '事件流 sse_emitter' },
  ]
})

watch(
  () => store.currentChatId,
  (val) => {
    selectedChatId.value = val
  },
)

watch(
  () => route.query.app,
  (app) => {
    if (app === 'manus' || app === 'love') {
      store.setActiveApp(app)
    }
  },
  { immediate: true },
)

watch(
  () => store.activeApp,
  (app) => {
    if (app === 'manus' && store.chatMode !== 'sse') {
      store.setChatMode('sse')
    }
  },
)

watch(
  () => messages.value.length,
  async () => {
    await nextTick()
    if (messageBox.value) {
      messageBox.value.scrollTop = messageBox.value.scrollHeight
    }
  },
)

function switchChat() {
  store.switchChatId(selectedChatId.value)
}

function newChat() {
  const id = store.createNewChatId()
  selectedChatId.value = id
}

function normalizeAssistantText(raw) {
  if (raw == null) return ''
  if (typeof raw === 'string') return raw
  if (typeof raw === 'object') return raw.content || raw.text || JSON.stringify(raw)
  return String(raw)
}

function addMessage(role, content, status = 'done', extra = {}) {
  const item = {
    id: uid('msg'),
    role,
    content,
    status,
    ...extra,
  }
  messages.value.push(item)
  return item
}

function patchMessage(id, patch) {
  const idx = messages.value.findIndex((it) => it.id === id)
  if (idx >= 0) {
    messages.value[idx] = { ...messages.value[idx], ...patch }
  }
}

async function send() {
  if (sending.value || (!input.value.trim() && !pendingAttachment.value)) return

  const question = input.value.trim()
  const attachment = pendingAttachment.value
  const messageForAi = question || (attachment ? '请解释这张图片内容，并给出关键结论。' : '')
  input.value = ''
  pendingAttachment.value = null

  const user = addMessage('user', question || '请解释这张图片', 'done', {
    attachments: attachment ? [attachment] : [],
  })
  const assistant = addMessage('assistant', '', 'loading', {
    retryPayload: { question, attachment, chatId: store.currentChatId, app: store.activeApp, userId: user.id },
  })

  sending.value = true
  const start = Date.now()

  try {
    store.addChatId(store.currentChatId)

    if (store.activeApp === 'love' && store.chatMode === 'sync') {
      const res = await loveChatSync(buildLoveParams(messageForAi, attachment))
      patchMessage(assistant.id, {
        content: normalizeAssistantText(res.data),
        status: 'done',
      })
      store.addLog({ endpoint: '/ai/love_app/chat/sync', status: 'success', elapsed: res.elapsed })
      return
    }

    const endpoint = store.activeApp === 'manus'
      ? '/ai/manus/chat'
      : (store.chatMode === 'sse' ? '/ai/love_app/chat/sse' : '/ai/love_app/chat/sse_emitter')

    const params = store.activeApp === 'manus'
      ? buildManusParams(messageForAi, attachment)
      : buildLoveParams(messageForAi, attachment)

    await sendViaSSE({
      messageId: assistant.id,
      endpoint,
      params,
      withNamedEvents: store.activeApp === 'love' && store.chatMode === 'sse_emitter',
      timeoutMs: store.activeApp === 'manus' ? 240000 : 45000,
      typewriter: store.activeApp === 'manus',
    })

    store.addLog({ endpoint, status: 'success', elapsed: Date.now() - start })
  } catch (error) {
    patchMessage(assistant.id, {
      content: error.message || '请求失败',
      status: 'error',
    })
    store.addLog({
      endpoint: store.activeApp === 'manus' ? '/ai/manus/chat' : store.chatMode,
      status: 'error',
      elapsed: Date.now() - start,
      message: error.message,
    })
  } finally {
    sending.value = false
  }
}

function sendViaSSE({ messageId, endpoint, params, withNamedEvents, timeoutMs = 45000, typewriter = false }) {
  return new Promise((resolve, reject) => {
    let finalText = ''
    let settled = false
    let timeoutId = null
    let timerId = null
    const queue = []

    const clearTimer = () => {
      if (timerId) {
        clearInterval(timerId)
        timerId = null
      }
    }

    const startTypewriter = () => {
      if (timerId) return
      timerId = setInterval(() => {
        if (!queue.length) {
          if (settled) clearTimer()
          return
        }
        const chunk = queue.splice(0, 12).join('')
        finalText += chunk
        patchMessage(messageId, { content: finalText, status: 'loading' })
      }, 12)
    }

    currentSSE?.close()
    currentSSE = openSSE({
      path: endpoint,
      params,
      withNamedEvents,
      onMessage: (chunk) => {
        if (typewriter) {
          queue.push(...chunk.split(''))
          startTypewriter()
        } else {
          finalText += chunk
          patchMessage(messageId, { content: finalText, status: 'loading' })
        }
      },
      onDone: () => {
        if (settled) return
        settled = true
        if (timeoutId) clearTimeout(timeoutId)
        if (typewriter && queue.length) {
          finalText += queue.join('')
          queue.length = 0
        }
        clearTimer()
        patchMessage(messageId, { content: finalText || '(empty)', status: 'done' })
        resolve()
      },
      onError: () => {
        if (settled) return
        settled = true
        if (timeoutId) clearTimeout(timeoutId)
        clearTimer()
        reject(new Error('SSE 连接失败，请检查后端接口'))
      },
    })

    if (timeoutMs > 0) {
      timeoutId = setTimeout(() => {
        if (settled) return
        settled = true
        if (sending.value && (finalText || queue.length)) {
          while (queue.length) {
            finalText += queue.shift()
          }
          clearTimer()
          patchMessage(messageId, { content: finalText, status: 'done' })
          currentSSE?.close()
          resolve()
        } else {
          clearTimer()
          currentSSE?.close()
          reject(new Error('SSE 长时间无响应，请重试'))
        }
      }, timeoutMs)
    }
  })
}

function retryMessage(message) {
  if (!message.retryPayload || sending.value) return
  input.value = message.retryPayload.question || ''
  pendingAttachment.value = message.retryPayload.attachment || null
  if (message.retryPayload.app) {
    store.setActiveApp(message.retryPayload.app)
  }
  send()
}

function handleInputKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    send()
  }
}

async function copyText(text) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    store.addLog({ endpoint: 'clipboard', status: 'success', message: '复制成功', elapsed: 0 })
  } catch {
    store.addLog({ endpoint: 'clipboard', status: 'error', message: '复制失败', elapsed: 0 })
  }
}

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(event) {
  const file = event.target?.files?.[0]
  event.target.value = ''
  if (!file || uploading.value) return

  uploading.value = true
  const start = Date.now()
  try {
    const formData = new FormData()
    formData.append('chatId', store.currentChatId)
    formData.append('file', file)

    const res = await uploadLoveImage(formData)
    const payload = res.data || {}

    if (pendingAttachment.value?.previewUrl) {
      URL.revokeObjectURL(pendingAttachment.value.previewUrl)
    }

    pendingAttachment.value = {
      fileName: payload.fileName || file.name,
      filePath: payload.filePath || '',
      relativePath: payload.relativePath || '',
      previewUrl: URL.createObjectURL(file),
      mimeType: file.type || '',
      size: file.size || 0,
    }

    store.addLog({
      endpoint: '/ai/love_app/image/upload',
      status: 'success',
      elapsed: res.elapsed || Date.now() - start,
      message: `已加入待发送: ${payload.fileName || file.name}`,
    })
  } catch (error) {
    store.addLog({
      endpoint: '/ai/love_app/image/upload',
      status: 'error',
      elapsed: Date.now() - start,
      message: error.message || '图片上传失败',
    })
  } finally {
    uploading.value = false
  }
}

function clearPendingAttachment() {
  if (pendingAttachment.value?.previewUrl) {
    URL.revokeObjectURL(pendingAttachment.value.previewUrl)
  }
  pendingAttachment.value = null
}

function buildLoveParams(message, attachment) {
  const params = {
    message,
    chatId: store.currentChatId,
  }
  if (attachment?.filePath) {
    params.option = 'image'
    params.imagePath = attachment.filePath
  }
  return params
}

function buildManusParams(message, attachment) {
  const params = { message }
  if (attachment?.filePath) {
    params.option = 'image'
    params.imagePath = attachment.filePath
  }
  return params
}
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 270px 1fr;
  gap: var(--space-3);
}

.chat-sidebar {
  height: fit-content;
  position: sticky;
  top: 82px;
}

.chat-main {
  min-height: 72vh;
  display: flex;
  flex-direction: column;
}

.chat-topbar {
  padding: var(--space-4);
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.chat-topbar p {
  margin-top: 6px;
  color: var(--text-subtle);
  font-size: 13px;
}

.top-actions {
  width: min(320px, 100%);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: var(--space-4);
}

.chat-input-wrap {
  border-top: 1px solid var(--border);
  padding: var(--space-3) var(--space-4) var(--space-4);
  background: rgba(255, 255, 255, 0.62);
}

.pending-attachment {
  margin-bottom: 10px;
  display: flex;
  gap: 10px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: #2d2f34;
  color: #fff;
  padding: 10px;
}

.pending-attachment img {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  object-fit: cover;
}

.pending-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pending-meta span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  word-break: break-all;
}

.send-bar {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--text-subtle);
  font-size: 13px;
}

.send-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.file-input {
  display: none;
}

.app-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mode-box {
  margin: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-tip {
  text-align: center;
  color: var(--text-subtle);
  margin: auto 0;
}

@media (max-width: 1024px) {
  .chat-page {
    grid-template-columns: 1fr;
  }

  .chat-sidebar {
    position: static;
  }
}
</style>
