import { cn } from '@/lib/cn'
import { Loader2 } from 'lucide-react'
import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'accent'
type Size = 'sm' | 'md' | 'lg'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  loading?: boolean
  icon?: ReactNode
  full?: boolean
}

const VARIANTS: Record<Variant, string> = {
  // White-on-black is the high-fashion default; the accent is reserved for one call to action.
  primary:
    'bg-mist-50 text-ink-950 hover:bg-white active:bg-mist-200 disabled:bg-ink-700 disabled:text-mist-500',
  accent:
    'bg-flare-500 text-ink-950 hover:bg-flare-400 active:bg-flare-600 disabled:bg-ink-700 disabled:text-mist-500',
  secondary:
    'bg-ink-800 text-mist-100 border border-ink-700 hover:bg-ink-750 hover:border-ink-600 disabled:text-mist-500',
  ghost: 'text-mist-300 hover:bg-ink-800 hover:text-mist-50 disabled:text-mist-500',
  danger:
    'bg-transparent text-flare-400 border border-flare-600/40 hover:bg-flare-600/10 disabled:text-mist-500',
}

const SIZES: Record<Size, string> = {
  sm: 'h-8 px-3 text-[13px] gap-1.5 rounded-lg',
  md: 'h-10 px-4 text-sm gap-2 rounded-xl',
  lg: 'h-12 px-6 text-[15px] gap-2 rounded-xl',
}

export function Button({
  variant = 'secondary',
  size = 'md',
  loading = false,
  icon,
  full = false,
  className,
  children,
  disabled,
  ...rest
}: ButtonProps) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={cn(
        'inline-flex shrink-0 items-center justify-center font-medium transition-colors duration-150',
        'disabled:cursor-not-allowed',
        VARIANTS[variant],
        SIZES[size],
        full && 'w-full',
        className,
      )}
    >
      {loading ? <Loader2 size={15} className="animate-spin" /> : icon}
      {children}
    </button>
  )
}
