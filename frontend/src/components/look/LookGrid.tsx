import type { LookSummary } from '@/lib/types'
import { LookCard } from './LookCard'
import { GridSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'

interface LookGridProps {
  looks: LookSummary[] | undefined
  loading?: boolean
  error?: unknown
  onRetry?: () => void
  emptyTitle?: string
  emptyHint?: string
  skeletonCount?: number
}

export function LookGrid({
  looks,
  loading = false,
  error,
  onRetry,
  emptyTitle = 'Nothing here yet',
  emptyHint = 'Try widening the filters.',
  skeletonCount = 10,
}: LookGridProps) {
  // A failed request is not an empty result: saying "no matches" would send people off
  // rewriting filters that were never the problem.
  if (error) {
    return <ErrorState error={error} onRetry={onRetry} />
  }
  if (loading && !looks) {
    return <GridSkeleton count={skeletonCount} />
  }
  if (!looks || looks.length === 0) {
    return <EmptyState title={emptyTitle} hint={emptyHint} />
  }
  return (
    <div className="grid grid-cols-2 gap-x-4 gap-y-7 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
      {looks.map((look, index) => (
        <LookCard key={look.id} look={look} priority={index < 5} />
      ))}
    </div>
  )
}
