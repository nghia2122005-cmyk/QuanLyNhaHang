import type { AccountDetails } from '../types/accounts'
import type { Role } from '../types/auth'

const searchable = (text: string) => text.normalize('NFD').replace(/\p{M}/gu, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLocaleLowerCase('vi-VN').trim()

export function filterAccounts(accounts: AccountDetails[], query: string, role?: Role, status?: AccountDetails['status']) {
  const normalized = searchable(query)
  const phoneQuery = /^[\d\s()+.-]+$/.test(normalized) ? normalized.replace(/\D/g, '') : ''
  return accounts.filter((account) => {
    if (role && account.role !== role) return false
    if (status && account.status !== status) return false
    if (!normalized) return true
    if (phoneQuery) return account.phone.includes(phoneQuery)
    const text = searchable(`${account.fullName} ${account.phone}`)
    return normalized.split(/\s+/).every((word) => text.includes(word))
  })
}
