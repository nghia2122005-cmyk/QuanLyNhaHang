import { Button, Form, Input, InputNumber, Modal, Select, Spin } from 'antd'
import { MinusOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { menuApi } from '../../api/menuApi'
import { sessionApi } from '../../api/sessionApi'
import { DishIcon } from '../../components/DishIcon'
import { MoneyText } from '../../components/MoneyText'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import { refreshLiveQueries } from '../../realtime/invalidation'
import type { OrderLine } from '../../types/sessions'
import { MAX_ORDER_LINES, MAX_ORDER_QUANTITY, orderLinesError } from '../../utils/orderLimits'
import { EmptyState } from '../../components/states/EmptyState'

type DraftLine = Partial<OrderLine> | undefined
const fold = (text: string) => text.normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/g, 'd').toLowerCase()

export default function OrderModal({ sessionId, onClose }: { sessionId: number; onClose: () => void }) {
  const [form] = Form.useForm<{ items: OrderLine[] }>()
  const [search, setSearch] = useState('')
  const cache = useQueryClient()
  const menu = useQuery({ queryKey: ['menu-items', { status: 'ON_SALE' }], queryFn: ({ signal }) => menuApi.list({ status: 'ON_SALE' }, signal) })
  const { report, message } = useApiFeedback(menu.error)
  const save = useMutation({ mutationFn: (values: { items: OrderLine[] }) => sessionApi.order(sessionId, values.items),
    onError: (error) => report(error, form), onSuccess: async () => {
      await refreshLiveQueries(cache, { type: 'ORDER', sessionId })
      void message.success('Đã gửi đợt gọi món đến bếp'); onClose()
    } })
  const lines: DraftLine[] = Form.useWatch('items', form) ?? []
  const quantity = (line: DraftLine) => Number(line?.quantity) || 0
  const quantityOf = (menuItemId: number) => lines.filter((line) => line?.menuItemId === menuItemId).reduce((sum, line) => sum + quantity(line), 0)
  const chosen = lines.filter((line) => line?.menuItemId != null)
  const portions = chosen.reduce((sum, line) => sum + quantity(line), 0)
  const dishes = (menu.data ?? []).filter((item) => fold(item.name).includes(fold(search.trim())))

  // Quick picks only edit the existing order lines: bump a plain line of that dish, fill an empty line, or append one.
  const addDish = (menuItemId: number) => {
    const items: DraftLine[] = form.getFieldValue('items') ?? []
    const same = items.findIndex((line) => line?.menuItemId === menuItemId && !line?.note)
    if (same >= 0) {
      if (quantity(items[same]) < MAX_ORDER_QUANTITY) form.setFieldValue(['items', same, 'quantity'], quantity(items[same]) + 1)
      return
    }
    const empty = items.findIndex((line) => line?.menuItemId == null)
    if (empty >= 0) form.setFieldValue(['items', empty], { ...items[empty], menuItemId, quantity: items[empty]?.quantity ?? 1 })
    else if (items.length < MAX_ORDER_LINES) form.setFieldValue('items', [...items, { menuItemId, quantity: 1 }])
  }
  const removeDish = (menuItemId: number) => {
    const items: DraftLine[] = form.getFieldValue('items') ?? []
    let last = -1
    items.forEach((line, index) => { if (line?.menuItemId === menuItemId) last = index })
    if (last < 0) return
    if (quantity(items[last]) > 1) form.setFieldValue(['items', last, 'quantity'], quantity(items[last]) - 1)
    else form.setFieldValue('items', items.filter((_, index) => index !== last))
  }

  return <Modal className="order-modal" rootClassName="floor-dialog" title="Gọi món / gọi thêm" open onCancel={onClose} width={1000} style={{ top: 16 }} mask={{ closable: !save.isPending }} closable={!save.isPending}
    styles={{ body: { maxHeight: 'calc(100vh - 260px)', overflowY: 'auto' } }}
    footer={<div className="order-modal-footer">
      <div className="order-modal-summary"><strong>{portions} phần · {chosen.length} dòng</strong></div>
      <div className="order-modal-buttons"><Button onClick={onClose} disabled={save.isPending}>Đóng</Button>
        <Button type="primary" loading={save.isPending} disabled={!menu.data?.length} onClick={() => form.submit()}>Gửi bếp</Button></div>
    </div>}>
    {menu.isPending ? <Spin /> : <Form form={form} layout="vertical" initialValues={{ items: [{ quantity: 1 }] }}
      onFinish={(values) => save.mutate(values)} disabled={save.isPending || !!menu.error}>
      {menu.data?.length === 0 && <EmptyState compact title="Chưa có món đang kinh doanh" />}
      {!!menu.data?.length && <section className="dish-picker" aria-label="Chọn nhanh món">
        <Input allowClear prefix={<SearchOutlined aria-hidden />} placeholder="Tìm món theo tên" aria-label="Tìm món" value={search} onChange={(event) => setSearch(event.target.value)} />
        {dishes.length === 0 ? <EmptyState compact title="Không tìm thấy món phù hợp." /> : <div className="dish-grid">{dishes.map((item) => {
          const count = quantityOf(item.id)
          return <div key={item.id} className={count ? 'dish-card dish-card-selected' : 'dish-card'}>
            <DishIcon name={item.name} />
            <div className="dish-card-info"><strong>{item.name}</strong><MoneyText amount={item.price} /></div>
            <div className="dish-stepper">
              <Button shape="circle" icon={<MinusOutlined aria-hidden />} aria-label={`Bớt ${item.name}`} disabled={!count} onClick={() => removeDish(item.id)} />
              <span className="dish-count">{count}</span>
              <Button shape="circle" type="primary" icon={<PlusOutlined aria-hidden />} aria-label={`Thêm ${item.name}`} onClick={() => addDish(item.id)} />
            </div>
          </div>
        })}</div>}
      </section>}
      <h3 className="order-lines-title">Món đã chọn</h3>
      <Form.List name="items" rules={[{ validator: async (_, lines) => { const error = orderLinesError(lines?.length ?? 0); if (error) throw new Error(error) } }]}>{(fields, { add, remove }, { errors }) => <>
        {fields.map(({ key, name }) => <div className="order-form-line" key={key}>
          <div className="order-form-choice">
            <Form.Item name={[name, 'menuItemId']} label={`Món ${name + 1}`} rules={[{ required: true, message: 'Chọn món' }]}>
              <Select showSearch optionFilterProp="label" placeholder="Chọn món đang kinh doanh" options={menu.data?.map((item) => ({ value: item.id, label: item.name,
                title: item.name, content: <>{item.name} · <MoneyText amount={item.price} /></> }))}
                optionRender={(option) => option.data.content} />
            </Form.Item>
            <Form.Item name={[name, 'quantity']} label="Số lượng" rules={[{ required: true, message: 'Nhập số lượng' }, { type: 'integer', min: 1, max: MAX_ORDER_QUANTITY, message: `Nhập số nguyên từ 1 đến ${MAX_ORDER_QUANTITY}` }]}>
              <InputNumber className="full-width" min={1} max={MAX_ORDER_QUANTITY} precision={0} />
            </Form.Item>
          </div>
          <Form.Item name={[name, 'note']} label="Ghi chú cho bếp" rules={[{ max: 500, message: 'Ghi chú tối đa 500 ký tự' }]}><Input.TextArea maxLength={500} autoSize={{ minRows: 1, maxRows: 3 }} /></Form.Item>
          <Button danger onClick={() => remove(name)} aria-label={`Bỏ món ${name + 1}`}>Bỏ dòng</Button>
        </div>)}
        <Form.ErrorList errors={errors} /><Button disabled={save.isPending || fields.length >= MAX_ORDER_LINES} icon={<PlusOutlined aria-hidden />} onClick={() => add({ quantity: 1 })}>Thêm món</Button>
      </>}</Form.List>
    </Form>}
  </Modal>
}
