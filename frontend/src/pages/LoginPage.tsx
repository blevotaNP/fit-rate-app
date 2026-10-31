import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { AuthLayout } from '@/components/layout/AuthLayout'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/lib/auth'
import { errorMessage } from '@/lib/api'

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [loginValue, setLoginValue] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (user) {
    return <Navigate to="/" replace />
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(loginValue.trim(), password)
      const from = (location.state as { from?: string } | null)?.from
      navigate(from ?? '/', { replace: true })
    } catch (caught) {
      setError(errorMessage(caught, 'Could not sign you in'))
    } finally {
      setSubmitting(false)
    }
  }

  const useDemoAccount = () => {
    setLoginValue('mara')
    setPassword('password123')
  }

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Sign in to score looks, save to your wardrobe and follow the charts."
      footer={
        <>
          No account yet?{' '}
          <Link to="/register" className="text-flare-400 hover:underline">
            Create one
          </Link>
        </>
      }
    >
      <form onSubmit={submit} className="space-y-4">
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-mist-500">
            Username or e-mail
          </span>
          <input
            required
            autoFocus
            autoComplete="username"
            value={loginValue}
            onChange={(event) => setLoginValue(event.target.value)}
            className="h-11 w-full rounded-xl border border-ink-700 bg-ink-850 px-3 text-sm text-mist-100 focus:border-flare-500 focus:outline-none"
          />
        </label>

        <label className="block">
          <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-mist-500">
            Password
          </span>
          <input
            required
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="h-11 w-full rounded-xl border border-ink-700 bg-ink-850 px-3 text-sm text-mist-100 focus:border-flare-500 focus:outline-none"
          />
        </label>

        {error && (
          <p role="alert" className="rounded-lg border border-flare-600/40 bg-flare-600/10 px-3 py-2 text-sm text-flare-300">
            {error}
          </p>
        )}

        <Button type="submit" variant="primary" size="lg" full loading={submitting}>
          Sign in
        </Button>
      </form>

      <button
        type="button"
        onClick={useDemoAccount}
        className="mt-4 w-full rounded-xl border border-dashed border-ink-700 px-3 py-2.5 text-left text-xs leading-relaxed text-mist-500 transition-colors hover:border-ink-600 hover:text-mist-300"
      >
        <span className="font-medium text-mist-300">Just looking around?</span> Click to fill in a
        demo account (<span className="text-mist-300">mara</span> /{' '}
        <span className="text-mist-300">password123</span>).
      </button>
    </AuthLayout>
  )
}
