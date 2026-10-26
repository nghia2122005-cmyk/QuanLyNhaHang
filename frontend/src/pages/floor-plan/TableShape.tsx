import type { CSSProperties } from 'react'
import type { FloorTable } from '../../types/tables'

const FRAME = { width: 160, height: 120 }
const CHAIR = { long: 24, short: 12, gap: 4 }
const tones: Record<FloorTable['status'], string> = { AVAILABLE: 'free', OCCUPIED: 'busy', INACTIVE: 'off' }

interface Layout { width: number; height: number; round: boolean; top: number; bottom: number; side: number }

// Table size and chair count per side follow the seat capacity.
function layoutFor(capacity: number): Layout {
  if (capacity <= 2) return { width: 60, height: 60, round: true, top: 1, bottom: 1, side: 0 }
  if (capacity <= 4) return { width: 74, height: 74, round: true, top: 1, bottom: 1, side: 1 }
  if (capacity <= 6) return { width: 104, height: 60, round: false, top: 2, bottom: 2, side: 1 }
  return { width: 128, height: 60, round: false, top: 3, bottom: 3, side: 1 }
}

/** Top-down drawing of a table and its chairs; decorative, the card states the capacity in text. */
export function TableShape({ capacity, status }: { capacity: number; status: FloorTable['status'] }) {
  const layout = layoutFor(capacity)
  const left = (FRAME.width - layout.width) / 2
  const top = (FRAME.height - layout.height) / 2
  const chairs: CSSProperties[] = []
  const row = (count: number, y: number) => {
    for (let index = 0; index < count; index++) {
      const center = left + layout.width / count * (index + .5)
      chairs.push({ left: center - CHAIR.long / 2, top: y, width: CHAIR.long, height: CHAIR.short })
    }
  }
  row(layout.top, top - CHAIR.gap - CHAIR.short)
  row(layout.bottom, top + layout.height + CHAIR.gap)
  if (layout.side) {
    const y = FRAME.height / 2 - CHAIR.long / 2
    chairs.push({ left: left - CHAIR.gap - CHAIR.short, top: y, width: CHAIR.short, height: CHAIR.long })
    chairs.push({ left: left + layout.width + CHAIR.gap, top: y, width: CHAIR.short, height: CHAIR.long })
  }
  return <div className={`table-shape table-shape-${tones[status]}`} aria-hidden="true">
    {chairs.map((style, index) => <span key={index} className="table-shape-chair" style={style} />)}
    <span className="table-shape-top" style={{ left, top, width: layout.width, height: layout.height, borderRadius: layout.round ? '50%' : 16 }}>{capacity}</span>
  </div>
}
