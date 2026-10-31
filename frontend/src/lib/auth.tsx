import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, onSessionExpired, refreshAccessToken, setAccessToken } from './api'
import type { AuthResponse, UpdateProfilePayload, UserProfile } from './types'

interface AuthState {
  user: UserProfile | null
  /** True until the silent refresh on page load has resolved one way or the other. */
  initialising: boolean
  login: (login: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => Promise<void>
  updateProfile: (input: UpdateProfilePayload) => Promise<UserProfile>
  refreshUser: () => Promise<void>
}

export interface RegisterInput {
  username: string
  email: string
  password: string
  displayName?: string
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [initialising, setInitialising] = useState(true)
  const queryClient = useQueryClient()

  const adopt = useCallback(
    (response: AuthResponse) => {
      setAccessToken(response.accessToken)
      setUser(response.user)
      // Anything cached while signed out lacks the viewer-specific flags.
      queryClient.invalidateQueries()
    },
    [queryClient],
  )

  // On a cold load the access token is gone but the refresh cookie may still be valid.
  //
  // Page content renders immediately rather than waiting on this round trip, which means the first
  // queries go out unauthenticated and come back without the viewer-specific fields (your own
  // verdict, whether a look is in your wardrobe). Once a session is recovered we therefore refetch;
  // a signed-out visitor has nothing to recover and pays nothing.
  useEffect(() => {
    let cancelled = false
    refreshAccessToken()
      .then(async (token) => {
        if (!token || cancelled) return
        const { data } = await api.get<UserProfile>('/auth/me')
        if (cancelled) return
        setUser(data)
        queryClient.invalidateQueries()
      })
      .catch(() => undefined)
      .finally(() => {
        if (!cancelled) setInitialising(false)
      })
    return () => {
      cancelled = true
    }
  }, [queryClient])

  useEffect(() => {
    onSessionExpired(() => {
      setAccessToken(null)
      setUser(null)
    })
  }, [])

  const login = useCallback(
    async (loginValue: string, password: string) => {
      const { data } = await api.post<AuthResponse>('/auth/login', {
        login: loginValue,
        password,
      })
      adopt(data)
    },
    [adopt],
  )

  const register = useCallback(
    async (input: RegisterInput) => {
      const { data } = await api.post<AuthResponse>('/auth/register', input)
      adopt(data)
    },
    [adopt],
  )

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout')
    } finally {
      setAccessToken(null)
      setUser(null)
      queryClient.clear()
    }
  }, [queryClient])

  const updateProfile = useCallback(async (input: UpdateProfilePayload) => {
    const { data } = await api.patch<UserProfile>('/auth/me', input)
    setUser(data)
    return data
  }, [])

  const refreshUser = useCallback(async () => {
    const { data } = await api.get<UserProfile>('/auth/me')
    setUser(data)
  }, [])

  const value = useMemo<AuthState>(
    () => ({ user, initialising, login, register, logout, updateProfile, refreshUser }),
    [user, initialising, login, register, logout, updateProfile, refreshUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside <AuthProvider>')
  }
  return context
}
