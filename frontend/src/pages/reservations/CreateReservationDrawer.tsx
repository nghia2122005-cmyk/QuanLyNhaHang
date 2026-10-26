import { Button, DatePicker, Drawer, Form, Input, InputNumber, Radio, Spin, Steps, Typography } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { Dayjs } from 'dayjs'
import axios from 'axios'
import { useState } from 'react'
import { reservationApi } from '../../api/reservationApi'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import type { CreateReservation, Reservation } from '../../types/tables'
import type { ApiError } from '../../types/auth'
import { formatTime, nowInAppZone, toInstant, instantForPicker } from '../../utils/time'
import { EmptyState } from '../../components/states/EmptyState'

interface ReservationForm { customerName: string; customerPhone: string; guestCount: number; startTime: Dayjs; endTime: Dayjs; note?: string }
function ChooseTable({ values, back, onClose, onFailure, reservation }: { values: Omit<CreateReservation, 'tableId'>; back: () => void; onClose: () => void; onFailure: (error: unknown) => void; reservation?: Reservation }) {
  const [tableId, setTableId] = useState<number | undefined>(reservation?.tableId)
  const cache = useQueryClient()
  const params = { startTime: values.startTime, endTime: values.endTime, guestCount: values.guestCount, excludedId: reservation?.id }
  const query = useQuery({ queryKey: ['available-tables', params], queryFn: ({ signal }) => reservationApi.available(params, signal), refetchOnWindowFocus: true })
  const { message } = useApiFeedback(query.error)
  const save = useMutation({ mutationFn: () => reservation ? reservationApi.update(reservation.id, { ...values, tableId: tableId! }) : reservationApi.create({ ...values, tableId: tableId! }),
    onSuccess: async () => { await Promise.all([cache.invalidateQueries({ queryKey: ['reservations'] }), cache.invalidateQueries({ queryKey: ['floor-plan'] })]); void message.success(reservation ? 'Đã cập nhật đặt bàn' : 'Đã tạo đặt bàn'); onClose() },
    onError: (error) => { onFailure(error); setTableId(undefined); void query.refetch() } })
  return <>
    <div className="reservation-summary"><strong>{values.customerName} · {values.guestCount} khách</strong><span>{formatTime(values.startTime)} – {formatTime(values.endTime)}</span></div>
    {query.isPending ? <Spin /> : query.data?.length ? <Radio.Group aria-label="Bàn phù hợp" className="available-table-list" value={tableId} onChange={(event) => setTableId(event.target.value)} disabled={save.isPending}>
      {query.data.map((table) => <Radio key={table.id} value={table.id}><strong>{table.code ?? table.name} · {table.name}</strong><span>{table.areaName} · {table.capacity} khách</span></Radio>)}
    </Radio.Group> : <EmptyState compact title="Chưa có bàn phù hợp trong khung giờ này. Thử đổi giờ hoặc số khách." />}
    <div className="form-actions-stack"><Button type="primary" size="large" block disabled={!tableId || !query.data?.some((table) => table.id === tableId)} loading={save.isPending} onClick={() => save.mutate()}>Lưu đặt bàn</Button><Button block disabled={save.isPending} onClick={back}>Quay lại</Button></div>
  </>
}

export default function CreateReservationDrawer({ onClose, reservation }: { onClose: () => void; reservation?: Reservation }) {
  const [form] = Form.useForm<ReservationForm>()
  const { report } = useApiFeedback()
  const [values, setValues] = useState<Omit<CreateReservation, 'tableId'> | null>(null)
  const start = nowInAppZone().add(1, 'hour').startOf('hour')
  return <Drawer open rootClassName="floor-dialog wide-drawer" title={reservation ? "Sửa đặt bàn" : "Tạo đặt bàn"} width={720} onClose={onClose}>
    <Steps size="small" current={values ? 1 : 0} items={[{ title: 'Thông tin khách' }, { title: 'Chọn bàn' }]} />
    <Typography.Paragraph className="page-note">Thời gian theo giờ Việt Nam.</Typography.Paragraph>
    {values && <ChooseTable reservation={reservation} values={values} back={() => setValues(null)} onClose={onClose} onFailure={(error) => {
      report(error, form)
      if (axios.isAxiosError<ApiError>(error) && Object.keys(error.response?.data.fieldErrors ?? {}).length) setValues(null)
    }} />}
    <Form form={form} layout="vertical" style={{ display: values ? 'none' : undefined }} initialValues={reservation ? { ...reservation, startTime: instantForPicker(reservation.startTime), endTime: instantForPicker(reservation.endTime) } : { guestCount: 2, startTime: start, endTime: start.add(2, 'hour') }}
      onFinish={(value) => setValues({ ...value, startTime: toInstant(value.startTime), endTime: toInstant(value.endTime) })}>
      <h3 className="form-section-title">Thông tin khách</h3>
      <div className="form-two-columns"><Form.Item name="customerName" label="Tên khách" rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập tên khách' }, { max: 100 }]}><Input maxLength={100} /></Form.Item>
      <Form.Item name="customerPhone" label="Số điện thoại" rules={[{ required: true, message: 'Vui lòng nhập số điện thoại' }, { pattern: /^0\d{9}$/, message: 'Nhập 10 chữ số, bắt đầu bằng 0' }]}><Input type="tel" maxLength={15} /></Form.Item></div>
      <Form.Item name="guestCount" label="Số khách" rules={[{ required: true, message: 'Vui lòng nhập số khách' }, { type: 'number', min: 1 }]}><InputNumber className="full-width" min={1} max={2147483647} precision={0} /></Form.Item>
      <h3 className="form-section-title">Thời gian</h3>
      <div className="form-two-columns"><Form.Item name="startTime" label="Bắt đầu" rules={[{ required: true, message: 'Chọn giờ bắt đầu' }]}><DatePicker className="full-width" showTime={{ format: 'HH:mm' }} format="DD/MM/YYYY HH:mm" /></Form.Item>
        <Form.Item name="endTime" label="Kết thúc" dependencies={['startTime']} rules={[{ required: true, message: 'Chọn giờ kết thúc' }, ({ getFieldValue }) => ({ validator: (_, end: Dayjs) => !end || !getFieldValue('startTime') || end.isAfter(getFieldValue('startTime')) ? Promise.resolve() : Promise.reject(new Error('Giờ kết thúc phải sau giờ bắt đầu')) })]}><DatePicker className="full-width" showTime={{ format: 'HH:mm' }} format="DD/MM/YYYY HH:mm" /></Form.Item></div>
      <Form.Item name="note" label="Ghi chú" rules={[{ max: 500 }]}><Input.TextArea rows={3} maxLength={500} showCount /></Form.Item>
      <div className="form-actions-stack"><Button type="primary" size="large" block htmlType="submit">Tìm bàn phù hợp</Button><Button block onClick={onClose}>Hủy</Button></div>
    </Form>
  </Drawer>
}
