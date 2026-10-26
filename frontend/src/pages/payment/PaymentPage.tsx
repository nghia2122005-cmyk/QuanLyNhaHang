import { useState } from 'react'
import {
  Alert,
  Badge,
  Button,
  Card,
  Drawer,
  Radio,
  Popconfirm,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import { BankOutlined, CheckCircleFilled, WalletOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { paymentApi } from '../../api/paymentApi'
import { MoneyText } from '../../components/MoneyText'
import { StatusTag } from '../../components/StatusTag'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import { liveQueryOptions } from '../../realtime/invalidation'
import type {
  OpenSessionPayment,
  PaymentMethod,
} from '../../types/payment'
import { formatTime } from '../../utils/time'
import { EmptyState } from '../../components/states/EmptyState'
import { PageHeader } from '../../components/PageHeader'

const { Title, Text } = Typography

const paymentMethods = [
  { value: 'CASH', label: 'Tiền mặt', icon: <WalletOutlined aria-hidden /> },
  { value: 'BANK_TRANSFER', label: 'Chuyển khoản', icon: <BankOutlined aria-hidden /> },
] as const

export default function PaymentPage() {
  const queryClient = useQueryClient()
  const { report, message } = useApiFeedback()

  const [selectedSessionId, setSelectedSessionId] = useState<number | null>(null)
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH')

  const {
    data: sessions = [],
    isLoading,
    isRefetching,
    error,
  } = useQuery({
    queryKey: ['payment', 'open-sessions'],
    queryFn: ({ signal }) => paymentApi.listOpenSessions(signal),
    ...liveQueryOptions,
  })

  useApiFeedback(error)

  const {
    data: preview,
    isLoading: isPreviewLoading,
    error: previewError,
  } = useQuery({
    queryKey: ['payment', 'preview', selectedSessionId],
    queryFn: ({ signal }) =>
      selectedSessionId ? paymentApi.getPreview(selectedSessionId, signal) : null,
    enabled: selectedSessionId != null,
    ...liveQueryOptions,
  })

  useApiFeedback(previewError)

  const payMutation = useMutation({
    mutationFn: (sessionId: number) =>
      paymentApi.pay({ sessionId, paymentMethod }),
    onSuccess: (invoice) => {
      void message.success(
        `Thanh toán thành công cho bàn ${invoice.tableName} (${invoice.paymentMethod === 'CASH' ? 'Tiền mặt' : 'Chuyển khoản'})`
      )
      setSelectedSessionId(null)
      void queryClient.invalidateQueries({ queryKey: ['payment'] })
      void queryClient.invalidateQueries({ queryKey: ['invoices'] })
      void queryClient.invalidateQueries({ queryKey: ['floor-plan'] })
      void queryClient.invalidateQueries({ queryKey: ['tables'] })
    },
    onError: (err) => {
      report(err)
      void queryClient.invalidateQueries({ queryKey: ['payment'] })
    },
  })

  const columns: ColumnsType<OpenSessionPayment> = [
    {
      title: 'Bàn',
      dataIndex: 'tableName',
      key: 'tableName',
      render: (tableName: string, record) => (
        <Space orientation="vertical" size={0}>
          <Text strong className="payment-table-code">
            {record.tableCode ?? tableName}
          </Text>
          {record.areaName && <Text type="secondary">{record.areaName}</Text>}
        </Space>
      ),
    },
    {
      title: 'Giờ mở',
      dataIndex: 'openedAt',
      key: 'openedAt',
      render: (openedAt: string) => formatTime(openedAt),
    },
    {
      title: 'Số món',
      dataIndex: 'totalItems',
      key: 'totalItems',
      align: 'center',
    },
    {
      title: 'Tạm tính',
      dataIndex: 'provisionalTotal',
      key: 'provisionalTotal',
      align: 'right',
      render: (amount: number) => <MoneyText amount={amount} />,
    },
    {
      title: 'Trạng thái món',
      key: 'status',
      render: (_, record) =>
        record.hasBlockingItems ? (
          <Tag color="gold">Còn món chưa xong</Tag>
        ) : (
          <Tag color="green">Sẵn sàng thanh toán</Tag>
        ),
    },
    {
      title: 'Hành động',
      key: 'action',
      align: 'right',
      render: (_, record) => (
        <Button
          type="primary"
          onClick={() => {
            setSelectedSessionId(record.sessionId)
            setPaymentMethod('CASH')
          }}
        >
          Xem & Thanh toán
        </Button>
      ),
    },
  ]

  return (
    <div className="payment-page">
      <PageHeader eyebrow="Thu ngân" title="Thanh toán" description="Chọn bàn đang phục vụ để xem trước hóa đơn và xác nhận thanh toán." />
      <Card
        className="section-card"
        title={
          <Space>
            <Title level={4} style={{ margin: 0 }}>
              Danh sách bàn cần thanh toán
            </Title>
            {isRefetching && <Badge status="processing" text="Đang đồng bộ..." />}
          </Space>
        }
        extra={<Text type="secondary">Cập nhật tự động mỗi 30 giây</Text>}
      >
        <Table
          rowKey="sessionId"
          columns={columns}
          dataSource={sessions}
          loading={isLoading}
          scroll={{ x: 760 }}
          locale={{
            emptyText: (
              <EmptyState compact title="Hiện không có bàn nào đang hoạt động cần thanh toán" />
            ),
          }}
          pagination={false}
        />
      </Card>

      <Drawer
        rootClassName="floor-dialog wide-drawer"
        title={
          preview ? (
            <Space>
              <span>Thanh toán bàn {preview.tableCode ?? preview.tableName}</span>
              {preview.areaName && <Tag>{preview.areaName}</Tag>}
            </Space>
          ) : (
            'Xem trước hóa đơn'
          )
        }
        size={1120}
        open={selectedSessionId != null}
        onClose={() => setSelectedSessionId(null)}
        loading={isPreviewLoading}
      >
        {preview && (
          <div className="payment-layout">
            <div className="payment-invoice">
              {preview.hasBlockingItems && (
                <Alert
                  type="warning"
                  showIcon
                  message="Chưa thể thanh toán phiên này"
                  description={
                    <div>
                      <p className="payment-blocking-intro">
                        Còn <strong>{preview.blockingItems.length}</strong> món chưa phục vụ (chờ chế biến, đang nấu hoặc hoàn thành).
                        Vui lòng xử lý và xác nhận đã phục vụ; món không thể chế biến hoặc đã hủy không chặn thanh toán:
                      </p>
                      <ul className="payment-blocking-list">
                        {preview.blockingItems.map((item) => (
                          <li key={item.id}>
                            <strong>{item.menuItemName}</strong> x {item.quantity} (
                            <StatusTag group="OrderItemStatus" value={item.status} />)
                          </li>
                        ))}
                      </ul>
                    </div>
                  }
                />
              )}

              <Card size="small" title="Thông tin phiên">
                <Space wrap split={<Text type="secondary">|</Text>}>
                  <Text>
                    Bàn: <strong>{preview.tableCode ?? preview.tableName}</strong>
                  </Text>
                  {preview.areaName && <Text>Khu vực: {preview.areaName}</Text>}
                  <Text>Giờ mở: {formatTime(preview.openedAt)}</Text>
                </Space>
              </Card>

              <Card size="small" title="Chi tiết các đợt gọi món">
                {preview.orders.length === 0 ? (
                  <EmptyState compact title="Chưa có món nào được gọi" />
                ) : (
                  preview.orders.map((order, orderIdx) => (
                    <div key={order.orderId} className="payment-order">
                      <Text type="secondary" className="payment-order-meta">
                        Đợt #{orderIdx + 1} • {formatTime(order.createdAt)} • Người gọi:{' '}
                        {order.createdByName}
                      </Text>
                      <Table
                        size="small"
                        rowKey="id"
                        pagination={false}
                        dataSource={order.items}
                        scroll={{ x: 560 }}
                        columns={[
                          {
                            title: 'Tên món',
                            dataIndex: 'menuItemName',
                            key: 'menuItemName',
                            render: (name: string, item) => (
                              <Space orientation="vertical" size={0}>
                                <Text
                                  delete={
                                    item.status === 'CANCELLED' || item.status === 'UNAVAILABLE'
                                  }
                                >
                                  {name}
                                </Text>
                                {item.note && <Text type="secondary" className="payment-item-detail">Ghi chú: {item.note}</Text>}
                                {item.unavailableReason && (
                                  <Text type="danger" className="payment-item-detail">Lý do: {item.unavailableReason}</Text>
                                )}
                              </Space>
                            ),
                          },
                          {
                            title: 'SL',
                            dataIndex: 'quantity',
                            key: 'quantity',
                            width: 50,
                            align: 'center',
                          },
                          {
                            title: 'Đơn giá',
                            dataIndex: 'unitPriceAtOrder',
                            key: 'unitPriceAtOrder',
                            width: 110,
                            align: 'right',
                            render: (price: number) => <MoneyText amount={price} />,
                          },
                          {
                            title: 'Trạng thái',
                            dataIndex: 'status',
                            key: 'status',
                            width: 140,
                            render: (status: string) => (
                              <StatusTag group="OrderItemStatus" value={status} />
                            ),
                          },
                          {
                            title: 'Thành tiền',
                            dataIndex: 'subtotal',
                            key: 'subtotal',
                            width: 120,
                            align: 'right',
                            render: (subtotal: number, item) => (
                              <span
                                className={
                                  item.status === 'CANCELLED' || item.status === 'UNAVAILABLE'
                                    ? 'payment-amount payment-amount-void'
                                    : 'payment-amount'
                                }
                              >
                                <MoneyText amount={subtotal} />
                              </span>
                            ),
                          },
                        ]}
                      />
                    </div>
                  ))
                )}
              </Card>
            </div>

            <aside className="payment-panel" aria-label="Thanh toán">
              <Card className="payment-panel-card">
                <div className="payment-total">
                  <span>Tổng cần thu</span>
                  <strong><MoneyText amount={preview.provisionalTotal} /></strong>
                </div>
                <div className="payment-method-label" id="payment-method-label">Phương thức thanh toán</div>
                <Radio.Group
                  className="payment-methods"
                  aria-labelledby="payment-method-label"
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                >
                  {paymentMethods.map((method) => (
                    <Radio key={method.value} value={method.value} className="payment-method">
                      <span className="payment-method-icon">{method.icon}</span>
                      <span className="payment-method-name">{method.label}</span>
                      {paymentMethod === method.value && <CheckCircleFilled className="payment-method-check" aria-hidden />}
                    </Radio>
                  ))}
                </Radio.Group>
                <Popconfirm
                  title="Xác nhận thanh toán"
                  description={
                    <div>
                      <p>
                        Xác nhận đã nhận đủ số tiền{' '}
                        <strong>
                          <MoneyText amount={preview.provisionalTotal} />
                        </strong>{' '}
                        qua hình thức{' '}
                        <strong>
                          {paymentMethod === 'CASH' ? 'Tiền mặt' : 'Chuyển khoản'}
                        </strong>
                        ?
                      </p>
                      <p className="payment-confirm-warning">
                        Bàn {preview.tableCode ?? preview.tableName} sẽ được trả về trạng thái Trống và phiên phục vụ sẽ đóng.
                      </p>
                    </div>
                  }
                  onConfirm={() => selectedSessionId && payMutation.mutate(selectedSessionId)}
                  okText="Xác nhận"
                  cancelText="Hủy"
                  disabled={preview.hasBlockingItems || payMutation.isPending}
                >
                  <Button
                    type="primary"
                    block
                    className="payment-submit"
                    disabled={preview.hasBlockingItems}
                    loading={payMutation.isPending}
                  >
                    Xác nhận thanh toán
                  </Button>
                </Popconfirm>
                <Button block className="payment-close" onClick={() => setSelectedSessionId(null)}>Đóng</Button>
              </Card>
            </aside>
          </div>
        )}
      </Drawer>
    </div>
  )
}
