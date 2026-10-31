import { NavLink } from 'react-router-dom'
import {
  Bookmark,
  Home,
  LayoutGrid,
  PlusSquare,
  Sparkles,
  Trophy,
  User as UserIcon,
} from 'lucide-react'
import clsx from 'clsx'
import { useAuth } from '@/lib/auth'

interface RailItem {
  to: string
  label: string
  icon: typeof Home
  requiresAuth?: boolean
}

const PRIMARY: RailItem[] = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/looks', label: 'Catalogue', icon: LayoutGrid },
  { to: '/charts', label: 'Leaderboards', icon: Trophy },
  { to: '/for-you', label: 'For you', icon: Sparkles, requiresAuth: true },
  { to: '/submit', label: 'Publish', icon: PlusSquare, requiresAuth: true },
]

/**
 * Narrow icon rail, mirroring the reference site's navigation model: always visible, never wider
 * than an icon, so the content column keeps the full width for photography.
 * Below `lg` it flattens into a bottom bar, which is where a thumb expects navigation to be.
 */
export function SideRail() {
  const { user } = useAuth()

  const items: RailItem[] = [
    ...PRIMARY,
    ...(user
      ? [
          { to: `/u/${user.username}?tab=wardrobe`, label: 'Wardrobe', icon: Bookmark },
          { to: `/u/${user.username}`, label: 'Profile', icon: UserIcon },
        ]
      : []),
  ].filter((item) => !item.requiresAuth || Boolean(user))

  return (
    <>
      <nav
        aria-label="Primary"
        className="fixed left-0 top-0 z-40 hidden h-screen w-16 flex-col items-center gap-1 border-r border-ink-800 bg-ink-900 py-4 lg:flex"
      >
        <NavLink to="/" className="mb-4 grid h-10 w-10 place-items-center" aria-label="OutfitRating home">
          <span className="font-display text-xl font-black leading-none text-mist-50">
            O<span className="text-flare-500">.</span>
          </span>
        </NavLink>

        {items.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            title={label}
            aria-label={label}
            className={({ isActive }) =>
              clsx(
                'group relative grid h-11 w-11 place-items-center rounded-xl transition-colors',
                isActive
                  ? 'bg-ink-800 text-mist-50'
                  : 'text-mist-500 hover:bg-ink-850 hover:text-mist-200',
              )
            }
          >
            {({ isActive }) => (
              <>
                <Icon size={19} strokeWidth={1.8} />
                {isActive && (
                  <span className="absolute left-0 h-5 w-0.5 rounded-r-full bg-flare-500" />
                )}
                <span className="pointer-events-none absolute left-full ml-3 hidden whitespace-nowrap rounded-md border border-ink-700 bg-ink-800 px-2 py-1 text-xs text-mist-200 shadow-lg group-hover:block">
                  {label}
                </span>
              </>
            )}
          </NavLink>
        ))}
      </nav>

      <nav
        aria-label="Primary"
        className="fixed inset-x-0 bottom-0 z-40 flex items-center justify-around border-t border-ink-800 bg-ink-900/95 px-2 pb-[max(0.35rem,env(safe-area-inset-bottom))] pt-1.5 backdrop-blur lg:hidden"
      >
        {items.slice(0, 5).map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            aria-label={label}
            className={({ isActive }) =>
              clsx(
                'flex min-w-0 flex-1 flex-col items-center gap-0.5 rounded-lg px-1 py-1.5 text-[10px] transition-colors',
                isActive ? 'text-mist-50' : 'text-mist-500',
              )
            }
          >
            <Icon size={19} strokeWidth={1.8} />
            <span className="max-w-full truncate">{label}</span>
          </NavLink>
        ))}
      </nav>
    </>
  )
}
