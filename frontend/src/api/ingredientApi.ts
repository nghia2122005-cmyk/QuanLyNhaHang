import { api } from './client'
import type { IngredientOption } from '../types/menu'
export const ingredientApi = {
  list: async (signal?: AbortSignal) => (await api.get<IngredientOption[]>('/inventory/ingredients', { params: { status: 'ACTIVE' }, signal })).data,
}
