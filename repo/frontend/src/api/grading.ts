import apiClient from '@/api/client'
import type { GradingState, RubricScore } from '@/types/grading'

export async function getGradingQueue(page = 0, size = 20, status?: string): Promise<{ content: GradingState[]; totalElements: number; totalPages: number }> {
  const params: any = { page, size }
  if (status) params.status = status
  const { data } = await apiClient.get('/api/grading/queue', { params })
  return data.data
}

export async function getGradingState(id: number): Promise<GradingState> {
  const { data } = await apiClient.get(`/api/grading/submissions/${id}`)
  return data.data
}

export async function gradeItem(id: number, score: number, feedback: string): Promise<GradingState> {
  const { data } = await apiClient.post(`/api/grading/submissions/${id}/grade`, { score, feedback })
  return data.data
}

export async function addRubricScores(gradingId: number, rubricScores: Partial<RubricScore>[]): Promise<GradingState> {
  const { data } = await apiClient.post(`/api/grading/submissions/${gradingId}/rubric-scores`, rubricScores)
  return data.data
}
