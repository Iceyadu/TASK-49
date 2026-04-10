export interface Submission {
  id: number
  quizPaperId: number
  studentId: number
  attemptNumber: number
  status: string
  startedAt: string
  submittedAt: string
  autoSavedAt: string
  timeRemainingSeconds: number
  totalScore: number
  maxScore: number
  percentage: number
  answers: SubmissionAnswer[]
}

export interface SubmissionAnswer {
  id: number
  questionId: number
  answerText: string
  selectedOption: string
  isCorrect: boolean
  score: number
  autoGraded: boolean
}

export interface AutosavePayload {
  answers: {
    questionId: number
    answerText?: string
    selectedOption?: string
  }[]
  timeRemainingSeconds: number
}
