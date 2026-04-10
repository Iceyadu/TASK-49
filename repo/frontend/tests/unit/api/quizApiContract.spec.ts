import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock the API client before importing the quiz API module
const mockGet = vi.fn().mockResolvedValue({ data: { data: {} } })
const mockPost = vi.fn().mockResolvedValue({ data: { data: {} } })
const mockPut = vi.fn().mockResolvedValue({ data: { data: {} } })
const mockDelete = vi.fn().mockResolvedValue({ data: {} })

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: any[]) => mockGet(...args),
    post: (...args: any[]) => mockPost(...args),
    put: (...args: any[]) => mockPut(...args),
    delete: (...args: any[]) => mockDelete(...args)
  }
}))

import {
  assembleQuiz,
  getQuizzes,
  getQuiz,
  scheduleQuiz,
  publishQuiz,
  getQuestionBanks,
  createQuestionBank,
  addQuestion,
  updateQuestion,
  deleteQuestion,
  getTags,
  createTag
} from '@/api/quiz'

describe('Quiz API Contract Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('Quiz paper operations', () => {
    it('assembleQuiz sends POST to /api/quizzes/assemble with full request object', async () => {
      const request = {
        title: 'Midterm',
        questionBankId: 1,
        totalQuestions: 20,
        rules: [{ ruleType: 'DIFFICULTY', difficultyLevel: 3, minCount: 5, maxCount: 10 }],
      }
      await assembleQuiz(request as any)

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/api/quizzes/assemble', request)
    })

    it('assembleQuiz sends questionBankId not bankId', async () => {
      const request = {
        title: 'Test',
        questionBankId: 42,
        totalQuestions: 10,
      }
      await assembleQuiz(request as any)

      const payload = mockPost.mock.calls[0][1]
      expect(payload).toHaveProperty('questionBankId', 42)
      expect(payload).not.toHaveProperty('bankId')
    })

    it('assembleQuiz requires totalQuestions in payload', async () => {
      const request = {
        title: 'Test',
        questionBankId: 1,
        totalQuestions: 15,
      }
      await assembleQuiz(request as any)

      const payload = mockPost.mock.calls[0][1]
      expect(payload).toHaveProperty('totalQuestions', 15)
    })

    it('getQuizzes sends GET to /api/quizzes with page/size params', async () => {
      await getQuizzes(0, 20)

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/api/quizzes', {
        params: { page: 0, size: 20 }
      })
    })

    it('getQuizzes uses default page=0 and size=20', async () => {
      await getQuizzes()

      expect(mockGet).toHaveBeenCalledWith('/api/quizzes', {
        params: { page: 0, size: 20 }
      })
    })

    it('getQuizzes passes custom page and size params', async () => {
      await getQuizzes(2, 50)

      expect(mockGet).toHaveBeenCalledWith('/api/quizzes', {
        params: { page: 2, size: 50 }
      })
    })

    it('getQuiz sends GET to /api/quizzes/{id}', async () => {
      await getQuiz(42)

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/api/quizzes/42')
    })

    it('scheduleQuiz sends PUT to /api/quizzes/{id}/schedule', async () => {
      await scheduleQuiz(7, '2026-05-01T09:00:00', '2026-05-01T10:00:00')

      expect(mockPut).toHaveBeenCalledTimes(1)
      expect(mockPut).toHaveBeenCalledWith('/api/quizzes/7/schedule', {
        releaseStart: '2026-05-01T09:00:00',
        releaseEnd: '2026-05-01T10:00:00'
      })
    })

    it('publishQuiz sends PUT to /api/quizzes/{id}/publish', async () => {
      await publishQuiz(5)

      expect(mockPut).toHaveBeenCalledTimes(1)
      expect(mockPut).toHaveBeenCalledWith('/api/quizzes/5/publish')
    })
  })

  describe('Question bank operations', () => {
    it('getQuestionBanks sends GET to /api/question-banks with page/size params', async () => {
      await getQuestionBanks(0, 20)

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/api/question-banks', {
        params: { page: 0, size: 20 }
      })
    })

    it('getQuestionBanks uses default page=0 and size=20', async () => {
      await getQuestionBanks()

      expect(mockGet).toHaveBeenCalledWith('/api/question-banks', {
        params: { page: 0, size: 20 }
      })
    })

    it('createQuestionBank sends POST to /api/question-banks', async () => {
      const bank = { name: 'CS101 Bank', description: 'Computer Science basics' }
      await createQuestionBank(bank)

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/api/question-banks', bank)
    })

    it('addQuestion sends POST to /api/question-banks/{bankId}/questions', async () => {
      const question = { text: 'What is 2+2?', type: 'MULTIPLE_CHOICE' }
      await addQuestion(10, question as any)

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/api/question-banks/10/questions', question)
    })
  })

  describe('Question operations', () => {
    it('updateQuestion sends PUT to /api/questions/{questionId}', async () => {
      const updates = { text: 'Updated question text' }
      await updateQuestion(10, 99, updates as any)

      expect(mockPut).toHaveBeenCalledTimes(1)
      expect(mockPut).toHaveBeenCalledWith('/api/questions/99', updates)
    })

    it('deleteQuestion sends DELETE to /api/questions/{questionId}', async () => {
      await deleteQuestion(10, 88)

      expect(mockDelete).toHaveBeenCalledTimes(1)
      expect(mockDelete).toHaveBeenCalledWith('/api/questions/88')
    })
  })

  describe('Knowledge tag operations', () => {
    it('getTags sends GET to /api/knowledge-tags', async () => {
      await getTags()

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/api/knowledge-tags')
    })

    it('createTag sends POST to /api/knowledge-tags', async () => {
      const tag = { name: 'Algebra', description: 'Algebra topics' }
      await createTag(tag as any)

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/api/knowledge-tags', tag)
    })
  })

  describe('HTTP method verification', () => {
    it('all read operations use GET', async () => {
      await getQuizzes()
      await getQuiz(1)
      await getQuestionBanks()
      await getTags()

      expect(mockGet).toHaveBeenCalledTimes(4)
      expect(mockPost).not.toHaveBeenCalled()
      expect(mockPut).not.toHaveBeenCalled()
      expect(mockDelete).not.toHaveBeenCalled()
    })

    it('assembleQuiz uses POST for create operation', async () => {
      await assembleQuiz({ title: 'Test', questionBankId: 1, totalQuestions: 10 } as any)
      await createQuestionBank({ name: 'Bank' })
      await addQuestion(1, {} as any)
      await createTag({ name: 'Tag' } as any)

      expect(mockPost).toHaveBeenCalledTimes(4)
      expect(mockGet).not.toHaveBeenCalled()
    })

    it('scheduleQuiz and publishQuiz use PUT for update operations', async () => {
      await scheduleQuiz(1, '2026-01-01', '2026-01-02')
      await publishQuiz(1)

      expect(mockPut).toHaveBeenCalledTimes(2)
      expect(mockPost).not.toHaveBeenCalled()
    })

    it('deleteQuestion uses DELETE', async () => {
      await deleteQuestion(1, 1)

      expect(mockDelete).toHaveBeenCalledTimes(1)
    })
  })
})
