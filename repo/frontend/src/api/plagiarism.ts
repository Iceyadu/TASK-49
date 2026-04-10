import apiClient from '@/api/client'
import type { PlagiarismCheck, PlagiarismMatch } from '@/types/plagiarism'

export async function getChecks(submissionId?: number, page = 0, size = 20): Promise<{ content: PlagiarismCheck[]; totalElements: number; totalPages: number }> {
  const params: any = { page, size }
  if (submissionId) params.submissionId = submissionId
  const { data } = await apiClient.get('/api/plagiarism/checks', { params })
  return data.data
}

export async function getCheck(id: number): Promise<PlagiarismCheck> {
  const { data } = await apiClient.get(`/api/plagiarism/checks/${id}`)
  return data.data
}

export async function getMatches(checkId: number): Promise<PlagiarismMatch[]> {
  const { data } = await apiClient.get(`/api/plagiarism/checks/${checkId}/matches`)
  return data.data
}
