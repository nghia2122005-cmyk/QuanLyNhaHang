import type { ReactNode } from 'react'
import { dishBackgrounds, dishKind } from './dishKind'
import type { DishKind } from './dishKind'

const ink = '#0D4F53'
const bowl = <>
  <path d="M8 24h32a16 13 0 0 1-32 0Z" fill="#FFFFFF" stroke={ink} strokeWidth="2" strokeLinejoin="round" />
  <path d="M19 38h10" stroke={ink} strokeWidth="2" strokeLinecap="round" />
</>
const art: Record<DishKind, ReactNode> = {
  peachTea: <>
    <path d="M15 12h18l-2.5 26a3 3 0 0 1-3 2.7h-7a3 3 0 0 1-3-2.7Z" fill="#F7B27A" stroke={ink} strokeWidth="2" strokeLinejoin="round" />
    <path d="M16 18h16" stroke={ink} strokeWidth="2" />
    <circle cx="30" cy="12" r="5" fill="#F28C5B" stroke={ink} strokeWidth="2" /><path d="M27 6l4-3" stroke="#3E8E5B" strokeWidth="2" strokeLinecap="round" />
  </>,
  tea: <>
    <path d="M12 18h20v10a10 10 0 0 1-20 0Z" fill="#FFFFFF" stroke={ink} strokeWidth="2" strokeLinejoin="round" />
    <path d="M32 21h3a4 4 0 0 1 0 8h-3" fill="none" stroke={ink} strokeWidth="2" />
    <path d="M14 22h16" stroke="#7FBF8E" strokeWidth="3" />
    <path d="M18 8c-2 2.5 2 3.5 0 6M24 8c-2 2.5 2 3.5 0 6" fill="none" stroke="#E0A458" strokeWidth="2" strokeLinecap="round" />
  </>,
  milkCoffee: <>
    <path d="M15 10h18l-2.5 28a3 3 0 0 1-3 2.7h-7a3 3 0 0 1-3-2.7Z" fill="#E9D4B8" stroke={ink} strokeWidth="2" strokeLinejoin="round" />
    <path d="M17.3 26h13.4l-1.2 12.5a2 2 0 0 1-2 1.7h-7a2 2 0 0 1-2-1.7Z" fill="#7A4A2A" />
    <rect x="19" y="14" width="5" height="5" rx="1" fill="#FFFFFF" stroke={ink} strokeWidth="1.5" />
    <rect x="25" y="17" width="5" height="5" rx="1" fill="#FFFFFF" stroke={ink} strokeWidth="1.5" />
    <path d="M29 4l-3 10" stroke={ink} strokeWidth="2" strokeLinecap="round" />
  </>,
  coffee: <>
    <path d="M14 14h20l-2 22a4 4 0 0 1-4 3.6h-8a4 4 0 0 1-4-3.6Z" fill="#FFFFFF" stroke={ink} strokeWidth="2" strokeLinejoin="round" />
    <path d="M15.2 20h17.6l-1.2 14H16.4Z" fill="#5A3420" />
    <path d="M20 6c-2 2.5 2 3.5 0 6M27 6c-2 2.5 2 3.5 0 6" fill="none" stroke="#E0A458" strokeWidth="2" strokeLinecap="round" />
  </>,
  pho: <>
    {bowl}
    <path d="M13 24c3-4 6 2 9-2s6 2 9-2 4 1 5 2" fill="none" stroke="#E0A458" strokeWidth="2" strokeLinecap="round" />
    <ellipse cx="20" cy="20" rx="4" ry="2.4" fill="#B5543A" /><ellipse cx="29" cy="20" rx="4" ry="2.4" fill="#B5543A" />
    <path d="M33 14l3-4M36 15l4-2" stroke="#3E8E5B" strokeWidth="2" strokeLinecap="round" />
  </>,
  bun: <>
    {bowl}
    <path d="M13 23c4-3 6 1 10-1s6 1 12-1" fill="none" stroke={ink} strokeWidth="1.5" strokeLinecap="round" />
    <path d="M14 20c4-3 6 1 10-1s6 1 11-1" fill="none" stroke={ink} strokeWidth="1.5" strokeLinecap="round" />
    <circle cx="31" cy="17" r="3.5" fill="#B5543A" /><path d="M16 15l3 3M19 13l1 4" stroke="#3E8E5B" strokeWidth="2" strokeLinecap="round" />
  </>,
  brokenRice: <>
    <ellipse cx="24" cy="29" rx="18" ry="9" fill="#FFFFFF" stroke={ink} strokeWidth="2" />
    <ellipse cx="18" cy="27" rx="8" ry="5" fill="#FFFDF5" stroke={ink} strokeWidth="1.5" />
    <path d="M24 24c5-3 12-2 12 3s-7 6-12 3Z" fill="#B5543A" stroke={ink} strokeWidth="1.5" />
    <path d="M28 34h7" stroke="#3E8E5B" strokeWidth="2" strokeLinecap="round" />
  </>,
  rice: <>
    <ellipse cx="24" cy="30" rx="18" ry="9" fill="#FFFFFF" stroke={ink} strokeWidth="2" />
    <path d="M14 29a10 9 0 0 1 20 0Z" fill="#FFFDF5" stroke={ink} strokeWidth="1.5" />
    <circle cx="30" cy="26" r="4" fill="#FFFFFF" stroke="#E0A458" strokeWidth="1.5" /><circle cx="30" cy="26" r="1.8" fill="#E0A458" />
  </>,
  springRoll: <>
    <ellipse cx="24" cy="33" rx="18" ry="7" fill="#FFFFFF" stroke={ink} strokeWidth="2" />
    <rect x="10" y="20" width="14" height="11" rx="5.5" fill="#F4F1E8" stroke={ink} strokeWidth="1.5" transform="rotate(-12 17 25)" />
    <rect x="24" y="20" width="14" height="11" rx="5.5" fill="#F4F1E8" stroke={ink} strokeWidth="1.5" transform="rotate(12 31 25)" />
    <path d="M14 24h6M28 24h6" stroke="#7FBF8E" strokeWidth="2" strokeLinecap="round" />
    <path d="M14 27h5M28 27h5" stroke="#E58C8A" strokeWidth="2" strokeLinecap="round" />
  </>,
  bowl: <>
    {bowl}
    <path d="M19 10c-2 2.5 2 3.5 0 6M24 8c-2 2.5 2 3.5 0 6M29 10c-2 2.5 2 3.5 0 6" fill="none" stroke="#E0A458" strokeWidth="2" strokeLinecap="round" />
  </>,
}

/** Round illustration tile for a dish until menu pictures exist; decorative only. */
export function DishIcon({ name, size = 48, framed = true }: { name: string; size?: number; framed?: boolean }) {
  const kind = dishKind(name)
  return <span className={framed ? 'dish-icon' : 'dish-icon dish-icon-bare'} aria-hidden="true"
    style={{ width: size, height: size, background: framed ? dishBackgrounds[kind] : 'transparent' }}>
    <svg viewBox="0 0 48 48" width={framed ? size * .78 : size} height={framed ? size * .78 : size} focusable="false">{art[kind]}</svg>
  </span>
}
