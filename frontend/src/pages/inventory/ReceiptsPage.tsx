import { useState } from 'react'
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { inventoryApi } from '../../api/inventoryApi'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import type { StockReceipt } from '../../types/inventory'
import type { Dayjs } from 'dayjs'
import { formatTime } from '../../utils/time'
import { PageHeader } from '../../components/PageHeader'

const { Text } = Typography

type ReceiptFormValues = {
  items: { ingredientId: number; quantity: number; lotNumber: string; expiryDate?: Dayjs }[]
}

export default function ReceiptsPage() {
  const queryClient = useQueryClient()
  const { report, message } = useApiFeedback()
  const [form] = Form.useForm<ReceiptFormValues>()
  const watchedItems = Form.useWatch('items', form)

  const [page, setPage] = useState<number>(0)
  const [pageSize, setPageSize] = useState<number>(10)

  const { data: ingredients = [] } = useQuery({
    queryKey: ['inventory', 'ingredients-active'],
    queryFn: ({ signal }) => inventoryApi.listIngredients({ status: 'ACTIVE' }, signal),
  })

  const {
    data: receiptsData,
    isLoading: isReceiptsLoading,
    error: receiptsError,
  } = useQuery({
    queryKey: ['inventory', 'receipts-list', { page, pageSize }],
    queryFn: ({ signal }) => inventoryApi.listReceipts({ page, size: pageSize }, signal),
  })

  useApiFeedback(receiptsError)

  const createReceiptMutation = useMutation({
    mutationFn: (payload: ReceiptFormValues) => {
      // Map expiryDate from Dayjs to string YYYY-MM-DD
      const formattedItems = payload.items.map((item) => ({
        ingredientId: item.ingredientId,
        quantity: item.quantity,
        lotNumber: item.lotNumber.trim(),
        expiryDate: item.expiryDate ? item.expiryDate.format('YYYY-MM-DD') : undefined,
      }))
      return inventoryApi.createReceipt({ items: formattedItems })
    },
    onSuccess: (receipt) => {
      void message.success(`Tạo thành công phiếu nhập kho #${receipt.id}`)
      form.resetFields()
      void queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
    onError: (err) => report(err, form),
  })

  const columns: ColumnsType<StockReceipt> = [
    {
      title: 'Mã phiếu',
      dataIndex: 'id',
      key: 'id',
      width: 100,
      render: (id: number) => <strong className="invoice-code">#{id}</strong>,
    },
    {
      title: 'Thời gian lập',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (t: string) => formatTime(t),
    },
    {
      title: 'Người lập',
      dataIndex: 'createdByName',
      key: 'createdByName',
      width: 180,
    },
    {
      title: 'Chi tiết nguyên vật liệu',
      key: 'items',
      render: (_, record) => (
        <Space wrap size={[4, 8]}>
          {record.items.map((item) => (
            <Tag key={item.id} className="table-chip">
              {item.ingredientName}: {Number(item.quantity).toFixed(3)} {item.unit}
              {item.lotNumber && ` (Lô: ${item.lotNumber})`}
              {item.expiryDate && ` - HSD: ${item.expiryDate}`}
            </Tag>
          ))}
        </Space>
      ),
    },
  ]

  return (
    <div className="receipts-page">
      <PageHeader eyebrow="Quản lý kho" title="Nhập nguyên vật liệu" description="Ghi nhận nguyên vật liệu đã kiểm tra và chấp nhận nhập kho." />
      <Space className="full-width" direction="vertical" size="large">
        <Card className="section-card" title="Lập phiếu nhập kho nguyên vật liệu">
          <Form
            form={form}
            layout="vertical"
            requiredMark={false}
            initialValues={{ items: [{}] }}
            onFinish={(values) => createReceiptMutation.mutate(values)}
          >
            <div className="receipt-line receipt-line-head" aria-hidden="true">
              <span>#</span><span>Nguyên vật liệu</span><span>Số lượng nhập</span><span>Số lô</span><span>Hạn sử dụng</span><span />
            </div>
            <Form.List name="items">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name, ...restField }, index) => {
                    const needsExpiry = ingredients.find((ingredient) => ingredient.id === watchedItems?.[name]?.ingredientId)?.trackExpiry ?? false
                    return (
                      <div key={key} className="receipt-line">
                        <span className="receipt-line-number">{index + 1}</span>
                        <Form.Item
                          {...restField}
                          name={[name, 'ingredientId']}
                          label={<span className="receipt-field-label">Nguyên vật liệu</span>}
                          rules={[{ required: true, message: 'Chọn nguyên vật liệu' }]}
                        >
                          <Select
                            showSearch
                            optionFilterProp="label"
                            placeholder="Chọn nguyên vật liệu"
                            options={ingredients.map((i) => ({
                              label: `${i.name} (${i.unit})`,
                              value: i.id,
                            }))}
                          />
                        </Form.Item>

                        <Form.Item
                          {...restField}
                          name={[name, 'quantity']}
                          label={<span className="receipt-field-label">Số lượng nhập</span>}
                          rules={[
                            { required: true, message: 'Nhập số lượng' },
                            { type: 'number', min: 0.001, message: 'Số lượng > 0' },
                          ]}
                        >
                          <InputNumber className="number-input" min={0.001} precision={3} />
                        </Form.Item>

                        <Form.Item
                          {...restField}
                          name={[name, 'lotNumber']}
                          label={<span className="receipt-field-label">Số lô</span>}
                          rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập số lô' },
                            { max: 50, message: 'Số lô không vượt quá 50 ký tự' }]}
                        >
                          <Input placeholder="Số lô SX" maxLength={50} />
                        </Form.Item>

                        <Form.Item
                          {...restField}
                          name={[name, 'expiryDate']}
                          label={<span className="receipt-field-label">Hạn sử dụng{needsExpiry && ' (bắt buộc)'}</span>}
                          dependencies={[["items", name, "ingredientId"]]}
                          rules={[{ required: needsExpiry, message: 'Nguyên vật liệu này bắt buộc nhập hạn sử dụng' }]}
                        >
                          <DatePicker format="DD/MM/YYYY" placeholder="Chọn ngày" />
                        </Form.Item>

                        {fields.length > 1 ? (
                          <Button
                            type="text"
                            danger
                            icon={<MinusCircleOutlined aria-hidden />}
                            aria-label={`Xóa dòng ${index + 1}`}
                            onClick={() => remove(name)}
                          />
                        ) : <span />}
                      </div>
                    )
                  })}

                  <Button
                    type="dashed"
                    className="add-line-button"
                    onClick={() => add()}
                    block
                    icon={<PlusOutlined aria-hidden />}
                  >
                    Thêm dòng nguyên vật liệu
                  </Button>
                </>
              )}
            </Form.List>

            <div className="sticky-form-footer">
              <Text type="secondary">Tồn kho được cộng ngay sau khi lưu phiếu.</Text>
              <Popconfirm
                title="Xác nhận nhập kho"
                description="Bạn có chắc chắn muốn tạo phiếu nhập này không? Số lượng tồn kho sẽ được cộng tăng ngay sau khi lưu."
                onConfirm={() => form.submit()}
                okText="Xác nhận"
                cancelText="Hủy"
              >
                <Button
                  type="primary"
                  size="large"
                  loading={createReceiptMutation.isPending}
                >
                  Hoàn tất & Lưu phiếu nhập
                </Button>
              </Popconfirm>
            </div>
          </Form>
        </Card>

        <Card className="section-card" title="Lịch sử phiếu nhập kho">
          <Table
            rowKey="id"
            columns={columns}
            dataSource={receiptsData?.items ?? []}
            loading={isReceiptsLoading}
            scroll={{ x: 760 }}
            pagination={{
              current: page + 1,
              pageSize,
              total: receiptsData?.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '50'],
              showTotal: (total) => `Tổng số ${total} phiếu nhập`,
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
