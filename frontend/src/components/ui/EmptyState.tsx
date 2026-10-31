import type { ReactNode } from 'react'
import { PackageOpen } from 'lucide-react'

interface EmptyStateProps {
  title: string
  hint?: string
  action?: ReactNode
  icon?: ReactNode
}

export function EmptyState({ title, hint, action, icon }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center rounded-card border border-dashed border-ink-700 bg-ink-900/40 px-6 py-16 text-center">
      <span className="mb-3 text-mist-600">
        {icon ?? <PackageOpen size={30} strokeWidth={1.4} className="text-mist-500" />}
      </span>
      <h3 className="font-display text-base font-semibold text-mist-200">{title}</h3>
      {hint && <p className="mt-1.5 max-w-sm text-sm text-mist-500">{hint}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}
