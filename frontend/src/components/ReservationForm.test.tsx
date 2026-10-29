import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ReservationForm } from './ReservationForm.tsx'

describe('ReservationForm', () => {
  it('shows the overlap error where a host will see it', () => {
    render(
      <ReservationForm
        onSubmit={() => undefined}
        error="Those dates overlap an existing stay. Check-in counts and check-out does not, so a guest can arrive the same day another guest leaves."
      />,
    )
    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('Those dates overlap an existing stay.')
    expect(alert).toHaveTextContent('Check-in counts')
  })

  it('stays quiet when the dates are free', () => {
    render(<ReservationForm onSubmit={() => undefined} />)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
