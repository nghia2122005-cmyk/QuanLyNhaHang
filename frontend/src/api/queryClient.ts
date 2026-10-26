import axios from 'axios'
import { QueryClient } from '@tanstack/react-query'

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: (failureCount, error) => {
          const status = axios.isAxiosError(error) ? error.response?.status : undefined
          return !(status && status >= 400 && status < 500) && failureCount < 3
        },
      },
    },
  })
}

export const queryClient = createQueryClient()
