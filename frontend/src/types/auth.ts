export type Role = 'OWNER' | 'MANAGER' | 'CASHIER' | 'WAITER' | 'KITCHEN_STAFF' | 'HEAD_CHEF'

export interface Account {
  accountId: number
  fullName: string
  role: Role
}

export interface LoginRequest {
  phone: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  expiresAt: string
  account: Account
}

export interface ApiError {
  status: number
  code: string
  message: string
  fieldErrors: Record<string, string>
}
