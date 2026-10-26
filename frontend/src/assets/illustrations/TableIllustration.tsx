// Top-down view of a dining table: three plates, chopsticks and two leaves inside a dashed ring.
const plates = [
  { x: 160, y: 92, core: 'var(--gold-500)' },
  { x: 102, y: 194, core: '#7FBF8E' },
  { x: 218, y: 194, core: '#D9764A' },
]
const leaf = 'M0 -24C15 -11 15 11 0 24C-15 11 -15 -11 0 -24Z'

export function TableIllustration({ className }: { className?: string }) {
  return <svg className={className} viewBox="0 0 320 320" aria-hidden="true" focusable="false">
    <circle cx="160" cy="160" r="150" fill="none" stroke="#2A8A8E" strokeWidth="2" strokeDasharray="6 10" />
    <g transform="translate(36 40) rotate(-35)"><path d={leaf} style={{ fill: 'var(--teal-700)' }} /><path d="M0 -20V20" stroke="#2A8A8E" strokeWidth="2" /></g>
    <g transform="translate(286 282) rotate(40)"><path d={leaf} style={{ fill: 'var(--teal-700)' }} /><path d="M0 -20V20" stroke="#2A8A8E" strokeWidth="2" /></g>
    <circle cx="160" cy="160" r="120" stroke="#2A8A8E" strokeWidth="4" style={{ fill: 'var(--teal-700)' }} />
    {plates.map(({ x, y, core }) => <g key={`${x}-${y}`}>
      <circle cx={x} cy={y} r="34" style={{ fill: 'var(--bg)' }} />
      <circle cx={x} cy={y} r="25" fill="none" style={{ stroke: 'var(--border)' }} strokeWidth="2" />
      <circle cx={x} cy={y} r="14" style={{ fill: core }} />
    </g>)}
    <g strokeWidth="5" strokeLinecap="round" style={{ stroke: 'var(--gold-500)' }}>
      <path d="M128 252L196 226" /><path d="M132 262L200 236" />
    </g>
  </svg>
}
