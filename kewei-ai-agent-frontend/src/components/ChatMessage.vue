<template>
  <article class="chat-message" :class="`is-${message.role}`">
    <div class="message-meta">
      <strong>{{ message.role === 'user' ? '你' : 'AI' }}</strong>
      <span>{{ message.status === 'error' ? '失败' : message.status === 'loading' ? '生成中' : '完成' }}</span>
    </div>

    <div v-if="message.attachments?.length" class="message-attachments">
      <figure v-for="item in message.attachments" :key="item.fileName" class="attachment-item">
        <img v-if="item.previewUrl" :src="item.previewUrl" :alt="item.fileName" />
        <figcaption>
          <strong>{{ item.fileName }}</strong>
          <span>{{ item.relativePath || item.filePath }}</span>
        </figcaption>
      </figure>
    </div>

    <div v-if="isMarkdown" class="message-content markdown-body" v-html="htmlContent" />
    <pre v-else class="message-content">{{ message.content }}</pre>

    <div class="message-actions">
      <button type="button" class="btn-mini" @click="$emit('copy', message)">复制</button>
      <button
        v-if="message.status === 'error'"
        type="button"
        class="btn-mini"
        @click="$emit('retry', message)"
      >
        重试
      </button>
    </div>
  </article>
</template>

<script setup>
import { computed, toRefs } from 'vue'
import { renderAssistantContent } from '../utils/markdown'

const props = defineProps({
  message: {
    type: Object,
    required: true,
  },
})

defineEmits(['retry', 'copy'])

const { message } = toRefs(props)
const isMarkdown = computed(() => message.value.role === 'assistant')
const htmlContent = computed(() => renderAssistantContent(message.value.content || ''))
</script>

<style scoped>
.message-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.attachment-item {
  margin: 0;
  width: min(240px, 100%);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

.attachment-item img {
  width: 100%;
  max-height: 160px;
  object-fit: cover;
  display: block;
}

.attachment-item figcaption {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
}

.attachment-item span {
  color: var(--text-subtle);
  word-break: break-all;
}

.markdown-body :deep(p),
.markdown-body :deep(ul),
.markdown-body :deep(ol),
.markdown-body :deep(pre),
.markdown-body :deep(blockquote) {
  margin: 0 0 8px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 22px;
}

.markdown-body :deep(li) {
  margin: 0 0 6px;
}

.markdown-body :deep(pre) {
  background: #fff6ee;
  border-radius: 10px;
  padding: 10px;
  overflow-x: auto;
}

.markdown-body :deep(h3) {
  margin: 10px 0 6px;
  font-size: 15px;
  color: var(--primary-deep);
}

.markdown-body :deep(code) {
  font-family: 'SFMono-Regular', Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.markdown-body :deep(a) {
  color: var(--primary-deep);
  overflow-wrap: anywhere;
  word-break: break-all;
  max-width: 100%;
}

.markdown-body :deep(.manus-trace) {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.markdown-body :deep(.trace-step) {
  border: 1px solid rgba(90, 90, 90, 0.25);
  background: #f3f4f6;
  color: #5f6368;
  border-radius: 12px;
  padding: 10px;
}

.markdown-body :deep(.trace-step h4) {
  margin: 0 0 8px;
  font-size: 13px;
  color: #6f7379;
}

.markdown-body :deep(.trace-step p),
.markdown-body :deep(.trace-step li),
.markdown-body :deep(.trace-step a),
.markdown-body :deep(.trace-step strong) {
  color: #5f6368;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.markdown-body :deep(.manus-final) {
  margin-top: 12px;
  border: 1px solid rgba(240, 128, 98, 0.35);
  background: linear-gradient(160deg, #fff8ef, #fff2e6);
  border-radius: 14px;
  padding: 12px;
  box-shadow: 0 8px 18px rgba(229, 103, 67, 0.12);
}

.markdown-body :deep(.manus-final h4) {
  margin: 0 0 10px;
  color: var(--primary-deep);
}
</style>
