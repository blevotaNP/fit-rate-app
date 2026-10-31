import { cn } from '@/lib/cn'
import { formatScore, scoreBand, scoreTextClass } from '@/lib/format'

type Size = 'xs' | 'sm' | 'md' | 'lg' | 'xl'

const SIZES: Record<Size, string> = {
  xs: 'text-[13px] w-8 h-8',
  sm: 'text-[15px] w-10 h-10',
  md: 'text-lg w-12 h-12',
  lg: 'text-2xl w-16 h-16',
  xl: 'text-[40px] w-24 h-24',
}

const RINGS: Record<string, string> = {
  elite: 'border-score-elite/45 bg-score-elite/10',
  high: 'border-score-high/45 bg-score-high/10',
  mid: 'border-score-mid/45 bg-score-mid/10',
  low: 'border-score-low/45 bg-score-low/10',
  poor: 'border-score-poor/45 bg-score-poor/10',
  none: 'border-ink-700 bg-ink-800',
}

interface ScoreBadgeProps {
  score: number | null | undefined
  size?: Size
  /** Number of verdicts behind the score; a 100 from one person is not a 100. */
  count?: number
  className?: string
}

/**
 * The 0..100 verdict. Shown as a bordered disc so it survives sitting on top of a photograph,
 * which is where it most often has to live.
 */
export function ScoreBadge({ score, size = 'md', count, className }: ScoreBadgeProps) {
  const band = scoreBand(score)
  return (
    <div
      className={cn(
        'numeric flex flex-col items-center justify-center rounded-full border font-display font-bold leading-none tabular-nums',
        SIZES[size],
        RINGS[band],
        scoreTextClass(score),
        className,
      )}
      title={
        count !== undefined
          ? `${formatScore(score)} from ${count} ${count === 1 ? 'verdict' : 'verdicts'}`
          : undefined
      }
    >
      <span>{formatScore(score)}</span>
      {count !== undefined && (size === 'lg' || size === 'xl') && (
        <span className="mt-1 text-[10px] font-medium tracking-wide text-mist-400">
          {count} {count === 1 ? 'vote' : 'votes'}
        </span>
      )}
    </div>
  )
}
