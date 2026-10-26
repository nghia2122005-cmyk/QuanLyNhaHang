import { api } from './client'
import type { Account, LoginRequest, LoginResponse } from '../types/auth'

export const authApi = {
  async login(request: LoginRequest) {
    return (await api.post<LoginResponse>('/auth/login', request)).data
  },
  async me(signal?: AbortSignal) {
    return (await api.get<Account>('/auth/me', { signal })).data
  },
}
