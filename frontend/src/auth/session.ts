const SESSION_KEY = 'restaurant.session'

export interface StoredSession {
  accessToken: string
  expiresAt: string
}

export class SessionStorageError extends Error {
  constructor() {
    super('Trình duyệt chưa cho phép lưu phiên đăng nhập. Vui lòng cho phép lưu dữ liệu trang web và thử lại.')
    this.name = 'SessionStorageError'
  }
}

export function readSession(): StoredSession | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    if (!raw) return null
    const value: unknown = JSON.parse(raw)
    if (
      typeof value !== 'object' || value === null ||
      !('accessToken' in value) || typeof value.accessToken !== 'string' ||
      !('expiresAt' in value) || typeof value.expiresAt !== 'string' ||
      !Number.isFinite(Date.parse(value.expiresAt)) || Date.parse(value.expiresAt) <= Date.now()
    ) {
      clearSession()
      return null
    }
    return { accessToken: value.accessToken, expiresAt: value.expiresAt }
  } catch {
    clearSession()
    return null
  }
}

export function saveSession(session: StoredSession) {
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  } catch {
    throw new SessionStorageError()
  }
}

export function clearSession() {
  try {
    localStorage.removeItem(SESSION_KEY)
  } catch {
    // Storage may be disabled by the browser. Logout must still clear React state.
  }
}
