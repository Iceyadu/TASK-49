import { describe, it, expect } from 'vitest'
import {
  formatDate,
  formatDateTime,
  formatTime,
  formatRelativeTime,
  formatCurrency,
  formatNumber,
  formatPercentage,
  formatFileSize,
  formatDuration
} from '@/utils/formatters'

describe('formatDate', () => {
  it('should format a valid ISO date string', () => {
    const result = formatDate('2026-03-15T10:30:00Z')
    expect(result).toMatch(/Mar/)
    expect(result).toMatch(/15/)
    expect(result).toMatch(/2026/)
  })

  it('should return dash for null', () => {
    expect(formatDate(null)).toBe('-')
  })

  it('should return dash for undefined', () => {
    expect(formatDate(undefined)).toBe('-')
  })

  it('should return dash for empty string', () => {
    expect(formatDate('')).toBe('-')
  })
})

describe('formatDateTime', () => {
  it('should format a valid ISO date string with time', () => {
    const result = formatDateTime('2026-06-01T14:30:00Z')
    expect(result).toMatch(/Jun/)
    expect(result).toMatch(/1/)
    expect(result).toMatch(/2026/)
  })

  it('should return dash for null', () => {
    expect(formatDateTime(null)).toBe('-')
  })

  it('should return dash for undefined', () => {
    expect(formatDateTime(undefined)).toBe('-')
  })
})

describe('formatTime', () => {
  it('should format a valid ISO date string to time only', () => {
    const result = formatTime('2026-01-01T08:05:00Z')
    // Result depends on locale and timezone, but should contain digits and colon
    expect(result).toMatch(/\d{1,2}:\d{2}/)
  })

  it('should return dash for null', () => {
    expect(formatTime(null)).toBe('-')
  })

  it('should return dash for undefined', () => {
    expect(formatTime(undefined)).toBe('-')
  })

  it('should return dash for empty string', () => {
    expect(formatTime('')).toBe('-')
  })
})

describe('formatRelativeTime', () => {
  it('should return dash for null', () => {
    expect(formatRelativeTime(null)).toBe('-')
  })

  it('should return dash for undefined', () => {
    expect(formatRelativeTime(undefined)).toBe('-')
  })

  it('should return "just now" for a very recent time', () => {
    const now = new Date().toISOString()
    expect(formatRelativeTime(now)).toBe('just now')
  })

  it('should return minutes ago for a time within the last hour', () => {
    const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000).toISOString()
    expect(formatRelativeTime(fiveMinutesAgo)).toBe('5 minutes ago')
  })

  it('should return singular minute for exactly 1 minute ago', () => {
    const oneMinuteAgo = new Date(Date.now() - 61 * 1000).toISOString()
    expect(formatRelativeTime(oneMinuteAgo)).toBe('1 minute ago')
  })

  it('should return hours ago for a time within the last day', () => {
    const threeHoursAgo = new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString()
    expect(formatRelativeTime(threeHoursAgo)).toBe('3 hours ago')
  })

  it('should return singular hour for exactly 1 hour ago', () => {
    const oneHourAgo = new Date(Date.now() - 61 * 60 * 1000).toISOString()
    expect(formatRelativeTime(oneHourAgo)).toBe('1 hour ago')
  })

  it('should return days ago for a time within the last month', () => {
    const fiveDaysAgo = new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString()
    expect(formatRelativeTime(fiveDaysAgo)).toBe('5 days ago')
  })

  it('should fall back to formatted date for times older than 30 days', () => {
    const sixtyDaysAgo = new Date(Date.now() - 60 * 24 * 60 * 60 * 1000).toISOString()
    const result = formatRelativeTime(sixtyDaysAgo)
    // Should not contain "ago", should be a formatted date
    expect(result).not.toContain('ago')
    expect(result).toMatch(/\d{4}/)
  })
})

describe('formatCurrency', () => {
  it('should format USD by default', () => {
    expect(formatCurrency(1234.56)).toBe('$1,234.56')
  })

  it('should format zero', () => {
    expect(formatCurrency(0)).toBe('$0.00')
  })

  it('should format negative amounts', () => {
    const result = formatCurrency(-50)
    expect(result).toContain('50.00')
  })

  it('should return dash for null', () => {
    expect(formatCurrency(null)).toBe('-')
  })

  it('should return dash for undefined', () => {
    expect(formatCurrency(undefined)).toBe('-')
  })

  it('should format with specified currency', () => {
    const result = formatCurrency(100, 'EUR', 'en-US')
    expect(result).toContain('100.00')
  })

  it('should add two decimal places to whole numbers', () => {
    expect(formatCurrency(100)).toBe('$100.00')
  })
})

describe('formatNumber', () => {
  it('should format numbers with thousand separators', () => {
    expect(formatNumber(1234567)).toBe('1,234,567')
  })

  it('should format zero', () => {
    expect(formatNumber(0)).toBe('0')
  })

  it('should return dash for null', () => {
    expect(formatNumber(null)).toBe('-')
  })

  it('should return dash for undefined', () => {
    expect(formatNumber(undefined)).toBe('-')
  })
})

describe('formatPercentage', () => {
  it('should format with default 1 decimal place', () => {
    expect(formatPercentage(85.5)).toBe('85.5%')
  })

  it('should format with custom decimal places', () => {
    expect(formatPercentage(85.567, 2)).toBe('85.57%')
  })

  it('should format integer percentages', () => {
    expect(formatPercentage(100, 0)).toBe('100%')
  })

  it('should return dash for null', () => {
    expect(formatPercentage(null)).toBe('-')
  })

  it('should return dash for undefined', () => {
    expect(formatPercentage(undefined)).toBe('-')
  })
})

describe('formatFileSize', () => {
  it('should format bytes', () => {
    expect(formatFileSize(500)).toBe('500 B')
  })

  it('should format kilobytes', () => {
    expect(formatFileSize(1024)).toBe('1.0 KB')
  })

  it('should format megabytes', () => {
    expect(formatFileSize(1048576)).toBe('1.0 MB')
  })

  it('should format gigabytes', () => {
    expect(formatFileSize(1073741824)).toBe('1.0 GB')
  })

  it('should format intermediate values', () => {
    expect(formatFileSize(1536)).toBe('1.5 KB')
  })

  it('should return dash for null', () => {
    expect(formatFileSize(null)).toBe('-')
  })

  it('should return dash for undefined', () => {
    expect(formatFileSize(undefined)).toBe('-')
  })

  it('should handle zero bytes', () => {
    expect(formatFileSize(0)).toBe('0 B')
  })
})

describe('formatDuration', () => {
  it('should format seconds to mm:ss', () => {
    expect(formatDuration(90)).toBe('01:30')
  })

  it('should format zero seconds', () => {
    expect(formatDuration(0)).toBe('00:00')
  })

  it('should format hours as hh:mm:ss', () => {
    expect(formatDuration(3661)).toBe('01:01:01')
  })

  it('should format exactly one hour', () => {
    expect(formatDuration(3600)).toBe('01:00:00')
  })

  it('should pad single-digit minutes and seconds', () => {
    expect(formatDuration(65)).toBe('01:05')
  })

  it('should return dash for null', () => {
    expect(formatDuration(null)).toBe('-')
  })

  it('should return dash for undefined', () => {
    expect(formatDuration(undefined)).toBe('-')
  })

  it('should handle large durations', () => {
    expect(formatDuration(36000)).toBe('10:00:00')
  })
})
