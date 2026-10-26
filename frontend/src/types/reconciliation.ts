import type { PaymentMethod } from './payment'

export interface RevenueSummary {
  from: string
  to: string
  invoiceCount: number
  systemRevenue: number
  methods: { paymentMethod: PaymentMethod; invoiceCount: number; revenue: number }[]
}

export interface PaymentSummary {
  revenue: RevenueSummary
  topItems: { menuItemId: number; menuItemName: string; quantity: number; revenue: number }[]
  dailyRevenue: DailyRevenue[]
  analysis: RevenueAnalysis
}

export interface DailyRevenue {
  date: string
  invoiceCount: number
  revenue: number
  cashRevenue: number
  bankTransferRevenue: number
}

export interface RevenueAnalysis {
  averageInvoiceAmount: number
  averageDailyRevenue: number
  peakRevenue: number
  peakDates: string[]
  activeDays: number
  calendarDays: number
}

export interface CreateReconciliationPayload {
  startTime: string
  endTime: string
  actualAmount: number
  differenceReason?: string
}

export interface Reconciliation extends CreateReconciliationPayload {
  id: number
  cashierId: number
  cashierName: string
  managerId: number | null
  managerName: string | null
  systemRevenue: number
  difference: number
  reconciledAt: string
  confirmed: boolean
}
