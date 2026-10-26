import { api } from './client'
import type { AccountDetails, CreateAccount } from '../types/accounts'
import type { Role } from '../types/auth'
export const accountApi = {
  list: async (signal?: AbortSignal) => (await api.get<AccountDetails[]>('/accounts', { signal })).data,
  create: async (request: CreateAccount) => (await api.post<AccountDetails>('/accounts', request)).data,
  lock: async (id: number) => (await api.patch<AccountDetails>(`/accounts/${id}/lock`)).data,
  unlock: async (id: number) => (await api.patch<AccountDetails>(`/accounts/${id}/unlock`)).data,
  role: async (id: number, role: Role) => (await api.patch<AccountDetails>(`/accounts/${id}/role`, { role })).data,
}
