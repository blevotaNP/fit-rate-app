import { useState } from 'react'
import { Link } from 'react-router-dom'
import clsx from 'clsx'
import { Trophy } from 'lucide-react'
import { useChart } from '@/lib/queries'
import { formatScore, humanise, scoreTextClass } from '@/lib/format'
import { TagPill } from '@/components/ui/TagPill'
import { Skeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import type { ChartEntry, ChartPeriod } from '@/lib/types'

const PERIODS: { value: ChartPeriod; label: string; blurb: string }[] = [
  { value: 'WEEK', label: 'This week', blurb: 'Ranked on the verdicts written in the last 7 days' },
  { value: 'MONTH', label: 'This month', blurb: 'Ranked on the verdicts written in the last 30 days' },
  { value: 'YEAR', label: 'This year', blurb: 'Ranked on the verdicts written in the last 12 months' },
  { value: 'ALL_TIME', label: 'All time', blurb: 'Everything the community has ever scored' },
]

export function ChartsPage() {
  const [period, setPeriod] = useState<ChartPeriod>('WEEK')
  const { data, isLoading, error, refetch } = useChart(period, 25)
  const active = PERIODS.find((entry) => entry.value === period)!

  return (
    <div className="space-y-7">
      <header>
        <h1 className="flex items-center gap-2.5 font-display text-2xl font-black tracking-tight text-mist-50 sm:text-3xl">
          <Trophy size={24} className="text-flare-500" />
          Leaderboards
        </h1>
        <p className="mt-1.5 max-w-2xl text-sm leading-relaxed text-mist-400">
          A period chart ranks by the mean of the verdicts left <em>inside</em> that window, not by
          an all-time average — so a look that peaked six months ago does not squat on the weekly
          chart forever. A minimum number of verdicts keeps single enthusiastic 100s out.
        </p>
      </header>

      <div className="flex flex-wrap gap-1 rounded-xl border border-ink-700 bg-ink-850 p-1">
        {PERIODS.map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => setPeriod(option.value)}
            className={clsx(
              'flex-1 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
              period === option.value
                ? 'bg-ink-700 text-mist-50'
                : 'text-mist-400 hover:text-mist-100',
            )}
          >
            {option.label}
          </button>
        ))}
      </div>

      <p className="text-xs text-mist-500">{active.blurb}</p>

      {error ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 8 }).map((_, index) => (
            <Skeleton key={index} className="h-28 rounded-card" />
          ))}
        </div>
      ) : !data || data.length === 0 ? (
        <EmptyState
          title="Not enough activity in this window"
          hint="A look needs a few verdicts before it can enter the chart. Try a longer period."
        />
      ) : (
        <ol className="space-y-3">
          {data.map((entry) => (
            <ChartRow key={entry.look.id} entry={entry} />
          ))}
        </ol>
      )}
    </div>
  )
}

function ChartRow({ entry }: { entry: ChartEntry }) {
  const { look, rank, periodScore, periodReviews } = entry
  const podium = rank <= 3

  return (
    <li
      className={clsx(
        'panel animate-rise flex items-stretch gap-3 overflow-hidden p-3 sm:gap-4',
        podium && 'border-ink-600',
      )}
    >
      <div className="flex w-8 shrink-0 items-center justify-center sm:w-12">
        <span
          className={clsx(
            'numeric font-display font-black leading-none',
            podium ? 'text-3xl text-mist-50 sm:text-4xl' : 'text-xl text-mist-500 sm:text-2xl',
          )}
        >
          {rank}
        </span>
      </div>

      <Link to={`/looks/${look.slug}`} className="shrink-0">
        <img
          src={look.imageUrl}
          alt={look.title}
          loading="lazy"
          className="h-24 w-[72px] rounded-lg object-cover sm:h-28 sm:w-[84px]"
        />
      </Link>

      <div className="flex min-w-0 flex-1 flex-col justify-center gap-1">
        <Link
          to={`/looks/${look.slug}`}
          className="line-clamp-1 font-display text-base font-semibold text-mist-50 transition-colors hover:text-flare-400"
        >
          {look.title}
        </Link>
        <p className="line-clamp-1 text-xs text-mist-500">
          {look.brand ? look.brand.name : humanise(look.category)} · by{' '}
          <Link to={`/u/${look.author.username}`} className="hover:text-mist-300">
            {look.author.displayName}
          </Link>
        </p>
        <p className="numeric text-xs text-mist-500">
          {periodReviews} {periodReviews === 1 ? 'verdict' : 'verdicts'} in this period · all-time{' '}
          {formatScore(look.avgScore)}
        </p>
      </div>

      <div className="hidden w-44 shrink-0 flex-wrap content-center gap-1.5 md:flex">
        {look.tags.slice(0, 3).map((tag) => (
          <TagPill key={tag.id} tag={tag} />
        ))}
      </div>

      <div className="flex w-16 shrink-0 flex-col items-end justify-center">
        <span
          className={clsx(
            'numeric font-display text-3xl font-black leading-none',
            scoreTextClass(periodScore),
          )}
        >
          {formatScore(periodScore)}
        </span>
        <span className="mt-1 text-[10px] uppercase tracking-wider text-mist-600">period</span>
      </div>
    </li>
  )
}
