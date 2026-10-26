import { Button, Drawer, Space } from 'antd'
import { BellOutlined, WarningOutlined } from '@ant-design/icons'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { sessionApi } from '../../api/sessionApi'
import { OrderQueueCard } from '../../components/OrderQueueCard'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import { refreshLiveQueries } from '../../realtime/invalidation'
import type { OrderQueueItem } from '../../types/sessions'
import type { FloorTable } from '../../types/tables'
import { EmptyState } from '../../components/states/EmptyState'

export function TableNotifications({ table, items, canAct }: { table: FloorTable; items: OrderQueueItem[]; canAct: boolean }) {
  const [kind, setKind] = useState<'READY' | 'UNAVAILABLE' | null>(null)
  const cache = useQueryClient()
  const { report } = useApiFeedback()
  const relevant = items.filter((item) => item.tableId === table.id && item.sessionId === table.activeSessionId)
  const ready = relevant.filter((item) => item.status === 'READY')
  const unavailable = relevant.filter((item) => item.status === 'UNAVAILABLE')
  const change = useMutation({ mutationFn: (item: OrderQueueItem) => item.status === 'READY' ? sessionApi.serve(item.id) : sessionApi.acknowledgeUnavailable(item.id),
    onError: (error) => report(error), onSuccess: async (_, item) => { await refreshLiveQueries(cache, { type: 'ORDER', sessionId: item.sessionId }) } })
  const visible = kind === 'READY' ? ready : unavailable
  return <>
    <Space wrap className="table-notifications">
      {ready.length > 0 && <Button aria-label={`${table.name}: ${ready.length} dòng món chờ phục vụ`} className="table-alert table-alert-ready" icon={<span className="table-alert-icon"><BellOutlined aria-hidden /></span>} onClick={() => setKind('READY')}>{ready.length}</Button>}
      {unavailable.length > 0 && <Button danger aria-label={`${table.name}: ${unavailable.length} dòng món không thể chế biến chưa xử lý`} className="table-alert table-alert-unavailable" icon={<span className="table-alert-icon"><WarningOutlined aria-hidden /></span>} onClick={() => setKind('UNAVAILABLE')}>{unavailable.length}</Button>}
    </Space>
    <Drawer rootClassName="floor-dialog" title={`${table.name} · ${kind === 'READY' ? 'Món chờ phục vụ' : 'Không thể chế biến — cần báo khách'}`} open={kind != null && table.activeSessionId != null} onClose={() => setKind(null)} width={700}>
      {!visible.length && <EmptyState compact title="Đã xử lý hết thông báo" />}
      {visible.map((item) => <OrderQueueCard key={item.id} item={item}>{canAct && <Button type="primary" size="large" disabled={change.isPending} onClick={() => change.mutate(item)}>
        {kind === 'READY' ? 'Đã phục vụ' : 'Đã thông báo cho khách'}</Button>}</OrderQueueCard>)}
    </Drawer>
  </>
}
