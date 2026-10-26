import dayjs from 'dayjs'

const weekdays = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']

export const formatDate = (date: string) => date.split('-').reverse().join('/')

/** "CN, 25/10/2026" for a calendar date string (YYYY-MM-DD); the weekday of a plain date does not depend on time zones. */
export const weekdayDate = (date: string) => `${weekdays[dayjs(date).day()]}, ${formatDate(date)}`
