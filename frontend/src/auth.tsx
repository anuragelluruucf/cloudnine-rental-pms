import { createContext, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api, loadSession, saveSession } from './api.ts'
import type { Session } from './types.ts'

interface AuthValue {
  session: Session | null
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => loadSession())

  const value = useMemo<AuthValue>(() => ({
    session,
    async login(email, password) {
      const next = await api<Session>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })
      saveSession(next)
      setSession(next)
    },
    async register(name, email, password) {
      const next = await api<Session>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({ name, email, password }),
      })
      saveSession(next)
      setSession(next)
    },
    logout() {
      saveSession(null)
      setSession(null)
    },
  }), [session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is missing')
  return value
}
