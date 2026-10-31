import { useMemo, useState, type FormEvent } from 'react'
import clsx from 'clsx'
import type { Review, SaveReviewPayload } from '@/lib/types'
import { CRITERIA, scoreLabel, scoreTextClass, type CriterionKey } from '@/lib/format'
import { Button } from '@/components/ui/Button'

type Criteria = Record<CriterionKey, number>

const DEFAULTS: Criteria = { style: 7, fit: 7, color: 7, originality: 7, quality: 7 }

interface RatingFormProps {
  existing: Review | null
  submitting: boolean
  onSubmit: (payload: SaveReviewPayload) => void
  onDelete?: () => void
}

/**
 * The scoring widget. The live composite updates as the sliders move so a rater can see the
 * verdict they are about to leave, rather than discovering it after submitting.
 */
export function RatingForm({ existing, submitting, onSubmit, onDelete }: RatingFormProps) {
  const [criteria, setCriteria] = useState<Criteria>(() =>
    existing
      ? {
          style: existing.style,
          fit: existing.fit,
          color: existing.color,
          originality: existing.originality,
          quality: existing.quality,
        }
      : DEFAULTS,
  )
  const [title, setTitle] = useState(existing?.title ?? '')
  const [body, setBody] = useState(existing?.body ?? '')
  const [writing, setWriting] = useState(Boolean(existing?.body))

  // Mirrors ScoreCalculator on the server: equal weights, stretched to 0..100.
  const composite = useMemo(() => {
    const sum = CRITERIA.reduce((total, { key }) => total + criteria[key], 0)
    return Math.round((sum * 100) / 50)
  }, [criteria])

  const submit = (event: FormEvent) => {
    event.preventDefault()
    onSubmit({
      ...criteria,
      title: writing && title.trim() ? title.trim() : null,
      body: writing && body.trim() ? body.trim() : null,
    })
  }

  return (
    <form onSubmit={submit} className="space-y-5">
      <div className="flex items-center justify-between gap-4 rounded-xl border border-ink-700 bg-ink-900 px-4 py-3">
        <div>
          <p className="text-xs uppercase tracking-widest text-mist-500">Your verdict</p>
          <p className={clsx('text-sm font-medium', scoreTextClass(composite))}>
            {scoreLabel(composite)}
          </p>
        </div>
        <span
          className={clsx('numeric font-display text-4xl font-black leading-none', scoreTextClass(composite))}
        >
          {composite}
        </span>
      </div>

      <div className="space-y-4">
        {CRITERIA.map(({ key, label, hint }) => (
          <div key={key}>
            <div className="mb-1.5 flex items-baseline justify-between gap-3">
              <label htmlFor={`criterion-${key}`} className="text-sm font-medium text-mist-200">
                {label}
              </label>
              <span className="numeric text-sm font-semibold text-mist-100">{criteria[key]}</span>
            </div>
            <input
              id={`criterion-${key}`}
              type="range"
              min={1}
              max={10}
              step={1}
              value={criteria[key]}
              onChange={(event) =>
                setCriteria((current) => ({ ...current, [key]: Number(event.target.value) }))
              }
              className="h-1.5 w-full cursor-pointer appearance-none rounded-full bg-ink-700 accent-flare-500"
              aria-describedby={`hint-${key}`}
            />
            <p id={`hint-${key}`} className="mt-1 text-[11px] text-mist-500">
              {hint}
            </p>
          </div>
        ))}
      </div>

      {writing ? (
        <div className="space-y-2.5">
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            maxLength={140}
            placeholder="Headline (optional)"
            className="h-10 w-full rounded-xl border border-ink-700 bg-ink-900 px-3 text-sm text-mist-100 placeholder:text-mist-500 focus:border-flare-500 focus:outline-none"
          />
          <textarea
            value={body}
            onChange={(event) => setBody(event.target.value)}
            maxLength={5000}
            rows={5}
            placeholder="What works, what does not, and why."
            className="w-full resize-y rounded-xl border border-ink-700 bg-ink-900 px-3 py-2.5 text-sm leading-relaxed text-mist-100 placeholder:text-mist-500 focus:border-flare-500 focus:outline-none"
          />
          <p className="numeric text-right text-[11px] text-mist-500">{body.length} / 5000</p>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setWriting(true)}
          className="w-full rounded-xl border border-dashed border-ink-700 py-2.5 text-sm text-mist-400 transition-colors hover:border-ink-600 hover:text-mist-200"
        >
          Add a written review (optional)
        </button>
      )}

      <div className="flex items-center gap-2">
        <Button type="submit" variant="accent" loading={submitting} full>
          {existing ? 'Update verdict' : 'Publish verdict'}
        </Button>
        {existing && onDelete && (
          <Button type="button" variant="danger" onClick={onDelete} disabled={submitting}>
            Delete
          </Button>
        )}
      </div>
    </form>
  )
}
