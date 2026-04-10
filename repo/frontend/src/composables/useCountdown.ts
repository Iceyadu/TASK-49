import { ref, computed, onUnmounted } from 'vue'

export function useCountdown(initialSeconds: number, onExpire?: () => void) {
  const remainingSeconds = ref(initialSeconds)
  const isRunning = ref(false)
  const isPaused = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  const minutes = computed(() => Math.floor(remainingSeconds.value / 60))
  const seconds = computed(() => remainingSeconds.value % 60)
  const formatted = computed(() => {
    const m = String(minutes.value).padStart(2, '0')
    const s = String(seconds.value).padStart(2, '0')
    return `${m}:${s}`
  })
  const isExpired = computed(() => remainingSeconds.value <= 0)
  const percentage = computed(() => {
    if (initialSeconds <= 0) return 0
    return (remainingSeconds.value / initialSeconds) * 100
  })

  function start() {
    if (isRunning.value && !isPaused.value) return
    isRunning.value = true
    isPaused.value = false
    timer = setInterval(() => {
      if (remainingSeconds.value > 0) {
        remainingSeconds.value--
      } else {
        stop()
        onExpire?.()
      }
    }, 1000)
  }

  function pause() {
    if (!isRunning.value || isPaused.value) return
    isPaused.value = true
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function resume() {
    if (!isPaused.value) return
    start()
  }

  function stop() {
    isRunning.value = false
    isPaused.value = false
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function reset(newSeconds?: number) {
    stop()
    remainingSeconds.value = newSeconds ?? initialSeconds
  }

  function setRemaining(secs: number) {
    remainingSeconds.value = Math.max(0, secs)
  }

  onUnmounted(() => {
    stop()
  })

  return {
    remainingSeconds,
    isRunning,
    isPaused,
    isExpired,
    minutes,
    seconds,
    formatted,
    percentage,
    start,
    pause,
    resume,
    stop,
    reset,
    setRemaining
  }
}
