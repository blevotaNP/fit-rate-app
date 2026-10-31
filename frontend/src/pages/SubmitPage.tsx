import { useRef, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ImagePlus, Loader2, Upload, X } from 'lucide-react'
import clsx from 'clsx'
import { useBrands, useCreateLook, useLook, useUpdateLook, useUploadImage } from '@/lib/queries'
import { CATEGORIES, LOOK_TYPES, OCCASIONS, SEASONS, humanise } from '@/lib/format'
import { Button } from '@/components/ui/Button'
import { Skeleton } from '@/components/ui/Skeleton'
import { useToast } from '@/components/ui/Toast'
import { errorMessage } from '@/lib/api'
import type { Category, CreateLookPayload, LookType, Occasion, Season } from '@/lib/types'

/**
 * Doubles as the publish and the edit form — the fields and validation are identical, only the
 * verb differs, so keeping two copies in sync would be the bigger risk.
 */
export function SubmitPage({ mode = 'create' }: { mode?: 'create' | 'edit' }) {
  const navigate = useNavigate()
  const { notify } = useToast()
  const { slug = '' } = useParams()
  const editing = mode === 'edit'

  const existing = useLook(editing ? slug : '')
  const brands = useBrands()
  const createLook = useCreateLook()
  const updateLook = useUpdateLook(slug)
  const uploadImage = useUploadImage()
  const fileInput = useRef<HTMLInputElement>(null)

  const [type, setType] = useState<LookType>('OUTFIT')
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState<Category>('STREETWEAR')
  const [season, setSeason] = useState<Season | ''>('')
  const [occasion, setOccasion] = useState<Occasion | ''>('')
  const [description, setDescription] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [brandName, setBrandName] = useState('')
  const [price, setPrice] = useState('')
  const [tagsInput, setTagsInput] = useState('')
  const [prefilledFor, setPrefilledFor] = useState<string | null>(null)

  // Fill the form once the look arrives, but never again — otherwise a background refetch
  // would wipe out whatever the author is in the middle of typing.
  const loaded = existing.data
  if (editing && loaded && prefilledFor !== loaded.slug) {
    setPrefilledFor(loaded.slug)
    setType(loaded.type)
    setTitle(loaded.title)
    setCategory(loaded.category)
    setSeason(loaded.season ?? '')
    setOccasion(loaded.occasion ?? '')
    setDescription(loaded.description ?? '')
    setImageUrl(loaded.imageUrl)
    setBrandName(loaded.brand?.name ?? '')
    setPrice(loaded.priceCents != null ? String(loaded.priceCents / 100) : '')
    setTagsInput(loaded.tags.map((tag) => tag.name).join(', '))
  }

  const pickFile = (file: File | undefined) => {
    if (!file) return
    uploadImage.mutate(file, {
      onSuccess: (data) => {
        setImageUrl(data.url)
        notify('Image uploaded', 'success')
      },
      onError: (error) => notify(errorMessage(error), 'error'),
    })
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!imageUrl.trim()) {
      notify('An image is required — this is a visual catalogue', 'error')
      return
    }

    const tags = tagsInput
      .split(',')
      .map((tag) => tag.trim().toLowerCase())
      .filter(Boolean)
      .slice(0, 8)

    const priceNumber = price.trim() ? Number(price) : null
    const payload: CreateLookPayload = {
      title: title.trim(),
      type,
      category,
      season: season || null,
      occasion: occasion || null,
      description: description.trim() || null,
      imageUrl: imageUrl.trim(),
      brandName: brandName.trim() || null,
      priceCents:
        priceNumber !== null && Number.isFinite(priceNumber) && priceNumber >= 0
          ? Math.round(priceNumber * 100)
          : null,
      currency: priceNumber !== null && Number.isFinite(priceNumber) ? 'EUR' : null,
      tags,
    }

    const mutation = editing ? updateLook : createLook
    mutation.mutate(payload, {
      onSuccess: (look) => {
        notify(editing ? 'Changes saved' : 'Published — the community can rate it now', 'success')
        navigate(`/looks/${look.slug}`)
      },
      onError: (error) => notify(errorMessage(error), 'error'),
    })
  }

  if (editing && existing.isLoading) {
    return (
      <div className="mx-auto max-w-3xl space-y-5">
        <Skeleton className="h-9 w-64" />
        <Skeleton className="h-28 rounded-card" />
        <Skeleton className="h-72 rounded-card" />
        <Skeleton className="h-96 rounded-card" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-7">
      <header>
        <h1 className="font-display text-2xl font-black tracking-tight text-mist-50 sm:text-3xl">
          {editing ? 'Edit look' : 'Publish a look'}
        </h1>
        <p className="mt-1.5 text-sm text-mist-400">
          {editing
            ? 'Verdicts already left on this look stay as they are — only the presentation changes.'
            : 'Submit a complete outfit or a single garment. You cannot rate your own submission — everyone else can.'}
        </p>
      </header>

      <form onSubmit={submit} className="space-y-6">
        <div className="panel p-4 sm:p-5">
          <FieldLabel>What are you posting?</FieldLabel>
          <div className="mt-2 flex gap-2">
            {LOOK_TYPES.map((option) => (
              <button
                key={option}
                type="button"
                onClick={() => setType(option)}
                className={clsx(
                  'flex-1 rounded-xl border px-4 py-3 text-left transition-colors',
                  type === option
                    ? 'border-flare-500 bg-flare-500/5'
                    : 'border-ink-700 hover:border-ink-600',
                )}
              >
                <span className="block text-sm font-semibold text-mist-100">
                  {option === 'OUTFIT' ? 'Complete outfit' : 'Single garment'}
                </span>
                <span className="mt-0.5 block text-xs text-mist-500">
                  {option === 'OUTFIT'
                    ? 'A full look, styled head to toe'
                    : 'One piece: a jacket, a shoe, a bag'}
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* --- Image ------------------------------------------------------- */}
        <div className="panel p-4 sm:p-5">
          <FieldLabel required>Image</FieldLabel>

          {imageUrl ? (
            <div className="relative mt-3 overflow-hidden rounded-xl border border-ink-700">
              <img src={imageUrl} alt="Preview" className="max-h-96 w-full object-cover" />
              <button
                type="button"
                onClick={() => setImageUrl('')}
                aria-label="Remove image"
                className="absolute right-2 top-2 grid h-8 w-8 place-items-center rounded-lg bg-black/70 text-white transition-colors hover:bg-black"
              >
                <X size={15} />
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => fileInput.current?.click()}
              disabled={uploadImage.isPending}
              className="mt-3 flex w-full flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-ink-700 py-12 transition-colors hover:border-ink-500"
            >
              {uploadImage.isPending ? (
                <Loader2 size={24} className="animate-spin text-mist-400" />
              ) : (
                <ImagePlus size={24} className="text-mist-500" />
              )}
              <span className="text-sm text-mist-300">
                {uploadImage.isPending ? 'Uploading…' : 'Upload a photo'}
              </span>
              <span className="text-xs text-mist-500">JPEG, PNG, WebP or AVIF, up to 8 MB</span>
            </button>
          )}

          <input
            ref={fileInput}
            type="file"
            accept="image/jpeg,image/png,image/webp,image/avif"
            className="hidden"
            onChange={(event) => pickFile(event.target.files?.[0])}
          />

          <div className="mt-3 flex items-center gap-2">
            <span className="text-xs text-mist-500">or paste a URL</span>
            <input
              value={imageUrl}
              onChange={(event) => setImageUrl(event.target.value)}
              placeholder="https://…"
              className="h-9 flex-1 rounded-lg border border-ink-700 bg-ink-900 px-3 text-sm text-mist-100 placeholder:text-mist-600 focus:border-flare-500 focus:outline-none"
            />
          </div>
        </div>

        {/* --- Details ----------------------------------------------------- */}
        <div className="panel space-y-4 p-4 sm:p-5">
          <div>
            <FieldLabel required>Title</FieldLabel>
            <input
              required
              minLength={3}
              maxLength={120}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder={type === 'OUTFIT' ? 'Blackout Layers, Kreuzberg' : 'Cable Knit, Undyed'}
              className="mt-1.5 h-10 w-full rounded-xl border border-ink-700 bg-ink-900 px-3 text-sm text-mist-100 placeholder:text-mist-600 focus:border-flare-500 focus:outline-none"
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <FieldLabel required>Category</FieldLabel>
              <Select value={category} onChange={(value) => setCategory(value as Category)}>
                {CATEGORIES.map((option) => (
                  <option key={option} value={option}>
                    {humanise(option)}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <FieldLabel>Season</FieldLabel>
              <Select value={season} onChange={(value) => setSeason(value as Season | '')}>
                <option value="">Any</option>
                {SEASONS.map((option) => (
                  <option key={option} value={option}>
                    {humanise(option)}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <FieldLabel>Occasion</FieldLabel>
              <Select value={occasion} onChange={(value) => setOccasion(value as Occasion | '')}>
                <option value="">Any</option>
                {OCCASIONS.map((option) => (
                  <option key={option} value={option}>
                    {humanise(option)}
                  </option>
                ))}
              </Select>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <FieldLabel>Brand</FieldLabel>
              <input
                list="brand-options"
                value={brandName}
                onChange={(event) => setBrandName(event.target.value)}
                placeholder="Leave blank if unbranded"
                className="mt-1.5 h-10 w-full rounded-xl border border-ink-700 bg-ink-900 px-3 text-sm text-mist-100 placeholder:text-mist-600 focus:border-flare-500 focus:outline-none"
              />
              <datalist id="brand-options">
                {brands.data?.map((brand) => <option key={brand.id} value={brand.name} />)}
              </datalist>
            </div>
            <div>
              <FieldLabel>Price (EUR)</FieldLabel>
              <input
                type="number"
                min={0}
                step="1"
                value={price}
                onChange={(event) => setPrice(event.target.value)}
                placeholder="Optional"
                className="mt-1.5 h-10 w-full rounded-xl border border-ink-700 bg-ink-900 px-3 text-sm text-mist-100 placeholder:text-mist-600 focus:border-flare-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <FieldLabel>Tags</FieldLabel>
            <input
              value={tagsInput}
              onChange={(event) => setTagsInput(event.target.value)}
              placeholder="minimal, layering, autumn — comma separated, up to 8"
              className="mt-1.5 h-10 w-full rounded-xl border border-ink-700 bg-ink-900 px-3 text-sm text-mist-100 placeholder:text-mist-600 focus:border-flare-500 focus:outline-none"
            />
          </div>

          <div>
            <FieldLabel>Notes</FieldLabel>
            <textarea
              rows={4}
              maxLength={2000}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="What you were going for, where it was worn, what you would change."
              className="mt-1.5 w-full resize-y rounded-xl border border-ink-700 bg-ink-900 px-3 py-2.5 text-sm leading-relaxed text-mist-100 placeholder:text-mist-600 focus:border-flare-500 focus:outline-none"
            />
          </div>
        </div>

        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={() => navigate(-1)}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="accent"
            icon={<Upload size={15} />}
            loading={createLook.isPending || updateLook.isPending}
          >
            {editing ? 'Save changes' : 'Publish'}
          </Button>
        </div>
      </form>
    </div>
  )
}

function FieldLabel({ children, required }: { children: React.ReactNode; required?: boolean }) {
  return (
    <span className="block text-xs font-medium uppercase tracking-wider text-mist-500">
      {children}
      {required && <span className="ml-1 text-flare-500">*</span>}
    </span>
  )
}

function Select({
  value,
  onChange,
  children,
}: {
  value: string
  onChange: (value: string) => void
  children: React.ReactNode
}) {
  return (
    <select
      value={value}
      onChange={(event) => onChange(event.target.value)}
      className="mt-1.5 h-10 w-full rounded-xl border border-ink-700 bg-ink-900 px-2.5 text-sm text-mist-100 focus:border-flare-500 focus:outline-none"
    >
      {children}
    </select>
  )
}
