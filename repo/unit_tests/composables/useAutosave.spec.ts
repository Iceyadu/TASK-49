import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { useAutosave } from '@/composables/useAutosave'

// Mock onUnmounted since we are not inside a component lifecycle
vi.mock('vue', async () => {
  const actual = await vi.importActual<typeof import('vue')>('vue')
  return {
    ...actual,
    onUnmounted: vi.fn()
  }
})

describe('useAutosave', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('should default to 15s interval', () => {
    const saveFn = vi.fn().mockResolvedValue(undefined)
    const { start, markDirty } = useAutosave(saveFn)

    markDirty({ text: 'hello' })
    start()

    expect(saveFn).not.toHaveBeenCalled()

    vi.advanceTimersByTime(15000)
    expect(saveFn).toHaveBeenCalledTimes(1)
  })

  it('should accept a custom interval', () => {
    const saveFn = vi.fn().mockResolvedValue(undefined)
    const { start, markDirty } = useAutosave(saveFn, 5000)

    markDirty('data')
    start()

    vi.advanceTimersByTime(5000)
    expect(saveFn).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(5000)
    // save should not fire again because isDirty was set to false after first save
  })

  it('should track dirty state', () => {
    const saveFn = vi.fn().mockResolvedValue(undefined)
    const { isDirty, markDirty } = useAutosave(saveFn)

    expect(isDirty.value).toBe(false)

    markDirty({ content: 'changed' })
    expect(isDirty.value).toBe(true)
  })

  it('should set isDirty to false after successful save', async () => {
    const saveFn = vi.fn().mockResolvedValue(undefined)
    const { isDirty, markDirty, save } = useAutosave(saveFn)

    markDirty('some data')
    expect(isDirty.value).toBe(true)

    await save()
    expect(isDirty.value).toBe(false)
  })

  it('should invoke the save callback with the current data', async () => {
    const saveFn = vi.fn().mockResolvedValue(undefined)
    const { markDirty, save } = useAutosave(saveFn)

    markDirty({ id: 1, text: 'essay content' })
    await save()

    expect(saveFn).toHaveBeenCalledWith({ id: 1, text: 'essay content' })
  })

  it('should not save when not dirty', async () => {
    const saveFn = vi.fn().mockResolvedValue(undefined)
    const { save } = useAutosave(saveFn)

    await save()
    expect(saveFn).not.toHaveBeenCalled()
  })

  it('should not save when already saving', async () => {
    let resolveFirst: () => void
    const saveFn = vi.fn().mockImplementation(() => {
      return new Promise<void>(resolve => {
        resolveFirst = resolve
      })
    })
    const { markDirty, save, isSaving } = useAutosave(saveFn)

    markDirty('data')
    const firstSave = save()
    expect(isSaving.value).toBe(true)

    // Attempt a second save while the first is in progress
    markDirty('data2')
    await save()
    // saveFn should have been called only once (the second call is a no-op)
    expect(saveFn).toHaveBeenCalledTimes(1)

    resolveFirst!()
    await firstSave
    expect(isSaving.value).toBe(false)
  })

  it('should set lastSavedAt after a successful save', async () => {
    const saveFn = vi.fn().mockResolvedValue(undefined)
    const { lastSavedAt, markDirty, save } = useAutosave(saveFn)

    expect(lastSavedAt.value).toBeNull()

    markDirty('data')
    await save()

    expect(lastSavedAt.value).toBeInstanceOf(Date)
  })

  it('should set error on save failure', async () => {
    const saveFn = vi.fn().mockRejectedValue(new Error('Network error'))
    const { error, markDirty, save } = useAutosave(saveFn)

    markDirty('data')
    await save()

    expect(error.value).toBe('Network error')
  })

  it('should keep isDirty true when save fails', async () => {
    const saveFn = vi.fn().mockRejectedValue(new Error('fail'))
    const { isDirty, markDirty, save } = useAutosave(saveFn)

    markDirty('data')
    await save()

    expect(isDirty.value).toBe(true)
  })

  it('should clear error when markDirty is called', async () => {
    const saveFn = vi.fn().mockRejectedValue(new Error('fail'))
    const { error, markDirty, save } = useAutosave(saveFn)

    markDirty('data')
    await save()
    expect(error.value).toBe('fail')

    markDirty('new data')
    expect(error.value).toBeNull()
  })

  it('should stop the interval timer', () => {
    const saveFn = vi.fn().mockResolvedValue(undefined)
    const { start, stop, markDirty } = useAutosave(saveFn)

    markDirty('data')
    start()
    stop()

    vi.advanceTimersByTime(30000)
    expect(saveFn).not.toHaveBeenCalled()
  })

  it('should reset all state', () => {
    const saveFn = vi.fn().mockResolvedValue(undefined)
    const { isDirty, isSaving, lastSavedAt, error, markDirty, reset } = useAutosave(saveFn)

    markDirty('data')
    expect(isDirty.value).toBe(true)

    reset()
    expect(isDirty.value).toBe(false)
    expect(isSaving.value).toBe(false)
    expect(lastSavedAt.value).toBeNull()
    expect(error.value).toBeNull()
  })

  it('should invoke save on interval when dirty', () => {
    const saveFn = vi.fn().mockResolvedValue(undefined)
    const { start, markDirty } = useAutosave(saveFn, 10000)

    markDirty('data')
    start()

    vi.advanceTimersByTime(10000)
    expect(saveFn).toHaveBeenCalledTimes(1)
  })
})
