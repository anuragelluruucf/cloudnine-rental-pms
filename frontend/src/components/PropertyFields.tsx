import type { PropertyInput } from '../types.ts'

export const emptyProperty: PropertyInput = {
  name: '',
  address: '',
  description: '',
  houseRules: '',
  amenities: '',
  checkInInfo: '',
  checkOutInfo: '',
  wifiNetwork: '',
  wifiPassword: '',
  faqNotes: '',
}

export function PropertyFields({
  value,
  onChange,
}: {
  value: PropertyInput
  onChange: (next: PropertyInput) => void
}) {
  function set<K extends keyof PropertyInput>(key: K, next: string) {
    onChange({ ...value, [key]: next })
  }

  return (
    <div className="stack">
      <label>
        Name
        <input value={value.name} required onChange={(event) => set('name', event.target.value)} placeholder="Cedar & Pine Cabin" />
      </label>
      <label>
        Address
        <input value={value.address} required onChange={(event) => set('address', event.target.value)} placeholder="18 Birch Lane, Bend" />
      </label>
      <label>
        Description
        <textarea rows={3} value={value.description} onChange={(event) => set('description', event.target.value)} />
      </label>
      <label>
        House rules
        <textarea rows={3} value={value.houseRules} onChange={(event) => set('houseRules', event.target.value)} placeholder="Quiet hours, smoking, pets" />
      </label>
      <label>
        Amenities
        <textarea rows={2} value={value.amenities} onChange={(event) => set('amenities', event.target.value)} />
      </label>
      <div className="split">
        <label>
          Check-in
          <textarea rows={3} value={value.checkInInfo} onChange={(event) => set('checkInInfo', event.target.value)} />
        </label>
        <label>
          Check-out
          <textarea rows={3} value={value.checkOutInfo} onChange={(event) => set('checkOutInfo', event.target.value)} />
        </label>
      </div>
      <div className="split">
        <label>
          Wifi network
          <input value={value.wifiNetwork} onChange={(event) => set('wifiNetwork', event.target.value)} />
        </label>
        <label>
          Wifi password
          <input value={value.wifiPassword} onChange={(event) => set('wifiPassword', event.target.value)} placeholder="the code you give guests" />
        </label>
      </div>
      <label>
        FAQ notes
        <textarea rows={3} value={value.faqNotes} onChange={(event) => set('faqNotes', event.target.value)} placeholder="Grill, parking, trash day" />
      </label>
    </div>
  )
}
