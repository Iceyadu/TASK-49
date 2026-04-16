import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import GradingQueue from '@/components/ta/GradingQueue.vue'
import type { GradingQueueItem } from '@/components/ta/GradingQueue.vue'

const sampleItems: GradingQueueItem[] = [
  {
    id: 10,
    questionText: 'Explain the difference between TCP and UDP.',
    studentId: 101,
    studentName: 'Alice Student',
    status: 'PENDING',
    assignedToId: undefined,
    assignedToName: undefined,
  },
  {
    id: 11,
    questionText: 'Describe the OSI model layers.',
    studentId: 102,
    studentName: 'Bob Student',
    status: 'IN_PROGRESS',
    assignedToId: 201,
    assignedToName: 'TA Charlie',
  },
  {
    id: 12,
    questionText: 'What is recursion?',
    studentId: 103,
    studentName: 'Dave Student',
    status: 'GRADED',
    assignedToId: 201,
    assignedToName: 'TA Charlie',
  },
  {
    id: 13,
    questionText: 'Explain the SUBMITTED status item here.',
    studentId: 104,
    status: 'SUBMITTED',
    assignedToId: undefined,
    assignedToName: undefined,
  },
]

describe('GradingQueue Component', () => {
  it('shows loading spinner when loading is true', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: [], loading: true, totalItems: 0 }
    })
    expect(wrapper.findComponent({ name: 'LoadingSpinner' }).exists()).toBe(true)
    expect(wrapper.find('table').exists()).toBe(false)
  })

  it('shows empty state when items array is empty', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: [], loading: false, totalItems: 0 }
    })
    expect(wrapper.findComponent({ name: 'EmptyState' }).exists()).toBe(true)
    expect(wrapper.find('table').exists()).toBe(false)
  })

  it('renders a row for each item', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    expect(rows.length).toBe(4)
  })

  it('renders student name when available', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    expect(rows[0].text()).toContain('Alice Student')
  })

  it('renders "Student #<id>" when student name is not provided', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    expect(rows[3].text()).toContain('Student #104')
  })

  it('renders "Unassigned" when assignedToName is not set', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    expect(rows[0].text()).toContain('Unassigned')
  })

  it('renders assigned TA name when assignedToName is set', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    expect(rows[1].text()).toContain('TA Charlie')
  })

  it('shows Claim button for PENDING items', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    const claimButtons = rows[0].findAll('button').filter(b => b.text() === 'Claim')
    expect(claimButtons.length).toBe(1)
  })

  it('shows Claim button for SUBMITTED items', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    const claimButtons = rows[3].findAll('button').filter(b => b.text() === 'Claim')
    expect(claimButtons.length).toBe(1)
  })

  it('hides Claim button for IN_PROGRESS items', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    const claimButtons = rows[1].findAll('button').filter(b => b.text() === 'Claim')
    expect(claimButtons.length).toBe(0)
  })

  it('always shows Grade button for every item', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const gradeButtons = wrapper.findAll('button').filter(b => b.text() === 'Grade')
    expect(gradeButtons.length).toBe(sampleItems.length)
  })

  it('shows Release button for IN_PROGRESS items', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    const releaseButtons = rows[1].findAll('button').filter(b => b.text() === 'Release')
    expect(releaseButtons.length).toBe(1)
  })

  it('hides Release button for PENDING items', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    const releaseButtons = rows[0].findAll('button').filter(b => b.text() === 'Release')
    expect(releaseButtons.length).toBe(0)
  })

  it('emits "claim" with item id when Claim button is clicked', async () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    const claimBtn = rows[0].findAll('button').find(b => b.text() === 'Claim')!
    await claimBtn.trigger('click')
    expect(wrapper.emitted('claim')).toBeTruthy()
    expect(wrapper.emitted('claim')![0][0]).toBe(10)
  })

  it('emits "grade" with item id when Grade button is clicked', async () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    const gradeBtn = rows[0].findAll('button').find(b => b.text() === 'Grade')!
    await gradeBtn.trigger('click')
    expect(wrapper.emitted('grade')).toBeTruthy()
    expect(wrapper.emitted('grade')![0][0]).toBe(10)
  })

  it('emits "release" with item id when Release button is clicked', async () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const rows = wrapper.findAll('tbody tr')
    const releaseBtn = rows[1].findAll('button').find(b => b.text() === 'Release')!
    await releaseBtn.trigger('click')
    expect(wrapper.emitted('release')).toBeTruthy()
    expect(wrapper.emitted('release')![0][0]).toBe(11)
  })

  it('applies correct status CSS class to status badge', () => {
    const wrapper = mount(GradingQueue, {
      props: { items: sampleItems, loading: false, totalItems: 4 }
    })
    const statusBadges = wrapper.findAll('.grading-queue__status')
    expect(statusBadges[0].classes()).toContain('grading-queue__status--pending')
    expect(statusBadges[1].classes()).toContain('grading-queue__status--in-progress')
    expect(statusBadges[2].classes()).toContain('grading-queue__status--graded')
  })

  it('truncates question text longer than 80 characters', () => {
    const longQuestion = 'A'.repeat(100)
    const itemWithLongQuestion: GradingQueueItem = {
      id: 99, questionText: longQuestion, studentId: 1, status: 'PENDING'
    }
    const wrapper = mount(GradingQueue, {
      props: { items: [itemWithLongQuestion], loading: false, totalItems: 1 }
    })
    const questionCell = wrapper.find('.grading-queue__question-text')
    expect(questionCell.text().length).toBeLessThanOrEqual(83) // 80 + '...'
    expect(questionCell.text()).toContain('...')
  })

  it('does not truncate question text of 80 characters or fewer', () => {
    const exactQuestion = 'B'.repeat(80)
    const item: GradingQueueItem = {
      id: 99, questionText: exactQuestion, studentId: 1, status: 'PENDING'
    }
    const wrapper = mount(GradingQueue, {
      props: { items: [item], loading: false, totalItems: 1 }
    })
    const questionCell = wrapper.find('.grading-queue__question-text')
    expect(questionCell.text()).not.toContain('...')
    expect(questionCell.text().length).toBe(80)
  })
})
