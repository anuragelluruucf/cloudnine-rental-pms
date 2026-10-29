import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AuthProvider } from '../auth.tsx'
import { LoginPage } from './LoginPage.tsx'

describe('LoginPage', () => {
  it('shows the sign-in form and the fake demo login', () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveValue('mara@cloudnine.test')
    expect(screen.getByText(/cloudnine-demo/)).toBeInTheDocument()
  })
})
