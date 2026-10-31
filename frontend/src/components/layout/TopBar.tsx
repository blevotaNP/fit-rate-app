import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { LogOut, Search, Settings, UserRound } from 'lucide-react'
import { useAuth } from '@/lib/auth'
import { Avatar } from '@/components/ui/Avatar'
import { Button } from '@/components/ui/Button'

export function TopBar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const activeQuery = searchParams.get('q') ?? ''
  const [term, setTerm] = useState(activeQuery)
  const [syncedQuery, setSyncedQuery] = useState(activeQuery)
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  // Keep the box in step with the URL, so landing on /looks?q=denim (a shared link, the back
  // button, or a tag click that clears the search) never leaves a stale term on screen.
  // Adjusted during render rather than in an effect: no second render pass, no flash of the
  // previous term.
  if (syncedQuery !== activeQuery) {
    setSyncedQuery(activeQuery)
    setTerm(activeQuery)
  }

  useEffect(() => {
    if (!menuOpen) return
    const close = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [menuOpen])

  const submitSearch = (event: FormEvent) => {
    event.preventDefault()
    const value = term.trim()
    navigate(value ? `/looks?q=${encodeURIComponent(value)}` : '/looks')
  }

  return (
    <header className="sticky top-0 z-30 border-b border-ink-800 bg-ink-950/85 backdrop-blur-xl">
      {/* Below `sm` the search drops onto its own row: squeezing a text field between the wordmark
          and the auth buttons at 375px leaves it unusable. */}
      <div className="mx-auto flex max-w-[1500px] flex-wrap items-center gap-x-3 gap-y-2 px-4 py-2.5 sm:h-16 sm:flex-nowrap sm:gap-5 sm:px-6 sm:py-0">
        <Link to="/" className="flex shrink-0 items-baseline gap-0.5">
          <span className="font-display text-[19px] font-black tracking-tight text-mist-50">
            Outfit<span className="text-flare-500">Rating</span>
          </span>
        </Link>

        <form
          onSubmit={submitSearch}
          className="relative order-last w-full sm:order-none sm:ml-auto sm:max-w-md"
        >
          <Search
            size={16}
            className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-mist-500"
          />
          <input
            type="search"
            value={term}
            onChange={(event) => setTerm(event.target.value)}
            placeholder="Search looks, items, brands…"
            aria-label="Search the catalogue"
            className="h-10 w-full rounded-xl border border-ink-700 bg-ink-850 pl-9 pr-3 text-sm text-mist-100 placeholder:text-mist-500 transition-colors hover:border-ink-600 focus:border-flare-500 focus:outline-none"
          />
        </form>

        {user ? (
          <div className="relative ml-auto shrink-0 sm:ml-0" ref={menuRef}>
            <button
              type="button"
              onClick={() => setMenuOpen((open) => !open)}
              aria-haspopup="menu"
              aria-expanded={menuOpen}
              className="flex items-center gap-2 rounded-xl border border-transparent p-1 pr-2 transition-colors hover:border-ink-700 hover:bg-ink-850"
            >
              <Avatar user={user} size="sm" />
              <span className="hidden max-w-28 truncate text-sm text-mist-200 sm:block">
                {user.displayName}
              </span>
            </button>

            {menuOpen && (
              <div
                role="menu"
                className="animate-fade absolute right-0 mt-2 w-56 overflow-hidden rounded-xl border border-ink-700 bg-ink-850 py-1 shadow-2xl shadow-black/60"
              >
                <div className="border-b border-ink-800 px-3 py-2.5">
                  <p className="truncate text-sm font-medium text-mist-100">{user.displayName}</p>
                  <p className="truncate text-xs text-mist-500">@{user.username}</p>
                </div>
                <MenuLink to={`/u/${user.username}`} onClick={() => setMenuOpen(false)}>
                  <UserRound size={15} /> My profile
                </MenuLink>
                <MenuLink to="/settings" onClick={() => setMenuOpen(false)}>
                  <Settings size={15} /> Settings
                </MenuLink>
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    setMenuOpen(false)
                    void logout()
                    navigate('/')
                  }}
                  className="flex w-full items-center gap-2.5 px-3 py-2 text-left text-sm text-mist-300 transition-colors hover:bg-ink-800 hover:text-flare-400"
                >
                  <LogOut size={15} /> Sign out
                </button>
              </div>
            )}
          </div>
        ) : (
          <div className="ml-auto flex shrink-0 items-center gap-2 sm:ml-0">
            <Button variant="ghost" size="sm" onClick={() => navigate('/login')}>
              Sign in
            </Button>
            <Button variant="primary" size="sm" onClick={() => navigate('/register')}>
              Join
            </Button>
          </div>
        )}
      </div>
    </header>
  )
}

function MenuLink({
  to,
  onClick,
  children,
}: {
  to: string
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <Link
      to={to}
      role="menuitem"
      onClick={onClick}
      className="flex items-center gap-2.5 px-3 py-2 text-sm text-mist-300 transition-colors hover:bg-ink-800 hover:text-mist-50"
    >
      {children}
    </Link>
  )
}
