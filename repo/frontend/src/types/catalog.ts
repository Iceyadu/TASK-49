export interface CatalogSearchParams {
  keyword?: string
  contentType?: string
  minPrice?: number
  maxPrice?: number
  availabilityStart?: string
  availabilityEnd?: string
  sortBy?: 'newest' | 'popularity'
  sortDirection?: 'asc' | 'desc'
  page?: number
  size?: number
}
