import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Bookmark, Calendar, MessageCircle, Pencil, Share2, Star, Trash2, Users } from 'lucide-react'
import clsx from 'clsx'
import {
  useDeleteLook,
  useDeleteReview,
  useLook,
  useLookComments,
  useLookReviews,
  useSaveReview,
  useSimilarLooks,
  useToggleFavorite,
} from '@/lib/queries'
import { formatDate, formatPrice, humanise, scoreLabel, scoreTextClass } from '@/lib/format'
import { ScoreBadge } from '@/components/ui/ScoreBadge'
import { ScoreBreakdownBars } from '@/components/look/ScoreBreakdown'
import { RatingForm } from '@/components/look/RatingForm'
import { ReviewCard } from '@/components/review/ReviewCard'
import { CommentThread } from '@/components/look/CommentThread'
import { LookCard } from '@/components/look/LookCard'
import { Avatar } from '@/components/ui/Avatar'
import { Button } from '@/components/ui/Button'
import { TagPill } from '@/components/ui/TagPill'
import { Skeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { useAuth } from '@/lib/auth'
import { useToast } from '@/components/ui/Toast'
import { errorMessage, isNotFound } from '@/lib/api'
import { ErrorState } from '@/components/ui/ErrorState'
import { NotFoundPage } from './NotFoundPage'

const REVIEW_SORTS = [
  { value: 'newest', label: 'Newest' },
  { value: 'helpful', label: 'Most helpful' },
  { value: 'highest', label: 'Highest' },
  { value: 'lowest', label: 'Lowest' },
]

export function LookPage() {
  const { slug = '' } = useParams()
  const navigate = useNavigate()
  const { user, refreshUser } = useAuth()
  const { notify } = useToast()

  const [tab, setTab] = useState<'reviews' | 'discussion'>('reviews')
  const [reviewSort, setReviewSort] = useState('newest')
  const [reviewPage, setReviewPage] = useState(0)

  // Jumping to a related look must not carry over page 3 of the previous look's reviews.
  // Reset during render so the new look never paints with the old look's tab or page.
  const [renderedSlug, setRenderedSlug] = useState(slug)
  if (renderedSlug !== slug) {
    setRenderedSlug(slug)
    setTab('reviews')
    setReviewSort('newest')
    setReviewPage(0)
  }

  const look = useLook(slug)
  const reviews = useLookReviews(slug, reviewSort, reviewPage)
  const comments = useLookComments(slug)
  const similar = useSimilarLooks(slug, 5)
  const saveReview = useSaveReview(slug)
  const deleteReview = useDeleteReview(slug)
  const toggleFavorite = useToggleFavorite()
  const deleteLook = useDeleteLook()

  if (look.isError) {
    // A dead API must not masquerade as a deleted look.
    return isNotFound(look.error) ? (
      <NotFoundPage />
    ) : (
      <ErrorState error={look.error} title="Could not load this look" onRetry={() => look.refetch()} />
    )
  }
  if (!look.data) {
    return <DetailSkeleton />
  }

  const data = look.data
  const price = formatPrice(data.priceCents, data.currency)
  const isOwnLook = user?.id === data.author.id

  const share = async () => {
    const url = window.location.href
    try {
      if (navigator.share) {
        await navigator.share({ title: data.title, url })
      } else {
        await navigator.clipboard.writeText(url)
        notify('Link copied to clipboard', 'success')
      }
    } catch {
      // The user dismissed the share sheet; nothing to report.
    }
  }

  return (
    <div className="space-y-12">
      <nav className="flex flex-wrap items-center gap-1.5 text-xs text-mist-500">
        <Link to="/looks" className="transition-colors hover:text-mist-200">
          Catalogue
        </Link>
        <span aria-hidden>/</span>
        <Link
          to={`/looks?category=${data.category}`}
          className="transition-colors hover:text-mist-200"
        >
          {humanise(data.category)}
        </Link>
        <span aria-hidden>/</span>
        <span className="truncate text-mist-300">{data.title}</span>
      </nav>

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_400px]">
        {/* --- Imagery + discussion ---------------------------------------- */}
        <div className="space-y-8">
          <div className="overflow-hidden rounded-card border border-ink-800 bg-ink-900">
            <img
              src={data.imageUrl}
              alt={data.title}
              className="max-h-[720px] w-full object-cover"
            />
          </div>

          {data.description && (
            <p className="max-w-2xl whitespace-pre-line text-[15px] leading-relaxed text-mist-300">
              {data.description}
            </p>
          )}

          <div className="border-b border-ink-800">
            <div className="flex gap-1">
              <TabButton active={tab === 'reviews'} onClick={() => setTab('reviews')}>
                Reviews <span className="numeric text-mist-500">{data.ratingsCount}</span>
              </TabButton>
              <TabButton active={tab === 'discussion'} onClick={() => setTab('discussion')}>
                Discussion <span className="numeric text-mist-500">{data.commentsCount}</span>
              </TabButton>
            </div>
          </div>

          {tab === 'reviews' ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between gap-3">
                <label className="flex items-center gap-2 text-sm text-mist-500">
                  Sort
                  <select
                    value={reviewSort}
                    onChange={(event) => {
                      setReviewSort(event.target.value)
                      setReviewPage(0)
                    }}
                    className="h-8 rounded-lg border border-ink-700 bg-ink-850 px-2 text-sm text-mist-100 focus:border-flare-500 focus:outline-none"
                  >
                    {REVIEW_SORTS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              {reviews.isLoading ? (
                <div className="space-y-3">
                  {Array.from({ length: 3 }).map((_, index) => (
                    <Skeleton key={index} className="h-36 rounded-card" />
                  ))}
                </div>
              ) : reviews.data && reviews.data.content.length > 0 ? (
                <>
                  <div className="space-y-3">
                    {reviews.data.content.map((review) => (
                      <ReviewCard key={review.id} review={review} lookSlug={slug} />
                    ))}
                  </div>
                  {reviews.data.totalPages > 1 && (
                    <div className="flex items-center justify-center gap-3">
                      <Button
                        variant="secondary"
                        size="sm"
                        disabled={reviewPage === 0}
                        onClick={() => setReviewPage((current) => current - 1)}
                      >
                        Previous
                      </Button>
                      <span className="numeric text-sm text-mist-400">
                        {reviewPage + 1} / {reviews.data.totalPages}
                      </span>
                      <Button
                        variant="secondary"
                        size="sm"
                        disabled={!reviews.data.hasNext}
                        onClick={() => setReviewPage((current) => current + 1)}
                      >
                        Next
                      </Button>
                    </div>
                  )}
                </>
              ) : (
                <EmptyState
                  title="No verdicts yet"
                  hint="Be the first to score this one."
                  icon={<Star size={28} strokeWidth={1.4} className="text-mist-500" />}
                />
              )}
            </div>
          ) : (
            <CommentThread slug={slug} comments={comments.data ?? []} />
          )}
        </div>

        {/* --- Details sidebar ----------------------------------------------- */}
        {/* Sticky, but capped to the viewport and scrollable inside it: the sidebar is taller
            than most screens, and a plain sticky block pins its top and puts the rating form
            permanently out of reach. */}
        <aside className="rail space-y-5 lg:sticky lg:top-[88px] lg:max-h-[calc(100vh-7rem)] lg:self-start lg:overflow-y-auto lg:pr-1">
          <div>
            <div className="mb-2 flex flex-wrap items-center gap-2">
              <span className="rounded-md border border-ink-700 bg-ink-850 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-mist-400">
                {data.type === 'ITEM' ? 'Garment' : 'Outfit'}
              </span>
              {data.featured && (
                <span className="rounded-md bg-flare-500 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-ink-950">
                  Editor pick
                </span>
              )}
            </div>

            <h1 className="font-display text-2xl font-black leading-tight text-mist-50 sm:text-3xl">
              {data.title}
            </h1>

            {data.brand && (
              <Link
                to={`/looks?brand=${data.brand.slug}`}
                className="mt-1.5 inline-block text-sm text-mist-400 transition-colors hover:text-flare-400"
              >
                {data.brand.name}
                {data.brand.country && <span className="text-mist-600"> · {data.brand.country}</span>}
              </Link>
            )}
          </div>

          <div className="panel flex items-center gap-4 p-4">
            <ScoreBadge score={data.scores.overall} size="lg" count={data.ratingsCount} />
            <div className="min-w-0">
              <p className={clsx('font-display text-lg font-bold', scoreTextClass(data.scores.overall))}>
                {scoreLabel(data.scores.overall)}
              </p>
              <p className="mt-0.5 text-xs leading-relaxed text-mist-500">
                {data.ratingsCount === 0
                  ? 'No verdicts yet — the first one sets the tone.'
                  : `${data.ratingsCount} ${data.ratingsCount === 1 ? 'verdict' : 'verdicts'}, ${data.reviewsCount} written`}
              </p>
            </div>
          </div>

          {data.ratingsCount > 0 && (
            <div className="panel p-4">
              <p className="section-title mb-3.5">
                <span>Breakdown</span>
              </p>
              <ScoreBreakdownBars scores={data.scores} />
            </div>
          )}

          <div className="panel divide-y divide-ink-800 text-sm">
            <Detail label="Category" value={humanise(data.category)} />
            {data.season && <Detail label="Season" value={humanise(data.season)} />}
            {data.occasion && <Detail label="Occasion" value={humanise(data.occasion)} />}
            {price && <Detail label="Price" value={price} />}
            {data.releaseDate && <Detail label="Released" value={formatDate(data.releaseDate)} />}
            <div className="flex items-center justify-between gap-3 px-4 py-2.5">
              <span className="text-mist-500">Submitted by</span>
              <Link
                to={`/u/${data.author.username}`}
                className="flex items-center gap-2 transition-colors hover:text-flare-400"
              >
                <Avatar user={data.author} size="xs" />
                <span className="truncate text-mist-200">{data.author.displayName}</span>
              </Link>
            </div>
          </div>

          {data.tags.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {data.tags.map((tag) => (
                <TagPill key={tag.id} tag={tag} />
              ))}
            </div>
          )}

          <div className="flex gap-2">
            {/* Saved state is shown with an accent outline, not a filled accent button: a solid
                block of orange here out-shouts the score, which is the point of the page. */}
            <Button
              variant="secondary"
              className={clsx('flex-1', data.favoritedByMe && 'border-flare-500/50 text-flare-400')}
              icon={<Bookmark size={15} fill={data.favoritedByMe ? 'currentColor' : 'none'} />}
              loading={toggleFavorite.isPending}
              onClick={() => {
                if (!user) {
                  notify('Sign in to save looks', 'info')
                  return
                }
                toggleFavorite.mutate(slug, {
                  onError: (error) => notify(errorMessage(error), 'error'),
                })
              }}
            >
              {data.favoritedByMe ? 'Saved' : 'Save'}
            </Button>
            <Button variant="secondary" icon={<Share2 size={15} />} onClick={share}>
              Share
            </Button>
          </div>

          {/* Author and admin controls — the API already allows both, so the UI should too. */}
          {data.editable && (
            <div className="flex gap-2">
              <Link to={`/looks/${data.slug}/edit`} className="flex-1">
                <Button variant="secondary" icon={<Pencil size={15} />} full>
                  Edit
                </Button>
              </Link>
              <Button
                variant="danger"
                icon={<Trash2 size={15} />}
                loading={deleteLook.isPending}
                onClick={() => {
                  if (!window.confirm(`Withdraw “${data.title}”? Its verdicts go with it.`)) return
                  deleteLook.mutate(data.slug, {
                    onSuccess: () => {
                      notify('Look withdrawn', 'success')
                      navigate('/looks')
                    },
                    onError: (error) => notify(errorMessage(error), 'error'),
                  })
                }}
              >
                Withdraw
              </Button>
            </div>
          )}

          <div className="flex items-center gap-4 px-1 text-xs text-mist-500">
            <span className="numeric inline-flex items-center gap-1.5">
              <Users size={13} /> {data.favoritesCount} saved
            </span>
            <span className="numeric inline-flex items-center gap-1.5">
              <MessageCircle size={13} /> {data.commentsCount} comments
            </span>
            <span className="inline-flex items-center gap-1.5">
              <Calendar size={13} /> {formatDate(data.createdAt)}
            </span>
          </div>

          {/* --- Your verdict ------------------------------------------------ */}
          <div className="panel p-4">
            <p className="section-title mb-4">
              <span>
                {isOwnLook ? 'Your submission' : data.myReview ? 'Your verdict' : 'Rate this'}
              </span>
            </p>

            {!user ? (
              <div className="text-sm text-mist-400">
                <Link to="/login" className="text-flare-400 hover:underline">
                  Sign in
                </Link>{' '}
                to score this {data.type === 'ITEM' ? 'garment' : 'outfit'} across the five criteria.
              </div>
            ) : isOwnLook ? (
              <p className="text-sm text-mist-500">
                You submitted this one, so you cannot rate it. Everyone else can.
              </p>
            ) : (
              <RatingForm
                key={data.myReview?.id ?? 'new'}
                existing={data.myReview}
                submitting={saveReview.isPending}
                onSubmit={(payload) =>
                  saveReview.mutate(payload, {
                    onSuccess: () => {
                      notify('Your verdict is live', 'success')
                      // user.stats drives the For You banner and is not a query, so refresh it.
                      void refreshUser()
                    },
                    onError: (error) => notify(errorMessage(error), 'error'),
                  })
                }
                onDelete={
                  data.myReview
                    ? () => {
                        if (!window.confirm('Remove your verdict from this look?')) return
                        deleteReview.mutate(data.myReview!.id, {
                          onSuccess: () => {
                            notify('Verdict removed', 'success')
                            void refreshUser()
                          },
                          onError: (error) => notify(errorMessage(error), 'error'),
                        })
                      }
                    : undefined
                }
              />
            )}
          </div>
        </aside>
      </div>

      {similar.data && similar.data.length > 0 && (
        <section>
          <h2 className="section-title mb-4">
            <span>In the same vein</span>
          </h2>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
            {similar.data.map((item) => (
              <LookCard key={item.id} look={item} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3 px-4 py-2.5">
      <span className="text-mist-500">{label}</span>
      <span className="truncate text-mist-200">{value}</span>
    </div>
  )
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={clsx(
        '-mb-px inline-flex items-center gap-2 border-b-2 px-3 py-2.5 text-sm font-medium transition-colors',
        active
          ? 'border-flare-500 text-mist-50'
          : 'border-transparent text-mist-500 hover:text-mist-200',
      )}
    >
      {children}
    </button>
  )
}

function DetailSkeleton() {
  return (
    <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_400px]">
      <Skeleton className="h-[520px] rounded-card" />
      <div className="space-y-4">
        <Skeleton className="h-9 w-3/4" />
        <Skeleton className="h-24 rounded-card" />
        <Skeleton className="h-52 rounded-card" />
        <Skeleton className="h-64 rounded-card" />
      </div>
    </div>
  )
}
