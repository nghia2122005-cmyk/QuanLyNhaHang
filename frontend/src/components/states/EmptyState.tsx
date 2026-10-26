import type { ReactNode } from 'react'
import { EmptyBowl } from '../../assets/illustrations/EmptyBowl'

interface EmptyStateProps {
  title: ReactNode
  description?: ReactNode
  icon?: ReactNode
  illustration?: ReactNode
  action?: ReactNode
  compact?: boolean
}

/** Empty list/table/screen message. `compact` is the small form used inside tables, selects and drawers. */
export function EmptyState({ title, description, icon, illustration, action, compact = false }: EmptyStateProps) {
  return <div className={compact ? 'empty-state empty-state-compact' : 'empty-state'}>
    {illustration ?? (icon ? <span className="empty-state-icon" aria-hidden="true">{icon}</span> : <EmptyBowl size={compact ? 64 : 96} />)}
    <div className="empty-state-title">{title}</div>
    {description && <div className="empty-state-description">{description}</div>}
    {action && <div className="empty-state-action">{action}</div>}
  </div>
}
