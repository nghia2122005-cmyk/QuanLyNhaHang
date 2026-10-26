import { api } from './client'
import type {
  CreateIngredientPayload,
  CreateStockDisposalPayload,
  CreateStockReceiptPayload,
  CreateStocktakePayload,
  Ingredient,
  IngredientLotAttention,
  IngredientLot,
  IngredientStatus,
  MovementFilter,
  StockMovement,
  StockReceipt,
  Stocktake,
  StocktakeTemplateItem,
  UpdateIngredientPayload,
} from '../types/inventory'
import type { PageResponse } from '../types/tables'

export const inventoryApi = {
  listIngredients: async (
    params?: { q?: string; status?: IngredientStatus; belowMinStockOnly?: boolean },
    signal?: AbortSignal
  ) =>
    (
      await api.get<Ingredient[]>('/inventory/ingredients', {
        params,
        signal,
      })
    ).data,

  createIngredient: async (payload: CreateIngredientPayload) =>
    (await api.post<Ingredient>('/inventory/ingredients', payload)).data,

  updateIngredient: async (id: number, payload: UpdateIngredientPayload) =>
    (await api.put<Ingredient>(`/inventory/ingredients/${id}`, payload)).data,

  deactivateIngredient: async (id: number) =>
    (await api.patch<Ingredient>(`/inventory/ingredients/${id}/deactivate`)).data,

  activateIngredient: async (id: number) =>
    (await api.patch<Ingredient>(`/inventory/ingredients/${id}/activate`)).data,

  listAttentionLots: async (days?: number, signal?: AbortSignal) =>
    (
      await api.get<IngredientLotAttention[]>('/inventory/lots/attention', {
        params: days != null ? { days } : undefined,
        signal,
      })
    ).data,

  listIngredientLots: async (ingredientId: number, days?: number, signal?: AbortSignal) =>
    (await api.get<IngredientLot[]>(`/inventory/ingredients/${ingredientId}/lots`, {
      params: days != null ? { days } : undefined, signal,
    })).data,

  createReceipt: async (payload: CreateStockReceiptPayload) =>
    (await api.post<StockReceipt>('/inventory/receipts', payload)).data,

  listReceipts: async (
    params?: { from?: string; to?: string; page?: number; size?: number },
    signal?: AbortSignal
  ) =>
    (
      await api.get<PageResponse<StockReceipt>>('/inventory/receipts', {
        params,
        signal,
      })
    ).data,

  getStocktakeTemplate: async (signal?: AbortSignal) =>
    (await api.get<StocktakeTemplateItem[]>('/inventory/stocktakes/template', { signal })).data,

  createStocktake: async (payload: CreateStocktakePayload) =>
    (await api.post<Stocktake>('/inventory/stocktakes', payload)).data,

  listStocktakes: async (
    params?: { from?: string; to?: string; page?: number; size?: number },
    signal?: AbortSignal
  ) =>
    (
      await api.get<PageResponse<Stocktake>>('/inventory/stocktakes', {
        params,
        signal,
      })
    ).data,

  createDisposal: async (payload: CreateStockDisposalPayload) =>
    (await api.post<StockMovement>('/inventory/disposals', payload)).data,

  listMovements: async (params?: MovementFilter, signal?: AbortSignal) =>
    (
      await api.get<PageResponse<StockMovement>>('/inventory/movements', {
        params,
        signal,
      })
    ).data,
}
