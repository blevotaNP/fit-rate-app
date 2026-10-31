import { Link } from 'react-router-dom'
import { ArrowRight, Flame, Sparkles, Trophy } from 'lucide-react'
import clsx from 'clsx'
import {
  useChart,
  useFeaturedLooks,
  useLooks,
  usePopularTags,
  useReviewFeed,
  useStats,
  useTrending,
} from '@/lib/queries'
import { formatCount, formatScore, humanise, scoreTextClass } from '@/lib/format'
import { LookCard } from '@/components/look/LookCard'
import { LookGrid } from '@/components/look/LookGrid'
import { ReviewCard } from '@/components/review/ReviewCard'
import { TagPill } from '@/components/ui/TagPill'
import { Skeleton } from '@/components/ui/Skeleton'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/lib/auth'
import type { ChartEntry } from '@/lib/types'

export function HomePage() {
  const { user } = useAuth()
  const stats = useStats()
  const featured = useFeaturedLooks(5)
  const trending = useTrending(10)
  const weekly = useChart('WEEK', 5)
  const latest = useLooks({ sort: 'NEWEST', size: 10 })
  const reviews = useReviewFeed(0, 6)
  const tags = usePopularTags(16)

  const hero = featured.data?.[0]
  const runnersUp = featured.data?.slice(1, 5) ?? []

  return (
    <div className="space-y-14">
      {/* --- Hero ------------------------------------------------------------ */}
      <section className="grid gap-4 lg:grid-cols-[1.55fr_1fr]">
        {hero ? (
          <Link
            to={`/looks/${hero.slug}`}
            /* The image is absolutely positioned so it fills whatever height the grid row
               settles on, instead of leaving a black band under a fixed-height photo. */
            className="group relative block min-h-[340px] overflow-hidden rounded-card border border-ink-800 sm:min-h-[440px] lg:min-h-[500px]"
          >
            <img
              src={hero.imageUrl}
              alt={hero.title}
              className="absolute inset-0 h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/25 to-black/10" />

            <div className="absolute inset-x-0 bottom-0 p-5 sm:p-7">
              <p className="mb-2 inline-flex items-center gap-1.5 rounded-full bg-flare-500 px-2.5 py-1 text-[11px] font-bold uppercase tracking-widest text-ink-950">
                Editor pick
              </p>
              <h1 className="max-w-lg font-display text-2xl font-black leading-tight text-white sm:text-4xl">
                {hero.title}
              </h1>
              <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-white/70">
                {hero.brand && <span className="text-white/90">{hero.brand.name}</span>}
                <span>{humanise(hero.category)}</span>
                <span className="numeric">{hero.ratingsCount} verdicts</span>
                <span
                  className={clsx(
                    'numeric font-display text-2xl font-black',
                    scoreTextClass(hero.avgScore),
                  )}
                >
                  {formatScore(hero.avgScore)}
                </span>
              </div>
            </div>
          </Link>
        ) : (
          <Skeleton className="h-[340px] w-full rounded-card sm:h-[440px] lg:h-[500px]" />
        )}

        <div className="grid grid-cols-2 grid-rows-2 gap-3 lg:h-[500px]">
          {runnersUp.map((look) => (
              <Link
                key={look.id}
                to={`/looks/${look.slug}`}
                className="group relative block aspect-[4/3] overflow-hidden rounded-card border border-ink-800 lg:aspect-auto"
              >
                <img
                  src={look.imageUrl}
                  alt={look.title}
                  loading="lazy"
                  className="absolute inset-0 h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/85 to-transparent" />
                <div className="absolute inset-x-0 bottom-0 flex items-end justify-between gap-2 p-2.5">
                  <span className="line-clamp-2 text-xs font-medium leading-tight text-white">
                    {look.title}
                  </span>
                  <span
                    className={clsx(
                      'numeric shrink-0 font-display text-sm font-bold',
                      scoreTextClass(look.avgScore),
                    )}
                  >
                    {formatScore(look.avgScore)}
                  </span>
                </div>
              </Link>
          ))}
        </div>
      </section>

      {/* --- Proposition band ------------------------------------------------ */}
      <section className="panel p-5 sm:p-6">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
          <div className="max-w-2xl">
            <h2 className="font-display text-xl font-bold leading-snug text-mist-50 sm:text-2xl">
              Five criteria. One honest number.
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-mist-400">
              Every outfit and garment here is scored on style, fit, colour, originality and
              quality. One verdict per person per look, averages recomputed on every write, and
              nothing can be bought into the leaderboards.
            </p>
          </div>

          <div className="flex shrink-0 gap-2">
            <Link to="/looks">
              <Button variant="primary">Browse catalogue</Button>
            </Link>
            {!user && (
              <Link to="/register">
                <Button variant="secondary">Join and rate</Button>
              </Link>
            )}
          </div>
        </div>

        {stats.data && (
          <dl className="mt-6 grid grid-cols-2 gap-x-6 gap-y-4 border-t border-ink-800 pt-5 sm:grid-cols-4">
            <Stat label="Looks" value={stats.data.looks} />
            <Stat label="Verdicts" value={stats.data.ratings} />
            <Stat label="Written reviews" value={stats.data.reviews} />
            <Stat label="Members" value={stats.data.members} />
          </dl>
        )}
      </section>

      {/* --- Trending rail --------------------------------------------------- */}
      <Section
        title="Trending right now"
        icon={<Flame size={14} />}
        hint="Most discussed in the last two days"
        to="/looks?sort=MOST_RATED"
      >
        {trending.isLoading ? (
          <div className="flex gap-4 overflow-hidden">
            {Array.from({ length: 6 }).map((_, index) => (
              <Skeleton key={index} className="h-56 w-40 shrink-0 rounded-card" />
            ))}
          </div>
        ) : (
          <div className="rail -mx-4 flex snap-x gap-4 overflow-x-auto px-4 pb-3 sm:mx-0 sm:px-0">
            {trending.data?.map((entry) => (
              <div key={entry.look.id} className="w-40 shrink-0 snap-start sm:w-44">
                <LookCard look={entry.look} />
              </div>
            ))}
          </div>
        )}
      </Section>

      {/* --- Weekly leaderboard ---------------------------------------------- */}
      <Section
        title="Top of the week"
        icon={<Trophy size={14} />}
        hint="Ranked by the verdicts written this week"
        to="/charts"
      >
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {weekly.isLoading
            ? Array.from({ length: 5 }).map((_, index) => (
                <Skeleton key={index} className="aspect-[3/4] rounded-card" />
              ))
            : weekly.data?.map((entry: ChartEntry) => (
                <LookCard key={entry.look.id} look={entry.look} rank={entry.rank} />
              ))}
        </div>
      </Section>

      {/* --- Latest ----------------------------------------------------------- */}
      <Section title="Just added" hint="Newest submissions from the community" to="/looks">
        <LookGrid
          looks={latest.data?.content}
          loading={latest.isLoading}
          error={latest.error}
          onRetry={() => latest.refetch()}
          skeletonCount={10}
        />
      </Section>

      {/* --- Reviews + tags --------------------------------------------------- */}
      <section className="grid gap-8 lg:grid-cols-[1.6fr_1fr]">
        <div>
          <div className="section-title mb-4">
            <span>Latest reviews</span>
            <Link to="/looks" className="ml-auto text-xs normal-case tracking-normal text-mist-500 hover:text-mist-200">
              Browse all
            </Link>
          </div>
          <div className="space-y-3">
            {reviews.isLoading
              ? Array.from({ length: 3 }).map((_, index) => (
                  <Skeleton key={index} className="h-40 rounded-card" />
                ))
              : reviews.data?.content.map((review) => (
                  <ReviewCard key={review.id} review={review} showLook />
                ))}
          </div>
        </div>

        <div className="space-y-6">
          <div>
            <div className="section-title mb-3">
              <span>Popular tags</span>
            </div>
            <div className="flex flex-wrap gap-2">
              {tags.data?.map((tag) => <TagPill key={tag.id} tag={tag} count={tag.usageCount} />)}
            </div>
          </div>

          {!user && (
            <div className="panel p-5">
              <Sparkles size={18} className="text-flare-500" />
              <h3 className="mt-2.5 font-display text-base font-bold text-mist-50">
                Recommendations that learn
              </h3>
              <p className="mt-1.5 text-sm leading-relaxed text-mist-400">
                Rate a handful of looks and the For You page starts matching the tags, categories
                and labels you actually score highly.
              </p>
              <Link to="/register" className="mt-4 block">
                <Button variant="accent" size="sm" full>
                  Create an account
                </Button>
              </Link>
            </div>
          )}

          {stats.data && (
            <div className="panel p-5">
              <div className="section-title mb-3">
                <span>By the numbers</span>
              </div>
              <dl className="space-y-2.5 text-sm">
                <Row label="Complete outfits" value={stats.data.outfits} />
                <Row label="Individual garments" value={stats.data.items} />
                <Row label="Written reviews" value={stats.data.reviews} />
                <Row label="Brands tracked" value={stats.data.brands} />
              </dl>
            </div>
          )}
        </div>
      </section>
    </div>
  )
}

function Section({
  title,
  hint,
  icon,
  to,
  children,
}: {
  title: string
  hint?: string
  icon?: React.ReactNode
  to?: string
  children: React.ReactNode
}) {
  return (
    <section>
      <div className="mb-4 flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <h2 className="section-title">
          {icon && <span className="text-flare-500">{icon}</span>}
          <span>{title}</span>
        </h2>
        {hint && <p className="text-xs text-mist-500">{hint}</p>}
        {to && (
          <Link
            to={to}
            className="ml-auto inline-flex items-center gap-1 text-xs text-mist-500 transition-colors hover:text-mist-100"
          >
            See all <ArrowRight size={13} />
          </Link>
        )}
      </div>
      {children}
    </section>
  )
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <dt className="text-[11px] uppercase tracking-wider text-mist-500">{label}</dt>
      <dd className="numeric font-display text-xl font-black text-mist-50">
        {formatCount(value)}
      </dd>
    </div>
  )
}

function Row({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex items-baseline justify-between gap-3">
      <dt className="text-mist-400">{label}</dt>
      <dd className="numeric font-semibold text-mist-100">{formatCount(value)}</dd>
    </div>
  )
}
