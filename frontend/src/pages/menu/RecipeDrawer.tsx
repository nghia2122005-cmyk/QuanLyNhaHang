import { Button, Drawer, Form, InputNumber, Select, Space, Spin, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { ingredientApi } from '../../api/ingredientApi'
import { menuApi } from '../../api/menuApi'
import { useApiFeedback } from '../../hooks/useApiFeedback'
import type { MenuItem, SaveRecipeLine } from '../../types/menu'
import { EmptyState } from '../../components/states/EmptyState'
import { DishIcon } from '../../components/DishIcon'

export default function RecipeDrawer({ item, onClose }: { item: MenuItem; onClose: () => void }) {
  const [form] = Form.useForm<{ lines: SaveRecipeLine[] }>()
  const lines = Form.useWatch('lines', form) ?? []
  const cache = useQueryClient()
  const recipe = useQuery({ queryKey: ['menu-items', item.id, 'recipe'], queryFn: ({ signal }) => menuApi.recipe(item.id, signal), refetchOnWindowFocus: false })
  const ingredients = useQuery({ queryKey: ['ingredients'], queryFn: ({ signal }) => ingredientApi.list(signal) })
  const { report, message } = useApiFeedback(recipe.error ?? ingredients.error)
  useEffect(() => { if (recipe.data) form.setFieldsValue({ lines: recipe.data.map(({ ingredientId, quantityPerServing }) => ({ ingredientId, quantityPerServing })) }) }, [recipe.data, form])
  const save = useMutation({ mutationFn: (values: { lines: SaveRecipeLine[] }) => menuApi.saveRecipe(item.id, values.lines), onError: (error) => report(error, form),
    onSuccess: async () => { await cache.invalidateQueries({ queryKey: ['menu-items', item.id, 'recipe'] }); void message.success('Đã lưu định lượng'); onClose() } })
  const busy = recipe.isPending || ingredients.isPending
  return <Drawer rootClassName="floor-dialog wide-drawer" title={<span className="drawer-dish-title"><DishIcon name={item.name} size={40} />{`Định lượng · ${item.name}`}</span>} open onClose={onClose} width={720} maskClosable={!save.isPending} closable={!save.isPending}>
    <Typography.Paragraph type="secondary">Nguyên vật liệu sử dụng cho một phần ăn. Lưu sẽ thay toàn bộ danh sách hiện tại.</Typography.Paragraph>
    {busy ? <Spin /> : <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)} disabled={save.isPending || !!recipe.error || !!ingredients.error}>
      <Form.List name="lines">{(fields, { add, remove }) => <>
        <div className="recipe-grid recipe-column-labels"><span>Nguyên vật liệu</span><span>Số lượng</span><span>Đơn vị</span><span /></div>
        {fields.length === 0 && <EmptyState compact title="Chưa có định lượng. Thêm nguyên vật liệu để bắt đầu." />}
        {fields.map(({ key, name }) => {
          const current = lines[name]?.ingredientId
          const ingredient = ingredients.data?.find((entry) => entry.id === current)
          const old = recipe.data?.find((entry) => entry.ingredientId === current)
          const options = (ingredients.data ?? []).filter((entry) => entry.id === current || !lines.some((line) => line?.ingredientId === entry.id)).map((entry) => ({ value: entry.id, label: entry.name }))
          if (current && !ingredient && old) options.push({ value: current, label: `${old.ingredientName} (Ngừng sử dụng)` })
          return <div className="recipe-grid" key={key}>
            <Form.Item name={[name, 'ingredientId']} rules={[{ required: true, message: 'Chọn nguyên vật liệu' }]}><Select aria-label={`Nguyên vật liệu ${name + 1}`} showSearch optionFilterProp="label" options={options} placeholder="Chọn nguyên vật liệu" /></Form.Item>
            <Form.Item name={[name, 'quantityPerServing']} rules={[{ required: true, message: 'Nhập số lượng' }, { type: 'number', min: 0.001, message: 'Số lượng phải lớn hơn 0' }]}><InputNumber className="full-width" aria-label={`Số lượng ${name + 1}`} min={0.001} max={999999999.999} precision={3} /></Form.Item>
            <span className="recipe-unit">{ingredient?.unit ?? old?.unit ?? '—'}</span>
            <Button danger type="text" aria-label={`Xóa dòng ${name + 1}`} onClick={() => remove(name)}>Xóa</Button>
          </div>
        })}
        <Button icon={<PlusOutlined aria-hidden />} onClick={() => add({ quantityPerServing: 0.001 })}>Thêm nguyên vật liệu</Button>
      </>}</Form.List>
      <Space className="form-actions"><Button onClick={onClose}>Đóng</Button><Button type="primary" htmlType="submit" loading={save.isPending}>Lưu định lượng</Button></Space>
    </Form>}
  </Drawer>
}
