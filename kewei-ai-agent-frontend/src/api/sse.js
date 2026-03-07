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

export function openSSE({ path, params, withNamedEvents = false, onOpen, onMessage, onQuestion, onTodo, onDone, onError }) {
  const es = new EventSource(buildUrl(path, params))
  let closed = false
  let done = false
  let hasChunk = false
  let hasQuestion = false

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
    es.addEventListener('question', (event) => {
      if (done || closed) return
      hasQuestion = true
      onQuestion?.(event.data || '')
    })
    es.addEventListener('todo', (event) => {
      if (done || closed) return
      onTodo?.(event.data || '')
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
    if (hasChunk || hasQuestion) {
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

export function openFetchSSE({ path, method = 'POST', body, onOpen, onMessage, onQuestion, onTodo, onDone, onError }) {
  let closed = false
  const controller = new AbortController()

  const close = () => {
    if (closed) return
    closed = true
    controller.abort()
  }

  const run = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}${path}`, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
        },
        body: body ? JSON.stringify(body) : undefined,
        signal: controller.signal,
      })

      if (!response.ok || !response.body) {
        throw new Error('SSE 请求失败')
      }

      onOpen?.()
      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let eventName = 'message'

      while (!closed) {
        const { value, done } = await reader.read()
        if (done) {
          onDone?.('[DONE]')
          close()
          return
        }
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const rawLine of lines) {
          const line = rawLine.trimEnd()
          if (!line) {
            eventName = 'message'
            continue
          }
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
            continue
          }
          if (!line.startsWith('data:')) {
            continue
          }
          const data = line.slice(5).trim()
          if (eventName === 'question') {
            onQuestion?.(data)
          } else if (eventName === 'todo') {
            onTodo?.(data)
          } else if (eventName === 'done' || data === '[DONE]') {
            onDone?.(data || '[DONE]')
            close()
            return
          } else {
            onMessage?.(data)
          }
        }
      }
    } catch (error) {
      if (!closed) {
        onError?.(error)
      }
    }
  }

  run()

  return {
    close,
  }
}
