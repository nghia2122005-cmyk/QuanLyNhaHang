import { ClockCircleOutlined } from '@ant-design/icons'
import { Card, Typography } from 'antd'
import dayjs from 'dayjs'
import type { ReactNode } from 'react'
import type { OrderQueueItem } from '../types/sessions'
import { formatTime } from '../utils/time'
import { StatusTag } from './StatusTag'

/** Minutes since the order was placed; recomputed on every render (live screens re-render on their 30 s poll). */
export function WaitingChip({ since }: { since: string }) {
  const minutes = Math.max(0, dayjs().diff(dayjs(since), 'minute'))
  return <span className="waiting-chip" title={`Gọi lúc ${formatTime(since)}`}><ClockCircleOutlined aria-hidden />{minutes} phút</span>
}

export function OrderQueueCard({ item, children }: { item: OrderQueueItem; children?: ReactNode }) {
  return <Card className="queue-card" data-item-id={item.id}>
    <div className="queue-card-heading"><strong>{item.tableCode ?? item.tableName} {item.tableCode && <small>· {item.tableName}</small>}</strong>
      <span className="queue-card-tags"><WaitingChip since={item.orderedAt} /><StatusTag group="OrderItemStatus" value={item.status} /></span></div>
    <Typography.Title level={3}>{item.menuItemName} <span className="item-quantity">× {item.quantity}</span></Typography.Title>
    <div className="queue-meta">Đợt #{item.orderId} · Phiên #{item.sessionId}<br /><ClockCircleOutlined aria-hidden /> {formatTime(item.orderedAt)}</div>
    {item.note && <p className="item-note">Ghi chú: {item.note}</p>}
    {item.unavailableReason && <p className="unavailable-reason"><strong>Lý do:</strong> {item.unavailableReason}</p>}
    {children && <div className="queue-actions">{children}</div>}
  </Card>
}
