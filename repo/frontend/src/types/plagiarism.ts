export interface PlagiarismCheck {
  id: number
  submissionId: number
  status: string
  maxSimilarityScore: number
  isFlagged: boolean
  checkedAt: string
  matches: PlagiarismMatch[]
}

export interface PlagiarismMatch {
  id: number
  matchedSubmissionId: number
  matchedContentId: number
  similarityScore: number
  matchedTextExcerpt: string
  sourceTextExcerpt: string
}
