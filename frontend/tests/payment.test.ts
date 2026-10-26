import { expect, test } from 'bun:test'
import { statusColors, statusLabel } from '../src/utils/labels'
import { api } from '../src/api/client'
import { paymentApi } from '../src/api/paymentApi'

test('PaymentMethod labels and colors match design system', () => {
  expect(statusLabel('PaymentMethod', 'CASH')).toBe('Tiền mặt')
  expect(statusLabel('PaymentMethod', 'BANK_TRANSFER')).toBe('Chuyển khoản')

  expect(statusColors['CASH']).toBe('green')
  expect(statusColors['BANK_TRANSFER']).toBe('blue')
})

test('Payment API preserves server eligibility for READY and sends no client total', async () => {
  const original = api.defaults.adapter
  const requests: unknown[] = []
  api.defaults.adapter = async (config) => {
    requests.push(config.data ? JSON.parse(config.data) : config.url)
    return { data: { hasBlockingItems: true, blockingItems: [{ id: 1, status: 'READY' }] }, status: 200, statusText: 'OK', headers: {}, config }
  }
  try {
    const preview = await paymentApi.getPreview(7)
    expect(preview.hasBlockingItems).toBe(true)
    expect(preview.blockingItems[0].status).toBe('READY')
    await paymentApi.pay({ sessionId: 7, paymentMethod: 'CASH' })
    expect(requests[1]).toEqual({ sessionId: 7, paymentMethod: 'CASH' })
  } finally { api.defaults.adapter = original }
})

test('Billable items calculation excludes cancelled and unavailable', () => {
  const items = [
    { unitPrice: 50000, quantity: 2, status: 'READY' },
    { unitPrice: 40000, quantity: 1, status: 'SERVED' },
    { unitPrice: 60000, quantity: 3, status: 'CANCELLED' },
    { unitPrice: 45000, quantity: 2, status: 'UNAVAILABLE' },
  ]

  const total = items.reduce((sum, item) => {
    if (item.status === 'CANCELLED' || item.status === 'UNAVAILABLE') return sum
    return sum + item.unitPrice * item.quantity
  }, 0)

  expect(total).toBe(140000)
})
