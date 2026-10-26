import type { QueryClient, QueryKey } from '@tanstack/react-query'

export interface RealtimeEvent { type: 'FLOOR_PLAN' | 'RESERVATION' | 'ORDER'; sessionId?: number }
export const liveQueryOptions = { refetchInterval: 30_000, refetchOnWindowFocus: true } as const

export function eventKeys(event: RealtimeEvent): QueryKey[] {
  const session: QueryKey[] = event.sessionId == null ? [] : [['session', Number(event.sessionId)]]
  switch (event.type) {
    case 'FLOOR_PLAN': return [['floor-plan'], ['payment'], ...session, ['awaiting-service'],
      ...(event.sessionId == null ? [['session'], ['reservations'], ['available-tables'], ['invoices']] : [])]
    case 'RESERVATION': return [['reservations'], ['floor-plan']]
    case 'ORDER': return [['kitchen'], ...session, ['awaiting-service'], ['payment'], ['inventory']]
    default: return []
  }
}

export function refreshLiveQueries(cache: QueryClient, event?: RealtimeEvent) {
  const keys: QueryKey[] = event ? eventKeys(event) : [['floor-plan'], ['reservations'], ['kitchen'], ['session'], ['awaiting-service'], ['payment'], ['inventory']]
  return Promise.all(keys.map((queryKey) => cache.invalidateQueries({ queryKey })))
}
