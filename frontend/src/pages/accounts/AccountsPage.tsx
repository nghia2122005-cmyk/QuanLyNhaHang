import { Avatar, Button, Form, Input, Modal, Select, Space, Table, Typography } from 'antd'
import { CrownOutlined, InfoCircleOutlined, LockOutlined, PhoneOutlined, SearchOutlined, SolutionOutlined, UnlockOutlined, UserAddOutlined, UserOutlined, WalletOutlined } from '@ant-design/icons'
import type { ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { accountApi } from '../../api/accountApi'
import { useAuth } from '../../auth/useAuth'
import { StatusTag } from '../../components/StatusTag'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import type { AccountDetails, CreateAccount } from '../../types/accounts'
import type { Role } from '../../types/auth'
import { roleLabels, statusLabel } from '../../utils/labels'
import { filterAccounts } from '../../utils/accountFilters'
import { PageHeader } from '../../components/PageHeader'
import { EmptyState } from '../../components/states/EmptyState'
import { ChefHatIcon } from '../../assets/icons/ChefHatIcon'

const roleOptions = Object.entries(roleLabels).map(([value, label]) => ({ value, label }))
// Each role has its own tint (class role-tone-<role> in index.css) shared by the summary cards and avatars.
const roleIcons: Record<Role, ReactNode> = {
  OWNER: <CrownOutlined />, MANAGER: <SolutionOutlined />, CASHIER: <WalletOutlined />, WAITER: <UserOutlined />,
  KITCHEN_STAFF: <ChefHatIcon />, HEAD_CHEF: <ChefHatIcon />,
}
const toneClass = (role: Role) => `role-tone-${role.toLowerCase().replace('_', '-')}`
export default function AccountsPage() {
  const { account } = useAuth()
  const cache = useQueryClient()
  const [open, setOpen] = useState(false)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState<Role>()
  const [statusFilter, setStatusFilter] = useState<AccountDetails['status']>()
  const [page, setPage] = useState(1)
  const [form] = Form.useForm<CreateAccount>()
  const query = useQuery({ queryKey: ['accounts'], queryFn: ({ signal }) => accountApi.list(signal) })
  const { report, message } = useApiFeedback(query.error)
  const refresh = () => cache.invalidateQueries({ queryKey: ['accounts'] })
  const create = useMutation({ mutationFn: accountApi.create, onSuccess: async () => {
    setOpen(false); form.resetFields(); void message.success('Đã tạo tài khoản'); await refresh()
  }, onError: (error) => report(error, form) })
  const change = useMutation({ mutationFn: (action: { id: number; type: 'lock' | 'unlock' | 'role'; role?: Role }) =>
    action.type === 'role' ? accountApi.role(action.id, action.role!) : accountApi[action.type](action.id),
  onSuccess: async () => { void message.success('Đã cập nhật tài khoản'); await refresh() }, onError: (error) => report(error) })
  const filtered = filterAccounts(query.data ?? [], search, roleFilter, statusFilter)
  // Head counts per role from the loaded list (people, not money).
  const roleCount = (role: Role) => (query.data ?? []).filter((entry) => entry.role === role).length

  return <section>
    <PageHeader eyebrow="Quản trị nhân sự" title="Tài khoản" description="Quản lý quyền truy cập của nhân viên."
      actions={<Button type="primary" size="large" icon={<UserAddOutlined aria-hidden />} onClick={() => setOpen(true)}>Tạo tài khoản</Button>} />
    <div className="count-cards" aria-label="Số tài khoản theo vai trò" role="group">{(Object.keys(roleLabels) as Role[]).map((role) => <div key={role} className="count-card">
      <span className={`count-card-icon ${toneClass(role)}`} aria-hidden="true">{roleIcons[role]}</span>
      <div><span>{roleLabels[role]}</span><strong>{query.isPending ? '–' : roleCount(role)}</strong></div>
    </div>)}</div>
    <div className="account-filters filter-card">
      <div className="account-search"><label htmlFor="account-search">Tên hoặc số điện thoại</label>
        <Input.Search id="account-search" prefix={<SearchOutlined aria-hidden />} placeholder="Ví dụ: Minh, phuc vu, 0900000004" allowClear enterButton={<Button type="primary" icon={<SearchOutlined aria-hidden />}>Tìm kiếm</Button>}
          value={searchInput} onChange={(event) => { setSearchInput(event.target.value); if (!event.target.value) { setSearch(''); setPage(1) } }}
          onSearch={(value) => { setSearch(value.trim()); setPage(1) }} />
      </div>
      <div><label htmlFor="account-role-filter">Vai trò</label><Select id="account-role-filter" value={roleFilter} allowClear placeholder="Tất cả vai trò"
        options={roleOptions} onChange={(value: Role | undefined) => { setRoleFilter(value); setPage(1) }} /></div>
      <div><label htmlFor="account-status-filter">Trạng thái</label><Select id="account-status-filter" value={statusFilter} allowClear placeholder="Tất cả trạng thái"
        options={(['ACTIVE', 'LOCKED'] as const).map((value) => ({ value, label: statusLabel('AccountStatus', value) }))}
        onChange={(value: AccountDetails['status'] | undefined) => { setStatusFilter(value); setPage(1) }} /></div>
      <Button onClick={() => { setSearchInput(''); setSearch(''); setRoleFilter(undefined); setStatusFilter(undefined); setPage(1) }}>Đặt lại</Button>
    </div>
    <Typography.Paragraph type="secondary" role="status">Tìm thấy {filtered.length} / {query.data?.length ?? 0} tài khoản</Typography.Paragraph>
    <div className="data-panel"><Table<AccountDetails> className="accounts-table" rowKey="id" loading={query.isPending} dataSource={filtered} scroll={{ x: 760 }}
      locale={{ emptyText: <EmptyState compact title="Không tìm thấy tài khoản. Thử tên/SĐT khác hoặc đặt lại bộ lọc." /> }}
      pagination={{ current: Math.min(page, Math.max(1, Math.ceil(filtered.length / 20))), pageSize: 20, hideOnSinglePage: true, onChange: setPage }} columns={[
        { title: 'Họ tên', dataIndex: 'fullName', render: (name: string, row) => <span className="account-name-cell">
          <Avatar size={36} className={`account-avatar ${toneClass(row.role)}`}>{name.slice(0, 1)}</Avatar><strong>{name}</strong>
          {row.id === account?.accountId && <span className="self-chip"><span className="sr-only">(</span>Bạn<span className="sr-only">)</span></span>}</span> },
        { title: 'Số điện thoại', dataIndex: 'phone', render: (phone: string) => <span className="text-nowrap"><PhoneOutlined aria-hidden /> {phone}</span> },
        { title: 'Vai trò', render: (_, row) => <Select aria-label={`Vai trò ${row.fullName}`} value={row.role} options={roleOptions}
          style={{ minWidth: 170 }} disabled={row.id === account?.accountId || change.isPending}
          onChange={(role: Role) => change.mutate({ id: row.id, type: 'role', role })} /> },
        { title: 'Trạng thái', render: (_, row) => <StatusTag group="AccountStatus" value={row.status} /> },
        { title: 'Thao tác', render: (_, row) => <Button icon={row.status === 'ACTIVE' ? <LockOutlined aria-hidden /> : <UnlockOutlined aria-hidden />} disabled={row.id === account?.accountId || change.isPending}
          onClick={() => change.mutate({ id: row.id, type: row.status === 'ACTIVE' ? 'lock' : 'unlock' })}>
          {row.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}</Button> },
      ]} /></div>
    <p className="info-note"><InfoCircleOutlined aria-hidden />Khóa hoặc đổi vai trò áp dụng cho lần đăng nhập mới. Phiên đã cấp giữ quyền đến khi hết hạn, tối đa 12 giờ.</p>
    <Modal title="Tạo tài khoản" open={open} onCancel={() => setOpen(false)} footer={null} maskClosable={!create.isPending} closable={!create.isPending}>
      <Form form={form} layout="vertical" initialValues={{ role: 'WAITER' }} onFinish={(values) => create.mutate(values)} disabled={create.isPending}>
        <Form.Item name="fullName" label="Họ tên" rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập họ tên' }, { max: 100 }]}><Input maxLength={100} /></Form.Item>
        <Form.Item name="phone" label="Số điện thoại" rules={[{ required: true, message: 'Vui lòng nhập số điện thoại' }, { pattern: /^0\d{9}$/, message: 'Nhập 10 chữ số, bắt đầu bằng 0' }]}><Input type="tel" maxLength={15} /></Form.Item>
        <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, min: 6, max: 72, message: 'Mật khẩu từ 6 đến 72 ký tự' },
          { validator: (_, value: string) => !value || new TextEncoder().encode(value).length <= 72 ? Promise.resolve() : Promise.reject(new Error('Mật khẩu không vượt quá 72 byte UTF-8')) }]}><Input.Password autoComplete="new-password" /></Form.Item>
        <Form.Item name="role" label="Vai trò" rules={[{ required: true }]}><Select options={roleOptions} /></Form.Item>
        <Space className="form-actions"><Button onClick={() => setOpen(false)}>Hủy</Button><Button type="primary" htmlType="submit" loading={create.isPending}>Tạo tài khoản</Button></Space>
      </Form>
    </Modal>
  </section>
}
