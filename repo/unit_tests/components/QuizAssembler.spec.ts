import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import QuizAssembler from '@/components/instructor/QuizAssembler.vue'

describe('QuizAssembler Component', () => {
  it('renders the title', () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    expect(wrapper.find('.quiz-assembler__title').text()).toBe('Quiz Assembly Rules')
  })

  it('renders 5 difficulty constraint rows', () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    const rows = wrapper.findAll('.quiz-assembler__constraint-row')
    expect(rows.length).toBe(5)
  })

  it('defaults to 20 total questions', () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    const input = wrapper.find('#totalQuestions') as any
    expect(input.element.value).toBe('20')
  })

  it('shows total min and max in the summary', () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    const summary = wrapper.find('.quiz-assembler__summary-text').text()
    // Default constraints: min = 0+2+6+3+2 = 13, max = 0+5+10+6+4 = 25
    expect(summary).toContain('13')
    expect(summary).toContain('25')
  })

  it('shows no validation error when constraints are satisfiable with default values', () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    // Default: totalMin=13, totalMax=25, target=20 → valid (13 <= 20 <= 25)
    expect(wrapper.find('.quiz-assembler__validation-error').exists()).toBe(false)
  })

  it('shows validation error when total min exceeds target questions', async () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    // Set target questions to 5 (below default min of 13)
    const input = wrapper.find('#totalQuestions')
    await input.setValue(5)
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.quiz-assembler__validation-error').exists()).toBe(true)
    expect(wrapper.find('.quiz-assembler__validation-error').text()).toContain('Minimum constraints')
  })

  it('shows validation error when total max is less than target questions', async () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    // Set target to 30 (above default max of 25)
    const input = wrapper.find('#totalQuestions')
    await input.setValue(30)
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.quiz-assembler__validation-error').exists()).toBe(true)
    expect(wrapper.find('.quiz-assembler__validation-error').text()).toContain('Maximum constraints')
  })

  it('disables assemble button when there is a validation error', async () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    const input = wrapper.find('#totalQuestions')
    await input.setValue(5)
    await wrapper.vm.$nextTick()
    const assembleBtn = wrapper.find('.quiz-assembler__btn--assemble')
    expect((assembleBtn.element as HTMLButtonElement).disabled).toBe(true)
  })

  it('disables assemble button when assembling prop is true', () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: true }
    })
    const assembleBtn = wrapper.find('.quiz-assembler__btn--assemble')
    expect((assembleBtn.element as HTMLButtonElement).disabled).toBe(true)
  })

  it('shows "Assembling..." button text while assembling is true', () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: true }
    })
    expect(wrapper.find('.quiz-assembler__btn--assemble').text()).toBe('Assembling...')
  })

  it('shows "Assemble Quiz" button text when not assembling', () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    expect(wrapper.find('.quiz-assembler__btn--assemble').text()).toBe('Assemble Quiz')
  })

  it('emits "preview" event with rules when Preview Selection button is clicked', async () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    await wrapper.find('.quiz-assembler__btn--preview').trigger('click')
    expect(wrapper.emitted('preview')).toBeTruthy()
    const emittedRules = wrapper.emitted('preview')![0][0] as any[]
    expect(Array.isArray(emittedRules)).toBe(true)
  })

  it('emits "assemble" event with totalQuestions and rules on form submit', async () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    await wrapper.find('.quiz-assembler__form').trigger('submit')
    expect(wrapper.emitted('assemble')).toBeTruthy()
    const payload = wrapper.emitted('assemble')![0][0] as any
    expect(payload).toHaveProperty('totalQuestions')
    expect(payload).toHaveProperty('rules')
    expect(typeof payload.totalQuestions).toBe('number')
    expect(Array.isArray(payload.rules)).toBe(true)
  })

  it('emitted assemble rules only include constraints with minCount > 0 or maxCount > 0', async () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    await wrapper.find('.quiz-assembler__form').trigger('submit')
    const payload = wrapper.emitted('assemble')![0][0] as any
    // Difficulty 1 has min=0, max=0 by default, so it should NOT be included
    for (const rule of payload.rules) {
      expect(rule.minCount > 0 || rule.maxCount > 0).toBe(true)
    }
  })

  it('emitted assemble rules include ruleType DIFFICULTY', async () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    await wrapper.find('.quiz-assembler__form').trigger('submit')
    const payload = wrapper.emitted('assemble')![0][0] as any
    for (const rule of payload.rules) {
      expect(rule.ruleType).toBe('DIFFICULTY')
    }
  })

  it('does not emit "assemble" when validation error exists', async () => {
    const wrapper = mount(QuizAssembler, {
      props: { assembling: false }
    })
    const input = wrapper.find('#totalQuestions')
    await input.setValue(5)
    await wrapper.vm.$nextTick()
    await wrapper.find('.quiz-assembler__form').trigger('submit')
    expect(wrapper.emitted('assemble')).toBeFalsy()
  })
})
