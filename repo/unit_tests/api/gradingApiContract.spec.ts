import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock the API client before importing the grading API module
const mockGet = vi.fn().mockResolvedValue({ data: { data: {} } })
const mockPost = vi.fn().mockResolvedValue({ data: { data: {} } })

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: any[]) => mockGet(...args),
    post: (...args: any[]) => mockPost(...args),
  }
}))

import {
  getGradingQueue,
  getGradingState,
  gradeItem,
  addRubricScores
} from '@/api/grading'

describe('Grading API Contract Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('Grading queue operations', () => {
    it('getGradingQueue sends GET to /api/grading/queue with page/size params', async () => {
      await getGradingQueue(0, 20)

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/api/grading/queue', {
        params: { page: 0, size: 20 }
      })
    })

    it('getGradingQueue uses default page=0 and size=20', async () => {
      await getGradingQueue()

      expect(mockGet).toHaveBeenCalledWith('/api/grading/queue', {
        params: { page: 0, size: 20 }
      })
    })

    it('getGradingQueue passes custom page and size params', async () => {
      await getGradingQueue(3, 50)

      expect(mockGet).toHaveBeenCalledWith('/api/grading/queue', {
        params: { page: 3, size: 50 }
      })
    })

    it('getGradingQueue passes status filter when provided', async () => {
      await getGradingQueue(0, 20, 'PENDING')

      expect(mockGet).toHaveBeenCalledWith('/api/grading/queue', {
        params: { page: 0, size: 20, status: 'PENDING' }
      })
    })

    it('getGradingState sends GET to /api/grading/submissions/{id}', async () => {
      await getGradingState(42)

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/api/grading/submissions/42')
    })
  })

  describe('Grading submission operations', () => {
    it('gradeItem sends POST to /api/grading/submissions/{id}/grade', async () => {
      await gradeItem(7, 85, 'Good work')

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/api/grading/submissions/7/grade', {
        score: 85,
        feedback: 'Good work'
      })
    })

    it('gradeItem sends { score, feedback } payload - not wrapped in extra object', async () => {
      await gradeItem(1, 90, 'Excellent')

      const payload = mockPost.mock.calls[0][1]
      expect(payload).toEqual({ score: 90, feedback: 'Excellent' })
      expect(Object.keys(payload)).toHaveLength(2)
      expect(payload).toHaveProperty('score')
      expect(payload).toHaveProperty('feedback')
    })

    it('addRubricScores sends POST to /api/grading/submissions/{id}/rubric-scores', async () => {
      const rubricScores = [
        { criterionId: 1, score: 10 },
        { criterionId: 2, score: 8 }
      ]
      await addRubricScores(5, rubricScores)

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith(
        '/api/grading/submissions/5/rubric-scores',
        rubricScores
      )
    })

    it('addRubricScores sends a raw array - not wrapped in an object', async () => {
      const rubricScores = [
        { criterionId: 1, score: 10 },
        { criterionId: 2, score: 8 }
      ]
      await addRubricScores(5, rubricScores)

      const payload = mockPost.mock.calls[0][1]
      // The payload must be an array, not wrapped in { rubricScores: [...] } or similar
      expect(Array.isArray(payload)).toBe(true)
      expect(payload).toHaveLength(2)
      expect(payload).toEqual(rubricScores)
    })
  })

  describe('API endpoint URL patterns', () => {
    it('all grading endpoints are under /api/grading/', async () => {
      await getGradingQueue()
      await getGradingState(1)
      await gradeItem(1, 100, 'Perfect')
      await addRubricScores(1, [])

      const allCalls = [...mockGet.mock.calls, ...mockPost.mock.calls]
      for (const call of allCalls) {
        const url = call[0] as string
        expect(url).toMatch(/^\/api\/grading\//)
      }
    })

    it('submission-specific endpoints include /submissions/{id}', async () => {
      await getGradingState(99)
      await gradeItem(99, 50, 'Needs improvement')
      await addRubricScores(99, [])

      const allCalls = [...mockGet.mock.calls, ...mockPost.mock.calls]
      for (const call of allCalls) {
        const url = call[0] as string
        expect(url).toMatch(/\/submissions\/99/)
      }
    })
  })

  describe('HTTP method verification', () => {
    it('all read operations use GET', async () => {
      await getGradingQueue()
      await getGradingState(1)

      expect(mockGet).toHaveBeenCalledTimes(2)
      expect(mockPost).not.toHaveBeenCalled()
    })

    it('all write operations use POST', async () => {
      await gradeItem(1, 100, 'Done')
      await addRubricScores(1, [])

      expect(mockPost).toHaveBeenCalledTimes(2)
      expect(mockGet).not.toHaveBeenCalled()
    })
  })
})
