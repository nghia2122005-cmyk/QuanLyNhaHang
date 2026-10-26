import { api } from './client'
import type { CreateReconciliationPayload, Reconciliation, RevenueSummary } from '../types/reconciliation'
import type { PageResponse } from '../types/tables'

export const reconciliationApi = {
  preview: async (params: { from: string; to: string }, signal?: AbortSignal) =>
    (await api.get<RevenueSummary>('/reconciliations/preview', { params, signal })).data,
  create: async (payload: CreateReconciliationPayload) =>
    (await api.post<Reconciliation>('/reconciliations', payload)).data,
  list: async (params: { confirmed?: boolean; page: number; size: number }, signal?: AbortSignal) =>
    (await api.get<PageResponse<Reconciliation>>('/reconciliations', { params, signal })).data,
  confirm: async (id: number) =>
    (await api.patch<Reconciliation>(`/reconciliations/${id}/confirm`)).data,
}
