import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
dayjs.extend(utc)
dayjs.extend(timezone)
export const APP_TIMEZONE = 'Asia/Ho_Chi_Minh'
export const nowInAppZone = () => dayjs().tz(APP_TIMEZONE)
export const instantForPicker = (instant: string) => dayjs(dayjs.utc(instant).tz(APP_TIMEZONE).format('YYYY-MM-DDTHH:mm'))
export const formatTime = (instant: string) => dayjs.utc(instant).tz(APP_TIMEZONE).format('DD/MM/YYYY HH:mm')
// Pickers edit wall-clock time. Interpret that wall-clock explicitly in Vietnam.
export const toInstant = (value: Dayjs) => dayjs.tz(value.format('YYYY-MM-DD HH:mm'), APP_TIMEZONE).toISOString()
