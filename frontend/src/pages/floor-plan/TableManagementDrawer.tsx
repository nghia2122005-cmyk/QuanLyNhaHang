import { Button, Drawer, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tabs, Tooltip, Typography } from 'antd'
import { CheckCircleOutlined, EditOutlined, PlusOutlined, StopOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { tableApi } from '../../api/tableApi'
import { StatusTag } from '../../components/StatusTag'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import { liveQueryOptions, refreshLiveQueries } from '../../realtime/invalidation'
import type { ManagedArea, ManagedTable, SaveArea } from '../../types/tables'
import { LoadingState } from '../../components/states/LoadingState'
import { ErrorState } from '../../components/states/ErrorState'

type Editing = { kind: 'area'; value: ManagedArea | null } | { kind: 'table'; value: ManagedTable | null }
type FormValues = SaveArea & { areaId?: number; capacity?: number }
const hasOrders = (table: ManagedTable) => table.status === 'OCCUPIED' || table.activeSessionId != null || table.hasLiveReservation
const blockedReason = 'Có phiên đang mở hoặc đặt bàn xác nhận còn hiệu lực. Hãy xử lý xong trước khi ngừng sử dụng.'

export function TableManagementDrawer({ onClose }: { onClose: () => void }) {
  const cache = useQueryClient()
  const areas = useQuery({ queryKey: ['floor-plan', 'management', 'areas'], queryFn: ({ signal }) => tableApi.areas(signal), ...liveQueryOptions })
  const tables = useQuery({ queryKey: ['floor-plan', 'management', 'tables'], queryFn: ({ signal }) => tableApi.tables(signal), ...liveQueryOptions })
  const [editing, setEditing] = useState<Editing | null>(null)
  const [form] = Form.useForm<FormValues>()
  const { report, message } = useApiFeedback()
  const refresh = () => refreshLiveQueries(cache, { type: 'FLOOR_PLAN' })
  const save = useMutation({
    mutationFn: async (values: FormValues) => {
      if (!editing) throw new Error('Chưa chọn bàn hoặc khu vực')
      const common = { name: values.name.trim(), code: values.code.trim().toUpperCase() }
      if (editing.kind === 'area') return editing.value ? tableApi.updateArea(editing.value.id, common) : tableApi.createArea(common)
      const payload = { ...common, areaId: values.areaId!, capacity: values.capacity! }
      return editing.value ? tableApi.updateTable(editing.value.id, payload) : tableApi.createTable(payload)
    },
    onSuccess: async () => { setEditing(null); await refresh(); void message.success('Đã lưu khu vực/bàn') },
    onError: error => report(error, form),
  })
  const change = useMutation({
    mutationFn: async ({ kind, id, active }: { kind: 'area' | 'table'; id: number; active: boolean }) =>
      kind === 'area' ? tableApi.changeAreaStatus(id, active) : tableApi.changeTableStatus(id, active),
    onSuccess: async () => { await refresh(); void message.success('Đã cập nhật trạng thái sử dụng') },
    onError: error => { report(error); void refresh() },
  })
  const busy = save.isPending || change.isPending
  const edit = (entry: Editing) => {
    form.resetFields()
    form.setFieldsValue(entry.value ?? (entry.kind === 'table' ? { capacity: 4 } : {}))
    setEditing(entry)
  }
  const statusAction = (kind: 'area' | 'table', row: ManagedArea | ManagedTable, blocked: boolean) => {
    const inactive = row.status === 'INACTIVE'
    return <Tooltip title={blocked ? (inactive ? 'Hãy sử dụng lại khu vực trước khi mở lại bàn' : blockedReason) : undefined}>
      <span><Popconfirm title={inactive ? 'Sử dụng lại?' : 'Ngừng sử dụng?'}
        description={!inactive && kind === 'area' ? 'Các bàn trong khu vực sẽ tạm không nhận khách; trạng thái riêng của từng bàn được giữ nguyên.' : undefined}
        disabled={blocked || busy} okText="Xác nhận" cancelText="Hủy"
        onConfirm={() => change.mutate({ kind, id: row.id, active: inactive })}>
        <Button disabled={blocked || busy} icon={inactive ? <CheckCircleOutlined aria-hidden /> : <StopOutlined aria-hidden />} aria-label={`${inactive ? 'Sử dụng lại' : 'Ngừng sử dụng'} ${kind === 'area' ? 'khu vực' : 'bàn'} ${row.code}`}>{inactive ? 'Sử dụng lại' : 'Ngừng sử dụng'}</Button>
      </Popconfirm></span>
    </Tooltip>
  }
  const areaRows = areas.data ?? []
  const tableRows = tables.data ?? []
  const editTable = editing?.kind === 'table' ? editing.value : null
  return <Drawer className="table-management" rootClassName="floor-dialog" title="Quản lý khu vực và bàn" open onClose={() => { if (!busy) onClose() }} size={1080} closable={!busy} mask={{ closable: !busy }}>
    <Typography.Paragraph>Chỉ ngừng bàn/khu vực khi không còn phiên đang mở hoặc đặt bàn xác nhận còn hiệu lực. Dữ liệu lịch sử được giữ nguyên.</Typography.Paragraph>
    {(areas.isError || tables.isError) ? <ErrorState title="Không tải được danh sách khu vực/bàn" onRetry={() => { void areas.refetch(); void tables.refetch() }} />
      : areas.isPending || tables.isPending ? <LoadingState variant="table" /> : <Tabs items={[
        { key: 'areas', label: 'Khu vực', children: <>
          <Button type="primary" icon={<PlusOutlined aria-hidden />} disabled={busy} onClick={() => edit({ kind: 'area', value: null })}>Thêm khu vực</Button>
          <Table<ManagedArea> rowKey="id" dataSource={areaRows} scroll={{ x: 680 }} pagination={{ pageSize: 10 }} columns={[
            { title: 'Mã khu vực', dataIndex: 'code' }, { title: 'Tên khu vực', dataIndex: 'name' },
            { title: 'Trạng thái', render: (_, row) => <StatusTag group="AreaStatus" value={row.status} /> },
            { title: 'Thao tác', render: (_, row) => <Space wrap>
              <Button icon={<EditOutlined aria-hidden />} disabled={busy} aria-label={`Sửa khu vực ${row.code}`} onClick={() => edit({ kind: 'area', value: row })}>Sửa</Button>
              {statusAction('area', row, row.status !== 'INACTIVE' && tableRows.some(table => table.areaId === row.id && hasOrders(table)))}
            </Space> },
          ]} />
        </> },
        { key: 'tables', label: 'Bàn', children: <>
          <Button type="primary" icon={<PlusOutlined aria-hidden />} disabled={busy || !areaRows.some(area => area.status === 'ACTIVE')} onClick={() => edit({ kind: 'table', value: null })}>Thêm bàn</Button>
          {!areaRows.some(area => area.status === 'ACTIVE') && <Typography.Paragraph type="secondary">Cần có khu vực hoạt động để thêm bàn.</Typography.Paragraph>}
          <Table<ManagedTable> rowKey="id" dataSource={tableRows} scroll={{ x: 900 }} pagination={{ pageSize: 10 }} columns={[
            { title: 'Mã bàn', dataIndex: 'code' }, { title: 'Tên bàn', dataIndex: 'name' },
            { title: 'Khu vực', render: (_, row) => <>{row.areaName}{row.areaStatus === 'INACTIVE' && <div><StatusTag group="AreaStatus" value="INACTIVE" /></div>}</> },
            { title: 'Sức chứa', dataIndex: 'capacity' },
            { title: 'Trạng thái', render: (_, row) => <><StatusTag group="TableStatus" value={row.status} />{row.hasLiveReservation && <div>Có đặt bàn còn hiệu lực</div>}</> },
            { title: 'Thao tác', render: (_, row) => <Space wrap>
              <Button icon={<EditOutlined aria-hidden />} disabled={busy} aria-label={`Sửa bàn ${row.code}`} onClick={() => edit({ kind: 'table', value: row })}>Sửa</Button>
              {statusAction('table', row, row.status === 'INACTIVE' ? row.areaStatus === 'INACTIVE' : hasOrders(row))}
            </Space> },
          ]} />
        </> },
      ]} />}
    <Modal rootClassName="floor-dialog" title={editing ? `${editing.value ? 'Sửa' : 'Thêm'} ${editing.kind === 'area' ? 'khu vực' : 'bàn'}` : ''}
      open={editing != null} footer={null} onCancel={() => { if (!busy) setEditing(null) }} closable={!busy} mask={{ closable: !busy }}>
      <Form form={form} layout="vertical" disabled={busy} onFinish={values => save.mutate(values)}>
        <Form.Item name="name" label={editing?.kind === 'area' ? 'Tên khu vực' : 'Tên bàn'} rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập tên' }, { max: editing?.kind === 'area' ? 100 : 50, message: 'Tên vượt quá độ dài cho phép' }]}><Input /></Form.Item>
        <Form.Item name="code" label={editing?.kind === 'area' ? 'Mã khu vực' : 'Mã bàn'} rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập mã' }, { max: editing?.kind === 'area' ? 40 : 64, message: 'Mã vượt quá độ dài cho phép' }, { pattern: /^[A-Za-z0-9][A-Za-z0-9_-]*$/, message: 'Mã gồm chữ không dấu, số, gạch ngang hoặc gạch dưới' }]}>
          <Input placeholder={editing?.kind === 'area' ? 'Ví dụ: SV, T01' : 'Ví dụ: T01-B05'} />
        </Form.Item>
        {editing?.kind === 'area' && <Typography.Paragraph type="secondary">Đổi mã khu vực không tự đổi mã các bàn.</Typography.Paragraph>}
        {editing?.kind === 'table' && <>
          <Form.Item name="areaId" label="Khu vực" rules={[{ required: true, message: 'Vui lòng chọn khu vực' }]}><Select disabled={busy || Boolean(editTable && hasOrders(editTable))} options={areaRows.map(area => ({ value: area.id, label: `${area.name} (${area.code})`, disabled: area.status === 'INACTIVE' && editTable?.areaId !== area.id }))} /></Form.Item>
          <Form.Item name="capacity" label="Sức chứa" rules={[{ required: true, message: 'Vui lòng nhập sức chứa' }, { type: 'number', min: 1, message: 'Sức chứa phải lớn hơn 0' }]}><InputNumber className="full-width" min={1} max={2147483647} precision={0} /></Form.Item>
        </>}
        <Space><Button disabled={busy} onClick={() => setEditing(null)}>Hủy</Button><Button type="primary" htmlType="submit" aria-label="Lưu" aria-busy={save.isPending} loading={save.isPending}>Lưu</Button></Space>
      </Form>
    </Modal>
  </Drawer>
}
