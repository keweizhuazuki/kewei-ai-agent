import { http } from '../http'

export function getHealth() {
  return http.get('/health')
}
