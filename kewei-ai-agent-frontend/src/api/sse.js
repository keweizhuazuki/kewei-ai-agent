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

  const finish = (data) => {
    if (done || closed) return
    done = true
    close()
    onDone?.(data)
  }

  const fail = (error) => {
    if (done || closed) return
    done = true
    close()
    onError?.(error)
  }

  const handleChunk = (text) => {
    if (done || closed) return
    if (text === '[DONE]') {
      finish(text)
      return
    }
    hasChunk = true
    onMessage?.(text)
  }

  es.onopen = () => {
    if (!done && !closed) onOpen?.()
  }

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
      finish(event.data || '[DONE]')
    })
  } else {
    es.onmessage = (event) => {
      handleChunk(event.data || '')
    }
  }

  es.onerror = (event) => {
    if (done || closed) return
    // 后端的命名 error 事件也会触发 onerror，不能当作正常断流。
    if (typeof event?.data === 'string') {
      fail(event)
      return
    }
    // 普通 Flux 流没有 done 事件；命名事件流只能在 done 或待回答时结束。
    if (hasQuestion || (!withNamedEvents && hasChunk)) {
      finish('[CLOSED]')
      return
    }
    fail(event)
  }

  return {
    close() {
      close()
    },
  }
}

export function openFetchSSE({ path, method = 'POST', body, onOpen, onMessage, onQuestion, onTodo, onDone, onError }) {
  let closed = false
  let reader = null
  const controller = new AbortController()

  const close = () => {
    if (closed) return
    closed = true
    controller.abort()
    if (reader) reader.cancel().catch(() => {})
  }

  const finish = (data) => {
    if (closed) return
    close()
    onDone?.(data)
  }

  const fail = (error) => {
    if (closed) return
    close()
    onError?.(error)
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

      if (closed) {
        await response.body?.cancel()
        return
      }
      if (!response.ok || !response.body) {
        throw new Error('SSE 请求失败')
      }

      reader = response.body.getReader()
      onOpen?.()
      if (closed) return
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let eventName = 'message'
      let dataLines = []
      let hasQuestion = false

      const dispatchEvent = () => {
        const name = eventName
        const data = dataLines.join('\n')
        const hasData = dataLines.length > 0
        eventName = 'message'
        dataLines = []
        if (!hasData) return

        if (name === 'error') {
          fail(new Error(data || 'SSE 请求失败'))
        } else if (name === 'done' || data === '[DONE]') {
          finish(data || '[DONE]')
        } else if (name === 'question') {
          hasQuestion = true
          onQuestion?.(data)
        } else if (name === 'todo') {
          onTodo?.(data)
        } else {
          onMessage?.(data)
        }
      }

      const processLines = (atEnd = false) => {
        while (!closed) {
          const separator = /\r\n|\r|\n/.exec(buffer)
          if (!separator) return
          // CRLF 可能被拆到两个网络 chunk 中，先保留末尾的 CR。
          if (!atEnd && separator[0] === '\r' && separator.index === buffer.length - 1) return
          const line = buffer.slice(0, separator.index)
          buffer = buffer.slice(separator.index + separator[0].length)
          if (!line) {
            dispatchEvent()
            continue
          }
          const colon = line.indexOf(':')
          const field = colon < 0 ? line : line.slice(0, colon)
          let value = colon < 0 ? '' : line.slice(colon + 1)
          if (value.startsWith(' ')) value = value.slice(1)
          if (field === 'event') eventName = value
          if (field === 'data') dataLines.push(value)
        }
      }

      while (!closed) {
        const { value, done } = await reader.read()
        if (closed) return
        if (done) {
          buffer += decoder.decode()
          processLines(true)
          if (closed) return
          if (hasQuestion) {
            finish('[DONE]')
          } else {
            fail(new Error('SSE 连接提前关闭，请重试'))
          }
          return
        }
        buffer += decoder.decode(value, { stream: true })
        processLines()
      }
    } catch (error) {
      fail(error)
    } finally {
      reader?.releaseLock()
    }
  }

  run()

  return {
    close,
  }
}
