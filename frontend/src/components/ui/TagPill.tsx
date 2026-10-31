import { Link } from 'react-router-dom'
import { cn } from '@/lib/cn'
import type { TagRef } from '@/lib/types'

export function TagPill({
  tag,
  count,
  className,
}: {
  tag: Pick<TagRef, 'name' | 'slug'>
  count?: number | null
  className?: string
}) {
  return (
    <Link
      to={`/looks?tag=${tag.slug}`}
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full border border-ink-700 bg-ink-850 px-2.5 py-1 text-xs text-mist-400 transition-colors hover:border-ink-600 hover:text-mist-100',
        className,
      )}
    >
      <span className="text-mist-600">#</span>
      {tag.name}
      {count != null && <span className="numeric text-mist-600">{count}</span>}
    </Link>
  )
}
