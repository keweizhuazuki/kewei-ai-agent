import { http } from '../http'

export function loveChatSync(params) {
  return http.get('/ai/love_app/chat/sync', {
    params,
    timeout: 180000,
  })
}

export function uploadLoveImage(formData) {
  return http.post('/ai/love_app/image/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    timeout: 120000,
  })
}
