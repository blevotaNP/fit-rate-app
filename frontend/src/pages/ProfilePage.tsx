import { useSearchParams, useParams, Link } from 'react-router-dom'
import { MapPin, CalendarDays, UserPlus, UserCheck } from 'lucide-react'
import clsx from 'clsx'
import {
  useProfile,
  useToggleFollow,
  useUserLooks,
  useUserReviews,
  useUserWardrobe,
} from '@/lib/queries'
import { formatDate, formatCount } from '@/lib/format'
import { Avatar } from '@/components/ui/Avatar'
import { Button } from '@/components/ui/Button'
import { LookGrid } from '@/components/look/LookGrid'
import { ReviewCard } from '@/components/review/ReviewCard'
import { Skeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { useAuth } from '@/lib/auth'
import { useToast } from '@/components/ui/Toast'
import { errorMessage, isNotFound } from '@/lib/api'
import { ErrorState } from '@/components/ui/ErrorState'
import { NotFoundPage } from './NotFoundPage'

type Tab = 'looks' | 'reviews' | 'wardrobe'

const TABS: { value: Tab; label: string }[] = [
  { value: 'looks', label: 'Published' },
  { value: 'reviews', label: 'Verdicts' },
  { value: 'wardrobe', label: 'Wardrobe' },
]

export function ProfilePage() {
  const { username = '' } = useParams()
  const [params, setParams] = useSearchParams()
  const { user } = useAuth()
  const { notify } = useToast()

  const tab = (params.get('tab') as Tab | null) ?? 'looks'
  const profile = useProfile(username)
  const toggleFollow = useToggleFollow(username)

  // Only the visible tab is fetched; loading all three on mount tripled the requests for data
  // most visitors never look at.
  const looks = useUserLooks(username, 0, tab === 'looks')
  const reviews = useUserReviews(username, 0, tab === 'reviews')
  const wardrobe = useUserWardrobe(username, 0, tab === 'wardrobe')

  if (profile.isError) {
    return isNotFound(profile.error) ? (
      <NotFoundPage />
    ) : (
      <ErrorState
        error={profile.error}
        title="Could not load this profile"
        onRetry={() => profile.refetch()}
      />
    )
  }
  if (!profile.data) return <ProfileSkeleton />

  const data = profile.data
  const isSelf = user?.id === data.id

  return (
    <div className="space-y-8">
      <header className="panel flex flex-col gap-5 p-5 sm:flex-row sm:items-start sm:gap-6 sm:p-6">
        <Avatar user={data} size="xl" className="shrink-0" />

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
            <h1 className="font-display text-2xl font-black tracking-tight text-mist-50">
              {data.displayName}
            </h1>
            {data.role !== 'USER' && (
              <span className="rounded bg-ink-750 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-mist-300">
                {data.role}
              </span>
            )}
          </div>
          <p className="text-sm text-mist-500">@{data.username}</p>

          {data.bio && (
            <p className="mt-3 max-w-xl text-sm leading-relaxed text-mist-300">{data.bio}</p>
          )}

          <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-mist-500">
            {data.location && (
              <span className="inline-flex items-center gap-1.5">
                <MapPin size={12} /> {data.location}
              </span>
            )}
            <span className="inline-flex items-center gap-1.5">
              <CalendarDays size={12} /> Joined {formatDate(data.joinedAt)}
            </span>
          </div>

          <dl className="mt-5 grid grid-cols-2 gap-x-6 gap-y-3 sm:grid-cols-4">
            <StatCell label="Verdicts given" value={data.stats.ratingsGiven} />
            <StatCell label="Reviews written" value={data.stats.reviewsWritten} />
            <StatCell label="Looks published" value={data.stats.looksPublished} />
            <StatCell
              label="Average score given"
              value={data.stats.averageScoreGiven}
              raw={data.stats.ratingsGiven > 0 ? data.stats.averageScoreGiven.toFixed(1) : '—'}
            />
            <StatCell label="Followers" value={data.stats.followers} />
            <StatCell label="Following" value={data.stats.following} />
            <StatCell label="Saved" value={data.stats.favorites} />
            <StatCell label="Helpful votes" value={data.stats.likesReceived} />
          </dl>
        </div>

        <div className="shrink-0">
          {isSelf ? (
            <Link to="/settings">
              <Button variant="secondary" size="sm">
                Edit profile
              </Button>
            </Link>
          ) : user ? (
            <Button
              variant={data.followedByMe ? 'secondary' : 'primary'}
              size="sm"
              icon={data.followedByMe ? <UserCheck size={15} /> : <UserPlus size={15} />}
              loading={toggleFollow.isPending}
              onClick={() =>
                toggleFollow.mutate(Boolean(data.followedByMe), {
                  onError: (error) => notify(errorMessage(error), 'error'),
                })
              }
            >
              {data.followedByMe ? 'Following' : 'Follow'}
            </Button>
          ) : null}
        </div>
      </header>

      <div className="border-b border-ink-800">
        <div className="flex gap-1">
          {TABS.map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => {
                const next = new URLSearchParams(params)
                next.set('tab', option.value)
                setParams(next, { replace: true })
              }}
              className={clsx(
                '-mb-px border-b-2 px-3 py-2.5 text-sm font-medium transition-colors',
                tab === option.value
                  ? 'border-flare-500 text-mist-50'
                  : 'border-transparent text-mist-500 hover:text-mist-200',
              )}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      {tab === 'looks' && (
        <LookGrid
          looks={looks.data?.content}
          loading={looks.isLoading}
          error={looks.error}
          onRetry={() => looks.refetch()}
          emptyTitle="Nothing published yet"
          emptyHint={
            isSelf ? 'Submit your first outfit or garment.' : 'This member has not posted yet.'
          }
        />
      )}

      {tab === 'reviews' &&
        (reviews.isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 4 }).map((_, index) => (
              <Skeleton key={index} className="h-36 rounded-card" />
            ))}
          </div>
        ) : reviews.data && reviews.data.content.length > 0 ? (
          <div className="space-y-3">
            {reviews.data.content.map((review) => (
              <ReviewCard key={review.id} review={review} showLook />
            ))}
          </div>
        ) : (
          <EmptyState title="No verdicts yet" hint="Scores this member leaves will show up here." />
        ))}

      {tab === 'wardrobe' && (
        <LookGrid
          looks={wardrobe.data?.content}
          loading={wardrobe.isLoading}
          error={wardrobe.error}
          onRetry={() => wardrobe.refetch()}
          emptyTitle="Wardrobe is empty"
          emptyHint="Saved looks land here."
        />
      )}
    </div>
  )
}

function StatCell({ label, value, raw }: { label: string; value: number; raw?: string }) {
  return (
    <div>
      <dt className="text-[11px] uppercase tracking-wider text-mist-500">{label}</dt>
      <dd className="numeric font-display text-lg font-bold text-mist-50">
        {raw ?? formatCount(value)}
      </dd>
    </div>
  )
}

function ProfileSkeleton() {
  return (
    <div className="space-y-8">
      <Skeleton className="h-56 rounded-card" />
      <Skeleton className="h-10 w-72" />
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        {Array.from({ length: 5 }).map((_, index) => (
          <Skeleton key={index} className="aspect-[3/4] rounded-card" />
        ))}
      </div>
    </div>
  )
}
