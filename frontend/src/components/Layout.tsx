import { NavLink, Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth.tsx'

export function Layout() {
  const { session, logout } = useAuth()
  if (!session) return <Navigate to="/login" replace />

  return (
    <div className="shell">
      <aside className="sidebar">
        <div>
          <p className="mark">CloudNine</p>
          <p className="mark-note">Listings, calendars, and guest replies in one notebook.</p>
        </div>
        <nav>
          <NavLink to="/" end>Today</NavLink>
          <NavLink to="/properties">Listings</NavLink>
          <NavLink to="/inbox">Inbox</NavLink>
        </nav>
        <div className="sidebar-user">
          <strong>{session.user.name}</strong>
          <span>{session.user.email}</span>
          <button type="button" className="text-button" onClick={logout}>Sign out</button>
        </div>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
