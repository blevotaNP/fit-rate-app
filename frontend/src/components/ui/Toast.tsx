import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { CheckCircle2, Info, X, XCircle } from 'lucide-react'
import clsx from 'clsx'

type ToastKind = 'success' | 'error' | 'info'

interface Toast {
  id: number
  kind: ToastKind
  message: string
}

interface ToastApi {
  notify: (message: string, kind?: ToastKind) => void
}

const ToastContext = createContext<ToastApi | null>(null)

const ICONS = {
  success: CheckCircle2,
  error: XCircle,
  info: Info,
}

const TONES: Record<ToastKind, string> = {
  success: 'text-score-elite',
  error: 'text-flare-400',
  info: 'text-mist-300',
}

let nextId = 1

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const notify = useCallback((message: string, kind: ToastKind = 'info') => {
    const id = nextId++
    setToasts((current) => [...current, { id, kind, message }])
    window.setTimeout(() => dismiss(id), 4200)
  }, [dismiss])

  const value = useMemo(() => ({ notify }), [notify])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        className="pointer-events-none fixed inset-x-0 bottom-5 z-[100] flex flex-col items-center gap-2 px-4"
        role="status"
        aria-live="polite"
      >
        {toasts.map((toast) => {
          const Icon = ICONS[toast.kind]
          return (
            <div
              key={toast.id}
              className="animate-rise pointer-events-auto flex max-w-md items-start gap-2.5 rounded-xl border border-ink-700 bg-ink-800/95 px-4 py-3 text-sm shadow-2xl shadow-black/50 backdrop-blur"
            >
              <Icon size={17} className={clsx('mt-0.5 shrink-0', TONES[toast.kind])} />
              <span className="text-mist-100">{toast.message}</span>
              <button
                type="button"
                onClick={() => dismiss(toast.id)}
                aria-label="Dismiss notification"
                className="-mr-1 -mt-0.5 shrink-0 rounded p-1 text-mist-500 transition-colors hover:text-mist-100"
              >
                <X size={14} />
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastApi {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used inside <ToastProvider>')
  }
  return context
}
