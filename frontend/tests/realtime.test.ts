import { expect, test } from 'bun:test'
import { createQueryClient } from '../src/api/queryClient'
import { eventKeys, liveQueryOptions, refreshLiveQueries } from '../src/realtime/invalidation'

test('event keys normalize session ids and retain payment compatibility', () => {
  expect(eventKeys({ type: 'ORDER', sessionId: Number('7') })).toEqual([['kitchen'], ['session', 7], ['awaiting-service'], ['payment'], ['inventory']])
  expect(eventKeys({ type: 'FLOOR_PLAN', sessionId: 7 })).toEqual([['floor-plan'], ['payment'], ['session', 7], ['awaiting-service']])
  expect(eventKeys({ type: 'RESERVATION' })).toEqual([['reservations'], ['floor-plan']])
})

test('reservation events invalidate filtered lists and table pickers by prefix', async () => {
  const cache = createQueryClient()
  try {
    const picker = ['reservations', { tableId: 5, unexpired: true }]
    const list = ['reservations', { page: 0 }]
    cache.setQueryData(picker, [])
    cache.setQueryData(list, [])
    cache.setQueryData(['session', 7], {})
    await refreshLiveQueries(cache, { type: 'RESERVATION' })
    expect(cache.getQueryState(picker)?.isInvalidated).toBe(true)
    expect(cache.getQueryState(list)?.isInvalidated).toBe(true)
    expect(cache.getQueryState(['session', 7])?.isInvalidated).toBe(false)
  } finally { cache.clear() }
})

test('reconnect invalidates every live prefix including inactive session detail', async () => {
  const cache = createQueryClient()
  try {
    const keys = [['floor-plan'], ['reservations', { tableId: 1 }], ['kitchen'], ['session', 7], ['awaiting-service'], ['payment']]
    keys.forEach((key) => cache.setQueryData(key, []))
    await refreshLiveQueries(cache)
    keys.forEach((key) => expect(cache.getQueryState(key)?.isInvalidated).toBe(true))
    expect(liveQueryOptions.refetchInterval).toBe(30_000)
    expect(liveQueryOptions.refetchOnWindowFocus).toBe(true)
  } finally { cache.clear() }
})

test('payment floor-plan signal invalidates every filtered awaiting-service list', async () => {
  const cache = createQueryClient()
  try {
    const key = ['awaiting-service', { tableId: 7 }]
    cache.setQueryData(key, [{ id: 1, status: 'READY' }])
    await refreshLiveQueries(cache, { type: 'FLOOR_PLAN', sessionId: 7 })
    expect(cache.getQueryState(key)?.isInvalidated).toBe(true)
  } finally { cache.clear() }
})

test('owner table metadata event refreshes management, pickers and historical labels', async () => {
  const cache = createQueryClient()
  try {
    const keys = [['floor-plan', 'management', 'tables'], ['floor-plan', 'management', 'areas'], ['available-tables', { guestCount: 4 }], ['reservations'], ['session', 7], ['invoices']]
    keys.forEach(key => cache.setQueryData(key, []))
    await refreshLiveQueries(cache, { type: 'FLOOR_PLAN' })
    keys.forEach(key => expect(cache.getQueryState(key)?.isInvalidated).toBe(true))
  } finally { cache.clear() }
})
