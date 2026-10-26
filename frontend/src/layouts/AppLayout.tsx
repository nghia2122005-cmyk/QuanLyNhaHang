import { Avatar, Breadcrumb, Button, ConfigProvider, Drawer, Layout, Menu } from 'antd'
import type { MenuProps } from 'antd'
import {
  AppstoreOutlined, AuditOutlined, BarChartOutlined, CalendarOutlined, CheckSquareOutlined, ExperimentOutlined,
  FileTextOutlined, ImportOutlined, InboxOutlined, LogoutOutlined, MenuOutlined, ReadOutlined, ShopOutlined, TeamOutlined, WalletOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'
import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { navigation } from '../auth/roles'
import type { Account } from '../types/auth'
import { roleLabels } from '../utils/labels'
import { useRealtime } from '../realtime/useRealtime'
import { ChefHatIcon } from '../assets/icons/ChefHatIcon'
import { SidebarRings } from '../assets/illustrations/SidebarRings'
import { Brand } from '../components/Brand'

const navigationIcons: Record<string, ReactNode> = {
  '/reports': <BarChartOutlined aria-hidden />, '/accounts': <TeamOutlined aria-hidden />, '/menu': <ReadOutlined aria-hidden />,
  '/floor-plan': <AppstoreOutlined aria-hidden />, '/reservations': <CalendarOutlined aria-hidden />, '/kitchen': <ChefHatIcon aria-hidden />,
  '/payment': <WalletOutlined aria-hidden />, '/invoices': <FileTextOutlined aria-hidden />, '/reconciliation': <AuditOutlined aria-hidden />,
  '/inventory': <InboxOutlined aria-hidden />, '/inventory/receipts': <ImportOutlined aria-hidden />, '/inventory/stocktake': <CheckSquareOutlined aria-hidden />,
}

// Display-only grouping: OWNER and MANAGER see administration entries apart; access still comes from roles.ts.
const administrationPaths = new Set(['/reports', '/accounts', '/menu'])
const menuGroups = ['Quản trị', 'Vận hành'] as const

// Literal values of the --teal-* and sidebar tokens; antd derives colours from these, so CSS variables cannot be used here.
const sidebarTheme = {
  components: {
    Menu: {
      darkItemBg: 'transparent', darkItemColor: '#C7DEDC', darkItemHoverBg: 'rgba(255, 255, 255, .06)', darkItemHoverColor: '#FFFFFF',
      darkItemSelectedBg: '#11666A', darkItemSelectedColor: '#FFFFFF', darkGroupTitleColor: '#8FBDBA',
      itemHeight: 46, itemBorderRadius: 12, itemMarginInline: 0, iconMarginInlineEnd: 12, iconSize: 16, fontSize: 15, groupTitleFontSize: 11,
    },
  },
}

function pageTitle(pathname: string): string | undefined {
  if (pathname.startsWith('/sessions/')) return 'Phiên phục vụ'
  return navigation.find((item) => item.path === pathname)?.label
}

function SidebarContent({ account, pathname, onNavigate, label }: { account: Account; pathname: string; onNavigate: () => void; label: string }) {
  const administration = account.role === 'OWNER' || account.role === 'MANAGER'
  const entries = navigation.filter((item) => item.roles.includes(account.role))
  const items: MenuProps['items'] = menuGroups.map((group) => ({
    type: 'group' as const, key: group, label: group,
    children: entries.filter((item) => (administration && administrationPaths.has(item.path) ? 'Quản trị' : 'Vận hành') === group)
      .map((item) => ({ key: item.path, icon: navigationIcons[item.path], label: <Link to={item.path}>{item.label}</Link> })),
  })).filter((group) => group.children.length > 0)
  return <div className="sidebar-content">
    <SidebarRings />
    <Brand />
    <nav aria-label={label} className="sidebar-nav">
      <ConfigProvider theme={sidebarTheme}><Menu mode="inline" theme="dark" selectedKeys={[pathname]} onClick={onNavigate} items={items} /></ConfigProvider>
    </nav>
    <div className="sidebar-note"><span><ShopOutlined aria-hidden />Bán hàng tại bàn</span><span><ExperimentOutlined aria-hidden />Quản lý nguyên vật liệu</span></div>
    <div className="sidebar-account">
      <Avatar size={40} className={account.role === 'OWNER' ? 'sidebar-avatar sidebar-avatar-owner' : 'sidebar-avatar'}>{account.fullName.slice(0, 1)}</Avatar>
      <div><strong>{account.fullName}</strong><span>{roleLabels[account.role]}</span></div>
    </div>
  </div>
}

export function AppLayout() {
  useRealtime()
  const { account, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  if (!account) return null
  const title = pageTitle(location.pathname)

  return (
    <Layout className="application-layout">
      <Layout.Sider width={264} className="desktop-sidebar">
        <SidebarContent account={account} pathname={location.pathname} onNavigate={() => setMenuOpen(false)} label="Điều hướng chính" />
      </Layout.Sider>
      <Layout className="workspace-layout">
        <Layout.Header className="workspace-header">
          <Button className="mobile-menu-button" icon={<MenuOutlined aria-hidden />} onClick={() => setMenuOpen(true)} aria-label="Mở menu điều hướng" />
          <Breadcrumb className="workspace-breadcrumb" separator="›"
            items={[{ title: 'Không gian làm việc' }, ...(title ? [{ title: <span aria-current="page">{title}</span> }] : [])]} />
          <Button className="logout-button" icon={<LogoutOutlined aria-hidden />} onClick={logout}>Đăng xuất</Button>
        </Layout.Header>
        <Layout.Content className="workspace-content"><Outlet /></Layout.Content>
      </Layout>
      <Drawer rootClassName="sidebar-drawer" title="Điều hướng" placement="left" open={menuOpen} onClose={() => setMenuOpen(false)} size={264}>
        <SidebarContent account={account} pathname={location.pathname} onNavigate={() => setMenuOpen(false)} label="Điều hướng trên thiết bị nhỏ" />
      </Drawer>
    </Layout>
  )
}
