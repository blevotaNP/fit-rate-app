import { Link, Outlet, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
import { SideRail } from './SideRail'
import { TopBar } from './TopBar'

export function AppShell() {
  const { pathname } = useLocation()

  // A single-page app has to restore the scroll behaviour a browser gives you for free.
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior })
  }, [pathname])

  return (
    <div className="min-h-screen lg:pl-16">
      {/* Keyboard users would otherwise tab through the whole rail and header on every page. */}
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-mist-50 focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-ink-950"
      >
        Skip to content
      </a>
      <SideRail />
      <TopBar />
      <main
        id="main"
        tabIndex={-1}
        className="mx-auto min-h-[70vh] w-full max-w-[1500px] px-4 pb-24 pt-6 outline-none sm:px-6 lg:pb-14"
      >
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}

function Footer() {
  return (
    <footer className="mt-10 border-t border-ink-800 bg-ink-900/60">
      <div className="mx-auto flex max-w-[1500px] flex-col gap-6 px-4 py-10 sm:px-6 md:flex-row md:items-start md:justify-between">
        <div className="max-w-sm">
          <p className="font-display text-lg font-black tracking-tight text-mist-50">
            Outfit<span className="text-flare-500">Rating</span>
          </p>
          <p className="mt-2 text-sm leading-relaxed text-mist-500">
            A community verdict on clothing. Five criteria, one score, no sponsored placements.
          </p>
        </div>

        <div className="grid grid-cols-2 gap-x-10 gap-y-2 text-sm sm:grid-cols-3">
          <FooterLink to="/looks">Catalogue</FooterLink>
          <FooterLink to="/charts">Leaderboards</FooterLink>
          <FooterLink to="/looks?type=ITEM">Garments</FooterLink>
          <FooterLink to="/looks?type=OUTFIT">Outfits</FooterLink>
          <FooterLink to="/looks?sort=TOP_RATED">Best rated</FooterLink>
          <a
            href="/swagger-ui.html"
            target="_blank"
            rel="noreferrer"
            className="text-mist-500 transition-colors hover:text-mist-200"
          >
            API docs
          </a>
        </div>
      </div>

      <div className="border-t border-ink-800/70">
        <p className="mx-auto max-w-[1500px] px-4 py-4 text-xs text-mist-500 sm:px-6">
          Built with Spring Boot and React. Photography from Unsplash contributors.
        </p>
      </div>
    </footer>
  )
}

function FooterLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <Link to={to} className="text-mist-500 transition-colors hover:text-mist-200">
      {children}
    </Link>
  )
}
