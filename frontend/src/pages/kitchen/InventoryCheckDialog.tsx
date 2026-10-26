import { Alert, Button, Card, Descriptions, Modal, Space } from 'antd'
import type { IngredientAvailability, OrderQueueItem } from '../../types/sessions'

export interface InventoryCheck { item: OrderQueueItem; blocked: boolean; ingredients: IngredientAvailability[] }
export function InventoryCheckDialog({ check, busy, onClose, onContinue, onUnavailable }: {
  check: InventoryCheck | null; busy: boolean; onClose: () => void; onContinue: () => void; onUnavailable: () => void
}) {
  return <Modal title={check?.blocked ? 'Không đủ nguyên vật liệu' : 'Cảnh báo tồn kho tối thiểu'} open={!!check}
    onCancel={onClose} closable={!busy} mask={{ closable: !busy }} width={840} styles={{ body: { maxHeight: '65vh', overflowY: 'auto' } }} footer={
      <Space wrap><Button disabled={busy} onClick={onClose}>Hủy</Button>{check?.blocked
        ? <Button key="unavailable" danger disabled={busy} onClick={onUnavailable}>Không thể chế biến</Button>
        : <Button key="continue" type="primary" loading={busy} onClick={onContinue}>Vẫn tiếp tục chế biến</Button>}</Space>}>
    <Alert showIcon type={check?.blocked ? 'error' : 'warning'} title={check?.blocked
      ? 'Chưa bắt đầu chế biến. Vui lòng nhập thêm nguyên liệu hoặc báo không thể chế biến.'
      : 'Chế biến món này sẽ khiến tồn kho của một hoặc nhiều nguyên vật liệu xuống dưới mức tồn kho tối thiểu.'} />
    {check?.ingredients.map((line) => <Card key={line.ingredientId} size="small" title={`${line.ingredientName} (${line.unit})`} style={{ marginTop: 16 }}>
      <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }} items={[
        { key: 'stock', label: 'Tồn sổ', children: line.currentStock },
        { key: 'held', label: 'Đang giữ', children: line.reservedStock },
        { key: 'available', label: 'Khả dụng', children: line.availableStock },
        { key: 'required', label: 'Cần dùng', children: line.requiredQuantity },
        { key: 'projected', label: 'Còn lại dự kiến', children: line.projectedStock },
        { key: 'minimum', label: 'Tối thiểu', children: line.minimumStock },
        { key: 'shortage', label: 'Thiếu', children: line.shortage > 0 ? <strong className="stock-shortage">{line.shortage}</strong> : '—' },
      ]} />
    </Card>)}
  </Modal>
}
