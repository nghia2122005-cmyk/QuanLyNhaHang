import { BankOutlined, FileTextOutlined, RiseOutlined, WalletOutlined } from '@ant-design/icons'
import type { RevenueSummary } from '../types/reconciliation'
import { MoneyText } from './MoneyText'
import { KpiCard } from './KpiCard'
import { statusLabel } from '../utils/labels'

export function RevenueStats({ data }: { data: RevenueSummary }) {
  return (
    <div className="kpi-grid">
      <KpiCard featured icon={<RiseOutlined />} label="Doanh thu hệ thống" value={<MoneyText amount={data.systemRevenue} />} />
      <KpiCard icon={<FileTextOutlined />} label="Số hóa đơn" value={data.invoiceCount} />
      {data.methods.map((method) => (
        <KpiCard key={method.paymentMethod} icon={method.paymentMethod === 'CASH' ? <WalletOutlined /> : <BankOutlined />}
          label={`${statusLabel('PaymentMethod', method.paymentMethod)} (${method.invoiceCount} hóa đơn)`} value={<MoneyText amount={method.revenue} />} />
      ))}
    </div>
  )
}
