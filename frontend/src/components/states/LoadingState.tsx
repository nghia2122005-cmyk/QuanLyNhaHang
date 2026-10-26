import { Skeleton } from 'antd'

interface LoadingStateProps {
  variant?: 'default' | 'table' | 'cards'
  count?: number
}

/** Skeleton placeholder: a card (default), table rows or a grid of cards. */
export function LoadingState({ variant = 'default', count }: LoadingStateProps) {
  const status = <span className="sr-only">Đang tải…</span>
  if (variant === 'cards') return <div className="loading-state-grid" role="status" aria-busy="true">{status}
    {Array.from({ length: count ?? 4 }, (_, index) => <div className="state-card" key={index}><Skeleton active title={{ width: '50%' }} paragraph={{ rows: 3 }} /></div>)}
  </div>
  return <div className="state-card" role="status" aria-busy="true">{status}
    {variant === 'table'
      ? Array.from({ length: count ?? 5 }, (_, index) => <Skeleton.Input key={index} active block className="loading-state-row" />)
      : <Skeleton active paragraph={{ rows: count ?? 4 }} />}
  </div>
}
