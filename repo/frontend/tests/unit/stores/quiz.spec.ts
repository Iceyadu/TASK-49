import { describe, it, expect, beforeEach, vi } from 'vitest'
import type { QuizPaper, QuestionBank, Question, QuizRule } from '@/types/quiz'

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

import apiClient from '@/api/client'
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

const mockedClient = vi.mocked(apiClient)

describe('Quiz API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('assembleQuiz', () => {
    it('should POST to the assemble endpoint with questionBankId and totalQuestions', async () => {
      const mockQuiz: QuizPaper = {
        id: 1,
        title: 'Midterm Quiz',
        description: '',
        totalQuestions: 10,
        totalPoints: 100,
        timeLimitMinutes: 60,
        maxAttempts: 1,
        releaseStart: '2026-05-01T09:00:00Z',
        releaseEnd: '2026-05-01T10:00:00Z',
        isPublished: false,
        questions: []
      }
      mockedClient.post.mockResolvedValue({ data: { data: mockQuiz } })

      const rules: QuizRule[] = [{ ruleType: 'DIFFICULTY', minCount: 5, difficultyLevel: 3 }]
      const result = await assembleQuiz({
        title: 'Midterm Quiz',
        questionBankId: 1,
        totalQuestions: 10,
        rules
      })

      expect(mockedClient.post).toHaveBeenCalledWith('/api/quizzes/assemble', {
        title: 'Midterm Quiz',
        questionBankId: 1,
        totalQuestions: 10,
        rules
      })
      expect(result).toEqual(mockQuiz)
    })
  })

  describe('getQuizzes', () => {
    it('should GET paginated quizzes with default params', async () => {
      const mockResponse = { content: [], totalElements: 0, totalPages: 0 }
      mockedClient.get.mockResolvedValue({ data: { data: mockResponse } })

      const result = await getQuizzes()
      expect(mockedClient.get).toHaveBeenCalledWith('/api/quizzes', { params: { page: 0, size: 20 } })
      expect(result).toEqual(mockResponse)
    })

    it('should pass custom page and size params', async () => {
      const mockResponse = { content: [], totalElements: 50, totalPages: 5 }
      mockedClient.get.mockResolvedValue({ data: { data: mockResponse } })

      const result = await getQuizzes(2, 10)
      expect(mockedClient.get).toHaveBeenCalledWith('/api/quizzes', { params: { page: 2, size: 10 } })
      expect(result.totalElements).toBe(50)
    })
  })

  describe('getQuiz', () => {
    it('should GET a single quiz by id', async () => {
      const mockQuiz: QuizPaper = {
        id: 42,
        title: 'Final Exam',
        description: 'End of term',
        totalQuestions: 20,
        totalPoints: 200,
        timeLimitMinutes: 120,
        maxAttempts: 1,
        releaseStart: '2026-06-01T09:00:00Z',
        releaseEnd: '2026-06-01T12:00:00Z',
        isPublished: true,
        questions: []
      }
      mockedClient.get.mockResolvedValue({ data: { data: mockQuiz } })

      const result = await getQuiz(42)
      expect(mockedClient.get).toHaveBeenCalledWith('/api/quizzes/42')
      expect(result.id).toBe(42)
      expect(result.title).toBe('Final Exam')
    })
  })

  describe('scheduleQuiz', () => {
    it('should PUT schedule dates for a quiz', async () => {
      const mockQuiz = { id: 1, releaseStart: '2026-05-01T09:00:00Z', releaseEnd: '2026-05-01T10:00:00Z' }
      mockedClient.put.mockResolvedValue({ data: { data: mockQuiz } })

      const result = await scheduleQuiz(1, '2026-05-01T09:00:00Z', '2026-05-01T10:00:00Z')
      expect(mockedClient.put).toHaveBeenCalledWith('/api/quizzes/1/schedule', {
        releaseStart: '2026-05-01T09:00:00Z',
        releaseEnd: '2026-05-01T10:00:00Z'
      })
      expect(result).toEqual(mockQuiz)
    })
  })

  describe('publishQuiz', () => {
    it('should PUT to publish a quiz', async () => {
      const mockQuiz = { id: 1, isPublished: true }
      mockedClient.put.mockResolvedValue({ data: { data: mockQuiz } })

      const result = await publishQuiz(1)
      expect(mockedClient.put).toHaveBeenCalledWith('/api/quizzes/1/publish')
      expect(result).toEqual(mockQuiz)
    })
  })

  describe('getQuestionBanks', () => {
    it('should GET paginated question banks', async () => {
      const mockResponse = {
        content: [{ id: 1, name: 'Math Bank', description: 'Algebra', subject: 'Math', questions: [] }],
        totalElements: 1,
        totalPages: 1
      }
      mockedClient.get.mockResolvedValue({ data: { data: mockResponse } })

      const result = await getQuestionBanks()
      expect(mockedClient.get).toHaveBeenCalledWith('/api/question-banks', { params: { page: 0, size: 20 } })
      expect(result.content).toHaveLength(1)
      expect(result.content[0].name).toBe('Math Bank')
    })
  })

  describe('createQuestionBank', () => {
    it('should POST a new question bank', async () => {
      const newBank = { name: 'Physics Bank', description: 'Mechanics', subject: 'Physics' }
      const mockBank: QuestionBank = { id: 2, ...newBank, questions: [] }
      mockedClient.post.mockResolvedValue({ data: { data: mockBank } })

      const result = await createQuestionBank(newBank)
      expect(mockedClient.post).toHaveBeenCalledWith('/api/question-banks', newBank)
      expect(result.id).toBe(2)
    })
  })

  describe('addQuestion', () => {
    it('should POST a new question to a bank', async () => {
      const questionData: Partial<Question> = {
        questionType: 'MULTIPLE_CHOICE',
        questionText: 'What is 2+2?',
        difficultyLevel: 1,
        options: '["3","4","5","6"]',
        correctAnswer: '4',
        explanation: 'Basic addition',
        points: 10
      }
      const mockQuestion = { id: 100, ...questionData, knowledgeTags: [] }
      mockedClient.post.mockResolvedValue({ data: { data: mockQuestion } })

      const result = await addQuestion(1, questionData)
      expect(mockedClient.post).toHaveBeenCalledWith('/api/question-banks/1/questions', questionData)
      expect(result.id).toBe(100)
    })
  })

  describe('updateQuestion', () => {
    it('should PUT updates to an existing question', async () => {
      const updates = { questionText: 'Updated question text' }
      const mockUpdated = { id: 100, questionText: 'Updated question text' }
      mockedClient.put.mockResolvedValue({ data: { data: mockUpdated } })

      const result = await updateQuestion(1, 100, updates)
      expect(mockedClient.put).toHaveBeenCalledWith('/api/questions/100', updates)
      expect(result.questionText).toBe('Updated question text')
    })
  })

  describe('deleteQuestion', () => {
    it('should DELETE a question from a bank', async () => {
      mockedClient.delete.mockResolvedValue({})

      await deleteQuestion(1, 100)
      expect(mockedClient.delete).toHaveBeenCalledWith('/api/questions/100')
    })
  })

  describe('getTags', () => {
    it('should GET all knowledge tags', async () => {
      const mockTags = [
        { id: 1, name: 'algebra', category: 'math' },
        { id: 2, name: 'mechanics', category: 'physics' }
      ]
      mockedClient.get.mockResolvedValue({ data: { data: mockTags } })

      const result = await getTags()
      expect(mockedClient.get).toHaveBeenCalledWith('/api/knowledge-tags')
      expect(result).toHaveLength(2)
    })
  })

  describe('createTag', () => {
    it('should POST a new knowledge tag', async () => {
      const tagData = { name: 'calculus', category: 'math' }
      const mockTag = { id: 3, ...tagData }
      mockedClient.post.mockResolvedValue({ data: { data: mockTag } })

      const result = await createTag(tagData)
      expect(mockedClient.post).toHaveBeenCalledWith('/api/knowledge-tags', tagData)
      expect(result.id).toBe(3)
    })
  })
})

describe('Quiz Types', () => {
  it('should define a valid QuizPaper shape', () => {
    const quiz: QuizPaper = {
      id: 1,
      title: 'Test Quiz',
      description: 'A test',
      totalQuestions: 5,
      totalPoints: 50,
      timeLimitMinutes: 30,
      maxAttempts: 2,
      releaseStart: '2026-01-01T00:00:00Z',
      releaseEnd: '2026-01-02T00:00:00Z',
      isPublished: false,
      questions: []
    }
    expect(quiz.id).toBe(1)
    expect(quiz.timeLimitMinutes).toBe(30)
    expect(quiz.questions).toEqual([])
  })

  it('should define a valid QuizRule shape', () => {
    const rule: QuizRule = {
      ruleType: 'DIFFICULTY',
      minCount: 2,
      maxCount: 5,
      difficultyLevel: 3
    }
    expect(rule.ruleType).toBe('DIFFICULTY')
    expect(rule.minCount).toBe(2)
  })

  it('should define a valid Question shape', () => {
    const question: Question = {
      id: 10,
      questionType: 'MULTIPLE_CHOICE',
      difficultyLevel: 2,
      questionText: 'What is Vue?',
      options: '["A framework","A library"]',
      correctAnswer: 'A framework',
      explanation: 'Vue is a progressive framework.',
      points: 5,
      knowledgeTags: [{ id: 1, name: 'vue', category: 'frontend' }]
    }
    expect(question.knowledgeTags).toHaveLength(1)
    expect(question.points).toBe(5)
  })
})
