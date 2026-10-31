import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SlidersHorizontal, X } from 'lucide-react'
import clsx from 'clsx'
import { useBrands, useLooks, usePopularTags } from '@/lib/queries'
import { CATEGORIES, LOOK_TYPES, OCCASIONS, SEASONS, humanise } from '@/lib/format'
import { LookGrid } from '@/components/look/LookGrid'
import { Button } from '@/components/ui/Button'
import { TagPill } from '@/components/ui/TagPill'
import type { Category, LookFilters, LookType, Occasion, Season, SortOrder } from '@/lib/types'

const SORTS: { value: SortOrder; label: string }[] = [
  { value: 'NEWEST', label: 'Newest' },
  { value: 'TOP_RATED', label: 'Best rated' },
  { value: 'MOST_RATED', label: 'Most rated' },
  { value: 'LOWEST_RATED', label: 'Lowest rated' },
  { value: 'ALPHABETICAL', label: 'A to Z' },
]

const PAGE_SIZE = 24

export function CataloguePage() {
  const [params, setParams] = useSearchParams()
  const brands = useBrands()
  const tags = usePopularTags(14)

  const page = Number(params.get('page') ?? 0)

  const filters = useMemo<LookFilters>(
    () => ({
      q: params.get('q') ?? undefined,
      type: (params.get('type') as LookType | null) ?? undefined,
      category: (params.get('category') as Category | null) ?? undefined,
      season: (params.get('season') as Season | null) ?? undefined,
      occasion: (params.get('occasion') as Occasion | null) ?? undefined,
      brand: params.get('brand') ?? undefined,
      tag: params.get('tag') ?? undefined,
      minScore: params.get('minScore') ? Number(params.get('minScore')) : undefined,
      sort: (params.get('sort') as SortOrder | null) ?? 'NEWEST',
      page,
      size: PAGE_SIZE,
    }),
    [params, page],
  )

  const { data, isLoading, isFetching, error, refetch } = useLooks(filters)

  /** Every filter change resets to page 0 — page 7 of a different query is meaningless. */
  const setFilter = (key: string, value: string | null) => {
    const next = new URLSearchParams(params)
    if (value === null || value === '') {
      next.delete(key)
    } else {
      next.set(key, value)
    }
    if (key !== 'page') next.delete('page')
    setParams(next, { replace: true })
  }

  const activeFilters = ['q', 'type', 'category', 'season', 'occasion', 'brand', 'tag', 'minScore']
    .map((key) => ({ key, value: params.get(key) }))
    .filter((entry): entry is { key: string; value: string } => Boolean(entry.value))

  const total = data?.totalElements ?? 0

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-black tracking-tight text-mist-50 sm:text-3xl">
            Catalogue
          </h1>
          <p className="mt-1 text-sm text-mist-400">
            {isLoading ? 'Loading…' : `${total} ${total === 1 ? 'entry' : 'entries'}`}
            {filters.q && (
              <>
                {' '}
                matching <span className="text-mist-100">“{filters.q}”</span>
              </>
            )}
          </p>
        </div>

        <label className="flex items-center gap-2 text-sm">
          <SlidersHorizontal size={15} className="text-mist-500" />
          <span className="sr-only">Sort by</span>
          <select
            value={filters.sort}
            onChange={(event) => setFilter('sort', event.target.value)}
            className="h-9 rounded-lg border border-ink-700 bg-ink-850 px-2.5 text-sm text-mist-100 focus:border-flare-500 focus:outline-none"
          >
            {SORTS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </header>

      <div className="flex flex-wrap items-center gap-2">
        <SegmentedType value={params.get('type')} onChange={(value) => setFilter('type', value)} />

        <FilterSelect
          label="Category"
          value={params.get('category')}
          options={CATEGORIES}
          onChange={(value) => setFilter('category', value)}
        />
        <FilterSelect
          label="Season"
          value={params.get('season')}
          options={SEASONS}
          onChange={(value) => setFilter('season', value)}
        />
        <FilterSelect
          label="Occasion"
          value={params.get('occasion')}
          options={OCCASIONS}
          onChange={(value) => setFilter('occasion', value)}
        />

        <label className="flex h-9 items-center gap-2 rounded-lg border border-ink-700 bg-ink-850 px-2.5 text-sm">
          <span className="text-mist-500">Brand</span>
          <select
            value={params.get('brand') ?? ''}
            onChange={(event) => setFilter('brand', event.target.value)}
            className="bg-transparent text-mist-100 focus:outline-none"
          >
            <option value="">Any</option>
            {brands.data?.map((brand) => (
              <option key={brand.id} value={brand.slug}>
                {brand.name}
              </option>
            ))}
          </select>
        </label>

        <label className="flex h-9 items-center gap-2 rounded-lg border border-ink-700 bg-ink-850 px-2.5 text-sm">
          <span className="text-mist-500">Min score</span>
          <select
            value={params.get('minScore') ?? ''}
            onChange={(event) => setFilter('minScore', event.target.value)}
            className="bg-transparent text-mist-100 focus:outline-none"
          >
            <option value="">Any</option>
            {[60, 70, 80, 90].map((score) => (
              <option key={score} value={score}>
                {score}+
              </option>
            ))}
          </select>
        </label>

        {activeFilters.length > 0 && (
          <Button
            variant="ghost"
            size="sm"
            icon={<X size={14} />}
            onClick={() => setParams(new URLSearchParams(), { replace: true })}
          >
            Clear {activeFilters.length}
          </Button>
        )}
      </div>

      {tags.data && tags.data.length > 0 && (
        <div className="rail -mx-4 flex gap-2 overflow-x-auto px-4 pb-1 sm:mx-0 sm:flex-wrap sm:px-0">
          {tags.data.map((tag) => (
            <TagPill
              key={tag.id}
              tag={tag}
              count={tag.usageCount}
              className={clsx(
                'shrink-0',
                params.get('tag') === tag.slug && 'border-flare-500/60 text-flare-400',
              )}
            />
          ))}
        </div>
      )}

      <div className={clsx(isFetching && !isLoading && 'opacity-60 transition-opacity')}>
        <LookGrid
          looks={data?.content}
          loading={isLoading}
          error={error}
          onRetry={() => refetch()}
          emptyTitle="No matches"
          emptyHint="Nothing in the catalogue fits that combination yet. Try clearing a filter."
        />
      </div>

      {data && data.totalPages > 1 && (
        <nav className="flex items-center justify-center gap-3 pt-2" aria-label="Pagination">
          <Button
            variant="secondary"
            size="sm"
            disabled={page === 0}
            onClick={() => setFilter('page', String(page - 1))}
          >
            Previous
          </Button>
          <span className="numeric text-sm text-mist-400">
            {page + 1} / {data.totalPages}
          </span>
          <Button
            variant="secondary"
            size="sm"
            disabled={!data.hasNext}
            onClick={() => setFilter('page', String(page + 1))}
          >
            Next
          </Button>
        </nav>
      )}
    </div>
  )
}

function SegmentedType({
  value,
  onChange,
}: {
  value: string | null
  onChange: (value: string | null) => void
}) {
  const options: { value: string | null; label: string }[] = [
    { value: null, label: 'Everything' },
    ...LOOK_TYPES.map((type) => ({
      value: type,
      label: type === 'OUTFIT' ? 'Outfits' : 'Garments',
    })),
  ]

  return (
    <div className="flex h-9 items-center rounded-lg border border-ink-700 bg-ink-850 p-0.5">
      {options.map((option) => (
        <button
          key={option.label}
          type="button"
          onClick={() => onChange(option.value)}
          className={clsx(
            'h-full rounded-[6px] px-3 text-sm transition-colors',
            value === option.value
              ? 'bg-ink-700 text-mist-50'
              : 'text-mist-400 hover:text-mist-100',
          )}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}

function FilterSelect({
  label,
  value,
  options,
  onChange,
}: {
  label: string
  value: string | null
  options: readonly string[]
  onChange: (value: string) => void
}) {
  return (
    <label className="flex h-9 items-center gap-2 rounded-lg border border-ink-700 bg-ink-850 px-2.5 text-sm">
      <span className="text-mist-500">{label}</span>
      <select
        value={value ?? ''}
        onChange={(event) => onChange(event.target.value)}
        className="bg-transparent text-mist-100 focus:outline-none"
      >
        <option value="">Any</option>
        {options.map((option) => (
          <option key={option} value={option}>
            {humanise(option)}
          </option>
        ))}
      </select>
    </label>
  )
}
