import { Drawer, Table, Tag, Typography } from 'antd'
import { CalendarOutlined, InboxOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useQuery } from '@tanstack/react-query'
import { inventoryApi } from '../../api/inventoryApi'
import { liveQueryOptions } from '../../realtime/invalidation'
import type { Ingredient, IngredientLot } from '../../types/inventory'
import { formatExpiryDate, lotExpiryPresentation } from '../../utils/inventory'
import { EmptyState } from '../../components/states/EmptyState'
import { ErrorState } from '../../components/states/ErrorState'

// Colour follows the server-classified expiry state; the label still comes from lotExpiryPresentation.
const expiryTone: Record<IngredientLot['status'], string> = { EXPIRED: 'danger', EXPIRING_SOON: 'busy', VALID: 'free', NO_EXPIRY: 'off' }

const columns: ColumnsType<IngredientLot> = [
  { title: 'Số lô', dataIndex: 'lotNumber', render: (value?: string | null) => <span className="lot-number"><InboxOutlined aria-hidden />{value || 'Không có (dữ liệu cũ)'}</span> },
  { title: 'Hạn sử dụng', dataIndex: 'expiryDate', render: (value?: string | null) => <span className="text-nowrap"><CalendarOutlined aria-hidden /> {formatExpiryDate(value)}</span> },
  { title: 'Số lượng nhập ban đầu', key: 'quantity', align: 'right', render: (_, lot) => `${Number(lot.initialQuantity).toFixed(3)} ${lot.unit}` },
  { title: 'Tình trạng HSD', key: 'status', render: (_, lot) => {
    const { label } = lotExpiryPresentation(lot)
    return <Tag className={`status-tag status-tag-${expiryTone[lot.status]}`}>{label}</Tag>
  } },
]

export function IngredientLotsDrawer({ ingredient, days, onClose }: {
  ingredient: Ingredient | null; days: number; onClose: () => void
}) {
  const lots = useQuery({
    queryKey: ['inventory', 'ingredient-lots', ingredient?.id, days],
    queryFn: ({ signal }) => inventoryApi.listIngredientLots(ingredient!.id, days, signal),
    enabled: ingredient != null,
    ...liveQueryOptions,
  })
  return <Drawer rootClassName="floor-dialog wide-drawer" title={`Các lô · ${ingredient?.name ?? ''}`} open={ingredient != null} onClose={onClose} size={900}>
    <Typography.Paragraph>Nguyên vật liệu: <strong>{ingredient?.name}</strong> · Đơn vị tính: <strong>{ingredient?.unit}</strong></Typography.Paragraph>
    <p className="info-note">Số lượng nhập ban đầu là tổng lượng đã nhập, không phải tồn thực tế của lô.</p>
    {lots.isError ? <ErrorState title="Không tải được danh sách lô" onRetry={() => void lots.refetch()} />
      : <Table rowKey="id" columns={columns} dataSource={lots.data ?? []} loading={lots.isPending} scroll={{ x: 660 }}
        locale={{ emptyText: <EmptyState compact title="Nguyên vật liệu chưa có lô nào" /> }} pagination={{ pageSize: 10 }} />}
  </Drawer>
}
