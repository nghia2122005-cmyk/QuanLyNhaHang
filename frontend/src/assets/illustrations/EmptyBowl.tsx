/** Default empty-state picture: a steaming bowl. Sized by the `size` prop (width in px). */
export function EmptyBowl({ size = 96 }: { size?: number }) {
  return <svg width={size} height={size * 0.8} viewBox="0 0 120 96" aria-hidden="true" focusable="false">
    <ellipse cx="60" cy="86" rx="40" ry="6" style={{ fill: 'var(--teal-50)' }} />
    <g fill="none" strokeWidth="3" strokeLinecap="round" style={{ stroke: 'var(--gold-500)' }}>
      <path d="M44 14c-5 6 5 10 0 17" /><path d="M60 8c-5 6 5 10 0 17" /><path d="M76 14c-5 6 5 10 0 17" />
    </g>
    <path d="M18 42h84a42 36 0 0 1-84 0Z" strokeWidth="3" strokeLinejoin="round" style={{ fill: 'var(--teal-100)', stroke: 'var(--teal-700)' }} />
    <path d="M14 42h92" strokeWidth="3" strokeLinecap="round" style={{ stroke: 'var(--teal-700)' }} />
    <path d="M48 80h24" strokeWidth="3" strokeLinecap="round" style={{ stroke: 'var(--teal-700)' }} />
  </svg>
}
