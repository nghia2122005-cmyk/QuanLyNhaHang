import type { IngredientLot } from '../types/inventory'

export const EXPIRY_METADATA_NOTE = 'HSD sớm nhất trong các lô đã ghi nhận, kể cả lô quá hạn. Hệ thống chưa theo dõi tồn thực tế từng lô; ngày này không xác nhận lô còn hàng.'

// LocalDate is a calendar date from the server, never an instant in the browser timezone.
export const formatExpiryDate = (date?: string | null) => date ? date.split('-').reverse().join('/') : '—'

export function lotExpiryPresentation(lot: Pick<IngredientLot, 'status' | 'daysRemaining'>) {
  switch (lot.status) {
    case 'EXPIRED': return { color: 'red', label: 'Đã hết hạn' }
    case 'EXPIRING_SOON': return { color: 'orange', label: `Sắp hết hạn · Còn ${lot.daysRemaining ?? 0} ngày` }
    case 'VALID': return { color: 'green', label: 'Còn hạn' }
    case 'NO_EXPIRY': return { color: 'default', label: 'Không có HSD' }
  }
}
