import { expect, test } from 'bun:test'
import { filterAccounts } from '../src/utils/accountFilters'
import type { AccountDetails } from '../src/types/accounts'

const accounts: AccountDetails[] = Array.from({ length: 120 }, (_, index) => ({
  id: index + 1, fullName: `Nhân viên ${index + 1}`, phone: `091${String(index).padStart(7, '0')}`, role: 'WAITER', status: 'ACTIVE',
}))
accounts[110] = { id: 111, fullName: 'Đặng Thị Hồng Ánh', phone: '0901234567', role: 'MANAGER', status: 'LOCKED' }

test('account search finds employees beyond the visible first page with or without Vietnamese accents', () => {
  expect(filterAccounts(accounts, 'dang thi hong anh').map((account) => account.id)).toEqual([111])
  expect(filterAccounts(accounts, 'HỒNG ĐẶNG').map((account) => account.id)).toEqual([111])
})

test('account search matches complete, partial and formatted phone numbers', () => {
  expect(filterAccounts(accounts, '0901234567').map((account) => account.id)).toEqual([111])
  expect(filterAccounts(accounts, '090 123 4567').map((account) => account.id)).toEqual([111])
  expect(filterAccounts(accounts, '234567').map((account) => account.id)).toEqual([111])
})

test('account role and status filters combine with search and preserve the full list', () => {
  expect(filterAccounts(accounts, 'hồng', 'MANAGER', 'LOCKED').map((account) => account.id)).toEqual([111])
  expect(filterAccounts(accounts, 'hồng', 'WAITER')).toHaveLength(0)
  expect(filterAccounts(accounts, 'hồng', undefined, 'ACTIVE')).toHaveLength(0)
  expect(filterAccounts(accounts, '   ')).toHaveLength(120)
  expect(accounts[110].status).toBe('LOCKED')
})
