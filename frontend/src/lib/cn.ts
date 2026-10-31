import clsx, { type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * Conditional classes, then a Tailwind-aware dedupe.
 *
 * Plain `clsx` concatenates, so a caller passing `text-flare-400` to a component whose variant
 * already sets `text-mist-100` leaves both in the attribute and the winner is decided by the
 * order the utilities happen to appear in the generated stylesheet — an override that silently
 * does nothing. `twMerge` drops the earlier utility of a conflicting pair, so the caller wins.
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}
