import { Alert, Button, Form, Input, Typography } from 'antd'
import { ExperimentOutlined, LockOutlined, PhoneOutlined, ShopOutlined } from '@ant-design/icons'
import axios from 'axios'
import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { landingRoutes } from '../../auth/roles'
import { SessionStorageError } from '../../auth/session'
import type { ApiError, LoginRequest } from '../../types/auth'
import { TableIllustration } from '../../assets/illustrations/TableIllustration'
import { BrandMark } from '../../components/Brand'

export default function LoginPage() {
  const { account, loading, login } = useAuth()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [form] = Form.useForm<LoginRequest>()
  if (account) return <Navigate to={landingRoutes[account.role]} replace />

  async function submit(values: LoginRequest) {
    setError(null)
    setSubmitting(true)
    try {
      await login(values)
    } catch (failure) {
      if (axios.isAxiosError<ApiError>(failure)) {
        setError(failure.response?.data.message ?? 'Chưa thể kết nối với hệ thống. Vui lòng thử lại.')
        const fields = failure.response?.data.fieldErrors
        if (fields) form.setFields((['phone', 'password'] as const)
          .filter((name) => fields[name])
          .map((name) => ({ name, errors: [fields[name]] })))
      } else if (failure instanceof SessionStorageError) {
        setError(failure.message)
      } else {
        setError('Không thể đăng nhập. Vui lòng thử lại.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-introduction" aria-label="Giới thiệu hệ thống">
        <BrandMark />
        <div className="login-introduction-content">
          <Typography.Title>DN Restaurant</Typography.Title>
          <p>Một không gian làm việc chung cho phục vụ, bếp, thu ngân và quản lý.</p>
          <div className="introduction-details"><span><ShopOutlined aria-hidden />Bán hàng tại bàn</span><span><ExperimentOutlined aria-hidden />Quản lý nguyên vật liệu</span></div>
          <TableIllustration className="login-illustration" />
        </div>
        <p className="login-footnote">Hệ thống dành cho nhân viên nội bộ.</p>
      </section>
      <section className="login-form-section" aria-labelledby="login-heading">
        <div className="login-form-container">
          <Typography.Title id="login-heading" level={2}>Đăng nhập</Typography.Title>
          <Typography.Paragraph className="login-description">Sử dụng tài khoản được cấp để vào không gian làm việc của bạn.</Typography.Paragraph>
          {error && <Alert type="error" showIcon title={error} className="login-error" />}
          <Form<LoginRequest> form={form} layout="vertical" onFinish={submit} requiredMark={false} size="large" autoComplete="on">
            <Form.Item name="phone" label="Số điện thoại" rules={[
              { required: true, message: 'Vui lòng nhập số điện thoại' },
              { pattern: /^0\d{9}$/, message: 'Số điện thoại gồm 10 chữ số và bắt đầu bằng 0' },
            ]}>
              <Input prefix={<PhoneOutlined aria-hidden />} type="tel" inputMode="tel" autoComplete="username" placeholder="Nhập số điện thoại" maxLength={10} autoFocus />
            </Form.Item>
            <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}>
              <Input.Password prefix={<LockOutlined aria-hidden />} autoComplete="current-password" placeholder="Nhập mật khẩu" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={submitting || loading}>Đăng nhập</Button>
          </Form>
          <p className="login-help">Nếu chưa có tài khoản, vui lòng liên hệ chủ quán.</p>
        </div>
      </section>
    </main>
  )
}
