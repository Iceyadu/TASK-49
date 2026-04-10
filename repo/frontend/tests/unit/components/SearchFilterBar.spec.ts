import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import SearchFilterBar from '@/components/common/SearchFilterBar.vue'

describe('SearchFilterBar Component', () => {
  it('should render with default placeholder', () => {
    const wrapper = mount(SearchFilterBar)
    const input = wrapper.find('.search-filter-bar__input')
    expect(input.attributes('placeholder')).toBe('Search...')
  })

  it('should render with custom placeholder', () => {
    const wrapper = mount(SearchFilterBar, {
      props: { placeholder: 'Find courses...' }
    })
    const input = wrapper.find('.search-filter-bar__input')
    expect(input.attributes('placeholder')).toBe('Find courses...')
  })

  it('should display the current search query', () => {
    const wrapper = mount(SearchFilterBar, {
      props: { searchQuery: 'algebra' }
    })
    const input = wrapper.find('.search-filter-bar__input')
    expect((input.element as HTMLInputElement).value).toBe('algebra')
  })

  it('should emit update:searchQuery when user types in the input', async () => {
    const wrapper = mount(SearchFilterBar, {
      props: { searchQuery: '' }
    })
    const input = wrapper.find('.search-filter-bar__input')
    await input.setValue('calculus')

    expect(wrapper.emitted('update:searchQuery')).toBeTruthy()
    const emitted = wrapper.emitted('update:searchQuery')!
    expect(emitted[emitted.length - 1]).toEqual(['calculus'])
  })

  it('should show the clear button when searchQuery is not empty', () => {
    const wrapper = mount(SearchFilterBar, {
      props: { searchQuery: 'something' }
    })
    expect(wrapper.find('.search-filter-bar__clear').exists()).toBe(true)
  })

  it('should not show the clear button when searchQuery is empty', () => {
    const wrapper = mount(SearchFilterBar, {
      props: { searchQuery: '' }
    })
    expect(wrapper.find('.search-filter-bar__clear').exists()).toBe(false)
  })

  it('should emit update:searchQuery with empty string when clear is clicked', async () => {
    const wrapper = mount(SearchFilterBar, {
      props: { searchQuery: 'test' }
    })
    await wrapper.find('.search-filter-bar__clear').trigger('click')

    const emitted = wrapper.emitted('update:searchQuery')!
    expect(emitted[emitted.length - 1]).toEqual([''])
  })

  it('should not render filters section when filters array is empty', () => {
    const wrapper = mount(SearchFilterBar, {
      props: { filters: [] }
    })
    expect(wrapper.find('.search-filter-bar__filters').exists()).toBe(false)
  })

  it('should render filter selects when filters are provided', () => {
    const wrapper = mount(SearchFilterBar, {
      props: {
        filters: [
          {
            key: 'status',
            label: 'Status',
            options: [
              { value: 'active', label: 'Active' },
              { value: 'inactive', label: 'Inactive' }
            ]
          }
        ],
        filterValues: {}
      }
    })
    expect(wrapper.find('.search-filter-bar__filters').exists()).toBe(true)
    expect(wrapper.find('.search-filter-bar__filter-label').text()).toBe('Status')
    const options = wrapper.findAll('.search-filter-bar__filter-select option')
    expect(options).toHaveLength(3) // "All" + 2 options
  })

  it('should emit update:filterValues when a filter select changes', async () => {
    const wrapper = mount(SearchFilterBar, {
      props: {
        filters: [
          {
            key: 'subject',
            label: 'Subject',
            options: [
              { value: 'math', label: 'Math' },
              { value: 'science', label: 'Science' }
            ]
          }
        ],
        filterValues: {}
      }
    })

    const select = wrapper.find('.search-filter-bar__filter-select')
    await select.setValue('math')

    const emitted = wrapper.emitted('update:filterValues')!
    expect(emitted).toBeTruthy()
    expect(emitted[emitted.length - 1]).toEqual([{ subject: 'math' }])
  })

  it('should render multiple filters', () => {
    const wrapper = mount(SearchFilterBar, {
      props: {
        filters: [
          { key: 'status', label: 'Status', options: [{ value: 'active', label: 'Active' }] },
          { key: 'level', label: 'Level', options: [{ value: 'beginner', label: 'Beginner' }] }
        ],
        filterValues: {}
      }
    })

    const labels = wrapper.findAll('.search-filter-bar__filter-label')
    expect(labels).toHaveLength(2)
    expect(labels[0].text()).toBe('Status')
    expect(labels[1].text()).toBe('Level')
  })

  it('should preserve existing filter values when a new filter changes', async () => {
    const wrapper = mount(SearchFilterBar, {
      props: {
        filters: [
          { key: 'status', label: 'Status', options: [{ value: 'active', label: 'Active' }] },
          { key: 'level', label: 'Level', options: [{ value: 'beginner', label: 'Beginner' }] }
        ],
        filterValues: { status: 'active' }
      }
    })

    const selects = wrapper.findAll('.search-filter-bar__filter-select')
    await selects[1].setValue('beginner')

    const emitted = wrapper.emitted('update:filterValues')!
    expect(emitted[emitted.length - 1]).toEqual([{ status: 'active', level: 'beginner' }])
  })

  it('should render the actions slot', () => {
    const wrapper = mount(SearchFilterBar, {
      slots: {
        actions: '<button class="custom-action">Export</button>'
      }
    })
    expect(wrapper.find('.custom-action').exists()).toBe(true)
    expect(wrapper.find('.custom-action').text()).toBe('Export')
  })
})
