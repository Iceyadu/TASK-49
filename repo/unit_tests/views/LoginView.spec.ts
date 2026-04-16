import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import LoginView from '@/views/LoginView.vue'

const pushMock = vi.fn()
const loginMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    login: loginMock,
  }),
}))

describe('LoginView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
})

  it('renders login form and app title', () => {
    const wrapper = mount(LoginView)
    expect(wrapper.text()).toContain('ScholarOps')
    expect(wrapper.find('#username').exists()).toBe(true)
    expect(wrapper.find('#password').exists()).toBe(true)
  })

  it('calls auth store login on submit success', async () => {
    loginMock.mockResolvedValue(undefined)
    const wrapper = mount(LoginView)

    await wrapper.get('#username').setValue('admin')
    await wrapper.get('#password').setValue('Admin@12345678')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(loginMock).toHaveBeenCalledWith('admin', 'Admin@12345678')
  })

  it('shows fallback invalid credentials message on login failure', async () => {
    loginMock.mockRejectedValue(new Error('boom'))
    const wrapper = mount(LoginView)

    await wrapper.get('#username').setValue('admin')
    await wrapper.get('#password').setValue('wrong')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Invalid credentials')
  })
})
