import { expect, test } from 'bun:test'
import { MAX_ORDER_LINES, MAX_ORDER_QUANTITY, orderLinesError } from '../src/utils/orderLimits'

test('order input limits match the approved 99 portions and 50 lines', () => {
  expect(MAX_ORDER_QUANTITY).toBe(99)
  expect(MAX_ORDER_LINES).toBe(50)
})

test('order lists accept both boundaries and reject empty or excessive rows', () => {
  expect(orderLinesError(0)).toBe('Vui lòng chọn ít nhất một món')
  expect(orderLinesError(1)).toBeUndefined()
  expect(orderLinesError(50)).toBeUndefined()
  expect(orderLinesError(51)).toBe('Mỗi đợt gọi món tối đa 50 dòng')
})
