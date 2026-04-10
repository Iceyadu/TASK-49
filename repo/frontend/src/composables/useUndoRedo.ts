import { computed, shallowRef } from 'vue'

export interface UndoRedoOptions {
  maxHistory?: number
}

export function useUndoRedo<T>(initialState: T, options: UndoRedoOptions = {}) {
  const { maxHistory = 50 } = options

  const undoStack = shallowRef<T[]>([])
  const redoStack = shallowRef<T[]>([])
  const currentState = shallowRef<T>(structuredClone(initialState))

  const canUndo = computed(() => undoStack.value.length > 0)
  const canRedo = computed(() => redoStack.value.length > 0)
  const undoCount = computed(() => undoStack.value.length)
  const redoCount = computed(() => redoStack.value.length)

  function pushState(newState: T) {
    const nextUndo = [...undoStack.value, structuredClone(currentState.value)]
    if (nextUndo.length > maxHistory) nextUndo.shift()
    undoStack.value = nextUndo
    currentState.value = structuredClone(newState)
    redoStack.value = []
  }

  function undo(): T | null {
    if (!canUndo.value) return null
    const nextUndo = [...undoStack.value]
    const previousState = nextUndo.pop()!
    undoStack.value = nextUndo
    redoStack.value = [...redoStack.value, structuredClone(currentState.value)]
    currentState.value = previousState
    return structuredClone(currentState.value)
  }

  function redo(): T | null {
    if (!canRedo.value) return null
    const nextRedo = [...redoStack.value]
    const nextState = nextRedo.pop()!
    redoStack.value = nextRedo
    undoStack.value = [...undoStack.value, structuredClone(currentState.value)]
    currentState.value = nextState
    return structuredClone(currentState.value)
  }

  function reset(state?: T) {
    undoStack.value = []
    redoStack.value = []
    currentState.value = structuredClone(state ?? initialState)
  }

  function getHistory(): { undoStack: T[]; redoStack: T[]; current: T } {
    return {
      undoStack: undoStack.value.map(s => structuredClone(s)),
      redoStack: redoStack.value.map(s => structuredClone(s)),
      current: structuredClone(currentState.value)
    }
  }

  return {
    currentState,
    canUndo,
    canRedo,
    undoCount,
    redoCount,
    pushState,
    undo,
    redo,
    reset,
    getHistory
  }
}
