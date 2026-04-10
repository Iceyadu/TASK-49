export interface CrawlSource {
  id: number
  name: string
  baseUrl: string
  description: string
  rateLimitPerMinute: number
  requiresAuth: boolean
  enabled: boolean
  createdAt: string
}

export interface CrawlRuleVersion {
  id: number
  sourceProfileId: number
  versionNumber: number
  extractionMethod: string
  ruleDefinition: string
  fieldMappings: string
  typeValidations: string
  isActive: boolean
  createdAt: string
  notes: string
}

export interface CrawlRun {
  id: number
  sourceProfileId: number
  ruleVersionId: number
  status: string
  startedAt: string
  completedAt: string
  totalPages: number
  pagesCrawled: number
  pagesFailed: number
  itemsExtracted: number
  errorLog: string
}
