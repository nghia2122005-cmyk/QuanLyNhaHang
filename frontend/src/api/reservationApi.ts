import { api } from './client'
import type { Reservation, ReservationFilters, PageResponse, AvailableTable, CreateReservation } from '../types/tables'
export const reservationApi = {
  list: async (params: ReservationFilters, signal?: AbortSignal) => (await api.get<PageResponse<Reservation>>('/reservations', { params, signal })).data,
  available: async (params: { startTime: string; endTime: string; guestCount: number; excludedId?: number }, signal?: AbortSignal) => (await api.get<AvailableTable[]>('/reservations/available-tables', { params, signal })).data,
  create: async (request: CreateReservation) => (await api.post<Reservation>('/reservations', request)).data,
  update: async (id: number, request: CreateReservation) => (await api.put<Reservation>(`/reservations/${id}`, request)).data,
  cancel: async (id: number) => (await api.patch<Reservation>(`/reservations/${id}/cancel`)).data,
}
