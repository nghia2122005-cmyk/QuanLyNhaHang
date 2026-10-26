import { ClockCircleOutlined } from '@ant-design/icons'
import { Typography } from 'antd'
import type { ReactNode } from 'react'
import { TableIllustration } from '../assets/illustrations/TableIllustration'

interface PageHeaderProps {
  eyebrow?: string
  title: string
  description?: ReactNode
  actions?: ReactNode
  extra?: ReactNode
  icon?: ReactNode
  variant?: 'default' | 'banner'
}

/** Page title block shared by every screen; the h1 keeps the page title as its accessible name. */
export function PageHeader({ eyebrow, title, description, actions, extra, icon, variant = 'default' }: PageHeaderProps) {
  const banner = variant === 'banner'
  return <header className={banner ? 'page-header page-header-banner' : 'page-header'}>
    {banner && <TableIllustration className="page-header-illustration" />}
    <div className="page-header-main">
      {eyebrow && <span className="page-header-eyebrow">{banner && <ClockCircleOutlined aria-hidden />}{eyebrow}</span>}
      {icon
        ? <div className="page-header-title"><span className="page-header-icon" aria-hidden="true">{icon}</span><Typography.Title level={1}>{title}</Typography.Title></div>
        : <Typography.Title level={1}>{title}</Typography.Title>}
      {description && <div className="page-header-description">{description}</div>}
    </div>
    {actions && <div className="page-header-actions">{actions}</div>}
    {extra && <div className="page-header-extra">{extra}</div>}
  </header>
}
