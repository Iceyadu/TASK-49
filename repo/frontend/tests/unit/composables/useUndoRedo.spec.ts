import { describe, it, expect, vi } from 'vitest'
import { useUndoRedo } from '@/composables/useUndoRedo'

describe('useUndoRedo', () => {
  it('should initialize with the given state', () => {
    const { currentState, canUndo, canRedo } = useUndoRedo({ text: 'hello' })
    expect(currentState.value).toEqual({ text: 'hello' })
    expect(canUndo.value).toBe(false)
    expect(canRedo.value).toBe(false)
  })

  it('should push a new state', () => {
    const { currentState, pushState, undoCount } = useUndoRedo('initial')
    pushState('second')
    expect(currentState.value).toBe('second')
    expect(undoCount.value).toBe(1)
  })

  it('should undo to the previous state', () => {
    const { currentState, pushState, undo, canUndo } = useUndoRedo('A')
    pushState('B')
    pushState('C')
    expect(currentState.value).toBe('C')

    const result = undo()
    expect(result).toBe('B')
    expect(currentState.value).toBe('B')
    expect(canUndo.value).toBe(true) // can still undo to 'A'
  })

  it('should undo all the way back to initial state', () => {
    const { currentState, pushState, undo, canUndo } = useUndoRedo('start')
    pushState('mid')
    pushState('end')

    undo()
    undo()
    expect(currentState.value).toBe('start')
    expect(canUndo.value).toBe(false)
  })

  it('should return null when undo is not possible', () => {
    const { undo } = useUndoRedo('only')
    const result = undo()
    expect(result).toBeNull()
  })

  it('should redo a previously undone state', () => {
    const { currentState, pushState, undo, redo, canRedo } = useUndoRedo('A')
    pushState('B')
    pushState('C')

    undo() // back to B
    expect(canRedo.value).toBe(true)

    const result = redo()
    expect(result).toBe('C')
    expect(currentState.value).toBe('C')
    expect(canRedo.value).toBe(false)
  })

  it('should return null when redo is not possible', () => {
    const { redo } = useUndoRedo('only')
    const result = redo()
    expect(result).toBeNull()
  })

  it('should clear redo stack when a new state is pushed after undo', () => {
    const { pushState, undo, canRedo, redoCount } = useUndoRedo('A')
    pushState('B')
    pushState('C')

    undo() // back to B, redo stack has C
    expect(canRedo.value).toBe(true)

    pushState('D') // new action clears redo stack
    expect(canRedo.value).toBe(false)
    expect(redoCount.value).toBe(0)
  })

  it('should respect the undo limit (maxHistory)', () => {
    const { pushState, undoCount } = useUndoRedo(0, { maxHistory: 3 })

    pushState(1)
    pushState(2)
    pushState(3)
    expect(undoCount.value).toBe(3)

    pushState(4) // should drop the oldest entry
    expect(undoCount.value).toBe(3) // still capped at 3
  })

  it('should undo correctly at the limit boundary', () => {
    const { currentState, pushState, undo, undoCount } = useUndoRedo('A', { maxHistory: 2 })

    pushState('B')
    pushState('C')
    pushState('D')
    // undo stack should be [B, C] (A was shifted out), current is D
    expect(undoCount.value).toBe(2)

    undo() // back to C
    expect(currentState.value).toBe('C')
    undo() // back to B
    expect(currentState.value).toBe('B')
    undo() // nothing left
    expect(currentState.value).toBe('B') // unchanged
  })

  it('should reset to initial state', () => {
    const { currentState, pushState, reset, canUndo, canRedo } = useUndoRedo('init')
    pushState('X')
    pushState('Y')

    reset()
    expect(currentState.value).toBe('init')
    expect(canUndo.value).toBe(false)
    expect(canRedo.value).toBe(false)
  })

  it('should reset to a custom state when provided', () => {
    const { currentState, pushState, reset } = useUndoRedo('init')
    pushState('X')
    reset('custom')
    expect(currentState.value).toBe('custom')
  })

  it('should deep-clone states to prevent mutation', () => {
    const initial = { items: [1, 2, 3] }
    const { currentState, pushState, undo } = useUndoRedo(initial)

    const newState = { items: [4, 5, 6] }
    pushState(newState)

    // Mutate the object passed in
    newState.items.push(7)
    // Current state should not be affected
    expect(currentState.value.items).toEqual([4, 5, 6])

    undo()
    // Original initial should not be mutated either
    expect(currentState.value.items).toEqual([1, 2, 3])
  })

  it('should return history snapshot via getHistory', () => {
    const { pushState, undo, getHistory } = useUndoRedo('A')
    pushState('B')
    pushState('C')
    undo()

    const history = getHistory()
    expect(history.current).toBe('B')
    expect(history.undoStack).toEqual(['A'])
    expect(history.redoStack).toEqual(['C'])
  })

  it('should track undoCount and redoCount correctly', () => {
    const { pushState, undo, redo, undoCount, redoCount } = useUndoRedo(0)

    pushState(1)
    pushState(2)
    expect(undoCount.value).toBe(2)
    expect(redoCount.value).toBe(0)

    undo()
    expect(undoCount.value).toBe(1)
    expect(redoCount.value).toBe(1)

    redo()
    expect(undoCount.value).toBe(2)
    expect(redoCount.value).toBe(0)
  })

  it('should default maxHistory to 50', () => {
    const { pushState, undoCount } = useUndoRedo(0)
    for (let i = 1; i <= 55; i++) {
      pushState(i)
    }
    expect(undoCount.value).toBe(50)
  })
})
