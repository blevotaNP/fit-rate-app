/** Wire types, mirroring the DTO records the Spring API serialises. */

export type Role = 'USER' | 'CURATOR' | 'ADMIN'
export type LookType = 'OUTFIT' | 'ITEM'

export type Category =
  | 'STREETWEAR'
  | 'CASUAL'
  | 'MINIMAL'
  | 'FORMAL'
  | 'TECHWEAR'
  | 'VINTAGE'
  | 'AVANT_GARDE'
  | 'SPORTSWEAR'
  | 'OUTERWEAR'
  | 'FOOTWEAR'
  | 'KNITWEAR'
  | 'DENIM'
  | 'ACCESSORIES'

export type Season = 'SPRING' | 'SUMMER' | 'AUTUMN' | 'WINTER' | 'ALL_SEASON'

export type Occasion =
  | 'EVERYDAY'
  | 'OFFICE'
  | 'EVENING'
  | 'PARTY'
  | 'SPORT'
  | 'TRAVEL'
  | 'RUNWAY'

export type SortOrder =
  | 'NEWEST'
  | 'OLDEST'
  | 'TOP_RATED'
  | 'MOST_RATED'
  | 'LOWEST_RATED'
  | 'ALPHABETICAL'

export type ChartPeriod = 'WEEK' | 'MONTH' | 'YEAR' | 'ALL_TIME'

export interface UserSummary {
  id: number
  username: string
  displayName: string
  avatarUrl: string | null
  role: Role
}

export interface UserStats {
  looksPublished: number
  ratingsGiven: number
  reviewsWritten: number
  likesReceived: number
  favorites: number
  followers: number
  following: number
  averageScoreGiven: number
}

export interface UserProfile {
  id: number
  username: string
  displayName: string
  email: string | null
  avatarUrl: string | null
  bio: string | null
  location: string | null
  role: Role
  joinedAt: string
  stats: UserStats
  followedByMe: boolean | null
}

export interface Brand {
  id: number
  name: string
  slug: string
  country: string | null
  logoUrl: string | null
  description: string | null
}

export interface TagRef {
  id: number
  name: string
  slug: string
  usageCount: number | null
}

export interface ScoreBreakdown {
  style: number | null
  fit: number | null
  color: number | null
  originality: number | null
  quality: number | null
  overall: number | null
}

export interface LookSummary {
  id: number
  slug: string
  title: string
  type: LookType
  category: Category
  season: Season | null
  imageUrl: string
  brand: Brand | null
  author: UserSummary
  priceCents: number | null
  currency: string | null
  featured: boolean
  ratingsCount: number
  reviewsCount: number
  avgScore: number | null
  tags: TagRef[]
  createdAt: string
  favoritedByMe: boolean | null
}

export interface Review {
  id: number
  author: UserSummary
  look: LookSummary | null
  style: number
  fit: number
  color: number
  originality: number
  quality: number
  score: number
  title: string | null
  body: string | null
  likesCount: number
  createdAt: string
  updatedAt: string
  likedByMe: boolean | null
  editable: boolean
}

export interface LookDetail {
  id: number
  slug: string
  title: string
  type: LookType
  category: Category
  season: Season | null
  occasion: Occasion | null
  description: string | null
  imageUrl: string
  brand: Brand | null
  author: UserSummary
  priceCents: number | null
  currency: string | null
  releaseDate: string | null
  featured: boolean
  ratingsCount: number
  reviewsCount: number
  commentsCount: number
  favoritesCount: number
  scores: ScoreBreakdown
  tags: TagRef[]
  createdAt: string
  favoritedByMe: boolean | null
  myReview: Review | null
  editable: boolean
}

export interface Comment {
  id: number
  author: UserSummary
  body: string
  createdAt: string
  editable: boolean
  replies: Comment[]
}

export interface ChartEntry {
  rank: number
  look: LookSummary
  periodScore: number
  periodReviews: number
}

export interface Recommendation {
  look: LookSummary
  reason: string
  affinity: number
}

export interface PlatformStats {
  looks: number
  outfits: number
  items: number
  ratings: number
  reviews: number
  members: number
  brands: number
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  user: UserProfile
}

export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors?: Record<string, string>
}

export interface SaveReviewPayload {
  style: number
  fit: number
  color: number
  originality: number
  quality: number
  title?: string | null
  body?: string | null
}

export interface CreateLookPayload {
  title: string
  type: LookType
  category: Category
  season?: Season | null
  occasion?: Occasion | null
  description?: string | null
  imageUrl: string
  brandName?: string | null
  priceCents?: number | null
  currency?: string | null
  releaseDate?: string | null
  tags?: string[]
}

export interface UpdateProfilePayload {
  displayName?: string
  bio?: string
  location?: string
  avatarUrl?: string
}

export interface LookFilters {
  q?: string
  type?: LookType
  category?: Category
  season?: Season
  occasion?: Occasion
  brand?: string
  tag?: string
  author?: string
  featured?: boolean
  minScore?: number
  sort?: SortOrder
  page?: number
  size?: number
}

export interface UploadResponse {
  url: string
  filename: string
  sizeBytes: number
  contentType: string
}
