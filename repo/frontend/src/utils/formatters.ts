/**
 * Format an ISO date string to a localized date string.
 */
export function formatDate(isoString: string | null | undefined, locale = 'en-US'): string {
  if (!isoString) return '-'
  try {
    return new Date(isoString).toLocaleDateString(locale, {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    })
  } catch {
    return isoString
  }
}

/**
 * Format an ISO date string to a localized date and time string.
 */
export function formatDateTime(isoString: string | null | undefined, locale = 'en-US'): string {
  if (!isoString) return '-'
  try {
    return new Date(isoString).toLocaleString(locale, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return isoString
  }
}

/**
 * Format an ISO date string to a localized time string.
 */
export function formatTime(isoString: string | null | undefined, locale = 'en-US'): string {
  if (!isoString) return '-'
  try {
    return new Date(isoString).toLocaleTimeString(locale, {
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return isoString
  }
}

/**
 * Format a number as a relative time string (e.g., "2 hours ago").
 */
export function formatRelativeTime(isoString: string | null | undefined): string {
  if (!isoString) return '-'
  try {
    const date = new Date(isoString)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffSeconds = Math.floor(diffMs / 1000)
    const diffMinutes = Math.floor(diffSeconds / 60)
    const diffHours = Math.floor(diffMinutes / 60)
    const diffDays = Math.floor(diffHours / 24)

    if (diffSeconds < 60) return 'just now'
    if (diffMinutes < 60) return `${diffMinutes} minute${diffMinutes !== 1 ? 's' : ''} ago`
    if (diffHours < 24) return `${diffHours} hour${diffHours !== 1 ? 's' : ''} ago`
    if (diffDays < 30) return `${diffDays} day${diffDays !== 1 ? 's' : ''} ago`
    return formatDate(isoString)
  } catch {
    return isoString
  }
}

/**
 * Format a number as currency.
 */
export function formatCurrency(amount: number | null | undefined, currency = 'USD', locale = 'en-US'): string {
  if (amount === null || amount === undefined) return '-'
  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount)
  } catch {
    return `$${amount.toFixed(2)}`
  }
}

/**
 * Format a number with separators.
 */
export function formatNumber(value: number | null | undefined, locale = 'en-US'): string {
  if (value === null || value === undefined) return '-'
  return new Intl.NumberFormat(locale).format(value)
}

/**
 * Format a percentage value.
 */
export function formatPercentage(value: number | null | undefined, decimals = 1): string {
  if (value === null || value === undefined) return '-'
  return `${value.toFixed(decimals)}%`
}

/**
 * Format file size in bytes to a human-readable string.
 */
export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = bytes
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return `${size.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

/**
 * Format seconds to mm:ss or hh:mm:ss.
 */
export function formatDuration(totalSeconds: number | null | undefined): string {
  if (totalSeconds === null || totalSeconds === undefined) return '-'
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = Math.floor(totalSeconds % 60)

  const mm = String(minutes).padStart(2, '0')
  const ss = String(seconds).padStart(2, '0')

  if (hours > 0) {
    const hh = String(hours).padStart(2, '0')
    return `${hh}:${mm}:${ss}`
  }
  return `${mm}:${ss}`
}
