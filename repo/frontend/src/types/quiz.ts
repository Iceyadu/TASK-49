export interface QuestionBank {
  id: number
  name: string
  description: string
  subject: string
  questions: Question[]
}

export interface Question {
  id: number
  questionType: string
  difficultyLevel: number
  questionText: string
  options: string
  correctAnswer: string
  explanation: string
  points: number
  knowledgeTags: KnowledgeTag[]
}

export interface KnowledgeTag {
  id: number
  name: string
  category: string
}

export interface QuizPaper {
  id: number
  title: string
  description: string
  totalQuestions: number
  totalPoints: number
  timeLimitMinutes: number
  maxAttempts: number
  releaseStart: string
  releaseEnd: string
  isPublished: boolean
  questions: Question[]
}

export interface QuizRule {
  ruleType: string
  minCount?: number
  maxCount?: number
  difficultyLevel?: number
  tagId?: number
}
