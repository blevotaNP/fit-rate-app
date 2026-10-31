import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import type { ReactNode } from 'react'

const COVER =
  'https://images.unsplash.com/photo-1490915785914-0af2806c22b6?w=1200&q=80&auto=format&fit=crop'

/** Split layout for sign-in and registration: editorial image on one half, form on the other. */
export function AuthLayout({
  title,
  subtitle,
  children,
  footer,
}: {
  title: string
  subtitle: string
  children: ReactNode
  footer: ReactNode
}) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden lg:block">
        <img src={COVER} alt="" className="h-full w-full object-cover" />
        <div className="absolute inset-0 bg-gradient-to-t from-ink-950 via-ink-950/40 to-transparent" />
        <div className="absolute inset-x-0 bottom-0 p-10">
          <p className="font-display text-3xl font-black leading-tight text-white">
            Five criteria.
            <br />
            One honest number.
          </p>
          <p className="mt-3 max-w-sm text-sm leading-relaxed text-white/70">
            Style, fit, colour, originality and quality — scored by people who actually care about
            clothes, not by an algorithm chasing engagement.
          </p>
        </div>
      </div>

      <div className="flex flex-col px-5 py-8 sm:px-10">
        <div className="flex items-center justify-between">
          <Link to="/" className="font-display text-lg font-black tracking-tight text-mist-50">
            Outfit<span className="text-flare-500">Rating</span>
          </Link>
          <Link
            to="/"
            className="inline-flex items-center gap-1.5 text-xs text-mist-500 transition-colors hover:text-mist-200"
          >
            <ArrowLeft size={13} /> Back to the site
          </Link>
        </div>

        <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center py-10">
          <h1 className="font-display text-3xl font-black tracking-tight text-mist-50">{title}</h1>
          <p className="mt-2 text-sm text-mist-400">{subtitle}</p>
          <div className="mt-7">{children}</div>
          <div className="mt-6 text-sm text-mist-400">{footer}</div>
        </div>
      </div>
    </div>
  )
}
