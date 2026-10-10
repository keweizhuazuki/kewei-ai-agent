import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { setImmediate } from 'node:timers/promises'
import { test } from 'node:test'
import vm from 'node:vm'

// 用独立 VM 注入浏览器 API 和 Vite 配置，无需浏览器或额外测试依赖。
const source = readFileSync(new URL('../src/api/sse.js', import.meta.url), 'utf8')
  .replace(/^import \{ API_BASE_URL \} from '\.\/constants'\r?\n/, '')
  .replace(/^export /gm, '')

function loadSSE(overrides = {}) {
  return vm.runInNewContext(`${source}\n;({ openSSE, openFetchSSE })`, {
    API_BASE_URL: 'http://localhost:8123/api',
    URL,
    AbortController,
    TextDecoder,
    ...overrides,
  })
}

function eventSourceHarness(options = {}) {
  let source
  class FakeEventSource {
    constructor(url) {
      this.url = url
      this.listeners = new Map()
      this.closeCount = 0
      source = this
    }

    addEventListener(name, listener) {
      this.listeners.set(name, listener)
    }

    close() {
      this.closeCount += 1
    }

    emit(name, event = {}) {
      this.listeners.get(name)?.(event)
      this[`on${name}`]?.(event)
    }
  }

  const events = []
  const { openSSE } = loadSSE({ EventSource: FakeEventSource })
  const stream = openSSE({
    path: '/ai/manus/chat',
    withNamedEvents: true,
    onOpen: () => events.push(['open']),
    onMessage: (data) => events.push(['message', data]),
    onQuestion: (data) => events.push(['question', data]),
    onTodo: (data) => events.push(['todo', data]),
    onDone: (data) => events.push(['done', data]),
    onError: (error) => events.push(['error', error.data]),
    ...options,
  })
  return { source, stream, events }
}

function encodedChunks(text, size) {
  const bytes = new TextEncoder().encode(text)
  if (!size) return [bytes]
  return Array.from({ length: Math.ceil(bytes.length / size) }, (_, index) => bytes.slice(index * size, (index + 1) * size))
}

function fetchHarness(text = '', options = {}) {
  const events = []
  let request
  let cancelCount = 0
  const body = new ReadableStream({
    start(controller) {
      for (const chunk of encodedChunks(text, options.chunkSize)) controller.enqueue(chunk)
      if (!options.keepOpen) controller.close()
    },
    cancel() {
      cancelCount += 1
    },
  })
  const { openFetchSSE } = loadSSE({
    fetch: async (url, init) => {
      request = { url, init }
      if (options.fetch) return options.fetch(url, init)
      return { ok: true, body, ...options.response }
    },
  })
  const stream = openFetchSSE({
    path: '/ai/manus/chat/continue',
    body: { chatId: 'chat-1', answers: { q_1: 'A' } },
    onOpen: () => events.push(['open']),
    onMessage: (data) => events.push(['message', data]),
    onQuestion: (data) => events.push(['question', data]),
    onTodo: (data) => events.push(['todo', data]),
    onDone: (data) => events.push(['done', data]),
    onError: (error) => events.push(['error', error.message]),
  })
  return { events, stream, body, get request() { return request }, get cancelCount() { return cancelCount } }
}

for (const prefix of [null, 'message', 'question']) {
  test(`EventSource reports named errors after ${prefix || 'no event'} exactly once`, () => {
    const { source, events } = eventSourceHarness()
    if (prefix) source.emit(prefix, { data: 'first' })
    source.emit('error', { data: '模型执行失败' })
    source.emit('error', {})
    source.emit('done', { data: '[DONE]' })
    assert.deepEqual(events, [...(prefix ? [[prefix, 'first']] : []), ['error', '模型执行失败']])
    assert.equal(source.closeCount, 1)
  })
}

test('EventSource reports an empty named error instead of successful EOF', () => {
  const { source, events } = eventSourceHarness()
  source.emit('message', { data: 'partial' })
  source.emit('error', { data: '' })
  assert.deepEqual(events, [['message', 'partial'], ['error', '']])
})

test('EventSource requires done for a named stream even after receiving text', () => {
  const { source, events } = eventSourceHarness()
  source.emit('message', { data: 'partial' })
  source.emit('error', {})
  assert.deepEqual(events, [['message', 'partial'], ['error', undefined]])
})

test('EventSource preserves EOF completion for plain Flux text streams', () => {
  const { source, events } = eventSourceHarness({ withNamedEvents: false })
  source.emit('message', { data: 'answer' })
  source.emit('error', {})
  assert.deepEqual(events, [['message', 'answer'], ['done', '[CLOSED]']])
})

test('EventSource preserves EOF completion when waiting for user answers', () => {
  const { source, events } = eventSourceHarness()
  source.emit('question', { data: '{"questions":[]}' })
  source.emit('error', {})
  assert.deepEqual(events, [['question', '{"questions":[]}'], ['done', '[CLOSED]']])
})

for (const terminal of ['done', 'sentinel', 'close']) {
  test(`EventSource ignores all late callbacks after ${terminal}`, () => {
    const { source, stream, events } = eventSourceHarness()
    if (terminal === 'close') stream.close()
    else source.emit(terminal === 'done' ? 'done' : 'message', { data: '[DONE]' })
    for (const name of ['open', 'message', 'question', 'todo', 'done', 'error']) source.emit(name, { data: 'late' })
    stream.close()
    assert.deepEqual(events, terminal === 'close' ? [] : [['done', '[DONE]']])
    assert.equal(source.closeCount, 1)
  })
}

for (const prefix of ['', 'event:message\ndata:partial\n\n', 'event:question\ndata:{"questions":[]}\n\n']) {
  test(`fetch SSE reports backend errors without done, prefix=${JSON.stringify(prefix)}`, async () => {
    const harness = fetchHarness(`${prefix}event:error\ndata:模型执行失败\n\nevent:done\ndata:[DONE]\n\n`)
    await setImmediate()
    assert.deepEqual(harness.events.at(-1), ['error', '模型执行失败'])
    assert.equal(harness.events.filter(([name]) => name === 'done').length, 0)
    assert.equal(harness.events.filter(([name]) => name === 'error').length, 1)
    assert.equal(harness.events.some(([name, data]) => name === 'message' && data === '模型执行失败'), false)
    assert.equal(harness.request.init.signal.aborted, true)
    assert.equal(harness.body.locked, false)
  })
}

test('fetch SSE assembles multiline UTF-8 errors before dispatch across every byte boundary', async () => {
  const harness = fetchHarness('data: 请求失败  \r\ndata:  保留缩进\r\nevent:error\r\n\r\n', { chunkSize: 1 })
  await setImmediate()
  assert.deepEqual(harness.events, [['open'], ['error', '请求失败  \n 保留缩进']])
})

test('fetch SSE treats an empty error event as failure', async () => {
  const harness = fetchHarness('event:error\ndata:\n\n')
  await setImmediate()
  assert.deepEqual(harness.events, [['open'], ['error', 'SSE 请求失败']])
})

for (const newline of ['\n', '\r\n', '\r']) {
  test(`fetch SSE preserves event framing and whitespace with ${JSON.stringify(newline)} line endings`, async () => {
    const payload = [
      ': heartbeat', '', 'event:message', 'data: first  ', 'data:  second', '',
      'event:todo', 'data:{"todo":{"items":[]}}', '',
      'event:done', 'data:[DONE]', '', '',
    ].join(newline)
    const harness = fetchHarness(payload, { chunkSize: 1 })
    await setImmediate()
    assert.deepEqual(harness.events, [
      ['open'], ['message', 'first  \n second'], ['todo', '{"todo":{"items":[]}}'], ['done', '[DONE]'],
    ])
  })
}

test('fetch SSE preserves question pauses without a done event', async () => {
  const harness = fetchHarness('event:question\ndata:{"questions":[]}\n\n')
  await setImmediate()
  assert.deepEqual(harness.events, [['open'], ['question', '{"questions":[]}'], ['done', '[DONE]']])
})

for (const payload of ['', 'event:message\ndata:partial\n\n', 'event:done\ndata:[DONE]\n']) {
  test(`fetch SSE rejects EOF before completion, payload=${JSON.stringify(payload)}`, async () => {
    const harness = fetchHarness(payload)
    await setImmediate()
    assert.deepEqual(harness.events.at(-1), ['error', 'SSE 连接提前关闭，请重试'])
    assert.equal(harness.events.some(([name]) => name === 'done'), false)
  })
}

for (const terminal of ['event:done\ndata:[DONE]\n\n', 'data:[DONE]\n\n']) {
  test(`fetch SSE stops exactly once after ${JSON.stringify(terminal)}`, async () => {
    const harness = fetchHarness(`${terminal}event:error\ndata:late failure\n\ndata:late text\n\n${terminal}`, { keepOpen: true })
    await setImmediate()
    assert.deepEqual(harness.events, [['open'], ['done', '[DONE]']])
    assert.equal(harness.cancelCount, 1)
    harness.stream.close()
    assert.equal(harness.cancelCount, 1)
  })
}

test('fetch SSE abort before response arrival suppresses callbacks and cancels the body', async () => {
  let respond
  const harness = fetchHarness('', { fetch: () => new Promise((resolve) => { respond = resolve }) })
  harness.stream.close()
  let cancelled = false
  respond({ ok: true, body: new ReadableStream({ cancel() { cancelled = true } }) })
  await setImmediate()
  assert.deepEqual(harness.events, [])
  assert.equal(cancelled, true)
  assert.equal(harness.request.init.signal.aborted, true)
})

test('fetch SSE abort during a pending read suppresses completion and cancels the body', async () => {
  const harness = fetchHarness('', { keepOpen: true })
  await setImmediate()
  harness.stream.close()
  harness.stream.close()
  await setImmediate()
  assert.deepEqual(harness.events, [['open']])
  assert.equal(harness.cancelCount, 1)
  assert.equal(harness.body.locked, false)
})

test('fetch SSE reports a rejected network request once', async () => {
  const harness = fetchHarness('', { fetch: () => { throw new Error('network unavailable') } })
  await setImmediate()
  assert.deepEqual(harness.events, [['error', 'network unavailable']])
  assert.equal(harness.request.init.signal.aborted, true)
})

for (const response of [{ ok: false }, { body: null }]) {
  test(`fetch SSE rejects unusable responses ${JSON.stringify(response)}`, async () => {
    const harness = fetchHarness('', { response })
    await setImmediate()
    assert.deepEqual(harness.events, [['error', 'SSE 请求失败']])
  })
}

test('fetch SSE reports a read failure after partial text instead of completing', async () => {
  let controller
  const body = new ReadableStream({
    start(value) {
      controller = value
      value.enqueue(new TextEncoder().encode('data:partial\n\n'))
    },
  })
  const harness = fetchHarness('', { response: { body } })
  await setImmediate()
  controller.error(new Error('connection reset'))
  await setImmediate()
  assert.deepEqual(harness.events, [['open'], ['message', 'partial'], ['error', 'connection reset']])
  assert.equal(body.locked, false)
})

test('fetch SSE keeps POST JSON and AbortSignal request options', async () => {
  const harness = fetchHarness('data:[DONE]\n\n')
  await setImmediate()
  assert.equal(harness.request.url, 'http://localhost:8123/api/ai/manus/chat/continue')
  assert.equal(harness.request.init.method, 'POST')
  assert.equal(harness.request.init.headers.Accept, 'text/event-stream')
  assert.equal(harness.request.init.headers['Content-Type'], 'application/json')
  assert.deepEqual(JSON.parse(harness.request.init.body), { chatId: 'chat-1', answers: { q_1: 'A' } })
})
