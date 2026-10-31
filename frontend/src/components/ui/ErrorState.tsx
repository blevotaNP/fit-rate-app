import { CloudOff, RotateCw } from 'lucide-react'
import { Button } from './Button'
import { errorMessage } from '@/lib/api'

/**
 * Shown when a query fails. Distinct from EmptyState on purpose: "nothing matched your filters"
 * and "we could not reach the server" need different wording and different next steps.
 */
export function ErrorState({
  error,
  onRetry,
  title = 'Could not load this',
}: {
  error: unknown
  onRetry?: () => void
  title?: string
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-card border border-dashed border-ink-700 bg-ink-900/40 px-6 py-14 text-center">
      <CloudOff size={28} strokeWidth={1.4} className="mb-3 text-mist-500" />
      <h3 className="font-display text-base font-semibold text-mist-200">{title}</h3>
      <p className="mt-1.5 max-w-sm text-sm text-mist-500">{errorMessage(error)}</p>
      {onRetry && (
        <Button className="mt-5" variant="secondary" icon={<RotateCw size={14} />} onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}
