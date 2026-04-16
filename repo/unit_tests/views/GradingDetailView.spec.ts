import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import GradingDetailView from '@/views/ta/GradingDetailView.vue'

const getGradingStateMock = vi.fn()
const gradeItemMock = vi.fn()

vi.mock('@/api/grading', () => ({
  getGradingState: (...args: unknown[]) => getGradingStateMock(...args),
  gradeItem: (...args: unknown[]) => gradeItemMock(...args),
}))

async function mountWithRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/ta/grading/:id', component: GradingDetailView }],
  })
  await router.push('/ta/grading/99')
  await router.isReady()
  return router
}

describe('GradingDetailView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getGradingStateMock.mockResolvedValue({
      submissionAnswer: { question: { questionText: 'Explain polymorphism' }, answerText: 'Sample answer' },
      rubricScores: [],
    })
    gradeItemMock.mockResolvedValue({
      submissionAnswer: { question: { questionText: 'Explain polymorphism' }, answerText: 'Updated answer' },
      rubricScores: [{ criterion: 'Accuracy', score: 5 }],
    })
  })

  it('loads grading state on mount and passes it to grading component', async () => {
    const router = await mountWithRouter()
    const wrapper = mount(GradingDetailView, {
      global: {
        plugins: [router],
        stubs: {
          SubjectiveGradingView: {
            template: '<div class="subjective-stub">{{ questionText }}</div>',
            props: ['questionText'],
          },
        },
      },
    })

    await flushPromises()
    expect(getGradingStateMock).toHaveBeenCalledWith(99)
    expect(wrapper.find('.subjective-stub').text()).toContain('Explain polymorphism')
  })

  it('submits TA grading payload and updates state', async () => {
    const router = await mountWithRouter()
    const wrapper = mount(GradingDetailView, {
      global: {
        plugins: [router],
        stubs: {
          SubjectiveGradingView: {
            emits: ['submit'],
            template: '<button class="grade-submit" @click="$emit(\'submit\', { score: 8, feedback: \'Well reasoned\' })">Grade</button>',
          },
        },
      },
    })

    await flushPromises()
    await wrapper.get('.grade-submit').trigger('click')
    await flushPromises()

    expect(gradeItemMock).toHaveBeenCalledWith(99, 8, 'Well reasoned')
  })
})
