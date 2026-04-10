import apiClient from '@/api/client'
import type { ContentRecord, MediaMetadata } from '@/types/content'

export async function getContent(page = 0, size = 20, published?: boolean): Promise<{ content: ContentRecord[]; totalElements: number; totalPages: number }> {
  const params: any = { page, size }
  if (published !== undefined) params.published = published
  const { data } = await apiClient.get('/api/content', { params })
  return data.data
}

export async function getContentById(id: number): Promise<ContentRecord> {
  const { data } = await apiClient.get(`/api/content/${id}`)
  return data.data
}

export async function publishContent(id: number): Promise<ContentRecord> {
  const { data } = await apiClient.post(`/api/content/${id}/publish`)
  return data.data
}

export async function getMediaMetadata(contentId: number): Promise<MediaMetadata[]> {
  const { data } = await apiClient.get(`/api/content/media-metadata/${contentId}`)
  return data.data
}
