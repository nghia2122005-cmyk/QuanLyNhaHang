import { expect, test } from 'bun:test'
import { groupKitchenQueue } from '../src/utils/kitchenQueue'
import type { OrderQueueItem } from '../src/types/sessions'

const line = (id: number, orderId: number, status: OrderQueueItem['status'], quantity = 1, orderedAt = '2026-10-25T10:00:00Z'): OrderQueueItem => ({
  id, orderId, sessionId: 1, tableId: 1, tableName: 'T01', tableCode: 'T01-B01', menuItemName: `Món ${id}`,
  quantity, status, orderedAt, note: null, unavailableReason: null,
})

test('Mixed batches appear only in cooking and counts represent lines, not portions', () => {
  const result = groupKitchenQueue([line(1, 30, 'PENDING', 50), line(2, 30, 'COOKING', 99), line(3, 31, 'PENDING')])
  expect(result.pending.map((batch) => batch.orderId)).toEqual([31])
  expect(result.cooking.map((batch) => batch.orderId)).toEqual([30])
  expect(result.cooking[0].pendingCount).toBe(1)
  expect(result.cooking[0].cookingCount).toBe(1)
  expect(result.cooking[0].items.map((item) => item.quantity)).toEqual([50, 99])
})

test('Queue excludes terminal lines and preserves orderId identity with stable FIFO sorting', () => {
  const result = groupKitchenQueue([
    line(1, 40, 'PENDING'), line(2, 30, 'PENDING'),
    line(3, 50, 'PENDING', 1, '2026-10-25T09:00:00Z'),
    line(4, 60, 'READY'), line(5, 60, 'SERVED'), line(6, 60, 'UNAVAILABLE'), line(7, 60, 'CANCELLED'),
  ])
  expect(result.pending.map((batch) => batch.orderId)).toEqual([50, 30, 40])
  expect(result.cooking).toEqual([])
  expect(groupKitchenQueue([])).toEqual({ pending: [], cooking: [] })
})

test('Batch moves between columns as individual items start or finish', () => {
  const items = [line(1, 1, 'PENDING'), line(2, 1, 'PENDING')]
  expect(groupKitchenQueue(items).pending).toHaveLength(1)
  items[0].status = 'COOKING'
  expect(groupKitchenQueue(items).cooking).toHaveLength(1)
  items[0].status = 'READY'
  expect(groupKitchenQueue(items).pending).toHaveLength(1)
  items[1].status = 'UNAVAILABLE'
  expect(groupKitchenQueue(items)).toEqual({ pending: [], cooking: [] })
})
