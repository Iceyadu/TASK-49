import { ref, onUnmounted, watch } from 'vue'

export function useAutosave<T>(
  saveFn: (data: T) => Promise<void>,
  intervalMs = 15000
) {
  const isDirty = ref(false)
  const isSaving = ref(false)
  const lastSavedAt = ref<Date | null>(null)
  const error = ref<string | null>(null)
  let timer: ReturnType<typeof setInterval> | null = null
  let currentData: T | null = null

  function markDirty(data: T) {
    currentData = data
    isDirty.value = true
    error.value = null
  }

  async function save() {
    if (!isDirty.value || isSaving.value || currentData === null) return

    isSaving.value = true
    error.value = null

    try {
      await saveFn(currentData)
      isDirty.value = false
      lastSavedAt.value = new Date()
    } catch (e: any) {
      error.value = e.message || 'Autosave failed'
    } finally {
      isSaving.value = false
    }
  }

  function start() {
    stop()
    timer = setInterval(() => {
      save()
    }, intervalMs)
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function reset() {
    stop()
    isDirty.value = false
    isSaving.value = false
    lastSavedAt.value = null
    error.value = null
    currentData = null
  }

  onUnmounted(() => {
    if (isDirty.value) {
      save()
    }
    stop()
  })

  return {
    isDirty,
    isSaving,
    lastSavedAt,
    error,
    markDirty,
    save,
    start,
    stop,
    reset
  }
}
