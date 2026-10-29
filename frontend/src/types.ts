export type Role = 'ADMIN' | 'MANAGER'
export type Channel = 'AIRBNB' | 'VRBO' | 'BOOKING' | 'DIRECT'
export type StayStatus = 'CONFIRMED' | 'BLOCKED' | 'CANCELLED' | 'CONFLICT'
export type MessageStatus = 'DRAFT' | 'SENT'

export interface User {
  id: string
  email: string
  name: string
  role: Role
}

export interface Session {
  token: string
  user: User
}

export interface Property {
  id: string
  name: string
  address: string
  description: string | null
  houseRules: string | null
  amenities: string | null
  checkInInfo: string | null
  checkOutInfo: string | null
  wifiNetwork: string | null
  wifiPassword: string | null
  faqNotes: string | null
  exportToken: string
  icalPath: string
  updatedAt: string
}

export interface PropertyInput {
  name: string
  address: string
  description: string
  houseRules: string
  amenities: string
  checkInInfo: string
  checkOutInfo: string
  wifiNetwork: string
  wifiPassword: string
  faqNotes: string
}

export interface Reservation {
  id: string
  propertyId: string
  propertyName: string
  channelSourceId: string | null
  channel: Channel
  guestName: string | null
  guestEmail: string | null
  checkIn: string
  checkOut: string
  status: StayStatus
  externalUid: string | null
  conflict: boolean
  notes: string | null
}

export interface ReservationInput {
  guestName: string
  guestEmail: string
  checkIn: string
  checkOut: string
  channel: Channel
  notes: string
  block: boolean
}

export interface ChannelSource {
  id: string
  propertyId: string
  channel: Channel
  importUrl: string | null
  lastSyncedAt: string | null
  lastSyncMessage: string | null
  enabled: boolean
}

export interface ImportReport {
  created: number
  updated: number
  conflicts: number
  cancelled: number
  message: string
}

export interface GuestMessage {
  id: string
  propertyId: string
  propertyName: string
  guestName: string
  body: string
  draftReply: string | null
  sentReply: string | null
  evidence: string[]
  status: MessageStatus
  createdAt: string
  sentAt: string | null
}

export interface StaySnippet {
  reservationId: string
  propertyId: string
  propertyName: string
  guestName: string | null
  checkIn: string
  checkOut: string
  channel: Channel
  status: StayStatus
}

export interface Dashboard {
  upcomingCheckIns: StaySnippet[]
  upcomingCheckOuts: StaySnippet[]
  occupancy: {
    from: string
    to: string
    bookedNights: number
    totalNights: number
    propertyCount: number
  }
}
