import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TimetableEditor from '@/components/student/TimetableEditor.vue'
import type { Schedule, LockedPeriod } from '@/types/schedule'
const sampleSessions: Schedule[] = [
  {
    id: 1, userId: 10, title: 'Math 101', description: 'Algebra',
    startTime: '09:00', endTime: '10:00', dayOfWeek: 1, isRecurring: true,
    color: '#3b82f6', contentRecordId: 0, quizPaperId: 0
  },
  {
    id: 2, userId: 10, title: 'Physics 201', description: 'Mechanics',
    startTime: '11:00', endTime: '12:30', dayOfWeek: 3, isRecurring: true,
    color: '#ef4444', contentRecordId: 0, quizPaperId: 0
  }
]

const sampleLockedPeriods: LockedPeriod[] = [
  { id: 100, title: 'Exam Block', startTime: '09:00', endTime: '11:00', dayOfWeek: 1, reason: 'Final examinations' }
]

describe('TimetableEditor Component', () => {
  it('renders sessions for each day', () => {
    const wrapper = mount(TimetableEditor, {
      props: { sessions: sampleSessions, lockedPeriods: [], undoStack: [], redoStack: [] }
    })
    const blocks = wrapper.findAll('.timetable-editor__session-positioned')
    expect(blocks.length).toBe(2)
  })

  it('emits slot-click when unlocked slot is clicked', async () => {
    const wrapper = mount(TimetableEditor, {
      props: { sessions: [], lockedPeriods: [], undoStack: [], redoStack: [] }
    })
    await wrapper.find('.timetable-editor__slot').trigger('click')
    expect(wrapper.emitted('slotClick')).toBeTruthy()
  })

  it('does not emit slot-click when slot is locked', async () => {
    const wrapper = mount(TimetableEditor, {
      props: { sessions: [], lockedPeriods: sampleLockedPeriods, undoStack: [], redoStack: [] }
    })
    const mondayNineSlot = wrapper.findAll('.timetable-editor__slot')[2]
    await mondayNineSlot.trigger('click')
    expect(wrapper.emitted('slotClick')).toBeFalsy()
  })

  it('renders locked indicator for matching day/hour only', () => {
    const wrapper = mount(TimetableEditor, {
      props: { sessions: [], lockedPeriods: sampleLockedPeriods, undoStack: [], redoStack: [] }
    })
    const lockedIndicators = wrapper.findAll('.timetable-editor__locked-indicator')
    expect(lockedIndicators.length).toBeGreaterThan(0)
  })
})
