import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, api } from '../api.ts'
import { MonthCalendar } from '../components/MonthCalendar.tsx'
import { ReservationForm } from '../components/ReservationForm.tsx'
import { channelLabel, shiftMonth, shortDate, statusLabel } from '../lib/dates.ts'
import type {
  Channel,
  ChannelSource,
  GuestMessage,
  ImportReport,
  Property,
  Reservation,
  ReservationInput,
} from '../types.ts'

export function PropertyPage() {
  const { id } = useParams()
  const today = new Date()
  const [cursor, setCursor] = useState({ year: today.getFullYear(), month: today.getMonth() + 1 })
  const [property, setProperty] = useState<Property | null>(null)
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [channels, setChannels] = useState<ChannelSource[]>([])
  const [messages, setMessages] = useState<GuestMessage[]>([])
  const [error, setError] = useState<string | null>(null)
  const [bookingError, setBookingError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    if (!id) return
    const from = `${cursor.year}-${String(cursor.month).padStart(2, '0')}-01`
    const next = shiftMonth(cursor.year, cursor.month, 1)
    const to = `${next.year}-${String(next.month).padStart(2, '0')}-01`
    const [listing, stays, feeds, notes] = await Promise.all([
      api<Property>(`/api/properties/${id}`),
      api<Reservation[]>(`/api/properties/${id}/reservations?from=${from}&to=${to}`),
      api<ChannelSource[]>(`/api/properties/${id}/channels`),
      api<GuestMessage[]>(`/api/properties/${id}/messages`),
    ])
    setProperty(listing)
    setReservations(stays)
    setChannels(feeds)
    setMessages(notes)
  }, [id, cursor])

  useEffect(() => {
    load().catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Could not load this listing.'))
  }, [load])

  async function saveStay(input: ReservationInput) {
    if (!id) return
    setSaving(true)
    setBookingError(null)
    setNotice(null)
    try {
      await api(`/api/properties/${id}/reservations`, {
        method: 'POST',
        body: JSON.stringify({
          ...input,
          guestName: input.guestName || null,
          guestEmail: input.guestEmail || null,
          notes: input.notes || null,
        }),
      })
      await load()
    } catch (caught) {
      if (caught instanceof ApiError) setBookingError(caught.message)
      else setBookingError('Could not save that stay.')
      throw caught
    } finally {
      setSaving(false)
    }
  }

  async function cancelStay(reservationId: string) {
    setError(null)
    try {
      await api(`/api/reservations/${reservationId}/cancel`, { method: 'POST' })
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Could not cancel that stay.')
    }
  }

  async function importSample() {
    if (!id) return
    setNotice(null)
    setError(null)
    try {
      let channel = channels.find((item) => item.channel === 'AIRBNB')
      if (!channel) {
        channel = await api<ChannelSource>(`/api/properties/${id}/channels`, {
          method: 'POST',
          body: JSON.stringify({ channel: 'AIRBNB' }),
        })
      }
      const ics = await fetch('/api/samples/airbnb-november.ics').then((response) => response.text())
      const report = await api<ImportReport>(`/api/channels/${channel.id}/import`, {
        method: 'POST',
        body: JSON.stringify({ ics }),
      })
      setCursor({ year: 2026, month: 11 })
      setNotice(report.message)
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Could not import the sample calendar.')
    }
  }

  if (error && !property) return <p className="banner danger" role="alert">{error}</p>
  if (!property || !id) return <p className="muted">Loading listing…</p>

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <p className="eyebrow">{property.address}</p>
          <h1>{property.name}</h1>
        </div>
        <div className="row">
          <Link to={`/properties/${id}/edit`}>Edit notes</Link>
          <a href={property.icalPath}>Export .ics</a>
        </div>
      </header>
      {error && <p className="banner danger" role="alert">{error}</p>}
      {notice && <p className="banner ok" role="status">{notice}</p>}
      <div className="property-layout">
        <MonthCalendar
          year={cursor.year}
          month={cursor.month}
          reservations={reservations}
          onShift={(delta) => setCursor((current) => shiftMonth(current.year, current.month, delta))}
        />
        <section className="card">
          <h2>Add a stay</h2>
          <ReservationForm onSubmit={saveStay} error={bookingError} submitting={saving} />
        </section>
      </div>
      <section className="card">
        <h2>Stays this month</h2>
        {reservations.length === 0 && <p className="muted">Nothing on the calendar for this month.</p>}
        <ul className="stay-list">
          {reservations.map((stay) => (
            <li key={stay.id}>
              <div>
                <strong>{stay.guestName || 'Blocked'}</strong>
                <p>
                  {shortDate(stay.checkIn)} – {shortDate(stay.checkOut)} · {channelLabel(stay.channel)} · {statusLabel(stay.status)}
                  {stay.conflict ? ' · flagged, other stay left alone' : ''}
                </p>
              </div>
              {stay.status !== 'CANCELLED' && (
                <button type="button" className="ghost" onClick={() => cancelStay(stay.id)}>Cancel</button>
              )}
            </li>
          ))}
        </ul>
      </section>
      <ChannelDesk
        propertyId={id}
        channels={channels}
        onChange={async (message) => {
          setNotice(message)
          await load()
        }}
        onImportSample={importSample}
      />
      <GuestDesk propertyId={id} propertyName={property.name} messages={messages} onChange={load} />
    </div>
  )
}

function ChannelDesk({
  propertyId,
  channels,
  onChange,
  onImportSample,
}: {
  propertyId: string
  channels: ChannelSource[]
  onChange: (message: string) => Promise<void>
  onImportSample: () => Promise<void>
}) {
  const [channel, setChannel] = useState<Channel>('VRBO')
  const [importUrl, setImportUrl] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function run(action: () => Promise<void>) {
    setBusy(true)
    setError(null)
    try {
      await action()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'That calendar action failed.')
    } finally {
      setBusy(false)
    }
  }

  function addChannel() {
    return run(async () => {
      await api(`/api/properties/${propertyId}/channels`, {
        method: 'POST',
        body: JSON.stringify({ channel, importUrl: importUrl || null }),
      })
      setImportUrl('')
      await onChange('Channel connected.')
    })
  }

  function sync(channelId: string) {
    return run(async () => {
      const report = await api<ImportReport>(`/api/channels/${channelId}/sync`, { method: 'POST' })
      await onChange(report.message)
    })
  }

  function saveUrl(item: ChannelSource, url: string) {
    return run(async () => {
      await api(`/api/channels/${item.id}`, {
        method: 'PUT',
        body: JSON.stringify({ channel: item.channel, importUrl: url }),
      })
      await onChange('Import URL saved.')
    })
  }

  function importFile(channelId: string, file: File) {
    return run(async () => {
      const ics = await file.text()
      const report = await api<ImportReport>(`/api/channels/${channelId}/import`, {
        method: 'POST',
        body: JSON.stringify({ ics }),
      })
      await onChange(report.message)
    })
  }

  return (
    <section className="card stack">
      <div className="page-head">
        <h2>Calendars</h2>
        <button type="button" onClick={onImportSample}>Import sample calendar</button>
      </div>
      {error && <p className="banner danger" role="alert">{error}</p>}
      <p className="hint">The sample is an Airbnb-style feed for November 2026. Re-importing the same file does not duplicate stays. If a date clashes with a stay you already have, it is flagged and the existing stay is left alone.</p>
      {channels.map((item) => (
        <ChannelRow key={item.id} item={item} onSync={sync} onSaveUrl={saveUrl} onFile={importFile} />
      ))}
      <div className="split end">
        <label>
          Connect a channel
          <select value={channel} onChange={(event) => setChannel(event.target.value as Channel)}>
            <option value="AIRBNB">Airbnb</option>
            <option value="VRBO">Vrbo</option>
            <option value="BOOKING">Booking.com</option>
            <option value="DIRECT">Direct</option>
          </select>
        </label>
        <label>
          iCal URL
          <input value={importUrl} placeholder="https://…" onChange={(event) => setImportUrl(event.target.value)} />
        </label>
        <button type="button" disabled={busy} onClick={addChannel}>Connect</button>
      </div>
    </section>
  )
}

function ChannelRow({
  item,
  onSync,
  onSaveUrl,
  onFile,
}: {
  item: ChannelSource
  onSync: (id: string) => Promise<void>
  onSaveUrl: (item: ChannelSource, url: string) => Promise<void>
  onFile: (id: string, file: File) => Promise<void>
}) {
  const [url, setUrl] = useState(item.importUrl ?? '')
  return (
    <div className="channel-row">
      <div>
        <strong>{channelLabel(item.channel)}</strong>
        <p className="muted">{item.lastSyncMessage || 'Not synced yet'}</p>
      </div>
      <input value={url} onChange={(event) => setUrl(event.target.value)} placeholder="iCal URL" />
      <div className="row">
        <button type="button" className="ghost" onClick={() => onSaveUrl(item, url)}>Save URL</button>
        <button type="button" className="ghost" onClick={() => onSync(item.id)}>Sync</button>
        <label className="file">
          Import file
          <input
            type="file"
            accept=".ics,text/calendar"
            onChange={(event) => {
              const file = event.target.files?.[0]
              if (file) void onFile(item.id, file)
            }}
          />
        </label>
      </div>
    </div>
  )
}

function GuestDesk({
  propertyId,
  propertyName,
  messages,
  onChange,
}: {
  propertyId: string
  propertyName: string
  messages: GuestMessage[]
  onChange: () => Promise<void>
}) {
  const [guestName, setGuestName] = useState('Sam Rivera')
  const [body, setBody] = useState('')
  const [drafts, setDrafts] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setDrafts(Object.fromEntries(messages.map((message) => [message.id, message.draftReply ?? ''])))
  }, [messages])

  async function ask(question: string) {
    setError(null)
    try {
      await api(`/api/properties/${propertyId}/messages`, {
        method: 'POST',
        body: JSON.stringify({ guestName, body: question }),
      })
      setBody('')
      await onChange()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Could not draft a reply.')
    }
  }

  async function approve(message: GuestMessage) {
    const reply = drafts[message.id] ?? message.draftReply ?? ''
    setError(null)
    try {
      await api(`/api/messages/${message.id}/approve`, {
        method: 'POST',
        body: JSON.stringify({ reply }),
      })
      await onChange()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Could not send that reply.')
    }
  }

  return (
    <section className="card stack">
      <h2>Guest questions</h2>
      <p className="hint">A reply is drafted from {propertyName}'s notes. Nothing is marked sent until you approve it.</p>
      {error && <p className="banner danger" role="alert">{error}</p>}
      <div className="chips">
        {['What is the wifi password?', 'What time is check-in?', 'Can we use the grill?'].map((question) => (
          <button key={question} type="button" className="chip" onClick={() => setBody(question)}>{question}</button>
        ))}
      </div>
      <label>
        Guest
        <input value={guestName} onChange={(event) => setGuestName(event.target.value)} />
      </label>
      <label>
        Question
        <textarea rows={3} value={body} onChange={(event) => setBody(event.target.value)} />
      </label>
      <button type="button" disabled={!body.trim() || !guestName.trim()} onClick={() => ask(body)}>Draft a reply</button>
      {messages.length === 0 && <p className="muted">No guest questions yet.</p>}
      {messages.map((message) => (
        <article key={message.id} className="thread">
          <header>
            <strong>{message.guestName}</strong>
            <span>{statusLabel(message.status)}</span>
          </header>
          <p>{message.body}</p>
          {message.evidence.length > 0 && (
            <ul className="evidence">
              {message.evidence.map((line) => <li key={line}>{line}</li>)}
            </ul>
          )}
          {message.status === 'DRAFT' ? (
            <>
              <label>
                Draft
                <textarea
                  rows={6}
                  value={drafts[message.id] ?? ''}
                  onChange={(event) => setDrafts((current) => ({ ...current, [message.id]: event.target.value }))}
                />
              </label>
              <button type="button" onClick={() => approve(message)}>Approve and mark sent</button>
            </>
          ) : (
            <p className="sent">{message.sentReply}</p>
          )}
        </article>
      ))}
    </section>
  )
}
