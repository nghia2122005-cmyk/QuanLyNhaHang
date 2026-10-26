export const roleLabels = {
  OWNER: 'Chủ quán', MANAGER: 'Quản lý', CASHIER: 'Thu ngân', WAITER: 'Nhân viên phục vụ',
  KITCHEN_STAFF: 'Nhân viên bếp', HEAD_CHEF: 'Bếp trưởng',
} as const

export const statusLabels = {
  AccountStatus: { ACTIVE: 'Hoạt động', LOCKED: 'Bị khóa' },
  AreaStatus: { ACTIVE: 'Hoạt động', INACTIVE: 'Ngừng sử dụng' },
  TableStatus: { AVAILABLE: 'Trống', OCCUPIED: 'Đang phục vụ', INACTIVE: 'Ngừng sử dụng' },
  ReservationStatus: { CONFIRMED: 'Đã xác nhận', SEATED: 'Đã nhận bàn', CANCELLED: 'Đã hủy' },
  SessionStatus: { ACTIVE: 'Đang hoạt động', CLOSED: 'Đã kết thúc' },
  OrderItemStatus: {
    PENDING: 'Chờ chế biến', COOKING: 'Đang chế biến', READY: 'Hoàn thành', SERVED: 'Đã phục vụ',
    UNAVAILABLE: 'Không thể chế biến', CANCELLED: 'Đã hủy',
  },
  MenuItemStatus: { ON_SALE: 'Đang kinh doanh', DISCONTINUED: 'Ngừng kinh doanh' },
  IngredientStatus: { ACTIVE: 'Đang sử dụng', INACTIVE: 'Ngừng sử dụng' },
  MovementType: { RECEIPT: 'Nhập', CONSUMPTION: 'Tiêu hao', STOCKTAKE_ADJUSTMENT: 'Điều chỉnh kiểm kê', DISPOSAL: 'Loại bỏ' },
  PaymentMethod: { CASH: 'Tiền mặt', BANK_TRANSFER: 'Chuyển khoản' },
  Role: roleLabels,
} as const

export const statusColors: Record<string, string> = {
  ACTIVE: 'green', LOCKED: 'red', INACTIVE: 'default', AVAILABLE: 'green', OCCUPIED: 'blue',
  CONFIRMED: 'blue', SEATED: 'cyan', CANCELLED: 'default', CLOSED: 'default', PENDING: 'gold',
  COOKING: 'processing', READY: 'green', SERVED: 'cyan', UNAVAILABLE: 'red', ON_SALE: 'green',
  DISCONTINUED: 'default', RECEIPT: 'green', CONSUMPTION: 'blue', STOCKTAKE_ADJUSTMENT: 'purple',
  DISPOSAL: 'red', CASH: 'green', BANK_TRANSFER: 'blue',
}

export type StatusGroup = keyof typeof statusLabels

export function statusLabel(group: StatusGroup, value: string): string {
  const labels: Readonly<Record<string, string>> = statusLabels[group]
  return labels[value] ?? value
}
