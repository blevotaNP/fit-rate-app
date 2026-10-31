import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/Button'

export function NotFoundPage() {
  return (
    <div className="flex min-h-[55vh] flex-col items-center justify-center text-center">
      <p className="numeric font-display text-7xl font-black leading-none text-ink-700">404</p>
      <h1 className="mt-4 font-display text-2xl font-bold text-mist-50">
        Nothing hanging on this rail
      </h1>
      <p className="mt-2 max-w-sm text-sm leading-relaxed text-mist-400">
        The page you asked for does not exist, or the look behind it was withdrawn by its author.
      </p>
      <div className="mt-6 flex gap-2">
        <Link to="/">
          <Button variant="primary">Back to home</Button>
        </Link>
        <Link to="/looks">
          <Button variant="secondary">Browse the catalogue</Button>
        </Link>
      </div>
    </div>
  )
}
