import { ReloadOutlined, WarningOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import type { ReactNode } from 'react'

interface ErrorStateProps {
  title: ReactNode
  description?: ReactNode
  onRetry?: () => void
  compact?: boolean
}

/** Load failure with an optional "Thử lại" button that calls `onRetry`. */
export function ErrorState({ title, description, onRetry, compact = false }: ErrorStateProps) {
  return <div className={compact ? 'error-state error-state-compact' : 'error-state'} role="alert">
    <span className="error-state-icon" aria-hidden="true"><WarningOutlined /></span>
    <div className="error-state-text">
      <div className="error-state-title">{title}</div>
      {description && <div className="error-state-description">{description}</div>}
    </div>
    {onRetry && <Button icon={<ReloadOutlined aria-hidden />} onClick={onRetry}>Thử lại</Button>}
  </div>
}
