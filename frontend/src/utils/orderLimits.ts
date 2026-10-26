export const MAX_ORDER_QUANTITY = 99
export const MAX_ORDER_LINES = 50

export function orderLinesError(count: number): string | undefined {
  if (count < 1) return 'Vui lòng chọn ít nhất một món'
  if (count > MAX_ORDER_LINES) return `Mỗi đợt gọi món tối đa ${MAX_ORDER_LINES} dòng`
}
