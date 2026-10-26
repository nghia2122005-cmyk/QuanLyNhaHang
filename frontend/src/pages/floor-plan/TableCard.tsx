import { CalendarOutlined, PlusOutlined, UserOutlined } from '@ant-design/icons'
import { Button, Typography } from 'antd'
import { StatusTag } from '../../components/StatusTag'
import type { OrderQueueItem } from '../../types/sessions'
import type { FloorTable } from '../../types/tables'
import { formatTime } from '../../utils/time'
import { TableNotifications } from './TableNotifications'
import { TableShape } from './TableShape'

interface TableCardProps {
  table: FloorTable
  notifications: OrderQueueItem[]
  canOpen: boolean
  canAct: boolean
  onOpen: (table: FloorTable) => void
  onView: (sessionId: number) => void
}

export function TableCard({ table, notifications, canOpen, canAct, onOpen, onView }: TableCardProps) {
  const reservation = table.upcomingReservation
  const sessionId = table.activeSessionId
  return <article className={`table-card table-${table.status.toLowerCase()}`} data-table-name={table.name}>
    <div className="table-card-visual">
      <span className="table-card-status"><StatusTag group="TableStatus" value={table.status} /></span>
      <TableShape capacity={table.capacity} status={table.status} />
    </div>
    <div className="table-card-body">
      <div className="table-card-heading">
        <div><Typography.Title level={3}>{table.code ?? table.name}</Typography.Title><div className="muted-text">Tên bàn: {table.name}</div></div>
        <span className="table-capacity-chip"><UserOutlined aria-hidden />{table.capacity} khách</span>
      </div>
      {reservation
        ? <div className="table-reservation table-reservation-booked"><CalendarOutlined aria-hidden />
          <div><strong>Có đặt trước</strong> · {reservation.customerName}<br />{formatTime(reservation.startTime)}</div></div>
        : <div className="table-reservation"><CalendarOutlined aria-hidden /><span>Chưa có đặt trước sắp tới</span></div>}
      <TableNotifications key={`${table.id}-${table.activeSessionId}`} table={table} items={notifications} canAct={canAct} />
      <div className="table-card-action">
        {canOpen && table.status === 'AVAILABLE' && <Button type="primary" block icon={<PlusOutlined aria-hidden />} onClick={() => onOpen(table)}>Mở phiên</Button>}
        {canOpen && sessionId && <Button block className="table-session-button" onClick={() => onView(sessionId)}>Xem phiên</Button>}
        {!canOpen && sessionId && <span className="table-card-note">Phiên #{sessionId}</span>}
        {table.status === 'INACTIVE' && <Button block disabled>Bàn tạm ngừng sử dụng</Button>}
      </div>
    </div>
  </article>
}
