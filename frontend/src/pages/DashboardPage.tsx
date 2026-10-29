import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, api } from '../api.ts'
import { channelLabel, shortDate } from '../lib/dates.ts'
import type { Dashboard, StaySnippet } from '../types.ts'

export function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api<Dashboard>('/api/dashboard')
      .then(setData)
      .catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Could not load today.'))
  }, [])

  if (error) return <p className="banner danger" role="alert">{error}</p>
  if (!data) return <p className="muted">Loading today…</p>

  const { occupancy } = data
  const percent = occupancy.totalNights === 0 ? 0 : Math.round((occupancy.bookedNights / occupancy.totalNights) * 100)

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <p className="eyebrow">Next 30 nights</p>
          <h1>Today</h1>
        </div>
      </header>
      <section className="stats">
        <article>
          <p>Booked nights</p>
          <strong>{occupancy.bookedNights}</strong>
          <span>of {occupancy.totalNights || 0} across {occupancy.propertyCount} {occupancy.propertyCount === 1 ? 'listing' : 'listings'}</span>
        </article>
        <article>
          <p>Occupancy</p>
          <strong>{percent}%</strong>
          <span>{shortDate(occupancy.from)} – {shortDate(occupancy.to)}</span>
        </article>
        <article>
          <p>Arrivals</p>
          <strong>{data.upcomingCheckIns.length}</strong>
          <span>in the next two weeks</span>
        </article>
      </section>
      <div className="two">
        <StayList title="Upcoming check-ins" empty="No arrivals in the next two weeks." stays={data.upcomingCheckIns} dateKey="checkIn" />
        <StayList title="Upcoming check-outs" empty="No departures in the next two weeks." stays={data.upcomingCheckOuts} dateKey="checkOut" />
      </div>
    </div>
  )
}

function StayList({
  title,
  empty,
  stays,
  dateKey,
}: {
  title: string
  empty: string
  stays: StaySnippet[]
  dateKey: 'checkIn' | 'checkOut'
}) {
  return (
    <section className="card">
      <h2>{title}</h2>
      {stays.length === 0 && <p className="muted">{empty}</p>}
      <ul className="stay-list">
        {stays.map((stay) => (
          <li key={`${stay.reservationId}-${dateKey}`}>
            <div>
              <Link to={`/properties/${stay.propertyId}`}>{stay.propertyName}</Link>
              <p>{stay.guestName || 'Guest'} · {channelLabel(stay.channel)}</p>
            </div>
            <time>{shortDate(stay[dateKey])}</time>
          </li>
        ))}
      </ul>
    </section>
  )
}
