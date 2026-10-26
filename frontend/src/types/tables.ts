export type ReservationStatus = 'CONFIRMED' | 'SEATED' | 'CANCELLED'
export interface Reservation {
  id: number; tableId: number; tableName: string; tableCode?: string; customerName: string; customerPhone: string; guestCount: number
  startTime: string; endTime: string; note: string | null; status: ReservationStatus; createdAt: string; createdById: number; overdue: boolean
}
export interface UpcomingReservation { id: number; customerName: string; guestCount: number; startTime: string; endTime: string }
export interface FloorTable { code?: string; id: number; name: string; capacity: number; status: 'AVAILABLE' | 'OCCUPIED' | 'INACTIVE'; activeSessionId: number | null; upcomingReservation: UpcomingReservation | null }
export interface FloorArea { code?: string; id: number; name: string; tables: FloorTable[] }
export interface ManagedArea { id: number; name: string; code: string; status: 'ACTIVE' | 'INACTIVE' }
export interface SaveArea { name: string; code: string }
export interface SaveTable { areaId: number; name: string; code: string; capacity: number }
export interface ManagedTable extends SaveTable {
  id: number; status: FloorTable['status']; areaName: string; areaStatus: ManagedArea['status'];
  activeSessionId: number | null; hasLiveReservation: boolean
}
export interface AvailableTable { code?: string; id: number; name: string; capacity: number; areaName: string }
export interface PageResponse<T> { items: T[]; page: number; size: number; total: number }
export interface ReservationFilters { date?: string; status?: ReservationStatus; q?: string; tableId?: number; unexpired?: boolean; page: number; size: number }
export interface CreateReservation { tableId: number; customerName: string; customerPhone: string; guestCount: number; startTime: string; endTime: string; note?: string }
export interface OpenSession { sessionId: number; tableId: number; reservationId: number | null; openedAt: string; status: 'ACTIVE' }
