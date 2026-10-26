import { Alert, App, Button, Modal, Radio, Select, Space, Typography } from 'antd'
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { reservationApi } from '../../api/reservationApi'
import { tableApi } from '../../api/tableApi'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import type { FloorTable } from '../../types/tables'
import { formatTime } from '../../utils/time'

export default function OpenSessionModal({ table, onClose }: { table: FloorTable; onClose: () => void }) {
  const [mode, setMode] = useState<'walk-in' | 'reservation'>('walk-in')
  const [reservationId, setReservationId] = useState<number>()
  const [confirmed, setConfirmed] = useState(false)
  const { modal } = App.useApp()
  const cache = useQueryClient()
  const navigate = useNavigate()
  const query = useInfiniteQuery({
    queryKey: ['reservations', { status: 'CONFIRMED', tableId: table.id, unexpired: true }],
    enabled: mode === 'reservation',
    initialPageParam: 0,
    queryFn: ({ signal, pageParam }) => reservationApi.list({ status: 'CONFIRMED', tableId: table.id, unexpired: true, page: pageParam, size: 100 }, signal),
    getNextPageParam: (lastPage) => (lastPage.page + 1) * lastPage.size < lastPage.total ? lastPage.page + 1 : undefined,
  })
  const reservations = query.data?.pages.flatMap((page) => page.items) ?? []
  const { report, message } = useApiFeedback(mode === 'reservation' ? query.error : null)
  const open = useMutation({ mutationFn: () => tableApi.open(table.id, mode === 'reservation' ? reservationId : undefined),
    onSuccess: async (session) => {
      await Promise.all([cache.invalidateQueries({ queryKey: ['floor-plan'] }), cache.invalidateQueries({ queryKey: ['reservations'] })])
      void message.success('Đã mở phiên phục vụ'); onClose(); navigate(`/sessions/${session.sessionId}`)
    }, onError: (error) => { report(error); void cache.invalidateQueries({ queryKey: ['floor-plan'] }) } })
  const submit = () => {
    if (mode === 'reservation' && (query.isPending || query.error || !reservationId)) return
    if (mode === 'walk-in' && table.upcomingReservation && !confirmed) {
      modal.confirm({ title: 'Bàn có khách đặt trước', content: `${table.upcomingReservation.customerName} đặt bàn lúc ${formatTime(table.upcomingReservation.startTime)}. Bạn vẫn muốn nhận khách vãng lai?`,
        okText: 'Vẫn mở phiên', cancelText: 'Quay lại', onOk: () => { setConfirmed(true); open.mutate() } })
    } else open.mutate()
  }
  return <Modal rootClassName="floor-dialog" title={`Mở phiên · ${table.name}`} open onCancel={onClose} onOk={submit} okText="Mở phiên" cancelText="Hủy"
    confirmLoading={open.isPending} closable={!open.isPending} maskClosable={!open.isPending}
    okButtonProps={{ disabled: mode === 'reservation' && (query.isPending || !!query.error || !reservationId) }} cancelButtonProps={{ disabled: open.isPending }}>
    <Typography.Paragraph>Sức chứa {table.capacity} khách</Typography.Paragraph>
    {table.upcomingReservation && <Alert type="warning" showIcon title="Có đặt trước" description={`${table.upcomingReservation.customerName} · ${formatTime(table.upcomingReservation.startTime)}`} />}
    <Space orientation="vertical" style={{ width: '100%', marginTop: 20 }} size="middle">
      <Radio.Group value={mode} disabled={open.isPending} onChange={(event) => setMode(event.target.value)} options={[{ label: 'Khách vãng lai (Walk-in)', value: 'walk-in' }, { label: 'Khách đặt trước', value: 'reservation' }]} />
      {mode === 'reservation' && <Select className="full-width" aria-label="Đặt bàn để nhận khách" placeholder="Chọn đặt bàn đã xác nhận" value={reservationId} loading={query.isPending} disabled={open.isPending}
        onChange={setReservationId} options={reservations.map((entry) => ({ value: entry.id, label: `${entry.customerName} · ${formatTime(entry.startTime)} · ${entry.guestCount} khách` }))} />}
      {mode === 'reservation' && query.hasNextPage && <Button loading={query.isFetchingNextPage} onClick={() => { void query.fetchNextPage() }}>Tải thêm đặt bàn</Button>}
      {mode === 'reservation' && query.isError && <Button loading={query.isFetching} onClick={() => { void query.refetch() }}>Thử tải lại đặt bàn</Button>}
    </Space>
  </Modal>
}
