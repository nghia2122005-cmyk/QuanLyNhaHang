import { fetchEventSource } from '@microsoft/fetch-event-source'
import { useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useAuth } from '../auth/useAuth'
import { clearSession, readSession } from '../auth/session'
import { refreshLiveQueries } from './invalidation'
import type { RealtimeEvent } from './invalidation'

class FatalStreamError extends Error {}

export function useRealtime() {
  const { account } = useAuth()
  const cache = useQueryClient()
  const token = account ? readSession()?.accessToken : undefined
  useEffect(() => {
    if (!token) return
    const controller = new AbortController()
    const isCurrent = () => !controller.signal.aborted && readSession()?.accessToken === token
    void fetchEventSource('/api/events', {
      headers: { Authorization: `Bearer ${token}` }, signal: controller.signal, openWhenHidden: true,
      async onopen(response) {
        if (!isCurrent()) { controller.abort(); return }
        if (response.status >= 400 && response.status < 500) {
          if (response.status === 401) {
            clearSession()
            window.dispatchEvent(new Event('auth:expired'))
          } else if (response.status === 403) window.dispatchEvent(new Event('api:forbidden'))
          throw new FatalStreamError()
        }
        if (!response.ok || !response.headers.get('content-type')?.includes('text/event-stream')) throw new Error('Stream unavailable')
        // No event replay: refetch on every open, including reconnect.
        void refreshLiveQueries(cache)
      },
      onmessage(message) {
        if (!isCurrent() || !message.data) return
        try {
          const event: RealtimeEvent = JSON.parse(message.data)
          if (['ORDER', 'RESERVATION', 'FLOOR_PLAN'].includes(event.type)) void refreshLiveQueries(cache, event)
        } catch { /* Ignore malformed signals; REST and polling remain the data source. */ }
      },
      onclose() { throw new Error('Stream closed') },
      onerror(error) {
        if (error instanceof FatalStreamError || !isCurrent()) throw error
        return 2_000
      },
    }).catch(() => { /* Polling continues when the stream is unavailable. */ })
    return () => controller.abort()
  }, [token, cache])
}
