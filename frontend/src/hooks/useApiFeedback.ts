import { App } from 'antd'
import type { FormInstance } from 'antd'
import axios from 'axios'
import { useCallback, useEffect } from 'react'
import type { ApiError } from '../types/auth'

export function useApiFeedback(queryError?: unknown) {
  const { message } = App.useApp()
  const report = useCallback((failure: unknown, form?: FormInstance) => {
    if (axios.isCancel(failure)) return
    if (axios.isAxiosError<ApiError>(failure)) {
      if (failure.response?.status === 401 || failure.response?.status === 403) return
      const fields = failure.response?.data.fieldErrors
      if (fields && form) form.setFields(Object.entries(fields).filter(([key]) => key !== '_global').map(([key, error]) => ({
        name: key.replace(/\[(\d+)\]/g, '.$1').split('.').map((part) => /^\d+$/.test(part) ? Number(part) : part), errors: [error],
      })))
      void message.error(failure.response?.data.message ?? 'Chưa thể kết nối với hệ thống. Vui lòng thử lại.')
    } else void message.error('Thao tác chưa thành công. Vui lòng thử lại.')
  }, [message])
  useEffect(() => { if (queryError) report(queryError) }, [queryError, report])
  return { report, message }
}
