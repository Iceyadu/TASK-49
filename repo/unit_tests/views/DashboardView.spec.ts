import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import DashboardView from '@/views/DashboardView.vue'

const authState = {
  user: { fullName: 'System Admin' },
  roles: ['ADMINISTRATOR'],
  isAdmin: true,
  isCurator: false,
  isInstructor: false,
  isStudent: false,
  isTA: false,
}

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => authState,
}))

describe('DashboardView', () => {
  it('renders admin cards when admin role is active', () => {
    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('Welcome, System Admin')
    expect(wrapper.text()).toContain('User Management')
    expect(wrapper.text()).toContain('Audit History')
    expect(wrapper.text()).not.toContain('Crawl Sources')
  })

  it('renders student cards for student role', () => {
    authState.roles = ['STUDENT']
    authState.user = { fullName: 'Student User' }
    authState.isAdmin = false
    authState.isStudent = true

    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('Welcome, Student User')
    expect(wrapper.text()).toContain('Learning Catalog')
    expect(wrapper.text()).toContain('My Timetable')
    expect(wrapper.text()).not.toContain('User Management')
  })
})
