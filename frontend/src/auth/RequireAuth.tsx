import { Spin } from 'antd'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './useAuth'

export function RequireAuth() {
  const { account, loading } = useAuth()
  if (loading) return <div className="page-loading" role="status" aria-label="Đang kiểm tra đăng nhập"><Spin size="large" /></div>
  return account ? <Outlet /> : <Navigate to="/login" replace />
}
