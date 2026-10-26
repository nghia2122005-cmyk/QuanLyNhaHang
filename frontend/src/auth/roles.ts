import type { Role } from '../types/auth'

export const landingRoutes: Record<Role, string> = {
  OWNER: '/reports', MANAGER: '/reservations', CASHIER: '/payment', WAITER: '/floor-plan',
  KITCHEN_STAFF: '/kitchen', HEAD_CHEF: '/kitchen',
}

export const navigation: { path: string; label: string; description: string; roles: readonly Role[] }[] = [
  { path: '/reports', label: 'Báo cáo', description: 'Theo dõi hoạt động bán hàng và nguyên vật liệu.', roles: ['OWNER'] },
  { path: '/accounts', label: 'Tài khoản', description: 'Quản lý quyền truy cập của nhân viên.', roles: ['OWNER'] },
  { path: '/menu', label: 'Thực đơn', description: 'Danh mục món ăn và định lượng nguyên vật liệu.', roles: ['OWNER', 'HEAD_CHEF'] },
  { path: '/floor-plan', label: 'Sơ đồ bàn', description: 'Theo dõi bố trí bàn và các phiên phục vụ.', roles: ['WAITER', 'MANAGER', 'CASHIER', 'OWNER'] },
  { path: '/reservations', label: 'Đặt bàn', description: 'Tra cứu và theo dõi lịch đặt bàn của khách.', roles: ['MANAGER', 'WAITER', 'OWNER'] },
  { path: '/kitchen', label: 'Bếp', description: 'Theo dõi món đang chờ và đang chế biến.', roles: ['KITCHEN_STAFF', 'HEAD_CHEF'] },
  { path: '/payment', label: 'Thanh toán', description: 'Theo dõi các phiên phục vụ cần thanh toán.', roles: ['CASHIER'] },
  { path: '/invoices', label: 'Lịch sử hóa đơn', description: 'Tra cứu các giao dịch đã thanh toán.', roles: ['CASHIER', 'MANAGER', 'OWNER'] },
  { path: '/reconciliation', label: 'Đối soát cuối ca', description: 'Đối chiếu số tiền thực nhận với doanh thu hệ thống.', roles: ['CASHIER', 'MANAGER'] },
  { path: '/inventory', label: 'Kho', description: 'Theo dõi nguyên vật liệu, lô hàng và biến động.', roles: ['OWNER', 'HEAD_CHEF'] },
  { path: '/inventory/receipts', label: 'Nhập nguyên vật liệu', description: 'Ghi nhận nguyên vật liệu được nhập vào kho.', roles: ['HEAD_CHEF'] },
  { path: '/inventory/stocktake', label: 'Kiểm kê', description: 'Đối chiếu số lượng thực tế của nguyên vật liệu.', roles: ['HEAD_CHEF'] },
]
