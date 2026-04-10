/**
 * Validate an email address format.
 */
export function isValidEmail(email: string): boolean {
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  return emailRegex.test(email)
}

/**
 * Validate password strength.
 * Requirements: min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit, 1 special char.
 */
export function isValidPassword(password: string): boolean {
  if (password.length < 8) return false
  if (!/[A-Z]/.test(password)) return false
  if (!/[a-z]/.test(password)) return false
  if (!/[0-9]/.test(password)) return false
  if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) return false
  return true
}

/**
 * Get password strength details for UI feedback.
 */
export function getPasswordStrength(password: string): {
  score: number
  label: string
  checks: { label: string; passed: boolean }[]
} {
  const checks = [
    { label: 'At least 8 characters', passed: password.length >= 8 },
    { label: 'Contains uppercase letter', passed: /[A-Z]/.test(password) },
    { label: 'Contains lowercase letter', passed: /[a-z]/.test(password) },
    { label: 'Contains a digit', passed: /[0-9]/.test(password) },
    { label: 'Contains special character', passed: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password) }
  ]

  const score = checks.filter(c => c.passed).length

  let label: string
  if (score <= 1) label = 'Very Weak'
  else if (score === 2) label = 'Weak'
  else if (score === 3) label = 'Fair'
  else if (score === 4) label = 'Strong'
  else label = 'Very Strong'

  return { score, label, checks }
}

/**
 * Validate a username (alphanumeric, underscores, 3-50 chars).
 */
export function isValidUsername(username: string): boolean {
  const usernameRegex = /^[a-zA-Z0-9_]{3,50}$/
  return usernameRegex.test(username)
}

/**
 * Validate a URL format.
 */
export function isValidUrl(url: string): boolean {
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

/**
 * Validate that a string is not empty after trimming.
 */
export function isNotEmpty(value: string | null | undefined): boolean {
  return value !== null && value !== undefined && value.trim().length > 0
}

/**
 * Validate string length within a range.
 */
export function isValidLength(value: string, min: number, max: number): boolean {
  return value.length >= min && value.length <= max
}

/**
 * Validate that a number is within a range.
 */
export function isInRange(value: number, min: number, max: number): boolean {
  return value >= min && value <= max
}

/**
 * Validate that a value is a positive integer.
 */
export function isPositiveInteger(value: number): boolean {
  return Number.isInteger(value) && value > 0
}

/**
 * Create a field validator that returns an error message or null.
 */
export function createValidator(
  rules: { test: (value: any) => boolean; message: string }[]
): (value: any) => string | null {
  return (value: any) => {
    for (const rule of rules) {
      if (!rule.test(value)) {
        return rule.message
      }
    }
    return null
  }
}
