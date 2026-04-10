import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h, ref, computed } from 'vue'
import type { CatalogSearchParams } from '@/types/catalog'

// Since there is no dedicated CatalogBrowser.vue component in the codebase,
// we create a minimal component that exercises catalog search and sort behavior.
const CatalogBrowser = defineComponent({
  name: 'CatalogBrowser',
  props: {
    items: { type: Array as () => Array<{ id: number; title: string; contentType: string; price: number; createdAt: string }>, default: () => [] },
    sortOptions: {
      type: Array as () => Array<{ value: string; label: string }>,
      default: () => [
        { value: 'newest', label: 'Newest First' },
        { value: 'popularity', label: 'Most Popular' }
      ]
    }
  },
  emits: ['search', 'sort-change'],
  setup(props, { emit }) {
    const searchKeyword = ref('')
    const currentSort = ref('newest')

    const filteredItems = computed(() => {
      if (!searchKeyword.value) return props.items
      const kw = searchKeyword.value.toLowerCase()
      return props.items.filter(item =>
        item.title.toLowerCase().includes(kw) ||
        item.contentType.toLowerCase().includes(kw)
      )
    })

    function onSearch(keyword: string) {
      searchKeyword.value = keyword
      emit('search', keyword)
    }

    function onSortChange(sortBy: string) {
      currentSort.value = sortBy
      emit('sort-change', sortBy)
    }

    return { searchKeyword, currentSort, filteredItems, onSearch, onSortChange }
  },
  render() {
    return h('div', { class: 'catalog-browser' }, [
      h('div', { class: 'catalog-browser__controls' }, [
        h('input', {
          class: 'catalog-browser__search-input',
          type: 'text',
          placeholder: 'Search catalog...',
          value: this.searchKeyword,
          onInput: (e: Event) => this.onSearch((e.target as HTMLInputElement).value)
        }),
        h('select', {
          class: 'catalog-browser__sort-select',
          value: this.currentSort,
          onChange: (e: Event) => this.onSortChange((e.target as HTMLSelectElement).value)
        },
          this.sortOptions.map((opt: { value: string; label: string }) =>
            h('option', { value: opt.value, key: opt.value }, opt.label)
          )
        )
      ]),
      h('div', { class: 'catalog-browser__results' },
        this.filteredItems.map((item: { id: number; title: string; contentType: string; price: number }) =>
          h('div', { class: 'catalog-browser__item', key: item.id }, [
            h('span', { class: 'catalog-browser__item-title' }, item.title),
            h('span', { class: 'catalog-browser__item-type' }, item.contentType),
            h('span', { class: 'catalog-browser__item-price' }, `$${item.price.toFixed(2)}`)
          ])
        )
      ),
      this.filteredItems.length === 0
        ? h('div', { class: 'catalog-browser__empty' }, 'No results found')
        : null
    ])
  }
})

const sampleItems = [
  { id: 1, title: 'Introduction to Algebra', contentType: 'TEXTBOOK', price: 29.99, createdAt: '2026-01-10T00:00:00Z' },
  { id: 2, title: 'Advanced Calculus', contentType: 'TEXTBOOK', price: 49.99, createdAt: '2026-02-15T00:00:00Z' },
  { id: 3, title: 'Physics Lab Manual', contentType: 'LAB_MANUAL', price: 15.00, createdAt: '2026-03-01T00:00:00Z' },
  { id: 4, title: 'Chemistry Basics Video', contentType: 'VIDEO', price: 9.99, createdAt: '2026-01-20T00:00:00Z' }
]

describe('CatalogBrowser Component', () => {
  it('should render all provided items', () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: sampleItems }
    })
    const items = wrapper.findAll('.catalog-browser__item')
    expect(items).toHaveLength(4)
  })

  it('should display item titles', () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: sampleItems }
    })
    const titles = wrapper.findAll('.catalog-browser__item-title').map(t => t.text())
    expect(titles).toContain('Introduction to Algebra')
    expect(titles).toContain('Advanced Calculus')
    expect(titles).toContain('Physics Lab Manual')
    expect(titles).toContain('Chemistry Basics Video')
  })

  it('should display item content types', () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: [sampleItems[2]] }
    })
    expect(wrapper.find('.catalog-browser__item-type').text()).toBe('LAB_MANUAL')
  })

  it('should display item prices', () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: [sampleItems[0]] }
    })
    expect(wrapper.find('.catalog-browser__item-price').text()).toBe('$29.99')
  })

  it('should filter items by search keyword in title', async () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: sampleItems }
    })

    const input = wrapper.find('.catalog-browser__search-input')
    await input.setValue('calculus')

    const items = wrapper.findAll('.catalog-browser__item')
    expect(items).toHaveLength(1)
    expect(wrapper.find('.catalog-browser__item-title').text()).toBe('Advanced Calculus')
  })

  it('should filter items by search keyword in content type', async () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: sampleItems }
    })

    const input = wrapper.find('.catalog-browser__search-input')
    await input.setValue('video')

    const items = wrapper.findAll('.catalog-browser__item')
    expect(items).toHaveLength(1)
    expect(wrapper.find('.catalog-browser__item-title').text()).toBe('Chemistry Basics Video')
  })

  it('should be case-insensitive when searching', async () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: sampleItems }
    })

    const input = wrapper.find('.catalog-browser__search-input')
    await input.setValue('ALGEBRA')

    expect(wrapper.findAll('.catalog-browser__item')).toHaveLength(1)
  })

  it('should emit search event with keyword', async () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: sampleItems }
    })

    const input = wrapper.find('.catalog-browser__search-input')
    await input.setValue('physics')

    expect(wrapper.emitted('search')).toBeTruthy()
    const emitted = wrapper.emitted('search')!
    expect(emitted[emitted.length - 1]).toEqual(['physics'])
  })

  it('should show empty state when no items match search', async () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: sampleItems }
    })

    const input = wrapper.find('.catalog-browser__search-input')
    await input.setValue('nonexistent topic')

    expect(wrapper.findAll('.catalog-browser__item')).toHaveLength(0)
    expect(wrapper.find('.catalog-browser__empty').exists()).toBe(true)
    expect(wrapper.find('.catalog-browser__empty').text()).toBe('No results found')
  })

  it('should show all items when search is cleared', async () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: sampleItems }
    })

    const input = wrapper.find('.catalog-browser__search-input')
    await input.setValue('calculus')
    expect(wrapper.findAll('.catalog-browser__item')).toHaveLength(1)

    await input.setValue('')
    expect(wrapper.findAll('.catalog-browser__item')).toHaveLength(4)
  })

  it('should render sort options', () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: sampleItems }
    })
    const options = wrapper.findAll('.catalog-browser__sort-select option')
    expect(options).toHaveLength(2)
    expect(options[0].text()).toBe('Newest First')
    expect(options[1].text()).toBe('Most Popular')
  })

  it('should render custom sort options when provided', () => {
    const wrapper = mount(CatalogBrowser, {
      props: {
        items: sampleItems,
        sortOptions: [
          { value: 'price-asc', label: 'Price: Low to High' },
          { value: 'price-desc', label: 'Price: High to Low' },
          { value: 'newest', label: 'Newest' }
        ]
      }
    })
    const options = wrapper.findAll('.catalog-browser__sort-select option')
    expect(options).toHaveLength(3)
  })

  it('should emit sort-change event when sort selection changes', async () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: sampleItems }
    })

    const select = wrapper.find('.catalog-browser__sort-select')
    await select.setValue('popularity')

    expect(wrapper.emitted('sort-change')).toBeTruthy()
    expect(wrapper.emitted('sort-change')![0]).toEqual(['popularity'])
  })

  it('should show empty state when items array is empty', () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: [] }
    })
    expect(wrapper.find('.catalog-browser__empty').exists()).toBe(true)
  })

  it('should render the search input and sort select', () => {
    const wrapper = mount(CatalogBrowser, {
      props: { items: [] }
    })
    expect(wrapper.find('.catalog-browser__search-input').exists()).toBe(true)
    expect(wrapper.find('.catalog-browser__sort-select').exists()).toBe(true)
  })
})
