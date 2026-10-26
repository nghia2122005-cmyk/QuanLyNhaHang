/** Decorative rings in the sidebar's bottom-right corner; purely visual and never receives pointer events. */
export function SidebarRings() {
  return <svg className="sidebar-rings" viewBox="0 0 240 240" aria-hidden="true" focusable="false">
    <circle cx="170" cy="170" r="70" fill="none" stroke="#5FB3B0" strokeWidth="1.5" />
    <circle cx="170" cy="170" r="110" fill="none" stroke="#5FB3B0" strokeWidth="1.5" />
    <circle cx="120" cy="190" r="44" fill="none" style={{ stroke: 'var(--gold-500)' }} strokeWidth="1.5" />
  </svg>
}
