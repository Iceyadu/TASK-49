import { describe, it, expect } from 'vitest'
import {
  isValidEmail,
  isValidPassword,
  getPasswordStrength,
  isValidUsername,
  isValidUrl,
  isNotEmpty,
  isValidLength,
  isInRange,
  isPositiveInteger,
  createValidator
} from '@/utils/validators'

describe('isValidEmail', () => {
  it('should accept a standard email', () => {
    expect(isValidEmail('user@example.com')).toBe(true)
  })

  it('should accept email with dots in local part', () => {
    expect(isValidEmail('first.last@example.com')).toBe(true)
  })

  it('should accept email with plus sign', () => {
    expect(isValidEmail('user+tag@example.com')).toBe(true)
  })

  it('should accept email with subdomain', () => {
    expect(isValidEmail('user@mail.example.com')).toBe(true)
  })

  it('should reject email without @', () => {
    expect(isValidEmail('userexample.com')).toBe(false)
  })

  it('should reject email without domain', () => {
    expect(isValidEmail('user@')).toBe(false)
  })

  it('should reject email without TLD', () => {
    expect(isValidEmail('user@example')).toBe(false)
  })

  it('should reject email with spaces', () => {
    expect(isValidEmail('user @example.com')).toBe(false)
  })

  it('should reject empty string', () => {
    expect(isValidEmail('')).toBe(false)
  })

  it('should reject email with single-char TLD', () => {
    expect(isValidEmail('user@example.c')).toBe(false)
  })
})

describe('isValidPassword', () => {
  it('should accept a strong password meeting all criteria', () => {
    expect(isValidPassword('MyP@ssw0rd!')).toBe(true)
  })

  it('should accept exactly 8 characters with all requirements', () => {
    expect(isValidPassword('Aa1!xxxx')).toBe(true)
  })

  it('should reject password shorter than 8 characters', () => {
    expect(isValidPassword('Aa1!xx')).toBe(false)
  })

  it('should reject password without uppercase', () => {
    expect(isValidPassword('abcdefg1!')).toBe(false)
  })

  it('should reject password without lowercase', () => {
    expect(isValidPassword('ABCDEFG1!')).toBe(false)
  })

  it('should reject password without digit', () => {
    expect(isValidPassword('Abcdefgh!')).toBe(false)
  })

  it('should reject password without special character', () => {
    expect(isValidPassword('Abcdefg1')).toBe(false)
  })

  it('should reject empty string', () => {
    expect(isValidPassword('')).toBe(false)
  })

  it('should accept a very long valid password', () => {
    expect(isValidPassword('ThisIsAVeryLongP@ssw0rd123!')).toBe(true)
  })

  it('should accept password with various special characters', () => {
    expect(isValidPassword('Aa1#$%^&')).toBe(true)
    expect(isValidPassword('Aa1[]{};')).toBe(true)
  })
})

describe('getPasswordStrength', () => {
  it('should return Very Weak for empty password', () => {
    const result = getPasswordStrength('')
    expect(result.score).toBe(0)
    expect(result.label).toBe('Very Weak')
  })

  it('should return Very Strong for password meeting all checks', () => {
    const result = getPasswordStrength('MyP@ssw0rd!')
    expect(result.score).toBe(5)
    expect(result.label).toBe('Very Strong')
  })

  it('should return proper checks array', () => {
    const result = getPasswordStrength('abc')
    expect(result.checks).toHaveLength(5)
    expect(result.checks[0].label).toBe('At least 8 characters')
    expect(result.checks[0].passed).toBe(false)
    expect(result.checks[2].label).toBe('Contains lowercase letter')
    expect(result.checks[2].passed).toBe(true)
  })

  it('should return Weak for score of 2', () => {
    // lowercase + 8+ chars only
    const result = getPasswordStrength('abcdefghi')
    expect(result.score).toBe(2)
    expect(result.label).toBe('Weak')
  })

  it('should return Fair for score of 3', () => {
    // lowercase + uppercase + 8+ chars
    const result = getPasswordStrength('Abcdefghi')
    expect(result.score).toBe(3)
    expect(result.label).toBe('Fair')
  })

  it('should return Strong for score of 4', () => {
    // lowercase + uppercase + digit + 8+ chars
    const result = getPasswordStrength('Abcdefg1i')
    expect(result.score).toBe(4)
    expect(result.label).toBe('Strong')
  })
})

describe('isValidUsername', () => {
  it('should accept alphanumeric username', () => {
    expect(isValidUsername('johndoe')).toBe(true)
  })

  it('should accept username with underscores', () => {
    expect(isValidUsername('john_doe_42')).toBe(true)
  })

  it('should accept exactly 3 characters', () => {
    expect(isValidUsername('abc')).toBe(true)
  })

  it('should accept exactly 50 characters', () => {
    expect(isValidUsername('a'.repeat(50))).toBe(true)
  })

  it('should reject username shorter than 3 characters', () => {
    expect(isValidUsername('ab')).toBe(false)
  })

  it('should reject username longer than 50 characters', () => {
    expect(isValidUsername('a'.repeat(51))).toBe(false)
  })

  it('should reject username with spaces', () => {
    expect(isValidUsername('john doe')).toBe(false)
  })

  it('should reject username with special characters', () => {
    expect(isValidUsername('john@doe')).toBe(false)
    expect(isValidUsername('john-doe')).toBe(false)
  })

  it('should reject empty string', () => {
    expect(isValidUsername('')).toBe(false)
  })
})

describe('isValidUrl', () => {
  it('should accept https URLs', () => {
    expect(isValidUrl('https://example.com')).toBe(true)
  })

  it('should accept http URLs', () => {
    expect(isValidUrl('http://example.com')).toBe(true)
  })

  it('should accept URLs with paths', () => {
    expect(isValidUrl('https://example.com/path/to/resource')).toBe(true)
  })

  it('should accept URLs with query params', () => {
    expect(isValidUrl('https://example.com?q=search&page=1')).toBe(true)
  })

  it('should reject strings without protocol', () => {
    expect(isValidUrl('example.com')).toBe(false)
  })

  it('should reject empty string', () => {
    expect(isValidUrl('')).toBe(false)
  })

  it('should reject random text', () => {
    expect(isValidUrl('not a url')).toBe(false)
  })
})

describe('isNotEmpty', () => {
  it('should return true for non-empty string', () => {
    expect(isNotEmpty('hello')).toBe(true)
  })

  it('should return false for empty string', () => {
    expect(isNotEmpty('')).toBe(false)
  })

  it('should return false for whitespace-only string', () => {
    expect(isNotEmpty('   ')).toBe(false)
  })

  it('should return false for null', () => {
    expect(isNotEmpty(null)).toBe(false)
  })

  it('should return false for undefined', () => {
    expect(isNotEmpty(undefined)).toBe(false)
  })
})

describe('isValidLength', () => {
  it('should return true for string within range', () => {
    expect(isValidLength('hello', 1, 10)).toBe(true)
  })

  it('should return true at minimum boundary', () => {
    expect(isValidLength('ab', 2, 5)).toBe(true)
  })

  it('should return true at maximum boundary', () => {
    expect(isValidLength('abcde', 2, 5)).toBe(true)
  })

  it('should return false for too short string', () => {
    expect(isValidLength('a', 2, 5)).toBe(false)
  })

  it('should return false for too long string', () => {
    expect(isValidLength('abcdef', 2, 5)).toBe(false)
  })
})

describe('isInRange', () => {
  it('should return true for value within range', () => {
    expect(isInRange(5, 1, 10)).toBe(true)
  })

  it('should return true at minimum boundary', () => {
    expect(isInRange(1, 1, 10)).toBe(true)
  })

  it('should return true at maximum boundary', () => {
    expect(isInRange(10, 1, 10)).toBe(true)
  })

  it('should return false for value below range', () => {
    expect(isInRange(0, 1, 10)).toBe(false)
  })

  it('should return false for value above range', () => {
    expect(isInRange(11, 1, 10)).toBe(false)
  })
})

describe('isPositiveInteger', () => {
  it('should return true for positive integers', () => {
    expect(isPositiveInteger(1)).toBe(true)
    expect(isPositiveInteger(100)).toBe(true)
  })

  it('should return false for zero', () => {
    expect(isPositiveInteger(0)).toBe(false)
  })

  it('should return false for negative integers', () => {
    expect(isPositiveInteger(-1)).toBe(false)
  })

  it('should return false for floating point numbers', () => {
    expect(isPositiveInteger(1.5)).toBe(false)
  })

  it('should return false for NaN', () => {
    expect(isPositiveInteger(NaN)).toBe(false)
  })
})

describe('createValidator', () => {
  it('should return null when all rules pass', () => {
    const validate = createValidator([
      { test: (v: string) => v.length >= 3, message: 'Too short' },
      { test: (v: string) => v.length <= 10, message: 'Too long' }
    ])
    expect(validate('hello')).toBeNull()
  })

  it('should return the first failing rule message', () => {
    const validate = createValidator([
      { test: (v: string) => v.length >= 3, message: 'Too short' },
      { test: (v: string) => v.length <= 10, message: 'Too long' }
    ])
    expect(validate('ab')).toBe('Too short')
  })

  it('should return second rule message when first passes but second fails', () => {
    const validate = createValidator([
      { test: (v: string) => v.length >= 1, message: 'Required' },
      { test: (v: string) => /^[a-z]+$/.test(v), message: 'Must be lowercase letters only' }
    ])
    expect(validate('ABC')).toBe('Must be lowercase letters only')
  })

  it('should handle no rules', () => {
    const validate = createValidator([])
    expect(validate('anything')).toBeNull()
  })
})
