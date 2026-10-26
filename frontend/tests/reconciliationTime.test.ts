import { expect, test } from 'bun:test'
import dayjs from 'dayjs'
import { initialReconciliationRange, isFutureReconciliationDate, reconciliationDisabledTime } from '../src/utils/reconciliationTime'

const now = dayjs('2026-10-23T15:17:00')

test('reconciliation defaults to today midnight through the current minute', () => {
  const [start, end] = initialReconciliationRange(now)
  expect(start.format('YYYY-MM-DD HH:mm')).toBe('2026-10-23 00:00')
  expect(end.format('YYYY-MM-DD HH:mm')).toBe('2026-10-23 15:17')
  expect(end.isAfter(now)).toBe(false)
})

test('reconciliation disables future calendar days and only future hours/minutes today', () => {
  expect(isFutureReconciliationDate(dayjs('2026-10-24'), now)).toBe(true)
  expect(isFutureReconciliationDate(dayjs('2026-10-23'), now)).toBe(false)
  const times = reconciliationDisabledTime(now, now)
  expect(times.disabledHours()).toEqual([16, 17, 18, 19, 20, 21, 22, 23])
  expect(times.disabledMinutes(15)).toContain(18)
  expect(times.disabledMinutes(15)).not.toContain(17)
  expect(times.disabledMinutes(14)).toEqual([])
  expect(reconciliationDisabledTime(dayjs('2026-10-22'), now).disabledHours()).toEqual([])
})
