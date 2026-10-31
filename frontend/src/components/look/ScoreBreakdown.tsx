import clsx from 'clsx'
import type { ScoreBreakdown as Breakdown } from '@/lib/types'
import { CRITERIA, formatCriterion, scoreBgClass } from '@/lib/format'

/**
 * The five criteria as horizontal bars.
 *
 * Each bar is coloured by that criterion's own value rather than the overall score, so a look
 * that is brilliant in colour and weak in fit reads as exactly that at a glance — which is the
 * whole reason for scoring five things instead of one.
 */
export function ScoreBreakdownBars({
  scores,
  compact = false,
}: {
  scores: Breakdown
  compact?: boolean
}) {
  return (
    <dl className={clsx('space-y-2.5', compact && 'space-y-2')}>
      {CRITERIA.map(({ key, label, hint }) => {
        const value = scores[key]
        const percent = value === null || value === undefined ? 0 : (value / 10) * 100
        // Criteria run 1..10 while the palette is keyed to the 0..100 verdict scale.
        const banded = value === null || value === undefined ? null : value * 10

        return (
          <div key={key} className="grid grid-cols-[84px_1fr_36px] items-center gap-3">
            <dt
              className={clsx('truncate text-xs text-mist-400', compact && 'text-[11px]')}
              title={hint}
            >
              {label}
            </dt>
            <div className="h-1.5 overflow-hidden rounded-full bg-ink-800">
              <div
                className={clsx('h-full rounded-full transition-[width] duration-700 ease-out', scoreBgClass(banded))}
                style={{ width: `${percent}%` }}
              />
            </div>
            <dd className="numeric text-right text-xs font-semibold text-mist-200">
              {formatCriterion(value)}
            </dd>
          </div>
        )
      })}
    </dl>
  )
}
