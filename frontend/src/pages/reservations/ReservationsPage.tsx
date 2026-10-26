import { Button, DatePicker, Input, Popconfirm, Select, Space, Table, Tag, Typography } from 'antd'
import { EditOutlined, PhoneOutlined, PlusOutlined, SearchOutlined, UserOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { reservationApi } from '../../api/reservationApi'
import { useAuth } from '../../auth/useAuth'
import { StatusTag } from '../../components/StatusTag'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import type { Reservation, ReservationFilters } from '../../types/tables'
import { statusLabels } from '../../utils/labels'
import { formatTime, nowInAppZone } from '../../utils/time'
import CreateReservationDrawer from './CreateReservationDrawer'
import { weekdayDate } from '../../utils/dateLabels'

// formatTime gives "DD/MM/YYYY HH:mm" in Vietnam time; the slot tile shows the hour and the date separately.
const timeOf = (instant: string) => formatTime(instant).slice(11)
const dateOf = (instant: string) => formatTime(instant).slice(0, 10)
import { PageHeader } from '../../components/PageHeader'

export default function ReservationsPage() {
  const { account } = useAuth()
  const manager = account?.role === 'MANAGER'
  const [filters, setFilters] = useState<ReservationFilters>({ date: nowInAppZone().format('YYYY-MM-DD'), page: 0, size: 20 })
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Reservation | undefined>()
  const cache = useQueryClient()
  const query = useQuery({ queryKey: ['reservations', filters], queryFn: ({ signal }) => reservationApi.list(filters, signal), refetchInterval: 30_000, refetchOnWindowFocus: true })
  const { report, message } = useApiFeedback(query.error)
  const cancel = useMutation({ mutationFn: reservationApi.cancel, onError: (error) => report(error), onSuccess: async () => {
    await Promise.all([cache.invalidateQueries({ queryKey: ['reservations'] }), cache.invalidateQueries({ queryKey: ['floor-plan'] })]); void message.success('Đã hủy đặt bàn')
  } })
  return <section>
    <PageHeader eyebrow="Lịch đặt bàn" title="Đặt bàn" description="Theo dõi lịch hẹn và chuẩn bị đón khách."
      actions={manager && <Button type="primary" size="large" icon={<PlusOutlined aria-hidden />} onClick={() => { setEditing(undefined); setOpen(true) }}>Tạo đặt bàn</Button>} />
    <div className="filter-card filter-bar"><DatePicker aria-label="Ngày đặt bàn" format="DD/MM/YYYY" defaultValue={nowInAppZone()} onChange={(date) => setFilters((old) => ({ ...old, date: date?.format('YYYY-MM-DD'), page: 0 }))} />
      <Select aria-label="Trạng thái đặt bàn" allowClear placeholder="Tất cả trạng thái" style={{ minWidth: 180 }} options={Object.entries(statusLabels.ReservationStatus).map(([value, label]) => ({ value, label }))}
        onChange={(status) => setFilters((old) => ({ ...old, status, page: 0 }))} />
      <Input.Search aria-label="Tìm đặt bàn" enterButton={<Button icon={<SearchOutlined aria-hidden />} aria-label="Tìm kiếm" />} placeholder="Tên hoặc số điện thoại khách" allowClear className="reservation-search" onSearch={(q) => setFilters((old) => ({ ...old, q: q || undefined, page: 0 }))} /></div>
    <div className="day-heading"><h2>{filters.date ? weekdayDate(filters.date) : 'Tất cả các ngày'}</h2><span>{query.data?.total ?? 0} đặt bàn</span></div>
    <div className="data-panel"><Table<Reservation> className="reservation-table" rowKey="id" dataSource={query.data?.items ?? []} loading={query.isPending} scroll={{ x: 1000 }}
      pagination={{ current: filters.page + 1, pageSize: filters.size, total: query.data?.total ?? 0, showSizeChanger: true, pageSizeOptions: [20, 50, 100],
        showTotal: (total) => `${total} đặt bàn`, onChange: (page, size) => setFilters((old) => ({ ...old, page: size !== old.size ? 0 : page - 1, size })) }} columns={[
        { title: 'Thời gian', render: (_, row) => <div className="time-slot"><strong>{timeOf(row.startTime)}</strong><span>{dateOf(row.startTime)}</span><span>đến {timeOf(row.endTime)}</span></div> },
        { title: 'Khách hàng', render: (_, row) => <div className="cell-stack"><strong>{row.customerName}</strong><span><PhoneOutlined aria-hidden /> {row.customerPhone}</span>{row.note && <Typography.Text type="secondary">{row.note}</Typography.Text>}</div> },
        { title: 'Bàn', render: (_, row) => <span className="table-chip">{row.tableCode ?? row.tableName} · {row.tableName}</span> },
        { title: 'Số khách', dataIndex: 'guestCount', render: (count: number) => <span className="text-nowrap"><UserOutlined aria-hidden /> {count} khách</span> },
        { title: 'Trạng thái', render: (_, row) => <Space wrap><StatusTag group="ReservationStatus" value={row.status} />{row.overdue && <Tag color="warning">Quá giờ</Tag>}</Space> },
        ...(manager ? [{ title: 'Thao tác', render: (_: unknown, row: Reservation) => row.status === 'CONFIRMED' && <Space><Button icon={<EditOutlined aria-hidden />} onClick={() => { setEditing(row); setOpen(true) }}>Sửa đặt bàn</Button><Popconfirm title="Hủy đặt bàn?" description="Lịch đặt bàn vẫn được lưu trong lịch sử." okText="Hủy đặt bàn" cancelText="Giữ lại" onConfirm={() => cancel.mutateAsync(row.id).catch(() => undefined)}>
          <Button danger disabled={cancel.isPending}>Hủy đặt bàn</Button></Popconfirm></Space> }] : []),
      ]} /></div>
    {open && <CreateReservationDrawer key={editing?.id ?? 'new'} reservation={editing} onClose={() => setOpen(false)} />}
  </section>
}
