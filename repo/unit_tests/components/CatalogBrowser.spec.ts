import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import CatalogBrowser from '@/components/student/CatalogBrowser.vue'
import type { ContentRecord } from '@/types/content'

const sampleItems: ContentRecord[] = [
  {
    id: 1,
    title: 'Introduction to Algebra',
    description: 'Algebra foundations',
    bodyText: 'Body',
    contentType: 'TEXTBOOK',
    standardizedTimestamp: '2026-01-10T00:00:00Z',
    timezoneId: 'UTC',
    normalizedAddress: 'N/A',
    detectedLanguage: 'en',
    price: 29.99,
    availabilityStart: '2026-01-10T00:00:00Z',
    availabilityEnd: '2026-12-31T23:59:59Z',
    popularityScore: 10,
    isPublished: true,
    publishedAt: '2026-01-11T00:00:00Z',
    sourceUrl: 'https://example.com/a'
  },
  {
    id: 2,
    title: 'Practice Workbook',
    description: 'Printable workbook',
    bodyText: 'Body',
    contentType: 'WORKBOOK',
    standardizedTimestamp: '2026-02-10T00:00:00Z',
    timezoneId: 'UTC',
    normalizedAddress: 'N/A',
    detectedLanguage: 'en',
    price: 0,
    availabilityStart: '2026-02-10T00:00:00Z',
    availabilityEnd: '2026-12-31T23:59:59Z',
    popularityScore: 5,
    isPublished: true,
    publishedAt: '2026-02-11T00:00:00Z',
    sourceUrl: 'https://example.com/b'
  }
]

function mountCatalog(overrides: Record<string, unknown> = {}) {
  return mount(CatalogBrowser, {
    props: {
      items: sampleItems,
      loading: false,
      totalItems: 2,
      currentPage: 0,
      pageSize: 20,
      ...overrides
    },
    global: {
      stubs: {
        LoadingSpinner: { template: '<div class="loading-spinner-stub" />' },
        EmptyState: { template: '<div class="empty-state-stub" />' },
        PaginationBar: { template: '<div class="pagination-stub" />' }
      }
    }
  })
}

describe('CatalogBrowser Component', () => {
  it('renders catalog cards from real component props', () => {
    const wrapper = mountCatalog()
    expect(wrapper.findAll('.catalog-browser__card')).toHaveLength(2)
    expect(wrapper.text()).toContain('Introduction to Algebra')
  })

  it('shows Free label when item price is zero', () => {
    const wrapper = mountCatalog()
    expect(wrapper.find('.catalog-browser__card-price--free').text()).toBe('Free')
  })

  it('emits select when a card is clicked', async () => {
    const wrapper = mountCatalog()
    await wrapper.findAll('.catalog-browser__card')[0].trigger('click')
    expect(wrapper.emitted('select')).toBeTruthy()
    expect(wrapper.emitted('select')?.[0]?.[0]?.id).toBe(1)
  })

  it('emits structured search payload when query changes', async () => {
    const wrapper = mountCatalog()
    await wrapper.find('.catalog-browser__search-input').setValue('algebra')
    const payload = wrapper.emitted('search')?.at(-1)?.[0] as Record<string, unknown>
    expect(payload.query).toBe('algebra')
    expect(payload.sortBy).toBe('newest')
  })

  it('adds and removes keyword chips and emits updated search payload', async () => {
    const wrapper = mountCatalog()
    const keywordInput = wrapper.find('.catalog-browser__keyword-input')

    await keywordInput.setValue('math')
    await keywordInput.trigger('keydown.enter')
    let payload = wrapper.emitted('search')?.at(-1)?.[0] as Record<string, unknown>
    expect(payload.keywords).toEqual(['math'])

    await wrapper.find('.catalog-browser__chip-remove').trigger('click')
    payload = wrapper.emitted('search')?.at(-1)?.[0] as Record<string, unknown>
    expect(payload.keywords).toEqual([])
  })

  it('renders loading and empty states from real template conditions', () => {
    const loadingWrapper = mountCatalog({ loading: true, items: [] })
    expect(loadingWrapper.find('.loading-spinner-stub').exists()).toBe(true)

    const emptyWrapper = mountCatalog({ loading: false, items: [] })
    expect(emptyWrapper.find('.empty-state-stub').exists()).toBe(true)
  })
})
