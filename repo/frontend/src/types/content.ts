export interface ContentRecord {
  id: number
  title: string
  description: string
  bodyText: string
  contentType: string
  standardizedTimestamp: string
  timezoneId: string
  normalizedAddress: string
  detectedLanguage: string
  price: number
  availabilityStart: string
  availabilityEnd: string
  popularityScore: number
  isPublished: boolean
  publishedAt: string
  sourceUrl: string
}

export interface MediaMetadata {
  id: number
  mediaType: string
  fileName: string
  fileSize: number
  mimeType: string
  width: number
  height: number
  durationSeconds: number
}
