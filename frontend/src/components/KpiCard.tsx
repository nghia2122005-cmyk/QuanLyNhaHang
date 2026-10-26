import type { ReactNode } from 'react'

interface KpiCardProps {
  icon: ReactNode
  label: ReactNode
  value: ReactNode
  hint?: ReactNode
  featured?: boolean
  tone?: 'teal' | 'gold'
}

/** Statistic tile: icon box, label, large value and an optional hint line. Shows server-provided values only. */
export function KpiCard({ icon, label, value, hint, featured = false, tone = 'teal' }: KpiCardProps) {
  return <div className={['kpi-card', featured && 'kpi-card-featured', `kpi-card-${tone}`].filter(Boolean).join(' ')}>
    <span className="kpi-icon" aria-hidden="true">{icon}</span>
    <div className="kpi-label">{label}</div>
    <div className="kpi-value">{value}</div>
    {hint && <div className="kpi-hint">{hint}</div>}
  </div>
}
