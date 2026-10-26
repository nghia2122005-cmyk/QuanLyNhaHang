import { api } from './client'
import type { CookingStartResponse, OrderItem, OrderQueueItem } from '../types/sessions'

export const kitchenApi = {
  queue: async (signal?: AbortSignal) => (await api.get<OrderQueueItem[]>('/kitchen/queue', { signal })).data,
  batch: async (id: number, signal?: AbortSignal) => (await api.get<OrderQueueItem[]>(`/kitchen/orders/${id}`, { signal })).data,
  start: async (id: number, confirmLowStock = false) => (await api.patch<CookingStartResponse>(`/kitchen/items/${id}/start`, { confirmLowStock })).data,
  complete: async (id: number) => (await api.patch<OrderItem>(`/kitchen/items/${id}/complete`)).data,
  unavailable: async (id: number, reason: string) => (await api.patch<OrderItem>(`/kitchen/items/${id}/unavailable`, { reason })).data,
}
