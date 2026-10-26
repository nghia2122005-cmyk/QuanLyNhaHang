export type IngredientStatus = 'ACTIVE' | 'INACTIVE'
export type MovementType =
  | 'RECEIPT'
  | 'CONSUMPTION'
  | 'STOCKTAKE_ADJUSTMENT'
  | 'DISPOSAL'

export interface Ingredient {
  id: number
  name: string
  unit: string
  minStock: number
  trackLot: boolean
  trackExpiry: boolean
  status: IngredientStatus
  reservedStock: number
  availableStock: number
  currentStock: number
  belowMinStock: boolean
  nearestExpiryDate: string | null
  lotCount: number
}

export interface CreateIngredientPayload {
  name: string
  unit: string
  minStock: number
  trackLot: boolean
  trackExpiry: boolean
}

export interface UpdateIngredientPayload {
  name: string
  unit: string
  minStock: number
  trackLot: boolean
  trackExpiry: boolean
}

export interface IngredientLot {
  id: number
  ingredientId: number
  ingredientName: string
  unit: string
  lotNumber?: string | null
  expiryDate: string | null
  initialQuantity: number
  status: 'EXPIRED' | 'EXPIRING_SOON' | 'VALID' | 'NO_EXPIRY'
  daysRemaining: number | null
}

export interface IngredientLotAttention extends IngredientLot {
  expiryDate: string
  status: 'EXPIRED' | 'EXPIRING_SOON'
  daysRemaining: number
}

export interface StockReceiptItemPayload {
  ingredientId: number
  quantity: number
  lotNumber: string
  expiryDate?: string
}

export interface CreateStockReceiptPayload {
  items: StockReceiptItemPayload[]
}

export interface StockReceiptItem {
  id: number
  ingredientId: number
  ingredientName: string
  unit: string
  lotId?: number
  lotNumber?: string
  expiryDate?: string
  quantity: number
}

export interface StockReceipt {
  id: number
  createdById: number
  createdByName: string
  createdAt: string
  items: StockReceiptItem[]
}

export interface StocktakeTemplateItem {
  ingredientId: number
  ingredientName: string
  unit: string
  reservedStock: number
  availableStock: number
  currentStock: number
}

export interface StocktakeItemPayload {
  ingredientId: number
  actualQuantity: number
  note?: string
}

export interface CreateStocktakePayload {
  items: StocktakeItemPayload[]
}

export interface StocktakeItem {
  id: number
  ingredientId: number
  ingredientName: string
  unit: string
  systemQuantity: number
  actualQuantity: number
  difference: number
  note?: string
}

export interface Stocktake {
  id: number
  createdById: number
  createdByName: string
  createdAt: string
  items: StocktakeItem[]
}

export interface CreateStockDisposalPayload {
  ingredientId: number
  lotId?: number
  quantity: number
  reason: string
}

export interface StockMovement {
  id: number
  ingredientId: number
  ingredientName: string
  unit: string
  lotId?: number
  lotNumber?: string
  expiryDate?: string
  movementType: MovementType
  quantityChange: number
  reason?: string
  performedById: number
  performedByName: string
  occurredAt: string
  source: string
}

export interface MovementFilter {
  ingredientId?: number
  movementType?: MovementType
  from?: string
  to?: string
  page?: number
  size?: number
}
