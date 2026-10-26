import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { MoneyText } from '../../components/MoneyText'
import { EmptyState } from '../../components/states/EmptyState'
import type { DailyRevenue } from '../../types/reconciliation'
import { weekdayDate } from '../../utils/dateLabels'

// Literal values of --teal-700, --gold-500, --border and --text-muted: recharts writes them into SVG attributes.
const colors = { cash: '#11666A', bank: '#E0A458', grid: '#DCE7E6', axis: '#5A7072' }
const decimal = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 })
// Axis labels only: 160000 -> "160k", 1500000 -> "1,5tr" (Intl's Vietnamese compact form "160 N" reads poorly).
const axisLabel = (value: number) => value >= 1e6 ? `${decimal.format(value / 1e6)}tr` : value >= 1e3 ? `${decimal.format(value / 1e3)}k` : decimal.format(value)

function ChartTooltip({ active, payload }: { active?: boolean; payload?: { payload: DailyRevenue }[] }) {
  const day = payload?.[0]?.payload
  if (!active || !day) return null
  return <div className="chart-tooltip">
    <strong>{weekdayDate(day.date)}</strong>
    <div><i style={{ background: colors.cash }} />Tiền mặt <MoneyText amount={day.cashRevenue} /></div>
    <div><i style={{ background: colors.bank }} />Chuyển khoản <MoneyText amount={day.bankTransferRevenue} /></div>
    <div className="chart-tooltip-total">Doanh thu <MoneyText amount={day.revenue} /></div>
  </div>
}

/** Stacked daily bars drawn only from the per-day amounts the server returned. */
export function DailyRevenueChart({ days }: { days: DailyRevenue[] }) {
  if (!days.some((day) => Number(day.revenue) > 0)) return <EmptyState compact title="Chưa phát sinh doanh thu" />
  return <div className="revenue-chart">
    <div className="revenue-chart-legend" aria-hidden="true"><span><i style={{ background: colors.cash }} />Tiền mặt</span><span><i style={{ background: colors.bank }} />Chuyển khoản</span></div>
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={days} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
        <CartesianGrid strokeDasharray="4 4" stroke={colors.grid} vertical={false} />
        <XAxis dataKey="date" tickFormatter={(date: string) => date.slice(8, 10) + '/' + date.slice(5, 7)} tick={{ fill: colors.axis, fontSize: 12 }} tickLine={false} axisLine={{ stroke: colors.grid }} minTickGap={12} />
        <YAxis tickFormatter={axisLabel} tick={{ fill: colors.axis, fontSize: 12 }} tickLine={false} axisLine={false} width={56} />
        <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(17, 102, 106, .06)' }} />
        <Bar dataKey="cashRevenue" name="Tiền mặt" stackId="revenue" fill={colors.cash} maxBarSize={36} />
        <Bar dataKey="bankTransferRevenue" name="Chuyển khoản" stackId="revenue" fill={colors.bank} radius={[6, 6, 0, 0]} maxBarSize={36} />
      </BarChart>
    </ResponsiveContainer>
  </div>
}
