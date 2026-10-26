import { Button, ConfigProvider, Segmented, Typography } from 'antd'
import { AppstoreOutlined, BankOutlined, ReloadOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { sessionApi } from '../../api/sessionApi'
import { tableApi } from '../../api/tableApi'
import { useAuth } from '../../auth/useAuth'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import type { FloorTable } from '../../types/tables'
import { statusLabel } from '../../utils/labels'
import { nowInAppZone } from '../../utils/time'
import OpenSessionModal from './OpenSessionModal'
import { liveQueryOptions } from '../../realtime/invalidation'
import { TableManagementDrawer } from './TableManagementDrawer'
import { PageHeader } from '../../components/PageHeader'
import { EmptyState } from '../../components/states/EmptyState'
import { ErrorState } from '../../components/states/ErrorState'
import { LoadingState } from '../../components/states/LoadingState'
import { TableCard } from './TableCard'

type TableFilter = 'ALL' | FloorTable['status']
const filterOrder: TableFilter[] = ['ALL', 'AVAILABLE', 'OCCUPIED', 'INACTIVE']
const weekdays = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy']
// Literal values of --teal-700 / --surface / --border; the segmented thumb colour is computed by antd.
const filterTheme = { components: { Segmented: { trackBg: '#FFFFFF', itemSelectedBg: '#11666A', itemSelectedColor: '#FFFFFF', itemColor: '#13292B', trackPadding: 4 } } }

// Display-only shift name in Vietnam time: morning before 11:00, afternoon until 17:00, evening after.
function shiftLabel() {
  const now = nowInAppZone()
  const shift = now.hour() < 11 ? 'sáng' : now.hour() < 17 ? 'chiều' : 'tối'
  return `Ca ${shift} · ${weekdays[now.day()]}, ${now.format('DD/MM/YYYY')}`
}

export default function FloorPlanPage() {
  const { account } = useAuth()
  const canOpen = account?.role === 'WAITER' || account?.role === 'MANAGER'
  const [selected, setSelected] = useState<FloorTable | null>(null)
  const [manageOpen, setManageOpen] = useState(false)
  const [filter, setFilter] = useState<TableFilter>('ALL')
  const navigate = useNavigate()
  const query = useQuery({ queryKey: ['floor-plan'], queryFn: ({ signal }) => tableApi.floorPlan(signal), ...liveQueryOptions })
  const notifications = useQuery({ queryKey: ['awaiting-service'], queryFn: ({ signal }) => sessionApi.awaitingService(signal), ...liveQueryOptions })
  useApiFeedback(notifications.error)
  useApiFeedback(query.error)
  const areas = query.data ?? []
  const tables = areas.flatMap((area) => area.tables)
  const count = (status: TableFilter) => status === 'ALL' ? tables.length : tables.filter((table) => table.status === status).length
  const booked = tables.filter((table) => table.upcomingReservation).length
  const refresh = () => { void query.refetch(); void notifications.refetch() }

  return <section className="floor-plan-page">
    <PageHeader variant="banner" eyebrow={shiftLabel()} title="Sơ đồ bàn" description="Chọn bàn để đón khách và theo dõi lượt đặt trước."
      actions={<div className="floor-stats">
        <div className="floor-stat"><span>Bàn trống</span><strong className="floor-stat-free">{count('AVAILABLE')}<small> / {tables.length}</small></strong></div>
        <div className="floor-stat"><span>Đang phục vụ</span><strong className="floor-stat-busy">{count('OCCUPIED')}</strong></div>
        <div className="floor-stat"><span>Đặt trước</span><strong className="floor-stat-booked">{booked}</strong></div>
      </div>} />
    <div className="floor-toolbar">
      <ConfigProvider theme={filterTheme}>
        <Segmented<TableFilter> className="floor-filter" aria-label="Lọc bàn theo trạng thái" value={filter} onChange={setFilter}
          options={filterOrder.map((value) => ({ value, label: <span className="floor-filter-option">
            <i className={`floor-filter-dot floor-filter-dot-${value.toLowerCase()}`} aria-hidden="true" />
            {value === 'ALL' ? 'Tất cả' : statusLabel('TableStatus', value)} · {count(value)}</span> }))} />
      </ConfigProvider>
      <div className="floor-toolbar-end">
        <span className="live-indicator"><i aria-hidden="true" />Tự cập nhật trực tiếp · Kiểm tra dự phòng mỗi 30 giây</span>
        {account?.role === 'OWNER' && <Button type="primary" icon={<AppstoreOutlined aria-hidden />} onClick={() => setManageOpen(true)}>Quản lý khu vực và bàn</Button>}
        <Button className="floor-refresh" icon={<ReloadOutlined aria-hidden />} loading={query.isFetching} onClick={refresh}>Làm mới</Button>
      </div>
    </div>
    {query.isPending ? <LoadingState variant="cards" />
      : query.isError && !query.data ? <ErrorState title="Chưa tải được sơ đồ bàn" onRetry={refresh} />
      : areas.length === 0 ? <EmptyState title="Chưa có bàn trong khu vực đang hoạt động." />
      : areas.map((area) => {
        const visible = area.tables.filter((table) => filter === 'ALL' || table.status === filter)
        return <section className="floor-area" key={area.id} aria-label={area.name}>
          <div className="area-heading">
            <span className="area-heading-icon" aria-hidden="true"><BankOutlined /></span>
            <Typography.Title level={2}>{area.name}</Typography.Title>
            {area.code && <span className="area-code">{area.code}</span>}
            <span className="area-count">{area.tables.length} bàn</span>
            <span className="area-rule" aria-hidden="true" />
          </div>
          {visible.length === 0
            ? <EmptyState compact title={area.tables.length ? 'Không có bàn ở trạng thái này.' : 'Khu vực chưa có bàn.'} />
            : <div className="floor-grid">{visible.map((table) => <TableCard key={table.id} table={table} notifications={notifications.data ?? []}
              canOpen={canOpen} canAct={account?.role === 'WAITER'} onOpen={setSelected} onView={(sessionId) => navigate(`/sessions/${sessionId}`)} />)}</div>}
        </section>
      })}
    {selected && <OpenSessionModal key={selected.id} table={selected} onClose={() => setSelected(null)} />}
    {manageOpen && <TableManagementDrawer onClose={() => setManageOpen(false)} />}
  </section>
}
