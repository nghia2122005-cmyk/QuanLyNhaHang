import { BowlIcon } from '../assets/icons/BowlIcon'

export function BrandMark() {
  return <span className="brand-mark" aria-hidden="true"><BowlIcon /></span>
}

export function Brand() {
  return <div className="brand"><BrandMark /><span><strong>DN Restaurant</strong><small>Không gian vận hành</small></span></div>
}
