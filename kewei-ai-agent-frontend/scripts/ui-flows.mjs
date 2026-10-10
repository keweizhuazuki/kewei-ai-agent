import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdir } from 'node:fs/promises'
import { setTimeout as delay } from 'node:timers/promises'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright'

// Real browser transports, intercepted fixture responses only. No backend, model or secrets.
const frontend = fileURLToPath(new URL('..', import.meta.url))
const output = fileURLToPath(new URL('../../docs/images/', import.meta.url))
const baseUrl = 'http://127.0.0.1:4174'
const capture = process.argv.includes('--screenshots')
const promptPlaceholder = '有问题尽管问。支持图片 + 文本一起发送（Enter 发送，Shift+Enter 换行）'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', 'preview', '--host', '127.0.0.1', '--port', '4174', '--strictPort'], {
  cwd: frontend,
  stdio: 'inherit',
})
let startupError
server.on('error', (error) => { startupError = error })
let browser
const watchdog = setTimeout(() => {
  server.kill('SIGKILL')
  console.error('Mocked UI flows exceeded the 120-second limit')
  process.exit(1)
}, 120000)
watchdog.unref()

function deferred() {
  let resolve
  const promise = new Promise((done) => { resolve = done })
  return { promise, resolve }
}

async function bounded(promise) {
  let timer
  try {
    return await Promise.race([
      promise,
      new Promise((_, reject) => { timer = setTimeout(() => reject(new Error('Fixture request did not arrive')), 10000) }),
    ])
  } finally {
    clearTimeout(timer)
  }
}

const event = (name, data) => `event:${name}\ndata:${typeof data === 'string' ? data : JSON.stringify(data)}\n\n`
const todo = (status) => ({ todo: { items: [{ id: 'build', content: '制作报告', status }] } })
const question = { questions: [{ id: 'q_1', header: '格式', question: '选择报告格式？', multiSelect: false, options: [{ label: 'PDF', description: '阅读版' }, { label: 'PPT', description: '演示版' }] }] }
const completed = (text) => event('todo', todo('completed')) + event('message', `Step 2 result: Final Answer: ${text}`) + event('done', '[DONE]')

async function createFixture(viewport, scenario) {
  const context = await browser.newContext({
    viewport: { width: viewport.width, height: viewport.height },
    isMobile: viewport.mobile,
    hasTouch: viewport.mobile,
    deviceScaleFactor: 1,
    reducedMotion: 'reduce',
    serviceWorkers: 'block',
  })
  await context.addInitScript(() => {
    localStorage.setItem('kewei.ai.chat.ids', JSON.stringify(['fixture-chat', 'other-chat']))
    localStorage.setItem('kewei.ai.chat.current', 'fixture-chat')
  })
  const getCalls = []
  const postCalls = []
  const errors = []
  const firstRequest = deferred()
  const releaseInitial = deferred()
  if (scenario !== 'continue') releaseInitial.resolve()
  const cors = {
    'access-control-allow-origin': baseUrl,
    'access-control-allow-methods': 'GET, POST, OPTIONS',
    'access-control-allow-headers': 'content-type',
  }

  await context.route('**/*', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    try {
      if (url.pathname.startsWith('/api/ai/')) {
        if (request.method() === 'OPTIONS') {
          await route.fulfill({ status: 204, headers: cors, body: '' })
          return
        }
        let body
        if (request.method() === 'GET' && url.pathname === '/api/ai/manus/chat') {
          getCalls.push(Object.fromEntries(url.searchParams))
          firstRequest.resolve()
          if (getCalls.length === 1) {
            await releaseInitial.promise
            body = scenario === 'retry'
              ? event('message', 'Step 1 result: 开始处理') + event('error', 'fixture failure')
              : event('todo', todo('in_progress')) + event('question', question)
          } else {
            body = completed(scenario === 'retry' ? '重试成功' : '新会话任务已完成')
          }
        } else if (request.method() === 'POST' && url.pathname === '/api/ai/manus/chat/continue' && scenario === 'continue') {
          const payload = request.postDataJSON()
          postCalls.push(payload)
          assert.deepEqual(payload, { chatId: 'fixture-chat', answers: { q_1: 'PDF' } })
          // First continuation fails, so the UI must preserve the selected answer for retry.
          body = postCalls.length === 1 ? event('error', 'fixture continuation failure') : completed('报告已完成')
        } else {
          throw new Error(`Unexpected API request: ${request.method()} ${url.pathname}`)
        }
        await route.fulfill({ status: 200, headers: { ...cors, 'content-type': 'text/event-stream', 'cache-control': 'no-cache' }, body })
        return
      }
      if (url.origin === baseUrl && request.method() === 'GET') {
        await route.continue()
        return
      }
      throw new Error(`Blocked unexpected network request: ${request.method()} ${url.origin}${url.pathname}`)
    } catch (error) {
      errors.push(error.message)
      await route.abort().catch(() => {})
    }
  })
  const page = await context.newPage()
  page.setDefaultTimeout(10000)
  page.setDefaultNavigationTimeout(15000)
  page.on('pageerror', (error) => errors.push(error.message))
  await page.goto(`${baseUrl}/chat?app=manus`, { waitUntil: 'domcontentloaded' })
  await page.getByRole('heading', { name: '会话中心', exact: true }).waitFor()
  assert.equal(await page.locator('.mode-box select').inputValue(), 'sse')
  assert.equal(await page.locator('.mode-box select option:checked').innerText(), '流式 sse（Manus）')
  return { context, page, getCalls, postCalls, errors, firstRequest, releaseInitial }
}

async function send(page, text) {
  await page.getByPlaceholder(promptPlaceholder).fill(text)
  await page.getByRole('button', { name: '发送', exact: true }).click()
}

async function ready(page) {
  await page.waitForFunction(() => document.querySelector('.send-bar > span')?.textContent === 'Ready')
}

async function questionPause(page) {
  await page.locator('.question-panel').waitFor()
  await ready(page)
}

async function continueFlow(fixture, viewport) {
  const { page } = fixture
  await send(page, '帮我制作一份报告')
  await bounded(fixture.firstRequest.promise)
  assert.equal(await page.getByRole('button', { name: '新建 chatId', exact: true }).isDisabled(), true)
  assert.equal(await page.locator('.top-actions select').isDisabled(), true)
  assert.equal(await page.getByRole('button', { name: /^LoveApp/ }).isDisabled(), true)
  assert.equal(await page.getByRole('button', { name: /^Manus/ }).isDisabled(), true)
  fixture.releaseInitial.resolve()
  await questionPause(page)
  const continueButton = page.getByRole('button', { name: '继续执行', exact: true })
  assert.equal(await continueButton.isDisabled(), true)
  await page.locator('#q_1').selectOption('PDF')
  await continueButton.click()
  await page.locator('.chat-message.is-assistant .message-meta').filter({ hasText: '失败' }).waitFor()
  await questionPause(page)
  assert.equal(await page.locator('#q_1').inputValue(), 'PDF')
  await continueButton.click()
  await page.getByText('报告已完成', { exact: true }).waitFor()
  await ready(page)
  assert.equal(fixture.getCalls.length, 1)
  assert.equal(fixture.postCalls.length, 2)
  assert.equal(await page.locator('.question-panel').count(), 0)
  assert.equal(await page.locator('.todo-pin').count(), 0)
  assert.equal(await page.locator('.todo-card__item.is-completed').count(), 1)
  assert.match(await page.locator('.todo-card__header').innerText(), /1\/1 已完成/)
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)
  assert.equal(overflow, false, `${viewport.name}: chat horizontal overflow`)
  if (capture) {
    await page.evaluate(() => new Promise((resolve) => {
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
      requestAnimationFrame(() => requestAnimationFrame(resolve))
    }))
    assert.equal(await page.evaluate(() => window.scrollY), 0)
    await page.screenshot({ path: `${output}chat-${viewport.name}-preview.png`, fullPage: true, animations: 'disabled' })
  }
}

async function switchFlow(fixture, action) {
  const { page } = fixture
  await send(page, '原会话任务')
  await questionPause(page)
  await page.locator('#q_1').selectOption('PDF')
  if (action === 'new') await page.getByRole('button', { name: '新建 chatId', exact: true }).click()
  else await page.locator('.top-actions select').selectOption('other-chat')
  await page.locator('.question-panel').waitFor({ state: 'hidden' })
  assert.equal(await page.getByPlaceholder(promptPlaceholder).isEnabled(), true)
  const replacementId = await page.locator('.top-actions select').inputValue()
  assert.notEqual(replacementId, 'fixture-chat')
  if (action === 'switch') assert.equal(replacementId, 'other-chat')
  await send(page, '新会话任务')
  await page.getByText('新会话任务已完成', { exact: true }).waitFor()
  await ready(page)
  assert.equal(fixture.getCalls.length, 2)
  assert.equal(fixture.getCalls[1].chatId, replacementId)
  assert.equal(fixture.postCalls.length, 0)
}

async function retryFlow(fixture) {
  const { page } = fixture
  await send(page, '失败后重试任务')
  const retry = page.getByRole('button', { name: '重试', exact: true })
  await retry.waitFor()
  await ready(page)
  await retry.click()
  await page.getByText('重试成功', { exact: true }).waitFor()
  await ready(page)
  assert.equal(fixture.getCalls.length, 2)
  assert.deepEqual(fixture.getCalls[0], fixture.getCalls[1])
  assert.equal(fixture.postCalls.length, 0)
}

async function switchAppFlow(fixture) {
  const { page } = fixture
  await send(page, '切换应用前的任务')
  await questionPause(page)
  await page.locator('#q_1').selectOption('PDF')
  await page.getByRole('button', { name: /^Manus/ }).click()
  assert.equal(await page.locator('#q_1').inputValue(), 'PDF')
  await page.getByRole('button', { name: /^LoveApp/ }).click()
  await page.locator('.question-panel').waitFor({ state: 'hidden' })
  assert.equal(await page.locator('.todo-pin').count(), 0)
  assert.equal(await page.getByPlaceholder(promptPlaceholder).isEnabled(), true)
  await page.locator('.mode-box select').selectOption('sync')
  await page.getByRole('button', { name: /^Manus/ }).click()
  assert.equal(await page.locator('.mode-box select').inputValue(), 'sse')
  assert.equal(await page.locator('.question-panel').count(), 0)
  assert.equal(fixture.getCalls.length, 1)
  assert.equal(fixture.postCalls.length, 0)
}

try {
  let serverReady = false
  for (let attempt = 0; attempt < 100; attempt++) {
    if (startupError) throw startupError
    if (server.exitCode !== null) throw new Error(`Preview server exited: ${server.exitCode}`)
    try {
      serverReady = (await fetch(baseUrl, { signal: AbortSignal.timeout(1000) })).ok
    } catch {
      // Wait only for the local static preview server.
    }
    if (serverReady) break
    await delay(100)
  }
  if (!serverReady) throw new Error('Preview server did not become ready')
  if (capture) await mkdir(output, { recursive: true })
  browser = await chromium.launch({ timeout: 20000 })
  for (const viewport of [
    { name: 'desktop', width: 1440, height: 1000, mobile: false },
    { name: 'mobile', width: 390, height: 844, mobile: true },
  ]) {
    for (const scenario of ['continue', 'new', 'switch', 'retry', 'app']) {
      const fixture = await createFixture(viewport, scenario)
      try {
        if (scenario === 'continue') await continueFlow(fixture, viewport)
        else if (scenario === 'retry') await retryFlow(fixture)
        else if (scenario === 'app') await switchAppFlow(fixture)
        else await switchFlow(fixture, scenario)
        assert.deepEqual(fixture.errors, [])
        console.log(`PASS ${viewport.name}: ${scenario}`)
      } finally {
        fixture.releaseInitial.resolve()
        await fixture.context.close()
      }
    }
  }
} finally {
  try {
    await browser?.close()
  } finally {
    server.kill()
    clearTimeout(watchdog)
  }
}
