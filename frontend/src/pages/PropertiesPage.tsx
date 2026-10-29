import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, api } from '../api.ts'
import type { Property } from '../types.ts'

export function PropertiesPage() {
  const [properties, setProperties] = useState<Property[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api<Property[]>('/api/properties')
      .then(setProperties)
      .catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Could not load listings.'))
  }, [])

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <p className="eyebrow">Places you host</p>
          <h1>Listings</h1>
        </div>
        <Link className="button" to="/properties/new">Add a listing</Link>
      </header>
      {error && <p className="banner danger" role="alert">{error}</p>}
      {properties && properties.length === 0 && (
        <div className="card empty-card">
          <h2>No listings yet</h2>
          <p>Add the first cabin, apartment, or studio. House rules and wifi notes are what the guest drafts are drawn from.</p>
        </div>
      )}
      <div className="property-grid">
        {properties?.map((property) => (
          <Link key={property.id} className="card property-card" to={`/properties/${property.id}`}>
            <h2>{property.name}</h2>
            <p>{property.address}</p>
            <span>{property.wifiNetwork ? `Wifi ${property.wifiNetwork}` : 'Wifi not listed yet'}</span>
          </Link>
        ))}
      </div>
      {!properties && !error && <p className="muted">Loading listings…</p>}
    </div>
  )
}
