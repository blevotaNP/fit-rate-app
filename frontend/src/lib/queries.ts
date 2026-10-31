import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseQueryOptions,
} from '@tanstack/react-query'
import { api } from './api'
import type {
  Brand,
  ChartEntry,
  ChartPeriod,
  Comment,
  CreateLookPayload,
  LookDetail,
  LookFilters,
  LookSummary,
  PageResponse,
  PlatformStats,
  Recommendation,
  Review,
  SaveReviewPayload,
  TagRef,
  UploadResponse,
  UserProfile,
} from './types'

/**
 * Query keys are declared in one place so an invalidation after a mutation can never drift out of
 * sync with the key a component subscribed to.
 */
export const keys = {
  looks: (filters: LookFilters) => ['looks', filters] as const,
  look: (slug: string) => ['look', slug] as const,
  similar: (slug: string) => ['look', slug, 'similar'] as const,
  lookReviews: (slug: string, sort: string, page: number) =>
    ['look', slug, 'reviews', sort, page] as const,
  lookComments: (slug: string) => ['look', slug, 'comments'] as const,
  featured: () => ['looks', 'featured'] as const,
  chart: (period: ChartPeriod, limit: number) => ['chart', period, limit] as const,
  trending: (limit: number) => ['chart', 'trending', limit] as const,
  feed: (page: number) => ['reviews', 'feed', page] as const,
  recommendations: (limit: number) => ['recommendations', limit] as const,
  stats: () => ['stats'] as const,
  brands: () => ['brands'] as const,
  tags: (limit: number) => ['tags', limit] as const,
  profile: (username: string) => ['user', username] as const,
  userLooks: (username: string, page: number) => ['user', username, 'looks', page] as const,
  userReviews: (username: string, page: number) => ['user', username, 'reviews', page] as const,
  userWardrobe: (username: string, page: number) => ['user', username, 'wardrobe', page] as const,
}

type QueryTweaks<T> = Omit<UseQueryOptions<T, Error, T, readonly unknown[]>, 'queryKey' | 'queryFn'>

async function get<T>(url: string, params?: object): Promise<T> {
  const { data } = await api.get<T>(url, { params })
  return data
}

// --- catalogue ---------------------------------------------------------------

export function useLooks(filters: LookFilters, options?: QueryTweaks<PageResponse<LookSummary>>) {
  return useQuery({
    queryKey: keys.looks(filters),
    queryFn: () => get<PageResponse<LookSummary>>('/looks', filters),
    placeholderData: (previous) => previous,
    ...options,
  })
}

export function useFeaturedLooks(limit = 5) {
  return useQuery({
    queryKey: keys.featured(),
    queryFn: () => get<LookSummary[]>('/looks/featured', { limit }),
  })
}

export function useLook(slug: string) {
  return useQuery({
    queryKey: keys.look(slug),
    queryFn: () => get<LookDetail>(`/looks/${slug}`),
    enabled: Boolean(slug),
  })
}

export function useSimilarLooks(slug: string, limit = 6) {
  return useQuery({
    queryKey: keys.similar(slug),
    queryFn: () => get<LookSummary[]>(`/looks/${slug}/similar`, { limit }),
    enabled: Boolean(slug),
  })
}

export function useLookReviews(slug: string, sort: string, page: number, size = 8) {
  return useQuery({
    queryKey: keys.lookReviews(slug, sort, page),
    queryFn: () => get<PageResponse<Review>>(`/looks/${slug}/reviews`, { sort, page, size }),
    enabled: Boolean(slug),
    placeholderData: (previous) => previous,
  })
}

export function useLookComments(slug: string) {
  return useQuery({
    queryKey: keys.lookComments(slug),
    queryFn: () => get<Comment[]>(`/looks/${slug}/comments`),
    enabled: Boolean(slug),
  })
}

// --- leaderboards and feeds --------------------------------------------------

export function useChart(period: ChartPeriod, limit = 20) {
  return useQuery({
    queryKey: keys.chart(period, limit),
    queryFn: () => get<ChartEntry[]>(`/charts/${period}`, { limit }),
  })
}

export function useTrending(limit = 12) {
  return useQuery({
    queryKey: keys.trending(limit),
    queryFn: () => get<ChartEntry[]>('/charts/trending/now', { limit }),
  })
}

export function useReviewFeed(page = 0, size = 8) {
  return useQuery({
    queryKey: keys.feed(page),
    queryFn: () => get<PageResponse<Review>>('/reviews/feed', { page, size }),
    placeholderData: (previous) => previous,
  })
}

export function useRecommendations(limit = 12, enabled = true) {
  return useQuery({
    queryKey: keys.recommendations(limit),
    queryFn: () => get<Recommendation[]>('/recommendations', { limit }),
    enabled,
  })
}

// --- reference data ----------------------------------------------------------

export function useStats() {
  return useQuery({ queryKey: keys.stats(), queryFn: () => get<PlatformStats>('/stats') })
}

export function useBrands() {
  return useQuery({ queryKey: keys.brands(), queryFn: () => get<Brand[]>('/brands') })
}

export function usePopularTags(limit = 20) {
  return useQuery({
    queryKey: keys.tags(limit),
    queryFn: () => get<TagRef[]>('/tags', { limit }),
  })
}

// --- profiles ----------------------------------------------------------------

export function useProfile(username: string) {
  return useQuery({
    queryKey: keys.profile(username),
    queryFn: () => get<UserProfile>(`/users/${username}`),
    enabled: Boolean(username),
  })
}

export function useUserLooks(username: string, page = 0, enabled = true) {
  return useQuery({
    queryKey: keys.userLooks(username, page),
    queryFn: () => get<PageResponse<LookSummary>>(`/users/${username}/looks`, { page }),
    enabled: enabled && Boolean(username),
  })
}

export function useUserReviews(username: string, page = 0, enabled = true) {
  return useQuery({
    queryKey: keys.userReviews(username, page),
    queryFn: () => get<PageResponse<Review>>(`/users/${username}/reviews`, { page }),
    enabled: enabled && Boolean(username),
  })
}

export function useUserWardrobe(username: string, page = 0, enabled = true) {
  return useQuery({
    queryKey: keys.userWardrobe(username, page),
    queryFn: () => get<PageResponse<LookSummary>>(`/users/${username}/wardrobe`, { page }),
    enabled: enabled && Boolean(username),
  })
}

// --- mutations ---------------------------------------------------------------

export function useSaveReview(slug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (payload: SaveReviewPayload) => {
      const { data } = await api.put<Review>(`/looks/${slug}/review`, payload)
      return data
    },
    onSuccess: () => {
      // The look's averages, the review list and every leaderboard just changed.
      queryClient.invalidateQueries({ queryKey: ['look', slug] })
      queryClient.invalidateQueries({ queryKey: ['looks'] })
      queryClient.invalidateQueries({ queryKey: ['chart'] })
      queryClient.invalidateQueries({ queryKey: ['reviews'] })
      queryClient.invalidateQueries({ queryKey: ['recommendations'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}

export function useDeleteReview(slug?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (reviewId: number) => api.delete(`/reviews/${reviewId}`),
    onSuccess: () => {
      if (slug) queryClient.invalidateQueries({ queryKey: ['look', slug] })
      queryClient.invalidateQueries({ queryKey: ['reviews'] })
      queryClient.invalidateQueries({ queryKey: ['looks'] })
      queryClient.invalidateQueries({ queryKey: ['chart'] })
    },
  })
}

export function useToggleReviewLike() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (reviewId: number) => {
      const { data } = await api.post<{ likesCount: number }>(`/reviews/${reviewId}/like`)
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] })
      queryClient.invalidateQueries({ queryKey: ['look'] })
    },
  })
}

export function useToggleFavorite() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (slug: string) => {
      const { data } = await api.post<{ favorited: boolean }>(`/looks/${slug}/favorite`)
      return data
    },
    onSuccess: (_data, slug) => {
      queryClient.invalidateQueries({ queryKey: ['look', slug] })
      queryClient.invalidateQueries({ queryKey: ['looks'] })
      queryClient.invalidateQueries({ queryKey: ['user'] })
    },
  })
}

export function usePostComment(slug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (input: { body: string; parentId?: number | null }) => {
      const { data } = await api.post<Comment>(`/looks/${slug}/comments`, input)
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.lookComments(slug) })
      queryClient.invalidateQueries({ queryKey: keys.look(slug) })
    },
  })
}

export function useDeleteComment(slug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (commentId: number) => api.delete(`/comments/${commentId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.lookComments(slug) })
      queryClient.invalidateQueries({ queryKey: keys.look(slug) })
    },
  })
}

export function useCreateLook() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (payload: CreateLookPayload) => {
      const { data } = await api.post<LookDetail>('/looks', payload)
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['looks'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
      queryClient.invalidateQueries({ queryKey: ['tags'] })
    },
  })
}

export function useUpdateLook(slug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (payload: CreateLookPayload) => {
      const { data } = await api.put<LookDetail>(`/looks/${slug}`, payload)
      return data
    },
    onSuccess: (look) => {
      // The slug can change with the title, so both the old and the new key are invalidated.
      queryClient.invalidateQueries({ queryKey: ['look', slug] })
      queryClient.invalidateQueries({ queryKey: ['look', look.slug] })
      queryClient.invalidateQueries({ queryKey: ['looks'] })
      queryClient.invalidateQueries({ queryKey: ['tags'] })
    },
  })
}

export function useDeleteLook() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (slug: string) => api.delete(`/looks/${slug}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['looks'] })
      queryClient.invalidateQueries({ queryKey: ['chart'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
      queryClient.invalidateQueries({ queryKey: ['user'] })
    },
  })
}

export function useUploadImage() {
  return useMutation({
    mutationFn: async (file: File) => {
      const form = new FormData()
      form.append('file', file)
      const { data } = await api.post<UploadResponse>('/uploads', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      return data
    },
  })
}

export function useToggleFollow(username: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (currentlyFollowing: boolean) => {
      if (currentlyFollowing) {
        await api.delete(`/users/${username}/follow`)
      } else {
        await api.post(`/users/${username}/follow`)
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user'] })
    },
  })
}
