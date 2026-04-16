import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppLayout from '@/components/layout/AppLayout.vue'

describe('AppLayout', () => {
  it('renders sidebar, header, breadcrumb and router content slots', () => {
    const wrapper = mount(AppLayout, {
      global: {
        stubs: {
          AppSidebar: { template: '<aside data-test="sidebar">Sidebar</aside>' },
          AppHeader: { template: '<header data-test="header">Header</header>' },
          AppBreadcrumb: { template: '<nav data-test="breadcrumb">Breadcrumb</nav>' },
          RouterView: { template: '<main data-test="view">View</main>' },
        },
      },
    })

    expect(wrapper.find('[data-test="sidebar"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="header"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="breadcrumb"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="view"]').exists()).toBe(true)
  })
})
