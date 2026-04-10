import apiClient from '@/api/client'
import type { QuestionBank, Question, QuizPaper, QuizRule, KnowledgeTag } from '@/types/quiz'

// Quiz paper operations
export async function assembleQuiz(request: {
  title: string
  description?: string
  questionBankId: number
  totalQuestions: number
  rules?: QuizRule[]
  timeLimitMinutes?: number
  maxAttempts?: number
  releaseStart?: string
  releaseEnd?: string
  shuffleQuestions?: boolean
  showImmediateFeedback?: boolean
}): Promise<QuizPaper> {
  const { data } = await apiClient.post('/api/quizzes/assemble', request)
  return data.data
}

export async function getQuizzes(page = 0, size = 20): Promise<{ content: QuizPaper[]; totalElements: number; totalPages: number }> {
  const { data } = await apiClient.get('/api/quizzes', { params: { page, size } })
  return data.data
}

export async function getQuiz(id: number): Promise<QuizPaper> {
  const { data } = await apiClient.get(`/api/quizzes/${id}`)
  return data.data
}

export async function scheduleQuiz(id: number, releaseStart: string, releaseEnd: string): Promise<QuizPaper> {
  const { data } = await apiClient.put(`/api/quizzes/${id}/schedule`, { releaseStart, releaseEnd })
  return data.data
}

export async function publishQuiz(id: number): Promise<QuizPaper> {
  const { data } = await apiClient.put(`/api/quizzes/${id}/publish`)
  return data.data
}

// Question bank operations
export async function getQuestionBanks(page = 0, size = 20): Promise<{ content: QuestionBank[]; totalElements: number; totalPages: number }> {
  const { data } = await apiClient.get('/api/question-banks', { params: { page, size } })
  return data.data
}

export async function createQuestionBank(bank: Partial<QuestionBank>): Promise<QuestionBank> {
  const { data } = await apiClient.post('/api/question-banks', bank)
  return data.data
}

export async function addQuestion(bankId: number, question: Partial<Question>): Promise<Question> {
  const { data } = await apiClient.post(`/api/question-banks/${bankId}/questions`, question)
  return data.data
}

export async function updateQuestion(bankId: number, questionId: number, updates: Partial<Question>): Promise<Question> {
  const { data } = await apiClient.put(`/api/questions/${questionId}`, updates)
  return data.data
}

export async function deleteQuestion(bankId: number, questionId: number): Promise<void> {
  await apiClient.delete(`/api/questions/${questionId}`)
}

// Knowledge tags
export async function getTags(): Promise<KnowledgeTag[]> {
  const { data } = await apiClient.get('/api/knowledge-tags')
  return data.data
}

export async function createTag(tag: Partial<KnowledgeTag>): Promise<KnowledgeTag> {
  const { data } = await apiClient.post('/api/knowledge-tags', tag)
  return data.data
}
