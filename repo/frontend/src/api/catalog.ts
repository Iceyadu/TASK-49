import apiClient from '@/api/client'
import type { ContentRecord } from '@/types/content'
import type { CatalogSearchParams } from '@/types/catalog'

export async function searchCatalog(params: CatalogSearchParams): Promise<{ content: ContentRecord[]; totalElements: number; totalPages: number }> {
  const { data } = await apiClient.get('/api/catalog', { params })
  return data.data
}

export async function getCatalogItem(id: number): Promise<ContentRecord> {
  const { data } = await apiClient.get(`/api/catalog/${id}`)
  return data.data
}
