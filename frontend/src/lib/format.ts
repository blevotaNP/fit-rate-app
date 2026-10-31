import { formatDistanceToNowStrict, format, parseISO } from 'date-fns'
import type { Category, LookType, Occasion, Season } from './types'

/** Colour bands for the 0..100 verdict. One number, one meaning, everywhere on the site. */
export type ScoreBand = 'elite' | 'high' | 'mid' | 'low' | 'poor' | 'none'

export function scoreBand(score: number | null | undefined): ScoreBand {
  if (score === null || score === undefined) return 'none'
  if (score >= 88) return 'elite'
  if (score >= 74) return 'high'
  if (score >= 58) return 'mid'
  if (score >= 42) return 'low'
  return 'poor'
}

const BAND_TEXT: Record<ScoreBand, string> = {
  elite: 'text-score-elite',
  high: 'text-score-high',
  mid: 'text-score-mid',
  low: 'text-score-low',
  poor: 'text-score-poor',
  none: 'text-mist-500',
}

const BAND_BG: Record<ScoreBand, string> = {
  elite: 'bg-score-elite',
  high: 'bg-score-high',
  mid: 'bg-score-mid',
  low: 'bg-score-low',
  poor: 'bg-score-poor',
  none: 'bg-ink-600',
}

const BAND_LABEL: Record<ScoreBand, string> = {
  elite: 'Exceptional',
  high: 'Strong',
  mid: 'Solid',
  low: 'Uneven',
  poor: 'Weak',
  none: 'Unrated',
}

export const scoreTextClass = (score: number | null | undefined) => BAND_TEXT[scoreBand(score)]
export const scoreBgClass = (score: number | null | undefined) => BAND_BG[scoreBand(score)]
export const scoreLabel = (score: number | null | undefined) => BAND_LABEL[scoreBand(score)]

export function formatScore(score: number | null | undefined): string {
  if (score === null || score === undefined) return '—'
  return String(Math.round(score))
}

export function formatCriterion(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return value.toFixed(1)
}

export function formatPrice(cents: number | null | undefined, currency: string | null): string | null {
  if (cents === null || cents === undefined) return null
  const amount = cents / 100
  try {
    return new Intl.NumberFormat('en-GB', {
      style: 'currency',
      currency: currency ?? 'EUR',
      maximumFractionDigits: 0,
    }).format(amount)
  } catch {
    return `${Math.round(amount)} ${currency ?? ''}`.trim()
  }
}

export function formatCount(value: number): string {
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`
  if (value >= 1_000) return `${(value / 1_000).toFixed(1)}k`
  return String(value)
}

export function timeAgo(iso: string | null | undefined): string {
  if (!iso) return ''
  try {
    return `${formatDistanceToNowStrict(parseISO(iso))} ago`
  } catch {
    return ''
  }
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return ''
  try {
    return format(parseISO(iso), 'd MMM yyyy')
  } catch {
    return ''
  }
}

/** SCREAMING_SNAKE enums become Title Case for display without a translation table per enum. */
export function humanise(value: string | null | undefined): string {
  if (!value) return ''
  return value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export const CATEGORIES: Category[] = [
  'STREETWEAR',
  'CASUAL',
  'MINIMAL',
  'FORMAL',
  'TECHWEAR',
  'VINTAGE',
  'AVANT_GARDE',
  'SPORTSWEAR',
  'OUTERWEAR',
  'FOOTWEAR',
  'KNITWEAR',
  'DENIM',
  'ACCESSORIES',
]

export const SEASONS: Season[] = ['SPRING', 'SUMMER', 'AUTUMN', 'WINTER', 'ALL_SEASON']

export const OCCASIONS: Occasion[] = [
  'EVERYDAY',
  'OFFICE',
  'EVENING',
  'PARTY',
  'SPORT',
  'TRAVEL',
  'RUNWAY',
]

export const LOOK_TYPES: LookType[] = ['OUTFIT', 'ITEM']

/** The five judging criteria, in the order they appear everywhere in the UI. */
export const CRITERIA = [
  { key: 'style', label: 'Style', hint: 'Does the whole thing read as deliberate?' },
  { key: 'fit', label: 'Fit', hint: 'Proportion, silhouette, how it sits on the body' },
  { key: 'color', label: 'Colour', hint: 'Palette discipline and contrast' },
  { key: 'originality', label: 'Originality', hint: 'Is there an idea here you have not seen?' },
  { key: 'quality', label: 'Quality', hint: 'Materials, construction, finish' },
] as const

export type CriterionKey = (typeof CRITERIA)[number]['key']
