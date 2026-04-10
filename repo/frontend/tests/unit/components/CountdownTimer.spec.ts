import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useCountdown } from '@/composables/useCountdown'

// Since there is no dedicated CountdownTimer.vue component in the codebase,
// we create a minimal component that uses the useCountdown composable
// to verify the rendering and warning-state behavior.
const CountdownTimer = defineComponent({
  name: 'CountdownTimer',
  props: {
    seconds: { type: Number, required: true },
    warningThreshold: { type: Number, default: 60 }
  },
  emits: ['expired'],
  setup(props, { emit }) {
    const {
      remainingSeconds,
      formatted,
      isExpired,
      isRunning,
      start,
      pause,
      resume,
      stop,
      percentage
    } = useCountdown(props.seconds, () => emit('expired'))

    return { remainingSeconds, formatted, isExpired, isRunning, start, pause, resume, stop, percentage }
  },
  computed: {
    isWarning(): boolean {
      return this.remainingSeconds <= this.warningThreshold && this.remainingSeconds > 0
    }
  },
  render() {
    return h('div', { class: ['countdown-timer', { 'countdown-timer--warning': this.isWarning, 'countdown-timer--expired': this.isExpired }] }, [
      h('span', { class: 'countdown-timer__display' }, this.formatted),
      h('div', { class: 'countdown-timer__bar', style: { width: `${this.percentage}%` } })
    ])
  }
})

describe('CountdownTimer Component', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should render the formatted time', () => {
    const wrapper = mount(CountdownTimer, {
      props: { seconds: 90 }
    })
    expect(wrapper.find('.countdown-timer__display').text()).toBe('01:30')
  })

  it('should render without warning class when above threshold', () => {
    const wrapper = mount(CountdownTimer, {
      props: { seconds: 300, warningThreshold: 60 }
    })
    expect(wrapper.classes()).not.toContain('countdown-timer--warning')
  })

  it('should show warning state when remaining time is at or below the threshold', () => {
    const wrapper = mount(CountdownTimer, {
      props: { seconds: 30, warningThreshold: 60 }
    })
    expect(wrapper.classes()).toContain('countdown-timer--warning')
  })

  it('should not show warning state when time is exactly zero', () => {
    const wrapper = mount(CountdownTimer, {
      props: { seconds: 0, warningThreshold: 60 }
    })
    // isWarning requires > 0
    expect(wrapper.classes()).not.toContain('countdown-timer--warning')
  })

  it('should show expired class when time is zero', () => {
    const wrapper = mount(CountdownTimer, {
      props: { seconds: 0 }
    })
    expect(wrapper.classes()).toContain('countdown-timer--expired')
  })

  it('should update the display after countdown starts', async () => {
    const wrapper = mount(CountdownTimer, {
      props: { seconds: 10 }
    })

    wrapper.vm.start()
    vi.advanceTimersByTime(3000)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.countdown-timer__display').text()).toBe('00:07')
  })

  it('should emit expired event when countdown reaches zero', async () => {
    const wrapper = mount(CountdownTimer, {
      props: { seconds: 2 }
    })

    wrapper.vm.start()
    vi.advanceTimersByTime(3000) // enough to expire
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('expired')).toBeTruthy()
  })

  it('should render the progress bar with correct width percentage', () => {
    const wrapper = mount(CountdownTimer, {
      props: { seconds: 100 }
    })
    const bar = wrapper.find('.countdown-timer__bar')
    expect(bar.attributes('style')).toContain('width: 100%')
  })

  it('should transition to warning as time decreases', async () => {
    const wrapper = mount(CountdownTimer, {
      props: { seconds: 65, warningThreshold: 60 }
    })

    expect(wrapper.classes()).not.toContain('countdown-timer--warning')

    wrapper.vm.start()
    vi.advanceTimersByTime(6000) // 65 - 6 = 59, below threshold
    await wrapper.vm.$nextTick()

    expect(wrapper.classes()).toContain('countdown-timer--warning')
  })
})
