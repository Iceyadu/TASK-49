import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import WrongAnswerReview from '@/components/student/WrongAnswerReview.vue'
import type { WrongAnswerItem } from '@/components/student/WrongAnswerReview.vue'

const sampleItems: WrongAnswerItem[] = [
  {
    questionText: 'What is the capital of France?',
    studentAnswer: 'London',
    correctAnswer: 'Paris',
    explanation: 'Paris has been the capital of France since the late 10th century.',
    quizTitle: 'World Geography Quiz',
  },
  {
    questionText: 'What is 2 + 2?',
    studentAnswer: '',
    correctAnswer: '4',
    explanation: '',
    quizTitle: 'Basic Math',
  },
]

describe('WrongAnswerReview Component', () => {
  it('renders title heading', () => {
    const wrapper = mount(WrongAnswerReview, {
      props: { items: [], loading: false }
    })
    expect(wrapper.find('.wrong-answers__title').text()).toBe('Wrong Answer Review')
  })

  it('shows loading spinner when loading is true', () => {
    const wrapper = mount(WrongAnswerReview, {
      props: { items: [], loading: true }
    })
    expect(wrapper.findComponent({ name: 'LoadingSpinner' }).exists()).toBe(true)
    expect(wrapper.find('.wrong-answers__list').exists()).toBe(false)
  })

  it('shows empty state when items array is empty and not loading', () => {
    const wrapper = mount(WrongAnswerReview, {
      props: { items: [], loading: false }
    })
    expect(wrapper.findComponent({ name: 'EmptyState' }).exists()).toBe(true)
    expect(wrapper.find('.wrong-answers__list').exists()).toBe(false)
  })

  it('renders the correct number of wrong answer items', () => {
    const wrapper = mount(WrongAnswerReview, {
      props: { items: sampleItems, loading: false }
    })
    const items = wrapper.findAll('.wrong-answers__item')
    expect(items.length).toBe(2)
  })

  it('renders question text for each item', () => {
    const wrapper = mount(WrongAnswerReview, {
      props: { items: sampleItems, loading: false }
    })
    const questionTexts = wrapper.findAll('.wrong-answers__question-text')
    expect(questionTexts[0].text()).toBe('What is the capital of France?')
    expect(questionTexts[1].text()).toBe('What is 2 + 2?')
  })

  it('renders quiz title label for each item', () => {
    const wrapper = mount(WrongAnswerReview, {
      props: { items: sampleItems, loading: false }
    })
    const quizLabels = wrapper.findAll('.wrong-answers__item-quiz')
    expect(quizLabels[0].text()).toBe('World Geography Quiz')
    expect(quizLabels[1].text()).toBe('Basic Math')
  })

  it('renders student answer text', () => {
    const wrapper = mount(WrongAnswerReview, {
      props: { items: sampleItems, loading: false }
    })
    const studentAnswers = wrapper.findAll('.wrong-answers__answer--student .wrong-answers__answer-text')
    expect(studentAnswers[0].text()).toBe('London')
  })

  it('renders "(No answer)" when student answer is empty', () => {
    const wrapper = mount(WrongAnswerReview, {
      props: { items: sampleItems, loading: false }
    })
    const studentAnswers = wrapper.findAll('.wrong-answers__answer--student .wrong-answers__answer-text')
    expect(studentAnswers[1].text()).toBe('(No answer)')
  })

  it('renders the correct answer for each item', () => {
    const wrapper = mount(WrongAnswerReview, {
      props: { items: sampleItems, loading: false }
    })
    const correctAnswers = wrapper.findAll('.wrong-answers__answer--correct .wrong-answers__answer-text')
    expect(correctAnswers[0].text()).toBe('Paris')
    expect(correctAnswers[1].text()).toBe('4')
  })

  it('shows explanation section when explanation is present', () => {
    const wrapper = mount(WrongAnswerReview, {
      props: { items: sampleItems, loading: false }
    })
    const explanations = wrapper.findAll('.wrong-answers__explanation')
    expect(explanations.length).toBe(1)
    expect(explanations[0].find('.wrong-answers__explanation-text').text()).toContain('Paris')
  })

  it('hides explanation section when explanation is empty', () => {
    const itemsWithNoExplanation: WrongAnswerItem[] = [
      {
        questionText: 'Q?',
        studentAnswer: 'Wrong',
        correctAnswer: 'Right',
        explanation: '',
        quizTitle: 'Test',
      },
    ]
    const wrapper = mount(WrongAnswerReview, {
      props: { items: itemsWithNoExplanation, loading: false }
    })
    expect(wrapper.find('.wrong-answers__explanation').exists()).toBe(false)
  })

  it('renders item number labels in order', () => {
    const wrapper = mount(WrongAnswerReview, {
      props: { items: sampleItems, loading: false }
    })
    const numbers = wrapper.findAll('.wrong-answers__item-number')
    expect(numbers[0].text()).toBe('Question 1')
    expect(numbers[1].text()).toBe('Question 2')
  })
})
