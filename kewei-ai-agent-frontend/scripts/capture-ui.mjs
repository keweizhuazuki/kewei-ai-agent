import { chromium } from 'playwright'
import { spawn } from 'node:child_process'
import { mkdir } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { setTimeout } from 'node:timers/promises'

const frontend = fileURLToPath(new URL('..', import.meta.url))
const output = fileURLToPath(new URL('../../docs/images/', import.meta.url))
const baseUrl = 'http://127.0.0.1:4173'
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', 'preview', '--host', '127.0.0.1', '--port', '4173', '--strictPort'], {
  cwd: frontend,
  stdio: 'inherit',
})
let startupError
server.on('error', (error) => { startupError = error })
let browser

try {
  let ready = false
  for (let attempt = 0; attempt < 100; attempt++) {
    if (startupError) throw startupError
    if (server.exitCode !== null) throw new Error(`Preview server exited: ${server.exitCode}`)
    try {
      ready = (await fetch(baseUrl)).ok
    } catch {
      // Wait for the local preview server; no backend or model is started.
    }
    if (ready) break
    await setTimeout(100)
  }
  if (!ready) throw new Error('Preview server did not become ready')

  await mkdir(output, { recursive: true })
  browser = await chromium.launch()
  for (const [name, width, height, mobile] of [
    ['desktop-preview', 1440, 1000, false],
    ['mobile-preview', 390, 844, true],
  ]) {
    const context = await browser.newContext({
      viewport: { width, height },
      isMobile: mobile,
      hasTouch: mobile,
      deviceScaleFactor: 1,
      reducedMotion: 'reduce',
    })
    try {
      const page = await context.newPage()
      await page.goto(baseUrl, { waitUntil: 'networkidle' })
      await page.locator('h1').first().waitFor()
      await page.waitForTimeout(500)
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)
      if (overflow) throw new Error(`${name}: horizontal overflow`)
      await page.screenshot({ path: `${output}${name}.png`, animations: 'disabled' })
    } finally {
      await context.close()
    }
  }
} finally {
  try {
    await browser?.close()
  } finally {
    server.kill()
  }
}
