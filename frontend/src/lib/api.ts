import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import type { ApiError, AuthResponse } from './types'

/**
 * The access token lives in memory only.
 *
 * Putting it in localStorage would make every XSS bug a permanent account takeover; keeping it in
 * a module variable means a page reload simply asks the HttpOnly refresh cookie for a new one.
 */
let accessToken: string | null = null
let onSessionLost: (() => void) | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

export function onSessionExpired(handler: () => void) {
  onSessionLost = handler
}

export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

/** Endpoints where a 401 is the answer, not a signal to go looking for a new token. */
const NO_RETRY = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout']

// A burst of parallel requests must not fire a refresh each; they all await the first one.
let refreshInFlight: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshInFlight) {
    refreshInFlight = axios
      .post<AuthResponse>('/api/auth/refresh', null, { withCredentials: true })
      .then((response) => {
        setAccessToken(response.data.accessToken)
        return response.data.accessToken
      })
      .catch(() => {
        setAccessToken(null)
        return null
      })
      .finally(() => {
        refreshInFlight = null
      })
  }
  return refreshInFlight
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError>) => {
    const original = error.config as (AxiosRequestConfig & { _retried?: boolean }) | undefined
    const url = original?.url ?? ''
    const shouldRetry =
      error.response?.status === 401 &&
      original &&
      !original._retried &&
      !NO_RETRY.some((path) => url.includes(path))

    if (shouldRetry) {
      original._retried = true
      const token = await refreshAccessToken()
      if (token) {
        return api(original)
      }
      onSessionLost?.()
    }
    return Promise.reject(error)
  },
)

/** Turns any thrown value into a sentence worth showing a person. */
export function errorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (axios.isAxiosError<ApiError>(error)) {
    const data = error.response?.data
    if (data?.fieldErrors) {
      const first = Object.entries(data.fieldErrors)[0]
      if (first) {
        return `${first[0]}: ${first[1]}`
      }
    }
    if (data?.message) {
      return data.message
    }
    if (error.code === 'ERR_NETWORK') {
      return 'Cannot reach the API. Is the backend running on :8080?'
    }
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

export { refreshAccessToken }

/** HTTP status behind a thrown query error, when there is one. */
export function httpStatus(error: unknown): number | undefined {
  return axios.isAxiosError(error) ? error.response?.status : undefined
}

/** True only for a genuine 404 — a network failure must not be reported as "does not exist". */
export function isNotFound(error: unknown): boolean {
  return httpStatus(error) === 404
}
