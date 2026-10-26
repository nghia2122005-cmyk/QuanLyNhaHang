import { Tag } from 'antd'
import { statusColors, statusLabel } from '../utils/labels'
import type { StatusGroup } from '../utils/labels'

type StatusTone = 'free' | 'busy' | 'booked' | 'off' | 'danger'

// Table, reservation and order-item states use the --st-* design tokens; every other group keeps the antd preset colours.
const statusTones: Partial<Record<StatusGroup, Readonly<Record<string, StatusTone>>>> = {
  TableStatus: { AVAILABLE: 'free', OCCUPIED: 'busy', INACTIVE: 'off' },
  ReservationStatus: { CONFIRMED: 'booked' },
  OrderItemStatus: { PENDING: 'booked', COOKING: 'busy', READY: 'free', SERVED: 'free', UNAVAILABLE: 'danger', CANCELLED: 'off' },
  AccountStatus: { ACTIVE: 'free', LOCKED: 'danger' },
  MenuItemStatus: { ON_SALE: 'free', DISCONTINUED: 'off' },
}

export function StatusTag({ group, value }: { group: StatusGroup; value: string }) {
  const tone = statusTones[group]?.[value]
  if (tone) return <Tag className={`status-tag status-tag-${tone}`}>{statusLabel(group, value)}</Tag>
  return <Tag color={statusColors[value]}>{statusLabel(group, value)}</Tag>
}
