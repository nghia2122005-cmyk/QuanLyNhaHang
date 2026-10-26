import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { nowInAppZone } from './time'

// Pickers edit Vietnam wall-clock values, independently of the browser timezone.
export const reconciliationNow = () => dayjs(nowInAppZone().format('YYYY-MM-DD HH:mm'))

export function initialReconciliationRange(now = reconciliationNow()): [Dayjs, Dayjs] {
  return [now.startOf('day'), now]
}

export const isFutureReconciliationDate = (date: Dayjs, now = reconciliationNow()) =>
  date.format('YYYY-MM-DD') > now.format('YYYY-MM-DD')

export function reconciliationDisabledTime(date: Dayjs | null, now = reconciliationNow()) {
  const selectedDay = date?.format('YYYY-MM-DD')
  const today = now.format('YYYY-MM-DD')
  const sameDay = selectedDay === today
  const futureDay = !!selectedDay && selectedDay > today
  const after = (value: number, count: number) => Array.from({ length: count }, (_, index) => index).filter((index) => index > value)
  return {
    disabledHours: () => futureDay ? after(-1, 24) : sameDay ? after(now.hour(), 24) : [],
    disabledMinutes: (hour: number) => futureDay || (sameDay && hour > now.hour()) ? after(-1, 60) : sameDay && hour === now.hour() ? after(now.minute(), 60) : [],
    disabledSeconds: (hour: number, minute: number) => sameDay && hour === now.hour() && minute === now.minute() ? after(now.second(), 60) : [],
  }
}
