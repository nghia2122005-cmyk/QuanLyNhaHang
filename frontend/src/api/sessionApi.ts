import { api } from './client'
import type { FoodOrder, OrderItem, OrderLine, OrderQueueItem, SessionDetail } from '../types/sessions'

export const sessionApi = {
  detail: async (id: number, signal?: AbortSignal) => (await api.get<SessionDetail>(`/sessions/${id}`, { signal })).data,
  order: async (id: number, items: OrderLine[]) => (await api.post<FoodOrder>(`/sessions/${id}/orders`, { items })).data,
  cancel: async (id: number) => (await api.patch<OrderItem>(`/sessions/items/${id}/cancel`)).data,
  serve: async (id: number) => (await api.patch<OrderItem>(`/sessions/items/${id}/serve`)).data,
  acknowledgeUnavailable: async (id: number) => (await api.patch<OrderItem>(`/sessions/items/${id}/acknowledge-unavailable`)).data,
  awaitingService: async (signal?: AbortSignal) => (await api.get<OrderQueueItem[]>('/sessions/items/awaiting-service', { signal })).data,
}
