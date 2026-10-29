import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, api } from '../api.ts'
import { statusLabel } from '../lib/dates.ts'
import type { GuestMessage } from '../types.ts'

export function InboxPage() {
  const [messages, setMessages] = useState<GuestMessage[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api<GuestMessage[]>('/api/messages')
      .then(setMessages)
      .catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Could not load the inbox.'))
  }, [])

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <p className="eyebrow">Drafts waiting on you</p>
          <h1>Inbox</h1>
        </div>
      </header>
      {error && <p className="banner danger" role="alert">{error}</p>}
      {messages && messages.length === 0 && (
        <div className="card empty-card">
          <h2>No guest questions yet</h2>
          <p>Open a listing and ask something a guest would ask. The draft stays here until you approve it.</p>
        </div>
      )}
      <div className="stack">
        {messages?.map((message) => (
          <article key={message.id} className="card thread">
            <header>
              <Link to={`/properties/${message.propertyId}`}>{message.propertyName}</Link>
              <span>{statusLabel(message.status)}</span>
            </header>
            <p><strong>{message.guestName}:</strong> {message.body}</p>
            <p className="muted">{message.status === 'SENT' ? message.sentReply : message.draftReply}</p>
          </article>
        ))}
      </div>
      {!messages && !error && <p className="muted">Loading inbox…</p>}
    </div>
  )
}
