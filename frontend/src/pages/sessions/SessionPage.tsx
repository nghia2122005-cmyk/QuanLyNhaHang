import { Button, Card, Descriptions, Popconfirm, Result, Space, Typography } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { BackNavigation } from '../../components/BackNavigation'
import { sessionApi } from '../../api/sessionApi'
import { useAuth } from '../../auth/useAuth'
import { DishIcon } from '../../components/DishIcon'
import { MoneyText } from '../../components/MoneyText'
import { StatusTag } from '../../components/StatusTag'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import { liveQueryOptions, refreshLiveQueries } from '../../realtime/invalidation'
import { formatTime } from '../../utils/time'
import OrderModal from './OrderModal'
import { PageHeader } from '../../components/PageHeader'
import { EmptyState } from '../../components/states/EmptyState'
import { LoadingState } from '../../components/states/LoadingState'
import { ErrorState } from '../../components/states/ErrorState'

export default function SessionPage() {
  const sessionId = Number(useParams().id)
  const validId = Number.isSafeInteger(sessionId) && sessionId > 0
  const { account } = useAuth()
  const [ordering, setOrdering] = useState(false)
  const cache = useQueryClient()
  const query = useQuery({ queryKey: ['session', sessionId], queryFn: ({ signal }) => sessionApi.detail(sessionId, signal), enabled: validId, ...liveQueryOptions })
  const { report, message } = useApiFeedback(query.error)
  const change = useMutation({ mutationFn: ({ id, action }: { id: number; action: 'cancel' | 'serve' }) => sessionApi[action](id),
    onError: (error) => report(error), onSuccess: async (_, { action }) => {
      await refreshLiveQueries(cache, { type: 'ORDER', sessionId })
      void message.success(action === 'cancel' ? 'Đã hủy món' : 'Đã xác nhận phục vụ')
    } })
  const back = <BackNavigation to="/floor-plan" label="Về sơ đồ bàn" />
  if (!validId) return <section>{back}<Result status="404" title="Mã phiên không hợp lệ" /></section>
  if (query.isPending) return <section>{back}<LoadingState /></section>
  if (!query.data) return <section>{back}<ErrorState title="Chưa tải được phiên phục vụ" onRetry={() => void query.refetch()} /></section>
  const session = query.data
  const guests = session.reservation ? ` · ${session.reservation.guestCount} khách` : ''
  return <section className="session-page">
    {back}
    <PageHeader eyebrow="Phiên phục vụ" title={`Phiên #${session.sessionId} · ${session.tableCode ?? session.tableName}`}
      description={`${session.areaName} · Mở lúc ${formatTime(session.openedAt)}${guests}`}
      actions={<Space wrap><StatusTag group="SessionStatus" value={session.status} />
        <Button type="primary" size="large" disabled={session.status !== 'ACTIVE'} onClick={() => setOrdering(true)}>{session.orders.length ? 'Gọi thêm' : 'Gọi món'}</Button></Space>} />
    <div className="session-layout">
      <div className="session-main">
        {session.orders.length === 0 ? <div className="state-card"><EmptyState title="Chưa có đợt gọi món. Chọn Gọi món để gửi bếp." /></div> : session.orders.map((order, index) => <Card key={order.id} className="food-order"
          title={`Đợt ${index + 1} · #${order.id}`} extra={<span className="muted-text">{formatTime(order.createdAt)}</span>}>
          <p className="muted-text food-order-author">Người gọi: {order.createdByName}</p>
          {order.items.map((item) => <article key={item.id} className="session-item" data-item-id={item.id}>
            <DishIcon name={item.menuItemName} />
            <div className="session-item-info">
              <Typography.Title level={3}>{item.menuItemName}</Typography.Title>
              <div className="session-item-tags"><span className="quantity-chip">× {item.quantity}</span><StatusTag group="OrderItemStatus" value={item.status} /></div>
              {item.note && <p className="session-item-note">Ghi chú: {item.note}</p>}
              {item.unavailableReason && <p className="unavailable-reason">Lý do: {item.unavailableReason}</p>}
              <Space wrap className="session-item-actions">
                {session.status === 'ACTIVE' && item.status === 'PENDING' && <Popconfirm title="Hủy món này?" description="Món đã hủy sẽ không được chế biến." okText="Hủy món" cancelText="Giữ món" onConfirm={() => change.mutate({ id: item.id, action: 'cancel' })} disabled={change.isPending}>
                  <Button danger disabled={change.isPending} loading={change.isPending && change.variables?.id === item.id}>Hủy món</Button></Popconfirm>}
                {session.status === 'ACTIVE' && account?.role === 'WAITER' && item.status === 'READY' && <Button type="primary" size="large" disabled={change.isPending} loading={change.isPending && change.variables?.id === item.id}
                  onClick={() => change.mutate({ id: item.id, action: 'serve' })}>Đã phục vụ</Button>}
              </Space>
            </div>
            <div className="session-item-price"><MoneyText amount={item.subtotal} /><small>Đơn giá: <MoneyText amount={item.unitPriceAtOrder} /></small></div>
          </article>)}
          <div className="order-total">Tạm tính đợt: <MoneyText amount={order.total} /></div>
        </Card>)}
      </div>
      <aside className="session-summary" aria-label="Tóm tắt phiên">
        <Card className="session-summary-card" title="Tóm tắt phiên">
          {session.orders.map((order, index) => <div className="summary-row" key={order.id}><span>Đợt {index + 1} · #{order.id}</span><MoneyText amount={order.total} /></div>)}
          <div className="summary-total"><span>Tạm tính</span><strong><MoneyText amount={session.total} /></strong></div>
          <p className="muted-text summary-note">Không tính món đã hủy hoặc không thể chế biến.</p>
        </Card>
        {session.reservation && <Card size="small" className="session-reservation" title="Đặt bàn"><Descriptions size="small" column={1} items={[
          { key: 'customer', label: 'Khách đặt bàn', children: session.reservation.customerName },
          { key: 'phone', label: 'Số điện thoại', children: session.reservation.customerPhone },
          { key: 'guests', label: 'Số khách', children: session.reservation.guestCount },
          { key: 'time', label: 'Thời gian đặt', children: `${formatTime(session.reservation.startTime)} – ${formatTime(session.reservation.endTime)}` },
        ]} /></Card>}
      </aside>
    </div>
    {ordering && <OrderModal sessionId={sessionId} onClose={() => setOrdering(false)} />}
  </section>
}
