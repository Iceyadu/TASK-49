export interface GradingState {
  id: number
  submissionAnswerId: number
  status: string
  assignedToId: number
  gradedById: number
  score: number
  feedback: string
  gradedAt: string
  rubricScores: RubricScore[]
}

export interface RubricScore {
  id: number
  criterionName: string
  maxScore: number
  awardedScore: number
  comment: string
}
