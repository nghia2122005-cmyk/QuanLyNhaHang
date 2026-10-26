import { afterEach, beforeEach, expect, test } from 'bun:test'
import { AxiosError, AxiosHeaders } from 'axios'
import { clearSession, readSession, saveSession, SessionStorageError } from '../src/auth/session'
import { api } from '../src/api/client'
import { createQueryClient } from '../src/api/queryClient'

const originalStorage = Object.getOwnPropertyDescriptor(globalThis, 'localStorage')
const originalWindow = Object.getOwnPropertyDescriptor(globalThis, 'window')
const session = (token) => ({ accessToken: token, expiresAt: new Date(Date.now() + 60_000).toISOString() })

beforeEach(() => {
  const values = new Map()
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  } })
  Object.defineProperty(globalThis, 'window', { configurable: true, value: new EventTarget() })
})

afterEach(() => {
  for (const [key, descriptor] of [['localStorage', originalStorage], ['window', originalWindow]]) {
    if (descriptor) Object.defineProperty(globalThis, key, descriptor)
    else delete globalThis[key]
  }
})

test('blocked storage leaves the login page usable and reports a failed save', () => {
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, get() { throw new Error('SecurityError') } })
  expect(readSession()).toBeNull()
  expect(() => clearSession()).not.toThrow()
  expect(() => saveSession(session('token'))).toThrow(SessionStorageError)
})

test('malformed and expired sessions are discarded', () => {
  localStorage.setItem('restaurant.session', '{invalid')
  expect(readSession()).toBeNull()
  saveSession({ accessToken: 'old', expiresAt: '2000-01-01T00:00:00Z' })
  expect(readSession()).toBeNull()
  expect(localStorage.getItem('restaurant.session')).toBeNull()
})

function rejectWith(status, config) {
  throw new AxiosError('Rejected', 'ERR_BAD_REQUEST', config, null, {
    status, data: {}, headers: {}, statusText: 'Rejected', config,
  })
}

test('a stale 401 cannot erase a session created in another tab', async () => {
  saveSession(session('old'))
  let expired = 0
  window.addEventListener('auth:expired', () => expired++)
  await expect(api.get('/auth/me', { adapter: async (config) => {
    expect(config.headers.get('Authorization')).toBe('Bearer old')
    saveSession(session('new'))
    rejectWith(401, config)
  } })).rejects.toBeInstanceOf(AxiosError)
  expect(readSession()?.accessToken).toBe('new')
  expect(expired).toBe(0)
})

test('concurrent 401s for the current token emit only one expiry event', async () => {
  saveSession(session('current'))
  let expired = 0
  window.addEventListener('auth:expired', () => expired++)
  const adapter = async (config) => rejectWith(401, config)
  await Promise.allSettled([api.get('/one', { adapter }), api.get('/two', { adapter })])
  expect(readSession()).toBeNull()
  expect(expired).toBe(1)
})

test('login failures do not expire an existing session', async () => {
  saveSession(session('current'))
  await expect(api.post('/auth/login', {}, { adapter: async (config) => rejectWith(401, config) })).rejects.toBeInstanceOf(AxiosError)
  expect(readSession()?.accessToken).toBe('current')
})

for (const status of [401, 403, 409, 422]) {
  test(`queries do not retry HTTP ${status}`, async () => {
    const client = createQueryClient()
    let calls = 0
    try {
      await expect(client.fetchQuery({ queryKey: ['client-error'], queryFn: async () => {
        calls++
        rejectWith(status, { headers: new AxiosHeaders() })
      } })).rejects.toBeInstanceOf(AxiosError)
      expect(calls).toBe(1)
    } finally { client.clear() }
  })
}

test('transient server errors keep the bounded retry policy', async () => {
  const client = createQueryClient()
  let calls = 0
  try {
    await expect(client.fetchQuery({ queryKey: ['server-error'], retryDelay: 0, queryFn: async () => {
      calls++
      rejectWith(503, { headers: new AxiosHeaders() })
    } })).rejects.toBeInstanceOf(AxiosError)
    expect(calls).toBe(4)
  } finally { client.clear() }
})
