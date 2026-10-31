import { useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { useAuth } from '@/lib/auth'
import { Avatar } from '@/components/ui/Avatar'
import { Button } from '@/components/ui/Button'
import { useToast } from '@/components/ui/Toast'
import { useUploadImage } from '@/lib/queries'
import { errorMessage } from '@/lib/api'

export function SettingsPage() {
  const { user, updateProfile } = useAuth()
  const { notify } = useToast()
  const uploadImage = useUploadImage()
  const fileInput = useRef<HTMLInputElement>(null)

  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [bio, setBio] = useState(user?.bio ?? '')
  const [location, setLocation] = useState(user?.location ?? '')
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl ?? '')
  const [saving, setSaving] = useState(false)

  if (!user) return null

  const save = async (event: FormEvent) => {
    event.preventDefault()
    setSaving(true)
    try {
      await updateProfile({
        displayName: displayName.trim(),
        bio: bio.trim(),
        location: location.trim(),
        avatarUrl: avatarUrl.trim(),
      })
      notify('Profile updated', 'success')
    } catch (error) {
      notify(errorMessage(error), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-7">
      <header>
        <h1 className="font-display text-2xl font-black tracking-tight text-mist-50 sm:text-3xl">
          Settings
        </h1>
        <p className="mt-1.5 text-sm text-mist-400">
          This is what other members see on{' '}
          <Link to={`/u/${user.username}`} className="text-flare-400 hover:underline">
            your profile
          </Link>
          .
        </p>
      </header>

      <form onSubmit={save} className="panel space-y-5 p-5">
        <div className="flex items-center gap-4">
          <Avatar user={{ ...user, avatarUrl }} size="lg" />
          <div className="min-w-0 flex-1">
            <p className="text-sm font-medium text-mist-100">Avatar</p>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <Button
                type="button"
                size="sm"
                variant="secondary"
                onClick={() => fileInput.current?.click()}
                loading={uploadImage.isPending}
              >
                Upload
              </Button>
              {avatarUrl && (
                <Button type="button" size="sm" variant="ghost" onClick={() => setAvatarUrl('')}>
                  Remove
                </Button>
              )}
              {uploadImage.isPending && (
                <Loader2 size={14} className="animate-spin text-mist-500" />
              )}
            </div>
            <input
              ref={fileInput}
              type="file"
              accept="image/jpeg,image/png,image/webp,image/avif"
              className="hidden"
              onChange={(event) => {
                const file = event.target.files?.[0]
                if (!file) return
                uploadImage.mutate(file, {
                  onSuccess: (data) => setAvatarUrl(data.url),
                  onError: (error) => notify(errorMessage(error), 'error'),
                })
              }}
            />
          </div>
        </div>

        <Field label="Display name">
          <input
            required
            maxLength={64}
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            className="h-10 w-full rounded-xl border border-ink-700 bg-ink-900 px-3 text-sm text-mist-100 focus:border-flare-500 focus:outline-none"
          />
        </Field>

        <Field label="Location">
          <input
            maxLength={80}
            value={location}
            onChange={(event) => setLocation(event.target.value)}
            placeholder="Berlin"
            className="h-10 w-full rounded-xl border border-ink-700 bg-ink-900 px-3 text-sm text-mist-100 placeholder:text-mist-600 focus:border-flare-500 focus:outline-none"
          />
        </Field>

        <Field label={`Bio (${bio.length}/280)`}>
          <textarea
            rows={3}
            maxLength={280}
            value={bio}
            onChange={(event) => setBio(event.target.value)}
            placeholder="What you look for in clothing."
            className="w-full resize-y rounded-xl border border-ink-700 bg-ink-900 px-3 py-2.5 text-sm leading-relaxed text-mist-100 placeholder:text-mist-600 focus:border-flare-500 focus:outline-none"
          />
        </Field>

        <div className="border-t border-ink-800 pt-4">
          <dl className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <dt className="text-xs uppercase tracking-wider text-mist-500">Username</dt>
              <dd className="text-mist-200">@{user.username}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wider text-mist-500">E-mail</dt>
              <dd className="truncate text-mist-200">{user.email}</dd>
            </div>
          </dl>
          <p className="mt-2 text-xs text-mist-500">
            Username and e-mail are fixed for now; changing them would break existing profile
            links.
          </p>
        </div>

        <div className="flex justify-end">
          <Button type="submit" variant="accent" loading={saving}>
            Save changes
          </Button>
        </div>
      </form>
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-mist-500">
        {label}
      </span>
      {children}
    </label>
  )
}
