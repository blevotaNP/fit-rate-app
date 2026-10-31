import { Link } from 'react-router-dom'
import { Bookmark, MessageSquareText } from 'lucide-react'
import clsx from 'clsx'
import type { LookSummary } from '@/lib/types'
import { formatPrice, formatScore, humanise, scoreTextClass } from '@/lib/format'
import { useAuth } from '@/lib/auth'
import { useToggleFavorite } from '@/lib/queries'
import { useToast } from '@/components/ui/Toast'
import { errorMessage } from '@/lib/api'

interface LookCardProps {
  look: LookSummary
  /** Position in a leaderboard, rendered as a large numeral over the image. */
  rank?: number
  priority?: boolean
}

export function LookCard({ look, rank, priority = false }: LookCardProps) {
  const { user } = useAuth()
  const { notify } = useToast()
  const toggleFavorite = useToggleFavorite()
  const price = formatPrice(look.priceCents, look.currency)

  const onToggleFavorite = (event: React.MouseEvent) => {
    event.preventDefault()
    event.stopPropagation()
    if (!user) {
      notify('Sign in to save looks to your wardrobe', 'info')
      return
    }
    toggleFavorite.mutate(look.slug, {
      onSuccess: (data) =>
        notify(data.favorited ? 'Saved to your wardrobe' : 'Removed from your wardrobe', 'success'),
      onError: (error) => notify(errorMessage(error), 'error'),
    })
  }

  return (
    <article className="group animate-rise">
      <Link to={`/looks/${look.slug}`} className="block">
        <div className="relative overflow-hidden rounded-card border border-ink-800 bg-ink-850">
          <img
            src={look.imageUrl}
            alt={look.title}
            loading={priority ? 'eager' : 'lazy'}
            decoding="async"
            className="aspect-[3/4] w-full object-cover transition-transform duration-500 ease-out group-hover:scale-[1.04]"
          />

          {/* A wash at the top keeps the score readable over pale photography. */}
          <div className="pointer-events-none absolute inset-x-0 top-0 h-24 bg-gradient-to-b from-black/65 to-transparent" />

          {rank !== undefined && (
            <span className="numeric absolute bottom-2 left-3 font-display text-4xl font-black leading-none text-white/90 drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]">
              {rank}
            </span>
          )}

          <div className="absolute left-3 top-3 flex items-center gap-1.5">
            <span className="rounded-md bg-black/55 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-white/85 backdrop-blur-sm">
              {look.type === 'ITEM' ? 'Item' : 'Outfit'}
            </span>
            {look.featured && (
              <span className="rounded-md bg-flare-500 px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-ink-950">
                Pick
              </span>
            )}
          </div>

          <div className="absolute right-3 top-3 flex flex-col items-end gap-2">
            <span
              className={clsx(
                'numeric rounded-lg bg-black/60 px-2 py-1 font-display text-base font-bold leading-none backdrop-blur-sm',
                scoreTextClass(look.avgScore),
              )}
              title={`${look.ratingsCount} verdicts`}
            >
              {formatScore(look.avgScore)}
            </span>

            <button
              type="button"
              onClick={onToggleFavorite}
              aria-label={look.favoritedByMe ? 'Remove from wardrobe' : 'Save to wardrobe'}
              aria-pressed={Boolean(look.favoritedByMe)}
              className={clsx(
                'grid h-8 w-8 place-items-center rounded-lg bg-black/55 backdrop-blur-sm transition-all',
                look.favoritedByMe
                  ? 'text-flare-400 opacity-100'
                  : 'text-white/80 opacity-0 hover:text-white focus-visible:opacity-100 group-hover:opacity-100',
              )}
            >
              <Bookmark size={15} fill={look.favoritedByMe ? 'currentColor' : 'none'} />
            </button>
          </div>
        </div>
      </Link>

      <div className="mt-2.5 space-y-1">
        <div className="flex items-start justify-between gap-2">
          <Link
            to={`/looks/${look.slug}`}
            className="line-clamp-1 font-display text-[15px] font-semibold text-mist-50 transition-colors hover:text-flare-400"
          >
            {look.title}
          </Link>
          {price && <span className="numeric shrink-0 text-xs text-mist-400">{price}</span>}
        </div>

        <div className="flex items-center gap-1.5 text-xs text-mist-500">
          {look.brand ? (
            <Link
              to={`/looks?brand=${look.brand.slug}`}
              className="truncate transition-colors hover:text-mist-200"
            >
              {look.brand.name}
            </Link>
          ) : (
            <span className="truncate">{humanise(look.category)}</span>
          )}
          <span aria-hidden>·</span>
          <span className="numeric inline-flex items-center gap-1 whitespace-nowrap">
            <MessageSquareText size={11} />
            {look.ratingsCount}
          </span>
        </div>
      </div>
    </article>
  )
}
