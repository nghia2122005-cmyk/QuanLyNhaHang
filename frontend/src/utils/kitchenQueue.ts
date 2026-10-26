import type { OrderQueueItem } from '../types/sessions'

export interface KitchenBatch {
  orderId: number
  items: OrderQueueItem[]
  pendingCount: number
  cookingCount: number
}

// Counts are order lines, never portions. A mixed batch belongs only to the cooking column.
export function groupKitchenQueue(items: OrderQueueItem[]) {
  const batches = new Map<number, KitchenBatch>()
  for (const item of items) {
    if (item.status !== 'PENDING' && item.status !== 'COOKING') continue
    let batch = batches.get(item.orderId)
    if (!batch) {
      batch = { orderId: item.orderId, items: [], pendingCount: 0, cookingCount: 0 }
      batches.set(item.orderId, batch)
    }
    batch.items.push(item)
    if (item.status === 'COOKING') batch.cookingCount++
    else batch.pendingCount++
  }
  const ordered = [...batches.values()].sort((a, b) =>
    Date.parse(a.items[0].orderedAt) - Date.parse(b.items[0].orderedAt) || a.orderId - b.orderId)
  return {
    pending: ordered.filter((batch) => batch.cookingCount === 0),
    cooking: ordered.filter((batch) => batch.cookingCount > 0),
  }
}
