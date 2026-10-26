import { createContext } from 'react'
import type { Account, LoginRequest } from '../types/auth'

export interface AuthState {
  account: Account | null
  loading: boolean
  login: (request: LoginRequest) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthState | null>(null)
