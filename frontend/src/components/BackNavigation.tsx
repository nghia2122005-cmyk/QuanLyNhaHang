import { Link } from 'react-router-dom'

export function BackNavigation({ to, label }: { to: string; label: string }) {
  return <Link to={to} className="back-navigation"><span aria-hidden="true">←</span>{label}</Link>
}
