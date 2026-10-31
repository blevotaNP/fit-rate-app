import { Link } from 'react-router-dom'
import { Sparkles } from 'lucide-react'
import { useRecommendations } from '@/lib/queries'
import { LookCard } from '@/components/look/LookCard'
import { CardSkeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/lib/auth'

export function ForYouPage() {
  const { user } = useAuth()
  const { data, isLoading, error, refetch } = useRecommendations(18)
  const ratingsGiven = user?.stats.ratingsGiven ?? 0

  return (
    <div className="space-y-7">
      <header>
        <h1 className="flex items-center gap-2.5 font-display text-2xl font-black tracking-tight text-mist-50 sm:text-3xl">
          <Sparkles size={24} className="text-flare-500" />
          For you
        </h1>
        <p className="mt-1.5 max-w-2xl text-sm leading-relaxed text-mist-400">
          Built from the tags on the looks you scored 70 or higher, then ranked on how many of
          those tags a candidate shares and how the community rated it. Every card says why it is
          here.
        </p>
      </header>

      {ratingsGiven < 3 && (
        <div className="panel flex flex-wrap items-center justify-between gap-4 p-4">
          <p className="text-sm text-mist-300">
            You have rated {ratingsGiven} {ratingsGiven === 1 ? 'look' : 'looks'}. Below three, this
            page falls back to the community favourites.
          </p>
          <Link to="/looks">
            <Button variant="primary" size="sm">
              Rate a few looks
            </Button>
          </Link>
        </div>
      )}

      {error ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : isLoading ? (
        <div className="grid grid-cols-2 gap-x-4 gap-y-7 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
          {Array.from({ length: 10 }).map((_, index) => (
            <CardSkeleton key={index} />
          ))}
        </div>
      ) : !data || data.length === 0 ? (
        <EmptyState
          title="Nothing to suggest yet"
          hint="Rate a few looks and this page fills up."
          action={
            <Link to="/looks">
              <Button variant="primary">Browse the catalogue</Button>
            </Link>
          }
        />
      ) : (
        <div className="grid grid-cols-2 gap-x-4 gap-y-7 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
          {data.map((recommendation) => (
            <div key={recommendation.look.id} className="flex flex-col">
              <LookCard look={recommendation.look} />
              <p className="mt-2 flex items-start gap-1.5 text-[11px] leading-snug text-mist-500">
                <Sparkles size={11} className="mt-0.5 shrink-0 text-flare-500" />
                {recommendation.reason}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
