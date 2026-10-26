import type { Role } from './auth'
export interface AccountDetails { id: number; fullName: string; phone: string; role: Role; status: 'ACTIVE' | 'LOCKED' }
export interface CreateAccount { fullName: string; phone: string; password: string; role: Role }
