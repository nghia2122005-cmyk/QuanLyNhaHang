import { useState } from 'react'
import { CalendarOutlined, FileTextOutlined, TrophyOutlined } from '@ant-design/icons'
import { Card, Space, Table, Tag, Tooltip, Typography } from 'antd'
import { KpiCard } from '../../components/KpiCard'
import { MoneyText } from '../../components/MoneyText'
import type { DailyRevenue, RevenueAnalysis } from '../../types/reconciliation'
import { dailyRevenuePage } from '../../utils/reportDays'
import { DailyRevenueChart } from './DailyRevenueChart'
import { formatDate, weekdayDate } from '../../utils/dateLabels'

// The chart shows every calendar day for ranges up to two months; longer ranges plot only the days the server returned.
const CHART_FULL_RANGE_DAYS = 62

export function DailyRevenueSection({ daily, analysis, firstDate }: { daily: DailyRevenue[]; analysis: RevenueAnalysis; firstDate: string }) {
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(10)
  const peaks = new Set(analysis.peakDates)
  const chartDays = analysis.calendarDays <= CHART_FULL_RANGE_DAYS ? dailyRevenuePage(daily, firstDate, analysis.calendarDays, 1, analysis.calendarDays) : daily
  return <section aria-label="Doanh thu theo ngày"><Space className="full-width" orientation="vertical" size={20}>
    <div className="kpi-grid kpi-grid-three">
      <KpiCard tone="gold" icon={<TrophyOutlined />} label="Ngày doanh thu cao nhất" value={<MoneyText amount={analysis.peakRevenue} />}
        hint={<Tooltip title={analysis.peakDates.map(formatDate).join(', ')}><span>
          {analysis.peakDates.length ? `${formatDate(analysis.peakDates[0])}${analysis.peakDates.length > 1 ? ` và ${analysis.peakDates.length - 1} ngày cùng mức` : ''}` : 'Chưa phát sinh doanh thu'}
        </span></Tooltip>} />
      <KpiCard tone="gold" icon={<FileTextOutlined />} label="Bình quân / hóa đơn" value={<MoneyText amount={analysis.averageInvoiceAmount} />} hint="Tính cả hóa đơn 0đ" />
      <KpiCard tone="gold" icon={<CalendarOutlined />} label="Bình quân / ngày trong kỳ" value={<MoneyText amount={analysis.averageDailyRevenue} />}
        hint={`Có hóa đơn: ${analysis.activeDays}/${analysis.calendarDays} ngày`} />
    </div>
    <Card className="report-card">
      <Typography.Title level={4} className="report-card-title">Doanh thu theo ngày</Typography.Title>
      <Typography.Paragraph type="secondary">Chỉ tính các ngày đến hôm nay. Ngày không có hóa đơn hiển thị 0đ. Số bình quân làm tròn đến đồng.</Typography.Paragraph>
      <DailyRevenueChart days={chartDays} />
      <Table<DailyRevenue> className="report-table" rowKey="date" dataSource={dailyRevenuePage(daily, firstDate, analysis.calendarDays, page, size)} scroll={{ x: 740 }}
        pagination={{ current: page, pageSize: size, total: analysis.calendarDays, showSizeChanger: true, pageSizeOptions: [10, 20, 31],
          showTotal: (total) => `${total} ngày trong kỳ`, onChange: (nextPage, nextSize) => { setPage(nextPage); setSize(nextSize) } }}
        columns={[
          { title: 'Ngày', dataIndex: 'date', width: 230, render: (date: string) => <Space wrap><time dateTime={date}>{weekdayDate(date)}</time>{peaks.has(date) && <Tag color="green">Cao nhất</Tag>}</Space> },
          { title: 'Số hóa đơn', dataIndex: 'invoiceCount', align: 'right' },
          { title: 'Tiền mặt', dataIndex: 'cashRevenue', align: 'right', render: (value: number) => <MoneyText amount={value} /> },
          { title: 'Chuyển khoản', dataIndex: 'bankTransferRevenue', align: 'right', render: (value: number) => <MoneyText amount={value} /> },
          { title: 'Doanh thu', dataIndex: 'revenue', align: 'right', render: (value: number) => <strong className="report-revenue"><MoneyText amount={value} /></strong> },
        ]} />
    </Card>
  </Space></section>
}
