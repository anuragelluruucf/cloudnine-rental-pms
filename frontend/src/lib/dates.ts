export function nightOccupied(day: string, checkIn: string, checkOut: string) {
  return day >= checkIn && day < checkOut
}

export function monthCells(year: number, month: number): Array<string | null> {
  const first = new Date(Date.UTC(year, month - 1, 1))
  const leading = first.getUTCDay()
  const count = new Date(Date.UTC(year, month, 0)).getUTCDate()
  const cells: Array<string | null> = []
  for (let i = 0; i < leading; i += 1) cells.push(null)
  for (let day = 1; day <= count; day += 1) {
    cells.push(`${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`)
  }
  while (cells.length % 7 !== 0) cells.push(null)
  return cells
}

export function monthLabel(year: number, month: number) {
  return new Date(Date.UTC(year, month - 1, 1)).toLocaleDateString('en-US', {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  })
}

export function shortDate(iso: string) {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day)).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    timeZone: 'UTC',
  })
}

export function shiftMonth(year: number, month: number, delta: number) {
  const date = new Date(Date.UTC(year, month - 1 + delta, 1))
  return { year: date.getUTCFullYear(), month: date.getUTCMonth() + 1 }
}

export function channelLabel(channel: string) {
  switch (channel) {
    case 'AIRBNB':
      return 'Airbnb'
    case 'VRBO':
      return 'Vrbo'
    case 'BOOKING':
      return 'Booking.com'
    case 'DIRECT':
      return 'Direct'
    default:
      return channel
  }
}

export function statusLabel(status: string) {
  switch (status) {
    case 'CONFIRMED':
      return 'Confirmed'
    case 'BLOCKED':
      return 'Blocked'
    case 'CANCELLED':
      return 'Cancelled'
    case 'CONFLICT':
      return 'Conflict'
    case 'DRAFT':
      return 'Draft'
    case 'SENT':
      return 'Sent'
    default:
      return status
  }
}
