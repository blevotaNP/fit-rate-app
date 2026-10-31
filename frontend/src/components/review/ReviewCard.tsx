import { Link } from 'react-router-dom'
import { Heart, Trash2 } from 'lucide-react'
import clsx from 'clsx'
import type { Review } from '@/lib/types'
import { CRITERIA, scoreTextClass, timeAgo } from '@/lib/format'
import { Avatar } from '@/components/ui/Avatar'
import { useAuth } from '@/lib/auth'
import { useDeleteReview, useToggleReviewLike } from '@/lib/queries'
import { useToast } from '@/components/ui/Toast'
import { errorMessage } from '@/lib/api'

interface ReviewCardProps {
  review: Review
  /** Feed context: show which look the verdict is about. */
  showLook?: boolean
  lookSlug?: string
}

export function ReviewCard({ review, showLook = false, lookSlug }: ReviewCardProps) {
  const { user } = useAuth()
  const { notify } = useToast()
  const toggleLike = useToggleReviewLike()
  const deleteReview = useDeleteReview(lookSlug)

  const like = () => {
    if (!user) {
      notify('Sign in to mark reviews as helpful', 'info')
      return
    }
    toggleLike.mutate(review.id, { onError: (error) => notify(errorMessage(error), 'error') })
  }

  const remove = () => {
    if (!window.confirm('Delete this review? This cannot be undone.')) return
    deleteReview.mutate(review.id, {
      onSuccess: () => notify('Review deleted', 'success'),
      onError: (error) => notify(errorMessage(error), 'error'),
    })
  }

  return (
    <article className="panel animate-rise p-4 sm:p-5">
      <header className="flex items-start gap-3">
        {/* The avatar is decorative, so without a label this link announces as just "link". */}
        <Link
          to={`/u/${review.author.username}`}
          aria-label={`${review.author.displayName}'s profile`}
          className="shrink-0"
        >
          <Avatar user={review.author} size="sm" />
        </Link>

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
            <Link
              to={`/u/${review.author.username}`}
              className="text-sm font-medium text-mist-100 transition-colors hover:text-flare-400"
            >
              {review.author.displayName}
            </Link>
            {review.author.role !== 'USER' && (
              <span className="rounded bg-ink-750 px-1.5 py-px text-[10px] font-semibold uppercase tracking-wider text-mist-400">
                {review.author.role}
              </span>
            )}
            <span className="text-xs text-mist-500">{timeAgo(review.createdAt)}</span>
          </div>

          {showLook && review.look && (
            <Link
              to={`/looks/${review.look.slug}`}
              className="mt-0.5 block truncate text-xs text-mist-500 transition-colors hover:text-mist-200"
            >
              on <span className="text-mist-300">{review.look.title}</span>
            </Link>
          )}
        </div>

        <span
          className={clsx(
            'numeric shrink-0 font-display text-2xl font-black leading-none',
            scoreTextClass(review.score),
          )}
        >
          {review.score}
        </span>
      </header>

      {/* Per-criterion numbers, compact enough to sit in a feed. */}
      <ul className="mt-3 flex flex-wrap gap-x-4 gap-y-1">
        {CRITERIA.map(({ key, label }) => (
          <li key={key} className="flex items-baseline gap-1 text-[11px] text-mist-500">
            {label}
            <span className="numeric font-semibold text-mist-300">{review[key]}</span>
          </li>
        ))}
      </ul>

      {(review.title || review.body) && (
        <div className="mt-3">
          {review.title && (
            <h4 className="font-display text-[15px] font-semibold text-mist-50">{review.title}</h4>
          )}
          {review.body && (
            <p className="mt-1.5 whitespace-pre-line text-sm leading-relaxed text-mist-300">
              {review.body}
            </p>
          )}
        </div>
      )}

      <footer className="mt-3.5 flex items-center gap-1">
        <button
          type="button"
          onClick={like}
          disabled={toggleLike.isPending}
          aria-pressed={Boolean(review.likedByMe)}
          className={clsx(
            'inline-flex items-center gap-1.5 rounded-lg px-2 py-1 text-xs transition-colors',
            review.likedByMe
              ? 'text-flare-400 hover:bg-flare-500/10'
              : 'text-mist-500 hover:bg-ink-800 hover:text-mist-200',
          )}
        >
          <Heart size={14} fill={review.likedByMe ? 'currentColor' : 'none'} />
          <span className="numeric">{review.likesCount}</span>
          <span className="sr-only">helpful votes</span>
        </button>

        {review.editable && (
          <button
            type="button"
            onClick={remove}
            disabled={deleteReview.isPending}
            className="ml-auto inline-flex items-center gap-1.5 rounded-lg px-2 py-1 text-xs text-mist-500 transition-colors hover:bg-ink-800 hover:text-flare-400"
          >
            <Trash2 size={14} /> Delete
          </button>
        )}
      </footer>
    </article>
  )
}
