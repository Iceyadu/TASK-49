import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { useCountdown } from '@/composables/useCountdown'

// Mock onUnmounted since we are not inside a component lifecycle
vi.mock('vue', async () => {
  const actual = await vi.importActual<typeof import('vue')>('vue')
  return {
    ...actual,
    onUnmounted: vi.fn()
  }
})

describe('useCountdown', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('should initialize with the given seconds', () => {
    const { remainingSeconds, isRunning, isPaused } = useCountdown(120)
    expect(remainingSeconds.value).toBe(120)
    expect(isRunning.value).toBe(false)
    expect(isPaused.value).toBe(false)
  })

  it('should count down each second when started', () => {
    const { remainingSeconds, start } = useCountdown(60)
    start()

    vi.advanceTimersByTime(1000)
    expect(remainingSeconds.value).toBe(59)

    vi.advanceTimersByTime(4000)
    expect(remainingSeconds.value).toBe(55)
  })

  it('should format display as mm:ss', () => {
    const { formatted } = useCountdown(125) // 2 min 5 sec
    expect(formatted.value).toBe('02:05')
  })

  it('should format single-digit seconds with padding', () => {
    const { formatted } = useCountdown(61)
    expect(formatted.value).toBe('01:01')
  })

  it('should format zero correctly', () => {
    const { formatted } = useCountdown(0)
    expect(formatted.value).toBe('00:00')
  })

  it('should compute minutes and seconds separately', () => {
    const { minutes, seconds } = useCountdown(90)
    expect(minutes.value).toBe(1)
    expect(seconds.value).toBe(30)
  })

  it('should pause the countdown', () => {
    const { remainingSeconds, start, pause, isPaused } = useCountdown(60)
    start()

    vi.advanceTimersByTime(3000)
    expect(remainingSeconds.value).toBe(57)

    pause()
    expect(isPaused.value).toBe(true)

    vi.advanceTimersByTime(5000)
    expect(remainingSeconds.value).toBe(57) // should not have changed
  })

  it('should resume from paused state', () => {
    const { remainingSeconds, start, pause, resume, isPaused } = useCountdown(60)
    start()

    vi.advanceTimersByTime(3000)
    pause()
    expect(remainingSeconds.value).toBe(57)

    resume()
    expect(isPaused.value).toBe(false)

    vi.advanceTimersByTime(2000)
    expect(remainingSeconds.value).toBe(55)
  })

  it('should not resume if not paused', () => {
    const { remainingSeconds, start, resume } = useCountdown(60)
    start()
    vi.advanceTimersByTime(2000)
    resume() // no-op since not paused
    vi.advanceTimersByTime(1000)
    expect(remainingSeconds.value).toBe(57)
  })

  it('should detect expired state when reaching zero', () => {
    const { isExpired, start } = useCountdown(3)
    expect(isExpired.value).toBe(false)

    start()
    vi.advanceTimersByTime(3000)
    expect(isExpired.value).toBe(true)
  })

  it('should call onExpire callback when time runs out', () => {
    const expireFn = vi.fn()
    const { start } = useCountdown(2, expireFn)

    start()
    vi.advanceTimersByTime(2000)
    // At 2s, remaining becomes 0. Next tick will trigger onExpire.
    vi.advanceTimersByTime(1000)
    expect(expireFn).toHaveBeenCalledTimes(1)
  })

  it('should stop running after expiration', () => {
    const { isRunning, start } = useCountdown(1)
    start()
    expect(isRunning.value).toBe(true)

    vi.advanceTimersByTime(1000)
    // After reaching 0, the next interval tick stops the countdown
    vi.advanceTimersByTime(1000)
    expect(isRunning.value).toBe(false)
  })

  it('should stop the countdown', () => {
    const { remainingSeconds, start, stop, isRunning } = useCountdown(60)
    start()
    vi.advanceTimersByTime(2000)
    stop()
    expect(isRunning.value).toBe(false)

    vi.advanceTimersByTime(5000)
    expect(remainingSeconds.value).toBe(58) // unchanged after stop
  })

  it('should reset to original seconds', () => {
    const { remainingSeconds, start, reset, isRunning } = useCountdown(60)
    start()
    vi.advanceTimersByTime(10000)
    expect(remainingSeconds.value).toBe(50)

    reset()
    expect(remainingSeconds.value).toBe(60)
    expect(isRunning.value).toBe(false)
  })

  it('should reset to a new value when provided', () => {
    const { remainingSeconds, reset } = useCountdown(60)
    reset(30)
    expect(remainingSeconds.value).toBe(30)
  })

  it('should compute percentage remaining', () => {
    const { percentage, start } = useCountdown(100)
    expect(percentage.value).toBe(100)

    start()
    vi.advanceTimersByTime(50000)
    expect(percentage.value).toBe(50)
  })

  it('should handle percentage when initialSeconds is zero', () => {
    const { percentage } = useCountdown(0)
    expect(percentage.value).toBe(0)
  })

  it('should set remaining seconds via setRemaining', () => {
    const { remainingSeconds, setRemaining } = useCountdown(60)
    setRemaining(30)
    expect(remainingSeconds.value).toBe(30)
  })

  it('should clamp setRemaining to zero', () => {
    const { remainingSeconds, setRemaining } = useCountdown(60)
    setRemaining(-10)
    expect(remainingSeconds.value).toBe(0)
  })

  it('should not start twice if already running and not paused', () => {
    const { remainingSeconds, start } = useCountdown(60)
    start()
    start() // second call should be a no-op

    vi.advanceTimersByTime(1000)
    expect(remainingSeconds.value).toBe(59) // only 1 interval active
  })
})
