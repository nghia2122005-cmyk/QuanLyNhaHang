import { Button, Card, Drawer, Form, Input, Modal, Progress, Space, Typography } from 'antd'
import { ClockCircleOutlined, ReloadOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { useState } from 'react'
import { kitchenApi } from '../../api/kitchenApi'
import { OrderQueueCard, WaitingChip } from '../../components/OrderQueueCard'
import { ChefHatIcon } from '../../assets/icons/ChefHatIcon'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import { liveQueryOptions, refreshLiveQueries } from '../../realtime/invalidation'
import type { IngredientAvailability, OrderQueueItem } from '../../types/sessions'
import { formatTime } from '../../utils/time'
import { groupKitchenQueue } from '../../utils/kitchenQueue'
import { InventoryCheckDialog } from './InventoryCheckDialog'
import type { InventoryCheck } from './InventoryCheckDialog'
import { PageHeader } from '../../components/PageHeader'
import { EmptyState } from '../../components/states/EmptyState'
import { LoadingState } from '../../components/states/LoadingState'
import { ErrorState } from '../../components/states/ErrorState'

export default function KitchenPage() {
  const cache = useQueryClient()
  const queue = useQuery({ queryKey: ['kitchen'], queryFn: ({ signal }) => kitchenApi.queue(signal), ...liveQueryOptions })
  const [selectedOrder, setSelectedOrder] = useState<number | null>(null)
  const detail = useQuery({ queryKey: ['kitchen', 'order', selectedOrder], queryFn: ({ signal }) => kitchenApi.batch(selectedOrder!, signal), enabled: selectedOrder != null, ...liveQueryOptions })
  const { report, message } = useApiFeedback(queue.error)
  const [unavailable, setUnavailable] = useState<OrderQueueItem | null>(null)
  const [check, setCheck] = useState<InventoryCheck | null>(null)
  const [form] = Form.useForm<{ reason: string }>()
  const start = useMutation({ mutationFn: ({ item, confirm }: { item: OrderQueueItem; confirm: boolean }) => kitchenApi.start(item.id, confirm),
    onError: (error, { item }) => {
      if (axios.isAxiosError<{ ingredients?: IngredientAvailability[]; code?: string }>(error) && error.response?.data.code === 'INSUFFICIENT_STOCK' && error.response.data.ingredients)
        setCheck({ item, blocked: true, ingredients: error.response.data.ingredients })
      else report(error)
      void refreshLiveQueries(cache, { type: 'ORDER', sessionId: item.sessionId })
    }, onSuccess: async (result, { item }) => {
      if (!result.started) { setCheck({ item, blocked: false, ingredients: result.ingredients }); return }
      setCheck(null)
      await refreshLiveQueries(cache, { type: 'ORDER', sessionId: item.sessionId })
      void message.success('Đã bắt đầu chế biến và giữ nguyên liệu')
    } })
  const change = useMutation({ mutationFn: ({ item, action, reason }: { item: OrderQueueItem; action: 'complete' | 'unavailable'; reason?: string }) =>
    action === 'unavailable' ? kitchenApi.unavailable(item.id, reason ?? '') : kitchenApi.complete(item.id),
    onError: (error) => report(error, form), onSuccess: async (_, { item }) => {
      setUnavailable(null)
      await refreshLiveQueries(cache, { type: 'ORDER', sessionId: item.sessionId })
      void message.success('Đã cập nhật trạng thái món')
    } })
  const busy = start.isPending || change.isPending
  const batches = groupKitchenQueue(queue.data ?? [])
  const showUnavailable = (item: OrderQueueItem, reason = '') => { form.setFieldsValue({ reason }); setUnavailable(item) }
  return <section className="kitchen-page">
    <PageHeader icon={<ChefHatIcon />} title="Bếp" description="Mỗi đợt gọi món là một đơn. Các món được xử lý độc lập."
      actions={<Button icon={<ReloadOutlined aria-hidden />} loading={queue.isFetching} onClick={() => void queue.refetch()}>Làm mới</Button>} />
    {queue.isError && <ErrorState title="Chưa tải được hàng đợi bếp" onRetry={() => void queue.refetch()} />}
    {queue.isPending ? <LoadingState variant="cards" count={2} /> : <div className="queue-columns">
      {([{ key: 'pending', title: 'Chờ chế biến' }, { key: 'cooking', title: 'Đang chế biến' }] as const).map(({ key, title }) =>
        <section key={key} aria-label={title} data-queue-status={key} className="queue-column">
          <div className="queue-column-heading"><i className={`queue-column-dot queue-column-dot-${key}`} aria-hidden="true" /><Typography.Title level={2}>{title}</Typography.Title><span className="queue-column-count">{batches[key].length} đợt</span></div>
          {!batches[key].length && <EmptyState compact title={`Chưa có đợt ${title.toLowerCase()}`} />}
          {batches[key].map(({ orderId, items, pendingCount, cookingCount }) =>
            <Card key={orderId} data-order-id={orderId} className="queue-card kitchen-batch" title={`${items[0].tableCode ?? items[0].tableName} · Đợt #${orderId}`} extra={<WaitingChip since={items[0].orderedAt} />}>
              <p className="queue-meta"><ClockCircleOutlined aria-hidden /> {formatTime(items[0].orderedAt)} · Phiên #{items[0].sessionId}</p>
              <p>{pendingCount} chờ chế biến · {cookingCount} đang chế biến <small>(dòng món)</small></p>
              {cookingCount > 0 && <div className="batch-progress">
                <Typography.Text type="secondary">Đã bắt đầu {cookingCount}/{items.length} dòng trong hàng đợi</Typography.Text>
                <Progress percent={Math.round(cookingCount / items.length * 100)} status="active" showInfo={false} />
              </div>}
              <ul className="kitchen-batch-items">{items.map((item) => <li key={item.id}><strong className="kitchen-batch-quantity">{item.quantity}×</strong><span>{item.menuItemName}</span></li>)}</ul>
              <Button type="primary" size="large" block onClick={() => setSelectedOrder(orderId)}>Xem đợt #{orderId}</Button>
            </Card>)}
        </section>)}
    </div>}
    <Drawer rootClassName="floor-dialog wide-drawer" title={`Chi tiết đợt #${selectedOrder ?? ''}`} open={selectedOrder != null} onClose={() => { if (!busy) setSelectedOrder(null) }} width={760} closable={!busy} mask={{ closable: !busy }}>
      {detail.data && <Typography.Paragraph>Đã xử lý {detail.data.filter((item) => !['PENDING', 'COOKING'].includes(item.status)).length}/{detail.data.length} dòng của đợt (hoàn thành, đã phục vụ, không thể chế biến hoặc đã hủy).</Typography.Paragraph>}
      {detail.isPending ? <LoadingState /> : detail.isError ? <ErrorState title="Chưa tải được đợt gọi món" onRetry={() => void detail.refetch()} /> : detail.data?.map((item) => <OrderQueueCard key={item.id} item={item}>
        {item.status === 'PENDING' && <Button type="primary" size="large" disabled={busy} onClick={() => start.mutate({ item, confirm: false })}>Bắt đầu</Button>}
        {item.status === 'COOKING' && <Button type="primary" size="large" disabled={busy} onClick={() => change.mutate({ item, action: 'complete' })}>Hoàn thành</Button>}
        {(item.status === 'PENDING' || item.status === 'COOKING') && <Button danger size="large" disabled={busy} onClick={() => showUnavailable(item)}>Không thể chế biến</Button>}
      </OrderQueueCard>)}
    </Drawer>
    <InventoryCheckDialog check={check} busy={busy} onClose={() => { if (!busy) setCheck(null) }} onContinue={() => { if (check) start.mutate({ item: check.item, confirm: true }) }}
      onUnavailable={() => { if (check) { showUnavailable(check.item, 'Hết nguyên liệu'); setCheck(null) } }} />
    <Modal title={`Không thể chế biến · ${unavailable?.menuItemName ?? ''}`} open={!!unavailable} onCancel={() => setUnavailable(null)} footer={null} closable={!busy} mask={{ closable: !busy }}>
      <Form form={form} layout="vertical" disabled={busy} onFinish={({ reason }) => { if (unavailable) change.mutate({ item: unavailable, action: 'unavailable', reason }) }}>
        <Form.Item name="reason" label="Lý do cần báo khách" rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập lý do không thể chế biến' }, { max: 500, message: 'Lý do tối đa 500 ký tự' }]}>
          <Input.TextArea maxLength={500} showCount rows={3} /></Form.Item>
        <Space className="form-actions"><Button disabled={busy} onClick={() => setUnavailable(null)}>Đóng</Button><Button danger type="primary" htmlType="submit" loading={busy}>Xác nhận</Button></Space>
      </Form>
    </Modal>
  </section>
}
