import { PageHeader } from '../../components/PageHeader'
import { EmptyState } from '../../components/states/EmptyState'

export default function WorkspacePage({ title, description }: { title: string; description: string }) {
  return <section>
    <PageHeader title={title} description={description} />
    <div className="state-card"><EmptyState title="Chưa có nội dung để hiển thị." /></div>
  </section>
}
