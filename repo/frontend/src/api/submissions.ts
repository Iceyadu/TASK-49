import apiClient from '@/api/client'
import type { Submission, AutosavePayload } from '@/types/submission'

export async function startSubmission(quizPaperId: number): Promise<Submission> {
  const { data } = await apiClient.post(`/api/quizzes/${quizPaperId}/submissions`)
  return data.data
}

export async function autosave(submissionId: number, payload: AutosavePayload): Promise<void> {
  await apiClient.put(`/api/submissions/${submissionId}/autosave`, payload)
}

export async function submitSubmission(submissionId: number): Promise<Submission> {
  const { data } = await apiClient.put(`/api/submissions/${submissionId}/submit`)
  return data.data
}

export async function getSubmission(submissionId: number): Promise<Submission> {
  const { data } = await apiClient.get(`/api/submissions/${submissionId}`)
  return data.data
}

export async function getFeedback(submissionId: number): Promise<Submission> {
  const { data } = await apiClient.get(`/api/submissions/${submissionId}/feedback`)
  return data.data
}
