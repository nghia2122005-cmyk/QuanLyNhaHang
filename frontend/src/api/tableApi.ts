import { api } from './client'
import type { FloorArea, OpenSession, ManagedArea, ManagedTable, SaveArea, SaveTable } from '../types/tables'
export const tableApi = {
  floorPlan: async (signal?: AbortSignal) => (await api.get<FloorArea[]>('/tables/floor-plan', { signal })).data,
  open: async (tableId: number, reservationId?: number) => (await api.post<OpenSession>('/sessions', { tableId, reservationId })).data,
  areas: async (signal?: AbortSignal) => (await api.get<ManagedArea[]>('/areas', { signal })).data,
  tables: async (signal?: AbortSignal) => (await api.get<ManagedTable[]>('/tables', { signal })).data,
  createArea: async (payload: SaveArea) => (await api.post<ManagedArea>('/areas', payload)).data,
  updateArea: async (id: number, payload: SaveArea) => (await api.put<ManagedArea>(`/areas/${id}`, payload)).data,
  changeAreaStatus: async (id: number, active: boolean) => (await api.patch<ManagedArea>(`/areas/${id}/${active ? 'reactivate' : 'deactivate'}`)).data,
  createTable: async (payload: SaveTable) => (await api.post<ManagedTable>('/tables', payload)).data,
  updateTable: async (id: number, payload: SaveTable) => (await api.put<ManagedTable>(`/tables/${id}`, payload)).data,
  changeTableStatus: async (id: number, active: boolean) => (await api.patch<ManagedTable>(`/tables/${id}/${active ? 'reactivate' : 'deactivate'}`)).data,
}
