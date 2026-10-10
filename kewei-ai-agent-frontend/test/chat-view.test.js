import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import vm from 'node:vm'
import { computed, nextTick, reactive, ref, watch } from 'vue'

// 执行组件本身的状态逻辑；浏览器交互另由 ui-flows.mjs 覆盖。
const component = readFileSync(new URL('../src/views/ChatView.vue', import.meta.url), 'utf8')
const source = component.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm, '')
const loveClick = component.match(/title="LoveApp"[\s\S]*?@click="([^"]+)"/)[1]
const manusClick = component.match(/title="Manus"[\s\S]*?@click="([^"]+)"/)[1]
const questions = {
  questions: [{ id: 'q_1', header: '格式', question: '选择格式？', multiSelect: false, options: [{ label: 'PDF' }, { label: 'PPT' }] }],
}
const todo = (status) => ({ todo: { items: [{ id: 'build', content: '制作报告', status }] } })
const plain = (value) => JSON.parse(JSON.stringify(value))

function harness() {
  let messageId = 0
  let timerId = 0
  const timers = new Map()
  const requests = []
  const store = reactive({
    activeApp: 'love', chatMode: 'sync', currentChatId: 'chat-1', chatIds: ['chat-1', 'chat-2'],
    setActiveApp(value) { this.activeApp = value },
    setChatMode(value) { this.chatMode = value },
    addChatId() {}, addLog() {},
    switchChatId(value) { this.currentChatId = value },
    createNewChatId() { this.currentChatId = 'new-chat'; return this.currentChatId },
  })
  const open = (transport) => (callbacks) => {
    const request = { transport, callbacks, closed: false }
    requests.push(request)
    return { close() { request.closed = true } }
  }
  const state = vm.runInNewContext(`${source}\n;({ input, send, submitPendingAnswers, newChat, switchChat, selectedChatId, pendingAnswers, pendingQuestions, pendingQuestionMessageId, pinnedTodo, messages, sending, modeOptions, clickLoveApp: () => { ${loveClick} }, clickManus: () => { ${manusClick} } })`, {
    ref, computed, watch, nextTick,
    useRoute: () => ({ query: { app: 'manus' } }), useAppStore: () => store,
    uid: () => `msg-${++messageId}`,
    openSSE: open('event-source'), openFetchSSE: open('fetch'),
    setTimeout: (callback) => { timers.set(++timerId, callback); return timerId },
    clearTimeout: (id) => timers.delete(id),
    setInterval: (callback) => { timers.set(++timerId, callback); return timerId },
    clearInterval: (id) => timers.delete(id),
  })
  return { ...state, store, requests, timers }
}

test('opening the Manus route initializes a reply mode that exists in its selector', () => {
  const app = harness()
  assert.equal(app.store.activeApp, 'manus')
  assert.equal(app.store.chatMode, 'sse')
  assert.equal(app.modeOptions.value.some((option) => option.value === app.store.chatMode), true)
})

test('switching applications clears pending Manus questions and todo state', async () => {
  const app = harness()
  await pauseForQuestion(app)
  app.clickLoveApp()
  await nextTick()

  assert.equal(app.store.activeApp, 'love')
  assert.equal(app.pendingQuestions.value.length, 0)
  assert.deepEqual(plain(app.pendingAnswers.value), {})
  assert.equal(app.pendingQuestionMessageId.value, '')
  assert.equal(app.pinnedTodo.value, null)
  await app.submitPendingAnswers()
  assert.equal(app.requests.length, 1)
  app.store.chatMode = 'sync'
  app.clickManus()
  await nextTick()
  assert.equal(app.store.chatMode, 'sse')
})

test('application clicks cannot retarget an active response', async () => {
  const app = harness()
  app.input.value = '正在执行的任务'
  const sending = app.send()
  app.clickLoveApp()
  const activeApp = app.store.activeApp
  app.requests[0].callbacks.onDone('[DONE]')
  await sending

  assert.equal(activeApp, 'manus')
  assert.equal(app.sending.value, false)
  assert.equal(app.timers.size, 0)
})

test('clicking the already active app preserves its pending answers', async () => {
  const app = harness()
  await pauseForQuestion(app)
  app.clickManus()
  await nextTick()
  assert.equal(app.pendingQuestions.value.length, 1)
  assert.equal(app.pendingAnswers.value.q_1, 'PDF')
})

async function pauseForQuestion(app) {
  app.input.value = '帮我制作报告'
  const sending = app.send()
  const request = app.requests.at(-1)
  request.callbacks.onTodo(JSON.stringify(todo('in_progress')))
  request.callbacks.onQuestion(JSON.stringify(questions))
  request.callbacks.onDone('[CLOSED]')
  await sending
  app.pendingAnswers.value.q_1 = 'PDF'
}

test('continuation decodes todo events and archives the updated completed snapshot', async () => {
  const app = harness()
  await pauseForQuestion(app)
  const continuing = app.submitPendingAnswers()
  const request = app.requests.at(-1)
  assert.equal(request.transport, 'fetch')
  assert.deepEqual(plain(request.callbacks.body), { chatId: 'chat-1', answers: { q_1: 'PDF' } })
  request.callbacks.onTodo(JSON.stringify(todo('completed')))
  const currentStatus = app.pinnedTodo.value.items[0].status
  request.callbacks.onMessage('Final Answer: 报告已完成')
  request.callbacks.onDone('[DONE]')
  await continuing

  assert.equal(currentStatus, 'completed')
  assert.equal(app.messages.value.find((message) => message.todoSnapshot).todoSnapshot.items[0].status, 'completed')
  assert.equal(app.pendingQuestions.value.length, 0)
  assert.equal(app.sending.value, false)
  assert.equal(app.timers.size, 0)
})

for (const action of ['newChat', 'switchChat']) {
  test(`${action} clears previous questions and prevents submitting them to the replacement chat`, async () => {
    const app = harness()
    await pauseForQuestion(app)
    app.selectedChatId.value = 'chat-2'
    app[action]()

    assert.equal(app.store.currentChatId, action === 'newChat' ? 'new-chat' : 'chat-2')
    assert.equal(app.pendingQuestions.value.length, 0)
    assert.deepEqual(plain(app.pendingAnswers.value), {})
    assert.equal(app.pendingQuestionMessageId.value, '')
    assert.equal(app.pinnedTodo.value, null)
    await app.submitPendingAnswers()
    assert.equal(app.requests.length, 1)
  })

  test(`${action} does not change the session while a response is active`, async () => {
    const app = harness()
    app.input.value = '正在执行的任务'
    const sending = app.send()
    app.selectedChatId.value = 'chat-2'
    app[action]()
    const currentChatId = app.store.currentChatId
    app.requests[0].callbacks.onDone('[DONE]')
    await sending

    assert.equal(currentChatId, 'chat-1')
    assert.equal(app.sending.value, false)
    assert.equal(app.timers.size, 0)
  })
}

test('failed continuation preserves answers for a repeat submission to the same chat', async () => {
  const app = harness()
  await pauseForQuestion(app)
  const firstAttempt = app.submitPendingAnswers()
  app.requests.at(-1).callbacks.onError(new Error('fixture failure'))
  await firstAttempt

  assert.equal(app.pendingQuestions.value.length, 1)
  assert.equal(app.pendingAnswers.value.q_1, 'PDF')
  assert.equal(app.sending.value, false)
  const secondAttempt = app.submitPendingAnswers()
  assert.deepEqual(plain(app.requests.at(-1).callbacks.body), { chatId: 'chat-1', answers: { q_1: 'PDF' } })
  app.requests.at(-1).callbacks.onTodo(JSON.stringify(todo('completed')))
  app.requests.at(-1).callbacks.onMessage('Final Answer: 重试成功')
  app.requests.at(-1).callbacks.onDone('[DONE]')
  await secondAttempt

  assert.equal(app.requests.length, 3)
  assert.equal(app.pendingQuestions.value.length, 0)
  assert.equal(app.messages.value.find((message) => message.todoSnapshot).todoSnapshot.items[0].status, 'completed')
  assert.equal(app.timers.size, 0)
})
