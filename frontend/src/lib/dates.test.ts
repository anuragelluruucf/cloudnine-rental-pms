import { describe, expect, it } from 'vitest'
import { monthCells, nightOccupied } from './dates.ts'

describe('nightOccupied', () => {
  it('counts the check-in night and not the check-out morning', () => {
    expect(nightOccupied('2026-11-06', '2026-11-06', '2026-11-09')).toBe(true)
    expect(nightOccupied('2026-11-08', '2026-11-06', '2026-11-09')).toBe(true)
    expect(nightOccupied('2026-11-09', '2026-11-06', '2026-11-09')).toBe(false)
  })

  it('lets a later stay start on the previous checkout day', () => {
    expect(nightOccupied('2026-11-09', '2026-11-09', '2026-11-12')).toBe(true)
  })
})

describe('monthCells', () => {
  it('pads the grid so each row is a week', () => {
    const cells = monthCells(2026, 11)
    expect(cells.length % 7).toBe(0)
    expect(cells.filter(Boolean)).toHaveLength(30)
    expect(cells.find((cell) => cell === '2026-11-01')).toBe('2026-11-01')
  })
})
