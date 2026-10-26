import Icon from '@ant-design/icons'
import type { GetProps } from 'antd'

function BowlSvg() {
  return <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M9 3.5c-.9 1 .9 1.8 0 3.2M12 2.8c-.9 1 .9 1.8 0 3.2M15 3.5c-.9 1 .9 1.8 0 3.2" />
    <path d="M3 11h18" /><path d="M4 11a8 7 0 0 0 16 0" /><path d="M9 20.5h6" />
  </svg>
}

/** Restaurant logo mark (a steaming bowl), sized and coloured like any Ant Design icon. */
export function BowlIcon(props: Partial<GetProps<typeof Icon>>) {
  return <Icon component={BowlSvg} {...props} />
}
