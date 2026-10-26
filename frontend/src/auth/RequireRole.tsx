import { Button, Result } from 'antd'
import { Link, Outlet } from 'react-router-dom'
import type { Role } from '../types/auth'
import { useAuth } from './useAuth'
import { landingRoutes } from './roles'

export function RequireRole({ roles }: { roles: readonly Role[] }) {
  const { account } = useAuth()
  if (!account || !roles.includes(account.role)) {
    return <Result status="403" title="Bạn không có quyền truy cập trang này"
      extra={<Link to={account ? landingRoutes[account.role] : '/login'}><Button type="primary">Về trang chính</Button></Link>} />
  }
  return <Outlet />
}
