import { expect, test } from 'bun:test'
import { statusColors, statusLabel } from '../src/utils/labels'
import { formatExpiryDate, lotExpiryPresentation } from '../src/utils/inventory'

test('MovementType labels and colors match design system', () => {
  expect(statusLabel('MovementType', 'RECEIPT')).toBe('Nhập')
  expect(statusLabel('MovementType', 'CONSUMPTION')).toBe('Tiêu hao')
  expect(statusLabel('MovementType', 'STOCKTAKE_ADJUSTMENT')).toBe('Điều chỉnh kiểm kê')
  expect(statusLabel('MovementType', 'DISPOSAL')).toBe('Loại bỏ')

  expect(statusColors['RECEIPT']).toBe('green')
  expect(statusColors['CONSUMPTION']).toBe('blue')
  expect(statusColors['STOCKTAKE_ADJUSTMENT']).toBe('purple')
  expect(statusColors['DISPOSAL']).toBe('red')
})

test('IngredientStatus labels and colors', () => {
  expect(statusLabel('IngredientStatus', 'ACTIVE')).toBe('Đang sử dụng')
  expect(statusLabel('IngredientStatus', 'INACTIVE')).toBe('Ngừng sử dụng')

  expect(statusColors['ACTIVE']).toBe('green')
  expect(statusColors['INACTIVE']).toBe('default')
})

test('Stock difference and status logic calculates properly', () => {
  const systemStock = 50.0
  const actualStock = 48.0
  const diff = actualStock - systemStock
  expect(diff).toBe(-2.0)

  const isBelowMin = (current: number, min: number) => current < min
  expect(isBelowMin(0.5, 1.0)).toBe(true)
  expect(isBelowMin(1.0, 1.0)).toBe(false)
  expect(isBelowMin(1.5, 1.0)).toBe(false)
  expect(isBelowMin(-0.2, 1.0)).toBe(true)
})

test('Lot attention status calculation', () => {
  expect(lotExpiryPresentation({ status: 'EXPIRED', daysRemaining: -2 }).label).toBe('Đã hết hạn')
  // Backend's LocalDate boundary: expiry today is EXPIRING_SOON, not EXPIRED.
  expect(lotExpiryPresentation({ status: 'EXPIRING_SOON', daysRemaining: 0 }).label).toBe('Sắp hết hạn · Còn 0 ngày')
  expect(lotExpiryPresentation({ status: 'VALID', daysRemaining: 120 }).label).toBe('Còn hạn')
  expect(lotExpiryPresentation({ status: 'NO_EXPIRY', daysRemaining: null }).label).toBe('Không có HSD')
})

test('Expiry metadata uses calendar dates without timezone conversion', () => {
  expect(formatExpiryDate('2026-10-30')).toBe('30/10/2026')
  expect(formatExpiryDate(null)).toBe('—')
  expect(formatExpiryDate(undefined)).toBe('—')
})
