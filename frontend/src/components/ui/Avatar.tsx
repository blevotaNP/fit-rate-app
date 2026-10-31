import { useState } from 'react'
import { cn } from '@/lib/cn'
import type { UserSummary } from '@/lib/types'

type Size = 'xs' | 'sm' | 'md' | 'lg' | 'xl'

const SIZES: Record<Size, string> = {
  xs: 'w-6 h-6 text-[10px]',
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-14 h-14 text-lg',
  xl: 'w-24 h-24 text-3xl',
}

interface AvatarProps {
  user: Pick<UserSummary, 'displayName' | 'username' | 'avatarUrl'> | null | undefined
  size?: Size
  className?: string
}

export function Avatar({ user, size = 'md', className }: AvatarProps) {
  const avatarUrl = user?.avatarUrl
  // Remembering *which* URL failed (rather than a boolean) means a new avatar is retried
  // automatically, with no effect and no extra render.
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  const failed = Boolean(avatarUrl) && failedUrl === avatarUrl

  const label = user?.displayName || user?.username || '?'
  const initials =
    label
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('') || '?'

  return (
    <span
      className={cn(
        'inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full border border-ink-700 bg-ink-800 font-display font-semibold text-mist-300',
        SIZES[size],
        className,
      )}
      title={label}
    >
      {avatarUrl && !failed ? (
        <img
          src={avatarUrl}
          alt=""
          loading="lazy"
          className="h-full w-full object-cover"
          // Swap to initials rather than leaving the broken-image glyph or an empty circle.
          onError={() => setFailedUrl(avatarUrl ?? null)}
        />
      ) : (
        initials
      )}
    </span>
  )
}
