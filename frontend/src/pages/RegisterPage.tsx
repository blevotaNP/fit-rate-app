import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { AuthLayout } from '@/components/layout/AuthLayout'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/lib/auth'
import { errorMessage } from '@/lib/api'

const USERNAME_PATTERN = /^[a-zA-Z0-9_.]{3,32}$/

export function RegisterPage() {
  const { user, register } = useAuth()
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (user) {
    return <Navigate to="/" replace />
  }

  // Mirrors the server-side constraints so the first feedback does not need a round trip.
  const usernameValid = username === '' || USERNAME_PATTERN.test(username)
  const passwordValid = password === '' || password.length >= 8

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await register({
        username: username.trim(),
        email: email.trim(),
        password,
        displayName: displayName.trim() || undefined,
      })
      navigate('/for-you', { replace: true })
    } catch (caught) {
      setError(errorMessage(caught, 'Could not create the account'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Join the jury"
      subtitle="One verdict per person per look. That is what keeps the scores worth reading."
      footer={
        <>
          Already a member?{' '}
          <Link to="/login" className="text-flare-400 hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <form onSubmit={submit} className="space-y-4">
        <label className="block">
          <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-mist-500">
            Username
          </span>
          <input
            required
            autoFocus
            minLength={3}
            maxLength={32}
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            className="h-11 w-full rounded-xl border border-ink-700 bg-ink-850 px-3 text-sm text-mist-100 focus:border-flare-500 focus:outline-none"
          />
          {!usernameValid && (
            <span className="mt-1 block text-xs text-flare-400">
              3–32 characters: letters, digits, underscore and dot only.
            </span>
          )}
        </label>

        <label className="block">
          <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-mist-500">
            E-mail
          </span>
          <input
            required
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
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
            minLength={8}
            maxLength={72}
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="h-11 w-full rounded-xl border border-ink-700 bg-ink-850 px-3 text-sm text-mist-100 focus:border-flare-500 focus:outline-none"
          />
          {!passwordValid && (
            <span className="mt-1 block text-xs text-flare-400">At least 8 characters.</span>
          )}
        </label>

        <label className="block">
          <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-mist-500">
            Display name <span className="normal-case tracking-normal">(optional)</span>
          </span>
          <input
            maxLength={64}
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            placeholder="Defaults to your username"
            className="h-11 w-full rounded-xl border border-ink-700 bg-ink-850 px-3 text-sm text-mist-100 placeholder:text-mist-600 focus:border-flare-500 focus:outline-none"
          />
        </label>

        {error && (
          <p role="alert" className="rounded-lg border border-flare-600/40 bg-flare-600/10 px-3 py-2 text-sm text-flare-300">
            {error}
          </p>
        )}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          full
          loading={submitting}
          disabled={!usernameValid || !passwordValid}
        >
          Create account
        </Button>
      </form>
    </AuthLayout>
  )
}
