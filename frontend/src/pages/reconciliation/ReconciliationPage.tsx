import { useState } from 'react'
import { Alert, Button, Card, DatePicker, Descriptions, Form, Input, InputNumber, Popconfirm, Select, Space, Table, Tag, Typography } from 'antd'
import { ArrowDownOutlined, ArrowUpOutlined, CheckCircleOutlined, ReloadOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { Dayjs } from 'dayjs'
import type { ColumnsType } from 'antd/es/table'
import { reconciliationApi } from '../../api/reconciliationApi'
import { useAuth } from '../../auth/useAuth'
import { MoneyText } from '../../components/MoneyText'
import { RevenueStats } from '../../components/RevenueStats'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import type { CreateReconciliationPayload, Reconciliation } from '../../types/reconciliation'
import { formatTime, toInstant } from '../../utils/time'
import { initialReconciliationRange, isFutureReconciliationDate, reconciliationDisabledTime } from '../../utils/reconciliationTime'
import { PageHeader } from '../../components/PageHeader'
import { EmptyState } from '../../components/states/EmptyState'
import { ErrorState } from '../../components/states/ErrorState'
import { LoadingState } from '../../components/states/LoadingState'

const differenceIcon = { positive: <ArrowUpOutlined aria-hidden />, negative: <ArrowDownOutlined aria-hidden />, zero: <CheckCircleOutlined aria-hidden /> }

type ActualForm = Pick<CreateReconciliationPayload, 'actualAmount' | 'differenceReason'>

export default function ReconciliationPage() {
  const { account } = useAuth()
  const isCashier = account?.role === 'CASHIER'
  const cache = useQueryClient()
  const { report, message } = useApiFeedback()
  const [form] = Form.useForm<ActualForm>()
  const actual = Form.useWatch('actualAmount', form)
  const [range, setRange] = useState<[Dayjs, Dayjs]>(initialReconciliationRange)
  const [reviewOpen, setReviewOpen] = useState(false)
  const [filter, setFilter] = useState(isCashier ? 'all' : 'pending')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const from = toInstant(range[0])
  const to = toInstant(range[1])
  const validRange = from < to
  const preview = useQuery({
    queryKey: ['reconciliation-preview', { from, to }],
    queryFn: ({ signal }) => reconciliationApi.preview({ from, to }, signal),
    enabled: isCashier && validRange,
  })
  const list = useQuery({
    queryKey: ['reconciliations', { filter, page, size }],
    queryFn: ({ signal }) => reconciliationApi.list({ confirmed: filter === 'all' ? undefined : filter === 'confirmed', page, size }, signal),
    refetchInterval: 30_000,
  })
  useApiFeedback(preview.error)
  useApiFeedback(list.error)
  const refresh = () => {
    void cache.invalidateQueries({ queryKey: ['reconciliations'] })
    void cache.invalidateQueries({ queryKey: ['reconciliation-preview'] })
  }
  const create = useMutation({
    mutationFn: reconciliationApi.create,
    onSuccess: () => {
      void message.success('Đã lập đối soát, chờ quản lý xác nhận')
      form.resetFields()
      setReviewOpen(false)
      refresh()
    },
    onError: (error) => { report(error, form); refresh() },
  })
  const confirm = useMutation({
    mutationFn: reconciliationApi.confirm,
    onSuccess: () => { void message.success('Đã xác nhận đối soát'); refresh() },
    onError: (error) => { report(error); refresh() },
  })
  // Spec §12.18 step 3: the UI shows actual − system revenue as a preview; the server recomputes revenue on save.
  const difference = actual != null && preview.data && !preview.isError ? actual - preview.data.systemRevenue : null
  const differenceTone = difference == null || difference === 0 ? 'zero' : difference > 0 ? 'positive' : 'negative'
  const columns: ColumnsType<Reconciliation> = [
    { title: 'Mã / Thu ngân', key: 'cashier', render: (_, row) => <Space orientation="vertical" size={0}><b>#{row.id}</b>{row.cashierName}</Space> },
    { title: 'Khoảng đối soát', key: 'range', render: (_, row) => <Space orientation="vertical" size={0}><span>Từ {formatTime(row.startTime)}</span><span>Đến trước {formatTime(row.endTime)}</span></Space> },
    { title: 'Hệ thống', dataIndex: 'systemRevenue', align: 'right', render: (value: number) => <MoneyText amount={value} /> },
    { title: 'Thực nhận', dataIndex: 'actualAmount', align: 'right', render: (value: number) => <MoneyText amount={value} /> },
    { title: 'Chênh lệch', dataIndex: 'difference', align: 'right', render: (value: number) => <strong className={`difference-${value === 0 ? 'zero' : value > 0 ? 'positive' : 'negative'}`}><MoneyText amount={value} /></strong> },
    { title: 'Xác nhận', key: 'confirmed', render: (_, row) => <Space orientation="vertical" size={0}><Tag className={`status-tag ${row.confirmed ? 'status-tag-free' : 'status-tag-busy'}`}>{row.confirmed ? 'Đã xác nhận' : 'Chưa xác nhận'}</Tag>{row.managerName}</Space> },
    ...(!isCashier ? [{ title: 'Thao tác', key: 'actions', render: (_: unknown, row: Reconciliation) => !row.confirmed && (
      <Popconfirm title={`Xác nhận đối soát #${row.id}?`} description="Số liệu đã lập sẽ được giữ nguyên."
        okText="Xác nhận" cancelText="Hủy" disabled={confirm.isPending}
        onConfirm={() => confirm.mutateAsync(row.id).catch(() => {})}>
        <Button disabled={confirm.isPending} loading={confirm.isPending && confirm.variables === row.id}>Xác nhận</Button>
      </Popconfirm>
    ) }] : []),
  ]
  return (
    <Space className="full-width" orientation="vertical" size={24}>
      <PageHeader title="Đối soát cuối ca" />
      {isCashier && <Card className="section-card" title="Lập đối soát">
        <Space className="full-width" orientation="vertical" size={20}>
          <div><label htmlFor="reconciliation-period">Khoảng thời gian (giờ Việt Nam)</label><br />
            <DatePicker.RangePicker id={{ start: 'reconciliation-period', end: 'reconciliation-period-end' }} showTime={{ format: 'HH:mm' }} format="DD/MM/YYYY HH:mm"
              value={range} allowClear={false} disabled={create.isPending}
              disabledDate={(date) => isFutureReconciliationDate(date)} disabledTime={(date) => reconciliationDisabledTime(date)}
              onChange={(value) => { if (value?.[0] && value[1]) { setRange([value[0], value[1]]); setReviewOpen(false); form.resetFields() } }} />
          </div>
          <Typography.Text type="secondary">Tính hóa đơn từ thời điểm bắt đầu đến trước thời điểm kết thúc. Tổng thực nhận gồm tiền mặt và chuyển khoản.</Typography.Text>
          {!validRange && <Alert type="warning" title="Thời điểm bắt đầu phải trước thời điểm kết thúc" />}
          {preview.isError ? <ErrorState title="Không tải được doanh thu" onRetry={() => void preview.refetch()} />
            : preview.isFetching && !preview.data ? <LoadingState variant="cards" count={4} />
            : preview.data && <div className="report-summary"><RevenueStats data={preview.data} />{preview.data.invoiceCount === 0 && <EmptyState compact title="Không có dữ liệu hóa đơn trong khoảng này." />}</div>}
          <div className="reconciliation-layout">
            <Form form={form} layout="vertical" className="reconciliation-entry" disabled={create.isPending} onValuesChange={() => setReviewOpen(false)} scrollToFirstError>
              <h3 className="form-section-title">Số tiền thực nhận</h3>
              <Form.Item name="actualAmount" label="Tổng thực nhận (đ)" rules={[{ required: true, message: 'Vui lòng nhập tổng thực nhận' }, { type: 'integer', min: 0, max: 99999999999999, message: 'Nhập số nguyên không âm, tối đa 14 chữ số' }]}>
                <InputNumber className="amount-input full-width" min={0} max={99999999999999} />
              </Form.Item>
              <Form.Item name="differenceReason" label="Lý do chênh lệch (không bắt buộc)" rules={[{ max: 500, message: 'Lý do không quá 500 ký tự' }]}><Input.TextArea maxLength={500} showCount rows={3} /></Form.Item>
              <Popconfirm title="Lập đối soát với số thực nhận đã nhập?" description="Doanh thu được tính lại khi lưu. Phiếu đã lập không thể sửa."
                open={reviewOpen} okText="Lập đối soát" cancelText="Hủy" okButtonProps={{ loading: create.isPending }}
                onOpenChange={(open) => { if (!open) setReviewOpen(false); else void form.validateFields().then(() => setReviewOpen(true)).catch(() => {}) }}
                onConfirm={async () => { const values = await form.validateFields(); await create.mutateAsync({ ...values, startTime: from, endTime: to }).catch(() => {}) }}>
                <Button type="primary" size="large" block loading={create.isPending} disabled={!validRange || preview.isFetching || !preview.data || preview.isError}>Lập đối soát</Button>
              </Popconfirm>
            </Form>
            <aside className="reconciliation-compare" aria-label="So sánh doanh thu và thực nhận">
              <div className="compare-row"><span>Doanh thu hệ thống</span><strong>{preview.data && !preview.isError ? <MoneyText amount={preview.data.systemRevenue} /> : '—'}</strong></div>
              <div className="compare-row"><span>Thực nhận</span><strong>{actual != null ? <MoneyText amount={actual} /> : '—'}</strong></div>
              <div className={`compare-row compare-difference compare-${differenceTone}`}>
                <span>Chênh lệch tạm tính</span>
                <strong>{difference != null ? <>{differenceIcon[differenceTone]}<MoneyText amount={difference} /></> : '—'}</strong>
              </div>
              <p className="muted-text">Chênh lệch = thực nhận − doanh thu hệ thống. Doanh thu được tính lại khi lưu.</p>
            </aside>
          </div>
        </Space>
      </Card>}
      <Card className="section-card" title="Danh sách đối soát" extra={<Button icon={<ReloadOutlined aria-hidden />} onClick={refresh}>Làm mới</Button>}>
        <Space className="mb-16"><label htmlFor="reconciliation-status">Xác nhận</label>
          <Select id="reconciliation-status" value={filter} style={{ width: 180 }} onChange={(value) => { setFilter(value); setPage(0) }}
            options={[{ value: 'all', label: 'Tất cả' }, { value: 'pending', label: 'Chưa xác nhận' }, { value: 'confirmed', label: 'Đã xác nhận' }]} />
        </Space>
        {list.isError && <ErrorState compact title="Không tải được danh sách đối soát" onRetry={() => void list.refetch()} />}
        <Table rowKey="id" columns={columns} dataSource={list.data?.items ?? []} loading={list.isFetching} scroll={{ x: 950 }}
          expandable={{ expandedRowRender: (row) => <Descriptions column={1} items={[
            { key: 'time', label: 'Lập lúc', children: formatTime(row.reconciledAt) },
            { key: 'reason', label: 'Lý do chênh lệch', children: row.differenceReason || '—' },
            { key: 'manager', label: 'Quản lý', children: row.managerName || 'Chưa xác nhận' },
          ]} /> }}
          pagination={{ current: page + 1, pageSize: size, total: list.data?.total ?? 0, showSizeChanger: true, onChange: (p, s) => { setPage(p - 1); setSize(s) } }} />
      </Card>
    </Space>
  )
}
