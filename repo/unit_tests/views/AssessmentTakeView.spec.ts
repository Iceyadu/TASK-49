import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import AssessmentTakeView from '@/views/student/AssessmentTakeView.vue'

const getQuizMock = vi.fn()
const startSubmissionMock = vi.fn()
const submitSubmissionMock = vi.fn()
const autosaveMock = vi.fn()

vi.mock('@/api/quiz', () => ({
  getQuiz: (...args: unknown[]) => getQuizMock(...args),
}))

vi.mock('@/api/submissions', () => ({
  startSubmission: (...args: unknown[]) => startSubmissionMock(...args),
  submitSubmission: (...args: unknown[]) => submitSubmissionMock(...args),
  autosave: (...args: unknown[]) => autosaveMock(...args),
}))

async function mountWithRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/student/assessment/:id', component: AssessmentTakeView },
      { path: '/student/dashboard', component: { template: '<div>Dashboard</div>' } },
    ],
  })
  await router.push('/student/assessment/42')
  await router.isReady()
  return router
}

describe('AssessmentTakeView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('alert', vi.fn())
    getQuizMock.mockResolvedValue({
      id: 42,
      title: 'Boundary Quiz',
      totalQuestions: 1,
      maxAttempts: 2,
      timeLimitMinutes: 60,
      questions: [],
    })
    startSubmissionMock.mockResolvedValue({
      id: 777,
      timeRemainingSeconds: 3000,
    })
    submitSubmissionMock.mockResolvedValue({ id: 777, status: 'SUBMITTED' })
    autosaveMock.mockResolvedValue(undefined)
  })

  it('loads quiz details and starts the assessment on click', async () => {
    const router = await mountWithRouter()
    const wrapper = mount(AssessmentTakeView, {
      global: {
        plugins: [router],
        stubs: {
          AssessmentView: {
            template: '<div class="assessment-view-stub"></div>',
          },
        },
      },
    })

    await flushPromises()
    expect(getQuizMock).toHaveBeenCalledWith(42)
    expect(wrapper.text()).toContain('Boundary Quiz')

    await wrapper.get('button.btn-primary').trigger('click')
    await flushPromises()

    expect(startSubmissionMock).toHaveBeenCalledWith(42)
    expect(wrapper.find('.assessment-view-stub').exists()).toBe(true)
  })

  it('submits and redirects student to dashboard after completion', async () => {
    const router = await mountWithRouter()
    const pushSpy = vi.spyOn(router, 'push')
    const wrapper = mount(AssessmentTakeView, {
      global: {
        plugins: [router],
        stubs: {
          AssessmentView: {
            emits: ['submit'],
            template: '<button class="submit-assessment" @click="$emit(\'submit\')">Submit</button>',
          },
        },
      },
    })

    await flushPromises()
    await wrapper.get('button.btn-primary').trigger('click')
    await flushPromises()

    await wrapper.get('.submit-assessment').trigger('click')
    await flushPromises()

    expect(submitSubmissionMock).toHaveBeenCalledWith(777)
    expect(pushSpy).toHaveBeenCalledWith('/student/dashboard')
  })
})
