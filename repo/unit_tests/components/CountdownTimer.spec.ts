import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import CountdownTimer from '@/components/student/CountdownTimer.vue'

describe('CountdownTimer Component', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders the formatted initial time', () => {
    const wrapper = mount(CountdownTimer, {
      props: { timeRemainingSeconds: 90 }
    })
    expect(wrapper.find('.countdown-timer__display').text()).toBe('01:30')
  })

  it('uses warning class when between 61 and 300 seconds', () => {
    const wrapper = mount(CountdownTimer, {
      props: { timeRemainingSeconds: 120 }
    })
    expect(wrapper.classes()).toContain('countdown-timer--warning')
    expect(wrapper.classes()).not.toContain('countdown-timer--critical')
  })

  it('uses critical class when <= 60 seconds', () => {
    const wrapper = mount(CountdownTimer, {
      props: { timeRemainingSeconds: 30 }
    })
    expect(wrapper.classes()).toContain('countdown-timer--critical')
  })

  it('emits tick every second with updated remaining time', async () => {
    const wrapper = mount(CountdownTimer, {
      props: { timeRemainingSeconds: 3 }
    })

    vi.advanceTimersByTime(1000)
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('tick')?.[0]).toEqual([2])
  })

  it('emits expired when timer reaches zero', async () => {
    const wrapper = mount(CountdownTimer, {
      props: { timeRemainingSeconds: 2 }
    })

    vi.advanceTimersByTime(3000)
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('expired')).toBeTruthy()
  })

  it('updates display text as countdown progresses', async () => {
    const wrapper = mount(CountdownTimer, {
      props: { timeRemainingSeconds: 5 }
    })

    vi.advanceTimersByTime(2000)
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.countdown-timer__display').text()).toBe('00:03')
  })

  it('updates from prop changes (server sync)', async () => {
    const wrapper = mount(CountdownTimer, {
      props: { timeRemainingSeconds: 500 }
    })

    await wrapper.setProps({ timeRemainingSeconds: 45 })
    await wrapper.vm.$nextTick()
    expect(wrapper.classes()).toContain('countdown-timer--critical')
  })
})
