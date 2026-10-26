export type PaymentMethod = 'CASH' | 'BANK_TRANSFER'

export interface OpenSessionPayment {
  sessionId: number
  tableId: number
  tableCode?: string
  tableName: string
  areaName: string | null
  openedAt: string
  provisionalTotal: number
  hasBlockingItems: boolean
  totalItems: number
}

export interface BlockingItem {
  id: number
  menuItemName: string
  quantity: number
  status: string
  createdAt: string
}

export interface OrderItemPreview {
  id: number
  menuItemId: number
  menuItemName: string
  quantity: number
  unitPriceAtOrder: number
  subtotal: number
  status: string
  note?: string
  unavailableReason?: string
}

export interface OrderPreview {
  orderId: number
  createdAt: string
  createdByName: string
  items: OrderItemPreview[]
}

export interface PaymentPreview {
  sessionId: number
  tableId: number
  tableCode?: string
  tableName: string
  areaName: string | null
  openedAt: string
  provisionalTotal: number
  hasBlockingItems: boolean
  blockingItems: BlockingItem[]
  orders: OrderPreview[]
}

export interface PaySessionPayload {
  sessionId: number
  paymentMethod: PaymentMethod
}

export interface Invoice {
  id: number
  sessionId: number
  tableCode?: string
  tableName: string
  areaName: string | null
  cashierId: number
  cashierName: string
  paymentMethod: PaymentMethod
  amount: number
  paidAt: string
}

export interface InvoiceFilter {
  from?: string
  to?: string
  paymentMethod?: PaymentMethod
  page?: number
  size?: number
}
