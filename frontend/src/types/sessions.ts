import type { statusLabels } from '../utils/labels'
import type { Reservation } from './tables'

export type OrderItemStatus = keyof typeof statusLabels.OrderItemStatus
export interface OrderItem {
  id: number; menuItemId: number; menuItemName: string; quantity: number; note: string | null
  status: OrderItemStatus; unavailableReason: string | null; unitPriceAtOrder: number; subtotal: number
}
export interface FoodOrder {
  id: number; createdById: number; createdByName: string; createdAt: string; items: OrderItem[]; total: number
}
export interface SessionDetail {
  tableCode?: string; sessionId: number; tableId: number; tableName: string; areaName: string; reservation: Reservation | null
  openedAt: string; closedAt: string | null; status: 'ACTIVE' | 'CLOSED'; orders: FoodOrder[]; total: number
}
export interface OrderLine { menuItemId: number; quantity: number; note?: string }
export interface OrderQueueItem {
  tableCode?: string; id: number; sessionId: number; orderId: number; tableId: number; tableName: string; menuItemName: string
  quantity: number; note: string | null; orderedAt: string; status: OrderItemStatus; unavailableReason: string | null
}

export interface IngredientAvailability {
  ingredientId: number; ingredientName: string; unit: string; currentStock: number; reservedStock: number
  availableStock: number; requiredQuantity: number; projectedStock: number; minimumStock: number; shortage: number
}
export interface CookingStartResponse { started: boolean; item: OrderItem; ingredients: IngredientAvailability[] }
