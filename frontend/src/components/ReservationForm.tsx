import { useState } from 'react'
import type { FormEvent } from 'react'
import type { Channel, ReservationInput } from '../types.ts'

const empty: ReservationInput = {
  guestName: '',
  guestEmail: '',
  checkIn: '',
  checkOut: '',
  channel: 'DIRECT',
  notes: '',
  block: false,
}

export function ReservationForm({
  onSubmit,
  error,
  submitting,
}: {
  onSubmit: (input: ReservationInput) => Promise<void> | void
  error?: string | null
  submitting?: boolean
}) {
  const [form, setForm] = useState<ReservationInput>(empty)

  function update<K extends keyof ReservationInput>(key: K, value: ReservationInput[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    try {
      await onSubmit(form)
      setForm(empty)
    } catch {
      // The parent shows the error. Keep the dates so they can be changed.
    }
  }

  return (
    <form className="stack" onSubmit={handleSubmit}>
      {error && (
        <div className="banner danger" role="alert">
          <strong>Those dates overlap an existing stay.</strong>
          <p>{error}</p>
        </div>
      )}
      <label>
        Guest name
        <input
          value={form.guestName}
          onChange={(event) => update('guestName', event.target.value)}
          placeholder={form.block ? 'Optional for a block' : 'Who is arriving?'}
          required={!form.block}
        />
      </label>
      <div className="split">
        <label>
          Check-in
          <input type="date" value={form.checkIn} required onChange={(event) => update('checkIn', event.target.value)} />
        </label>
        <label>
          Check-out
          <input type="date" value={form.checkOut} required onChange={(event) => update('checkOut', event.target.value)} />
        </label>
      </div>
      <label>
        Channel
        <select value={form.channel} onChange={(event) => update('channel', event.target.value as Channel)}>
          <option value="DIRECT">Direct</option>
          <option value="AIRBNB">Airbnb</option>
          <option value="VRBO">Vrbo</option>
          <option value="BOOKING">Booking.com</option>
        </select>
      </label>
      <label className="check">
        <input type="checkbox" checked={form.block} onChange={(event) => update('block', event.target.checked)} />
        Block these dates (no guest)
      </label>
      <label>
        Notes
        <textarea value={form.notes} rows={2} onChange={(event) => update('notes', event.target.value)} />
      </label>
      <p className="hint">Check-in counts. Check-out does not, so a guest can arrive the day another leaves. The sample feed blocks 6–9 Nov and 18–22 Nov 2026.</p>
      <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save stay'}</button>
    </form>
  )
}
