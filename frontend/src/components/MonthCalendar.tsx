import type { Reservation } from '../types.ts'
import { channelLabel, monthCells, monthLabel, nightOccupied } from '../lib/dates.ts'

export function MonthCalendar({
  year,
  month,
  reservations,
  onShift,
}: {
  year: number
  month: number
  reservations: Reservation[]
  onShift: (delta: number) => void
}) {
  const cells = monthCells(year, month)
  const visible = reservations.filter((stay) => stay.status !== 'CANCELLED')

  return (
    <section className="calendar">
      <header className="calendar-head">
        <button type="button" className="ghost" onClick={() => onShift(-1)} aria-label="Previous month">←</button>
        <h2>{monthLabel(year, month)}</h2>
        <button type="button" className="ghost" onClick={() => onShift(1)} aria-label="Next month">→</button>
      </header>
      <div className="weekdays">
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day) => <span key={day}>{day}</span>)}
      </div>
      <div className="month">
        {cells.map((day, index) => {
          if (!day) return <div key={`empty-${index}`} className="day empty" />
          const stays = visible.filter((stay) => nightOccupied(day, stay.checkIn, stay.checkOut))
          const conflict = stays.some((stay) => stay.conflict || stay.status === 'CONFLICT')
          return (
            <div key={day} className={conflict ? 'day conflict' : 'day'}>
              <span className="day-number">{Number(day.slice(-2))}</span>
              {stays.map((stay) => (
                <span key={stay.id} className={`pill channel-${stay.channel.toLowerCase()} ${stay.conflict ? 'pill-conflict' : ''}`}>
                  {stay.guestName || channelLabel(stay.channel)}
                </span>
              ))}
            </div>
          )
        })}
      </div>
    </section>
  )
}
