import { Button, Form, Input, InputNumber, Modal, Pagination, Segmented, Select, Space, Table, Typography } from 'antd'
import { AppstoreOutlined, CheckCircleOutlined, EditOutlined, ExperimentOutlined, PlusOutlined, ReadOutlined, SearchOutlined, StopOutlined, UnorderedListOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { menuApi } from '../../api/menuApi'
import type { MenuFilters } from '../../api/menuApi'
import { useAuth } from '../../auth/useAuth'
import { MoneyText } from '../../components/MoneyText'
import { StatusTag } from '../../components/StatusTag'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import type { MenuItem, SaveMenuItem } from '../../types/menu'
import { statusLabels } from '../../utils/labels'
import RecipeDrawer from './RecipeDrawer'
import { PageHeader } from '../../components/PageHeader'
import { DishIcon } from '../../components/DishIcon'
import { dishBackground } from '../../components/dishKind'
import { EmptyState } from '../../components/states/EmptyState'
import { LoadingState } from '../../components/states/LoadingState'

type MenuView = 'table' | 'grid'
const PAGE_SIZE = 20
const VIEW_KEY = 'menu.viewMode'
// The view mode is a per-browser convenience; storage may be unavailable (private mode), so table stays the default.
const readView = (): MenuView => { try { return localStorage.getItem(VIEW_KEY) === 'grid' ? 'grid' : 'table' } catch { return 'table' } }

export default function MenuPage() {
  const { account } = useAuth()
  const owner = account?.role === 'OWNER'
  const [filters, setFilters] = useState<MenuFilters>({})
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [editing, setEditing] = useState<MenuItem | 'new' | null>(null)
  const [recipe, setRecipe] = useState<MenuItem | null>(null)
  const [view, setView] = useState<MenuView>(readView)
  const [form] = Form.useForm<SaveMenuItem>()
  const cache = useQueryClient()
  const query = useQuery({ queryKey: ['menu-items', filters], queryFn: ({ signal }) => menuApi.list(filters, signal) })
  const { report, message } = useApiFeedback(query.error)
  const refresh = () => cache.invalidateQueries({ queryKey: ['menu-items'] })
  const save = useMutation({ mutationFn: (values: SaveMenuItem) => editing && editing !== 'new' ? menuApi.update(editing.id, values) : menuApi.create(values),
    onSuccess: async () => { if (editing === 'new') { setFilters({}); setSearch(''); setPage(1) }; setEditing(null); void message.success('Đã lưu món ăn'); await refresh() }, onError: (error) => report(error, form) })
  const change = useMutation({ mutationFn: (item: MenuItem) => menuApi.status(item.id, item.status === 'ON_SALE' ? 'DISCONTINUED' : 'ON_SALE'),
    onSuccess: async () => { void message.success('Đã cập nhật trạng thái món'); await refresh() }, onError: (error) => report(error) })
  const edit = (item: MenuItem | 'new') => { form.resetFields(); if (item !== 'new') form.setFieldsValue(item); setEditing(item) }
  const changeView = (next: MenuView) => { setView(next); try { localStorage.setItem(VIEW_KEY, next) } catch { /* keep the in-memory choice */ } }
  const items = query.data ?? []
  const countByStatus = (status: MenuItem['status']) => items.filter((item) => item.status === status).length
  const actions = (item: MenuItem) => <Space wrap className="menu-actions"><Button icon={<ExperimentOutlined aria-hidden />} onClick={() => setRecipe(item)}>Định lượng</Button>
    {owner && <><Button icon={<EditOutlined aria-hidden />} onClick={() => edit(item)}>Sửa</Button><Button className={item.status === 'ON_SALE' ? 'menu-stop-button' : undefined}
      icon={item.status === 'ON_SALE' ? <StopOutlined aria-hidden /> : <CheckCircleOutlined aria-hidden />} disabled={change.isPending} onClick={() => change.mutate(item)}>{item.status === 'ON_SALE' ? 'Ngừng kinh doanh' : 'Kinh doanh lại'}</Button></>}</Space>
  return <section>
    <PageHeader eyebrow="Danh mục món" title="Thực đơn" description="Danh mục món ăn và nguyên vật liệu cho mỗi phần."
      actions={owner && <Button type="primary" size="large" icon={<PlusOutlined aria-hidden />} onClick={() => edit('new')}>Thêm món</Button>} />
    <div className="count-cards" role="group" aria-label="Số món theo trạng thái">
      {[{ label: 'Tổng số món', count: items.length, icon: <ReadOutlined />, tone: 'count-tone-teal' },
        { label: 'Đang kinh doanh', count: countByStatus('ON_SALE'), icon: <CheckCircleOutlined />, tone: 'count-tone-free' },
        { label: 'Ngừng kinh doanh', count: countByStatus('DISCONTINUED'), icon: <StopOutlined />, tone: 'count-tone-off' }].map(({ label, count, icon, tone }) => <div key={label} className="count-card">
        <span className={`count-card-icon ${tone}`} aria-hidden="true">{icon}</span><div><span>{label}</span><strong>{query.isPending ? '–' : count}</strong></div>
      </div>)}
    </div>
    <div className="filter-card menu-filters">
      <Input.Search aria-label="Tìm món" enterButton={<Button icon={<SearchOutlined aria-hidden />} aria-label="Tìm kiếm" />} prefix={<SearchOutlined aria-hidden />} placeholder="Tìm theo tên món" allowClear value={search} onChange={(event) => setSearch(event.target.value)} onSearch={(q) => { setPage(1); setFilters((old) => ({ ...old, q: q || undefined })) }} className="menu-search" />
      <Select aria-label="Trạng thái món" placeholder="Tất cả trạng thái" allowClear value={filters.status} className="menu-status-filter"
        options={Object.entries(statusLabels.MenuItemStatus).map(([value, label]) => ({ value, label }))} onChange={(status) => { setPage(1); setFilters((old) => ({ ...old, status })) }} />
      <Segmented<MenuView> className="menu-view-switch" aria-label="Chế độ hiển thị" value={view} onChange={changeView} options={[
        { value: 'table', label: 'Bảng', icon: <UnorderedListOutlined aria-hidden /> }, { value: 'grid', label: 'Lưới', icon: <AppstoreOutlined aria-hidden /> }]} />
    </div>
    <Typography.Paragraph type="secondary">Ưu tiên món đang kinh doanh, món mới thêm hiển thị trước.</Typography.Paragraph>
    {view === 'table' ? <div className="data-panel"><Table<MenuItem> className="menu-table" rowKey="id" loading={query.isPending} dataSource={items} scroll={{ x: 760 }} pagination={{ current: page, onChange: setPage, pageSize: PAGE_SIZE, hideOnSinglePage: true }} columns={[
      { title: 'Món ăn', dataIndex: 'name', render: (name: string) => <span className="dish-cell"><DishIcon name={name} size={56} /><strong>{name}</strong></span> },
      { title: 'Giá bán', dataIndex: 'price', render: (price) => <strong><MoneyText amount={price} /></strong> },
      { title: 'Trạng thái', render: (_, item) => <StatusTag group="MenuItemStatus" value={item.status} /> },
      { title: 'Thao tác', render: (_, item) => actions(item) },
    ]} /></div>
      : query.isPending ? <LoadingState variant="cards" />
      : items.length === 0 ? <div className="state-card"><EmptyState title="Không có món phù hợp." /></div>
      : <>
        <div className="menu-grid">{items.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE).map((item) => <article key={item.id} className="menu-card">
          <div className="menu-card-art" style={{ background: dishBackground(item.name) }}><DishIcon name={item.name} size={104} framed={false} /></div>
          <div className="menu-card-body">
            <div className="menu-card-heading"><h3>{item.name}</h3><StatusTag group="MenuItemStatus" value={item.status} /></div>
            <strong className="menu-card-price"><MoneyText amount={item.price} /></strong>
            {actions(item)}
          </div>
        </article>)}</div>
        <Pagination className="menu-pagination" current={page} pageSize={PAGE_SIZE} total={items.length} onChange={setPage} hideOnSinglePage />
      </>}
    <Modal title={editing === 'new' ? 'Thêm món' : 'Sửa món'} open={editing !== null} onCancel={() => setEditing(null)} footer={null} maskClosable={!save.isPending} closable={!save.isPending}>
      <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} disabled={save.isPending}>
        <Form.Item name="name" label="Tên món" rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập tên món' }, { max: 100 }]}><Input maxLength={100} /></Form.Item>
        <Form.Item name="price" label="Giá bán (VND)" rules={[{ required: true, message: 'Vui lòng nhập giá bán' }, { type: 'number', min: 0 }]}><InputNumber className="full-width" min={0} max={99999999999999} precision={0} step={1000} /></Form.Item>
        <Space className="form-actions"><Button onClick={() => setEditing(null)}>Hủy</Button><Button type="primary" htmlType="submit" loading={save.isPending}>Lưu món</Button></Space>
      </Form>
    </Modal>
    {recipe && <RecipeDrawer key={recipe.id} item={recipe} onClose={() => setRecipe(null)} />}
  </section>
}
