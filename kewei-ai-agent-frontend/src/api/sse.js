import { API_BASE_URL } from './constants'

function buildUrl(path, params = {}) {
  const url = new URL(`${API_BASE_URL}${path}`)
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value))
    }
  })
  return url
}

export function openSSE({ path, params, withNamedEvents = false, onOpen, onMessage, onDone, onError }) {
  const es = new EventSource(buildUrl(path, params))
  let closed = false
  let done = false
  let hasChunk = false

  const close = () => {
    if (closed) return
    closed = true
    es.close()
  }

  const handleChunk = (text) => {
    if (done || closed) return
    if (text === '[DONE]') {
      done = true
      onDone?.(text)
      close()
      return
    }
    hasChunk = true
    onMessage?.(text)
  }

  es.onopen = () => onOpen?.()

  if (withNamedEvents) {
    es.addEventListener('message', (event) => {
      handleChunk(event.data || '')
    })
    es.addEventListener('done', (event) => {
      if (done || closed) return
      done = true
      onDone?.(event.data || '[DONE]')
      close()
    })
  } else {
    es.onmessage = (event) => {
      handleChunk(event.data || '')
    }
  }

  es.onerror = (event) => {
    if (done || closed) return
    // 部分后端会在发送完最后一个 chunk 后直接断开，而不是发送 done 事件
    if (hasChunk) {
      done = true
      onDone?.('[CLOSED]')
      close()
      return
    }
    onError?.(event)
    close()
  }

  return {
    close() {
      close()
    },
  }
}
