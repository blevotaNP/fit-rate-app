import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import type { ReactElement } from 'react'
import { AppShell } from './components/layout/AppShell'
import { useAuth } from './lib/auth'
import { HomePage } from './pages/HomePage'
import { CataloguePage } from './pages/CataloguePage'
import { LookPage } from './pages/LookPage'
import { ChartsPage } from './pages/ChartsPage'
import { ForYouPage } from './pages/ForYouPage'
import { SubmitPage } from './pages/SubmitPage'
import { ProfilePage } from './pages/ProfilePage'
import { SettingsPage } from './pages/SettingsPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { NotFoundPage } from './pages/NotFoundPage'

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<HomePage />} />
        <Route path="looks" element={<CataloguePage />} />
        <Route path="looks/:slug" element={<LookPage />} />
        <Route
          path="looks/:slug/edit"
          element={
            <RequireAuth>
              <SubmitPage mode="edit" />
            </RequireAuth>
          }
        />
        <Route path="charts" element={<ChartsPage />} />
        <Route path="u/:username" element={<ProfilePage />} />
        <Route
          path="for-you"
          element={
            <RequireAuth>
              <ForYouPage />
            </RequireAuth>
          }
        />
        <Route
          path="submit"
          element={
            <RequireAuth>
              <SubmitPage />
            </RequireAuth>
          }
        />
        <Route
          path="settings"
          element={
            <RequireAuth>
              <SettingsPage />
            </RequireAuth>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
    </Routes>
  )
}

/** Holds the route until the silent refresh resolves, so a reload does not bounce to /login. */
function RequireAuth({ children }: { children: ReactElement }) {
  const { user, initialising } = useAuth()
  const location = useLocation()

  if (initialising) {
    return (
      <div className="grid min-h-[50vh] place-items-center">
        <Loader2 size={22} className="animate-spin text-mist-500" />
      </div>
    )
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  return children
}
