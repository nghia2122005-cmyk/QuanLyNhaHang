import Icon from '@ant-design/icons'
import type { GetProps } from 'antd'

function ChefHatSvg() {
  return <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M7 15.5A4 4 0 0 1 6.2 7.7a5.5 5.5 0 0 1 10.6-1.1 4.5 4.5 0 1 1 .2 8.9Z" />
    <path d="M7 15.5V19a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1v-3.5M7 17.5h10" />
  </svg>
}

/** Kitchen icon (a chef's hat); Ant Design has no equivalent. */
export function ChefHatIcon(props: Partial<GetProps<typeof Icon>>) {
  return <Icon component={ChefHatSvg} {...props} />
}
