import { useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { inventoryApi } from '../../api/inventoryApi'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import type {
  CreateStocktakePayload,
  Stocktake,
  StocktakeItem,
} from '../../types/inventory'
import { formatTime } from '../../utils/time'
import { PageHeader } from '../../components/PageHeader'

const { Text } = Typography

interface FormItemState {
  ingredientId: number
  ingredientName: string
  unit: string
  systemQuantity: number
  actualQuantity: number | null
  note?: string
}

export default function StocktakePage() {
  const queryClient = useQueryClient()
  const { report, message } = useApiFeedback()
  const [form] = Form.useForm()

  const [overrides, setOverrides] = useState<
    Record<number, { actualQuantity?: number | null; note?: string }>
  >({})
  const [page, setPage] = useState<number>(0)
  const [pageSize, setPageSize] = useState<number>(10)

  const {
    data: template = [],
    isLoading: isTemplateLoading,
    error: templateError,
  } = useQuery({
    queryKey: ['inventory', 'stocktake-template'],
    queryFn: ({ signal }) => inventoryApi.getStocktakeTemplate(signal),
  })

  useApiFeedback(templateError)

  const {
    data: stocktakesData,
    isLoading: isHistoryLoading,
    error: historyError,
  } = useQuery({
    queryKey: ['inventory', 'stocktakes-list', { page, pageSize }],
    queryFn: ({ signal }) => inventoryApi.listStocktakes({ page, size: pageSize }, signal),
  })

  useApiFeedback(historyError)

  const items: FormItemState[] = template.map((t) => ({
    ingredientId: t.ingredientId,
    ingredientName: t.ingredientName,
    unit: t.unit,
    systemQuantity: Number(t.currentStock),
    actualQuantity: overrides[t.ingredientId]?.actualQuantity ?? null,
    note: overrides[t.ingredientId]?.note ?? '',
  }))
  const payloadItems = items.flatMap((item) =>
    item.actualQuantity == null ? [] : [{
      ingredientId: item.ingredientId,
      actualQuantity: item.actualQuantity,
      note: item.note,
    }]
  )

  const createStocktakeMutation = useMutation({
    mutationFn: (payload: CreateStocktakePayload) => inventoryApi.createStocktake(payload),
    onSuccess: (stocktake) => {
      void message.success(`Tạo thành công phiếu kiểm kê #${stocktake.id}`)
      setOverrides({})
      void queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
    onError: (err) => report(err, form),
  })
  const cannotSubmit = payloadItems.length === 0 || createStocktakeMutation.isPending

  const handleActualChange = (ingredientId: number, val: number | null) => {
    setOverrides((prev) => ({
      ...prev,
      [ingredientId]: { ...prev[ingredientId], actualQuantity: val },
    }))
  }

  const handleNoteChange = (ingredientId: number, val: string) => {
    setOverrides((prev) => ({
      ...prev,
      [ingredientId]: { ...prev[ingredientId], note: val },
    }))
  }

  const templateColumns: ColumnsType<FormItemState> = [
    {
      title: 'Tên nguyên vật liệu',
      dataIndex: 'ingredientName',
      key: 'ingredientName',
      width: 200,
      render: (name: string, record) => (
        <Space orientation="vertical" size={0}>
          <Text strong>{name}</Text>
          <Text type="secondary" className="text-small">ĐVT: {record.unit}</Text>
        </Space>
      ),
    },
    {
      title: 'Tồn hệ thống tạm tính',
      dataIndex: 'systemQuantity',
      key: 'systemQuantity',
      width: 170,
      align: 'right',
      render: (sys: number, record) => (
        <Text type="secondary">
          {Number(sys).toFixed(3)} {record.unit}
        </Text>
      ),
    },
    {
      title: 'Số lượng thực tế kiểm kê',
      dataIndex: 'actualQuantity',
      key: 'actualQuantity',
      width: 200,
      render: (_, record) => (
        <Space className="full-width" orientation="vertical" size={4}>
        <InputNumber
          aria-label={`Số lượng thực tế ${record.ingredientName}`}
          placeholder="Chưa nhập số thực đếm"
          disabled={createStocktakeMutation.isPending}
          min={0}
          precision={3}
          className="number-input full-width"
          value={record.actualQuantity}
          onChange={(val) => handleActualChange(record.ingredientId, val)}
        />
        <Tooltip title={record.systemQuantity < 0 ? 'Tồn hệ thống đang âm. Vui lòng nhập số thực đếm không âm.' : 'Chỉ bấm sau khi đã đếm và thấy khớp với số tồn đang hiển thị.'}>
          <Button type="link" size="small" className="link-button-compact"
            aria-label={`Khớp tồn hệ thống ${record.ingredientName}`}
            disabled={createStocktakeMutation.isPending || record.systemQuantity < 0}
            onClick={() => handleActualChange(record.ingredientId, record.systemQuantity)}>
            Khớp tồn hệ thống
          </Button>
        </Tooltip>
        </Space>
      ),
    },
    {
      title: 'Chênh lệch tạm',
      key: 'diff',
      width: 150,
      align: 'right',
      render: (_, record) => {
        if (record.actualQuantity == null) return <Text type="secondary">Chưa kiểm kê</Text>
        const diff = record.actualQuantity - record.systemQuantity
        if (Math.abs(diff) < 0.0001) {
          return <span className="difference-zero">0.000 {record.unit}</span>
        }
        const isPositive = diff > 0
        return (
          <span className={isPositive ? 'difference-positive quantity-strong' : 'difference-negative quantity-strong'}>
            {isPositive ? `+${diff.toFixed(3)}` : diff.toFixed(3)} {record.unit}
          </span>
        )
      },
    },
    {
      title: 'Ghi chú',
      dataIndex: 'note',
      key: 'note',
      width: 240,
      render: (_, record) => (
        <Input
          aria-label={`Ghi chú ${record.ingredientName}`}
          disabled={createStocktakeMutation.isPending}
          placeholder="Lý do chênh lệch (nếu có)"
          maxLength={500}
          value={record.note}
          onChange={(e) => handleNoteChange(record.ingredientId, e.target.value)}
        />
      ),
    },
  ]

  const historyColumns: ColumnsType<Stocktake> = [
    {
      title: 'Mã phiếu',
      dataIndex: 'id',
      key: 'id',
      width: 100,
      render: (id: number) => <strong className="invoice-code">#{id}</strong>,
    },
    {
      title: 'Thời gian kiểm kê',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (t: string) => formatTime(t),
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'createdByName',
      key: 'createdByName',
      width: 180,
    },
    {
      title: 'Số lượng mặt hàng',
      key: 'itemCount',
      render: (_, record) => {
        const adjustedCount = record.items.filter((i) => Math.abs(Number(i.difference)) > 0.0001).length
        return (
          <Space>
            <span>{record.items.length} mặt hàng</span>
            {adjustedCount > 0 ? (
              <Tag className="status-tag status-tag-busy">{adjustedCount} mặt hàng có chênh lệch</Tag>
            ) : (
              <Tag className="status-tag status-tag-free">Khớp hoàn toàn</Tag>
            )}
          </Space>
        )
      },
    },
  ]

  return (
    <div className="stocktake-page">
      <PageHeader eyebrow="Quản lý kho" title="Kiểm kê" description="Đối chiếu số lượng thực tế với tồn hệ thống và ghi nhận chênh lệch." />
      <Space className="full-width" direction="vertical" size="large">
        <Card className="section-card" title="Thực hiện kiểm kê kho nguyên vật liệu">
          <Alert
            type="info"
            showIcon
            message="Nhập kết quả thực đếm"
            description="Chỉ lưu các nguyên vật liệu đã nhập số lượng thực tế. Nếu đã đếm và thấy khớp, bấm Khớp tồn hệ thống ở dòng đó. Để trống để bỏ qua; nhập 0 khi không còn hàng. Tồn sổ bao gồm lượng đã giữ cho món đang nấu; số thực đếm không được thấp hơn lượng đang giữ. Chênh lệch được tính lại tại thời điểm xác nhận."
            className="section-alert"
          />

          <Table
            rowKey="ingredientId"
            columns={templateColumns}
            dataSource={items}
            loading={isTemplateLoading}
            pagination={false}
            scroll={{ x: 960 }}
          />

          <div className="sticky-form-footer">
            <Text type="secondary">Đã nhập {payloadItems.length} / {items.length} nguyên vật liệu</Text>
            <Popconfirm
              disabled={cannotSubmit}
              title="Xác nhận lưu kết quả kiểm kê"
              description={`Lưu kết quả thực đếm của ${payloadItems.length} nguyên vật liệu và điều chỉnh tồn kho nếu có chênh lệch?`}
              onConfirm={() => {
                if (cannotSubmit) return
                createStocktakeMutation.mutate({ items: payloadItems })
              }}
              okText="Xác nhận kiểm kê"
              cancelText="Hủy"
            >
              <Button
                type="primary"
                size="large"
                loading={createStocktakeMutation.isPending}
                disabled={cannotSubmit}
              >
                Xác nhận & Lưu phiếu kiểm kê
              </Button>
            </Popconfirm>
          </div>
        </Card>

        <Card className="section-card" title="Lịch sử các đợt kiểm kê">
          <Table
            rowKey="id"
            scroll={{ x: 760 }}
            columns={historyColumns}
            dataSource={stocktakesData?.items ?? []}
            loading={isHistoryLoading}
            expandable={{
              expandedRowRender: (record) => (
                <Table<StocktakeItem>
                  size="small"
                  scroll={{ x: 650 }}
                  rowKey="id"
                  pagination={false}
                  dataSource={record.items}
                  columns={[
                    {
                      title: 'Nguyên vật liệu',
                      dataIndex: 'ingredientName',
                      key: 'ingredientName',
                    },
                    {
                      title: 'Tồn hệ thống',
                      dataIndex: 'systemQuantity',
                      key: 'systemQuantity',
                      align: 'right',
                      render: (sys: number, item) => (
                        <span>
                          {Number(sys).toFixed(3)} {item.unit}
                        </span>
                      ),
                    },
                    {
                      title: 'Thực tế kiểm kê',
                      dataIndex: 'actualQuantity',
                      key: 'actualQuantity',
                      align: 'right',
                      render: (act: number, item) => (
                        <span>
                          {Number(act).toFixed(3)} {item.unit}
                        </span>
                      ),
                    },
                    {
                      title: 'Chênh lệch',
                      dataIndex: 'difference',
                      key: 'difference',
                      align: 'right',
                      render: (diff: number, item) => {
                        const val = Number(diff)
                        const isZero = Math.abs(val) < 0.0001
                        return (
                          <span className={isZero ? 'difference-zero' : val > 0 ? 'difference-positive quantity-strong' : 'difference-negative quantity-strong'}>
                            {val > 0 ? `+${val.toFixed(3)}` : val.toFixed(3)} {item.unit}
                          </span>
                        )
                      },
                    },
                    {
                      title: 'Ghi chú',
                      dataIndex: 'note',
                      key: 'note',
                      render: (note?: string) => note || '—',
                    },
                  ]}
                />
              ),
            }}
            pagination={{
              current: page + 1,
              pageSize,
              total: stocktakesData?.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '50'],
              showTotal: (total) => `Tổng số ${total} đợt kiểm kê`,
              onChange: (p, s) => {
                setPage(p - 1)
                setPageSize(s)
              },
            }}
          />
        </Card>
      </Space>
    </div>
  )
}
