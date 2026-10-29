import type { Session } from './types.ts'

const SESSION_KEY = 'cloudnine.session'

export class ApiError extends Error {
  status: number
  code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export function loadSession(): Session | null {
  const raw = localStorage.getItem(SESSION_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as Session
  } catch {
    return null
  }
}

export function saveSession(session: Session | null) {
  if (!session) {
    localStorage.removeItem(SESSION_KEY)
    return
  }
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const session = loadSession()
  if (session && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${session.token}`)
  }
  const response = await fetch(path, { ...options, headers })
  if (response.status === 204) {
    return undefined as T
  }
  const text = await response.text()
  const data = text ? (JSON.parse(text) as { error?: string; message?: string }) : null
  if (!response.ok) {
    throw new ApiError(response.status, data?.error ?? 'ERROR', data?.message ?? 'Request failed.')
  }
  return data as T
}
