import axios from 'axios'
import { clearSession, readSession } from '../auth/session'

export const api = axios.create({
  baseURL: '/api',
  timeout: 15_000,
})

api.interceptors.request.use((config) => {
  if (config.url !== '/auth/login') {
    const session = readSession()
    if (session) config.headers.set('Authorization', `Bearer ${session.accessToken}`)
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.config?.url !== '/auth/login') {
      if (error.response?.status === 401) {
        const current = readSession()
        const rejectedToken = error.config?.headers.get('Authorization')
        if (current && rejectedToken === `Bearer ${current.accessToken}`) {
          clearSession()
          window.dispatchEvent(new Event('auth:expired'))
        }
      } else if (error.response?.status === 403) {
        window.dispatchEvent(new Event('api:forbidden'))
      }
    }
    return Promise.reject(error)
  },
)
