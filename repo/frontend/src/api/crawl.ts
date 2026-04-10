import apiClient from '@/api/client'
import type { CrawlSource, CrawlRuleVersion, CrawlRun } from '@/types/crawl'

// Source profiles
export async function getSources(): Promise<CrawlSource[]> {
  const { data } = await apiClient.get('/api/crawl-sources')
  return data.data ?? []
}

export async function createSource(source: Partial<CrawlSource>): Promise<CrawlSource> {
  const payload = {
    name: source.name,
    baseUrl: source.baseUrl,
    description: source.description,
    rateLimitPerMinute: source.rateLimitPerMinute,
    requiresAuth: source.requiresAuth,
  }
  const { data } = await apiClient.post('/api/crawl-sources', payload)
  return data.data
}

export async function getSource(id: number): Promise<CrawlSource> {
  const { data } = await apiClient.get(`/api/crawl-sources/${id}`)
  return data.data
}

export async function updateSource(id: number, updates: Partial<CrawlSource>): Promise<CrawlSource> {
  const payload = {
    name: updates.name,
    baseUrl: updates.baseUrl,
    description: updates.description,
    rateLimitPerMinute: updates.rateLimitPerMinute,
    requiresAuth: updates.requiresAuth,
  }
  const { data } = await apiClient.put(`/api/crawl-sources/${id}`, payload)
  return data.data
}

export async function deleteSource(id: number): Promise<void> {
  await apiClient.delete(`/api/crawl-sources/${id}`)
}

// Rule versions
export async function getRules(sourceId: number): Promise<CrawlRuleVersion[]> {
  const { data } = await apiClient.get(`/api/crawl-sources/${sourceId}/rules`)
  return data.data
}

export async function createRule(
  sourceId: number,
  rule: Partial<Omit<CrawlRuleVersion, 'ruleDefinition' | 'fieldMappings'>> & {
    ruleDefinition?: string | Record<string, unknown>
    fieldMappings?: string | Record<string, unknown>
  }
): Promise<CrawlRuleVersion> {
  const payload = {
    extractionMethod: rule.extractionMethod,
    ruleDefinition: rule.ruleDefinition,
    fieldMappings: rule.fieldMappings,
    typeValidations: rule.typeValidations,
    notes: rule.notes,
  }
  const { data } = await apiClient.post(`/api/crawl-sources/${sourceId}/rules`, payload)
  return data.data
}

export async function revertRule(ruleId: number, targetVersionId: number): Promise<CrawlRuleVersion> {
  const { data } = await apiClient.post(`/api/crawl-rules/${ruleId}/revert/${targetVersionId}`)
  return data.data
}

export async function testExtraction(sampleUrl: string, extractionMethod: string, ruleDefinition: Record<string, unknown>, fieldMappings?: Record<string, string>): Promise<any> {
  const { data } = await apiClient.post('/api/crawl-rules/test-extraction', {
    sampleUrl,
    extractionMethod,
    ruleDefinition,
    fieldMappings,
  })
  return data.data
}

// Crawl runs
export async function getRuns(sourceId?: number, page = 0, size = 20): Promise<{ content: CrawlRun[]; totalElements: number; totalPages: number }> {
  const params: any = { page, size }
  if (sourceId) params.sourceId = sourceId
  const { data } = await apiClient.get('/api/crawl-runs', { params })
  return data.data
}

export async function startRun(sourceProfileId: number, ruleVersionId: number): Promise<CrawlRun> {
  const { data } = await apiClient.post('/api/crawl-runs', { sourceProfileId, ruleVersionId })
  return data.data
}

export async function cancelRun(runId: number): Promise<void> {
  await apiClient.post(`/api/crawl-runs/${runId}/cancel`)
}
