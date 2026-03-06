import axios from 'axios'
import { API_BASE_URL } from './constants'

const friendlyError = {
  timeout: '请求超时（后端处理较慢），建议重试或切换 SSE 流式模式',
  network: '网络异常，请检查后端服务是否启动',
  server: '服务异常，请稍后再试',
}

function normalizePayload(raw) {
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw)
    } catch {
      return raw
    }
  }
  return raw
}

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 120000,
})

http.interceptors.request.use((config) => {
  config.metadata = { start: Date.now() }
  return config
})

http.interceptors.response.use(
  (response) => {
    const parsed = normalizePayload(response.data)
    const elapsed = Date.now() - (response.config.metadata?.start || Date.now())

    if (parsed && typeof parsed === 'object' && 'code' in parsed) {
      if (parsed.code !== 0) {
        const error = new Error(parsed.message || '请求失败')
        error.payload = parsed
        error.elapsed = elapsed
        throw error
      }
      return {
        data: parsed.data,
        message: parsed.message,
        elapsed,
      }
    }

    return {
      data: parsed,
      message: 'ok',
      elapsed,
    }
  },
  (error) => {
    if (error.code === 'ECONNABORTED') {
      error.message = friendlyError.timeout
    } else if (!error.response) {
      error.message = friendlyError.network
    } else {
      error.message = error.response?.data?.message || friendlyError.server
    }
    return Promise.reject(error)
  },
)
