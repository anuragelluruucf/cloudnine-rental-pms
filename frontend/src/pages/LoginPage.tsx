import { useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { ApiError } from '../api.ts'
import { useAuth } from '../auth.tsx'

export function LoginPage() {
  const { session, login, register } = useAuth()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('mara@cloudnine.test')
  const [password, setPassword] = useState('cloudnine-demo')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (session) return <Navigate to="/" replace />

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (mode === 'login') await login(email, password)
      else await register(name, email, password)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Could not sign in.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login">
      <section className="login-story">
        <p className="mark">CloudNine</p>
        <h1>One notebook for the places you host.</h1>
        <p>
          A personal project for keeping listings, reservations, and guest replies from drifting apart.
          Check-in counts, check-out does not, and a draft is not sent until you say so.
        </p>
      </section>
      <form className="card login-card stack" onSubmit={onSubmit}>
        <h2>{mode === 'login' ? 'Sign in' : 'Create a manager account'}</h2>
        {error && <div className="banner danger" role="alert">{error}</div>}
        {mode === 'register' && (
          <label>
            Name
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
        )}
        <label>
          Email
          <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={8} />
        </label>
        <button type="submit" disabled={busy}>{busy ? 'Working…' : mode === 'login' ? 'Sign in' : 'Create account'}</button>
        <p className="hint">
          Demo manager: mara@cloudnine.test / cloudnine-demo
          <br />
          Demo admin: admin@cloudnine.test / cloudnine-demo
        </p>
        <button
          type="button"
          className="text-button"
          onClick={() => {
            setMode(mode === 'login' ? 'register' : 'login')
            setError(null)
          }}
        >
          {mode === 'login' ? 'Need an account? Register' : 'Already have an account? Sign in'}
        </button>
      </form>
    </div>
  )
}
