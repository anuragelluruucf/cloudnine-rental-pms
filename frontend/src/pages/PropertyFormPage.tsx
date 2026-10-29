import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError, api } from '../api.ts'
import { PropertyFields, emptyProperty } from '../components/PropertyFields.tsx'
import type { Property, PropertyInput } from '../types.ts'

export function PropertyFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState<PropertyInput>(emptyProperty)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(Boolean(id))

  useEffect(() => {
    if (!id) return
    api<Property>(`/api/properties/${id}`)
      .then((property) => {
        setForm({
          name: property.name,
          address: property.address,
          description: property.description ?? '',
          houseRules: property.houseRules ?? '',
          amenities: property.amenities ?? '',
          checkInInfo: property.checkInInfo ?? '',
          checkOutInfo: property.checkOutInfo ?? '',
          wifiNetwork: property.wifiNetwork ?? '',
          wifiPassword: property.wifiPassword ?? '',
          faqNotes: property.faqNotes ?? '',
        })
      })
      .catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Could not load the listing.'))
      .finally(() => setLoading(false))
  }, [id])

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const saved = await api<Property>(id ? `/api/properties/${id}` : '/api/properties', {
        method: id ? 'PUT' : 'POST',
        body: JSON.stringify(form),
      })
      navigate(`/properties/${saved.id}`)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Could not save the listing.')
    }
  }

  return (
    <div className="page narrow">
      <header className="page-head">
        <div>
          <p className="eyebrow">{id ? 'Edit listing' : 'New listing'}</p>
          <h1>{id ? 'Update the notes guests ask about' : 'Add a listing'}</h1>
        </div>
        <Link to={id ? `/properties/${id}` : '/properties'}>Back</Link>
      </header>
      {loading && <p className="muted">Loading listing…</p>}
      {!loading && (
        <form className="card stack" onSubmit={onSubmit}>
          {error && <div className="banner danger" role="alert">{error}</div>}
          <PropertyFields value={form} onChange={setForm} />
          <button type="submit">Save listing</button>
        </form>
      )}
    </div>
  )
}
