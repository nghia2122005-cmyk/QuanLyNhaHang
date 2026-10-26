import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntdApp, ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import { BrowserRouter } from 'react-router-dom'
import 'antd/dist/reset.css'
import './index.css'
import App from './App.tsx'
import { queryClient } from './api/queryClient'
import { EmptyState } from './components/states/EmptyState'

dayjs.locale('vi')

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider locale={viVN} theme={{
      token: {
        // Mirrors the design tokens on :root in index.css.
        colorPrimary: '#11666A', colorText: '#13292B', colorTextSecondary: '#5A7072', colorBgLayout: '#F3F7F6',
        colorBorder: '#DCE7E6', colorError: '#C0392B', colorLink: '#11666A', colorInfo: '#168187', colorInfoBg: '#EEF7F6', colorInfoBorder: '#DCEFEE',
        colorTextDescription: '#5A7072', colorTextPlaceholder: '#5A7072',
        borderRadius: 12, borderRadiusLG: 20, fontFamily: "'Be Vietnam Pro', system-ui, sans-serif",
        controlHeight: 44, controlHeightLG: 48,
      },
      components: {
        Menu: { itemSelectedBg: '#DCEFEE', itemSelectedColor: '#0D4F53' },
        Table: { headerBg: '#EEF7F6', headerColor: '#0D4F53' },
        Card: { borderRadiusLG: 20 },
        Button: { primaryShadow: 'none' },
      },
    }} renderEmpty={() => <EmptyState compact title="Không có dữ liệu" />}>
      <AntdApp>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </QueryClientProvider>
      </AntdApp>
    </ConfigProvider>
  </StrictMode>,
)
