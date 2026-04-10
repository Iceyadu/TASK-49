export interface Schedule {
  id: number
  userId: number
  title: string
  description: string
  startTime: string
  endTime: string
  dayOfWeek: number
  isRecurring: boolean
  color: string
  contentRecordId: number
  quizPaperId: number
}

export interface LockedPeriod {
  id: number
  title: string
  startTime: string
  endTime: string
  reason: string
  dayOfWeek?: number
}

export interface ScheduleChangeJournal {
  id: number
  scheduleId: number
  changeType: string
  previousState: string
  newState: string
  isUndone: boolean
  sequenceNumber: number
  createdAt: string
}
