import { api } from './client'
import type { MenuItem, SaveMenuItem, RecipeLine, SaveRecipeLine } from '../types/menu'
export interface MenuFilters { status?: MenuItem['status']; q?: string }
export const menuApi = {
  list: async (params: MenuFilters, signal?: AbortSignal) => (await api.get<MenuItem[]>('/menu-items', { params, signal })).data,
  create: async (request: SaveMenuItem) => (await api.post<MenuItem>('/menu-items', request)).data,
  update: async (id: number, request: SaveMenuItem) => (await api.put<MenuItem>(`/menu-items/${id}`, request)).data,
  status: async (id: number, status: MenuItem['status']) => (await api.patch<MenuItem>(`/menu-items/${id}/${status === 'ON_SALE' ? 'reactivate' : 'discontinue'}`)).data,
  recipe: async (id: number, signal?: AbortSignal) => (await api.get<RecipeLine[]>(`/menu-items/${id}/recipe`, { signal })).data,
  saveRecipe: async (id: number, lines: SaveRecipeLine[]) => (await api.put<RecipeLine[]>(`/menu-items/${id}/recipe`, { lines })).data,
}
