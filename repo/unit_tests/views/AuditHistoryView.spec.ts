import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AuditHistoryView from '@/views/admin/AuditHistoryView.vue'

const apiGetMock = vi.fn()

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => apiGetMock(...args),
  },
}))

describe('AuditHistoryView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiGetMock
      .mockResolvedValueOnce({ data: { data: { content: [{ id: 1, action: 'USER_UPDATE' }], totalElements: 1 } } })
      .mockResolvedValueOnce({ data: { data: { content: [{ id: 2, action: 'GRANT' }], totalElements: 1 } } })
  })

  it('loads audit logs on mount and permission logs on tab switch', async () => {
    const wrapper = mount(AuditHistoryView, {
      global: {
        stubs: {
          PermissionAuditLog: {
            template: '<div class="permission-log-stub">{{ entries.length }}</div>',
            props: ['entries'],
          },
        },
      },
    })

    await flushPromises()
    expect(apiGetMock).toHaveBeenCalledWith('/api/audit-logs', { params: { page: 0, size: 50 } })
    expect(wrapper.find('.permission-log-stub').text()).toBe('1')

    const tabButtons = wrapper.findAll('button')
    expect(tabButtons.length).toBeGreaterThan(1)
    await tabButtons[1].trigger('click')
    await flushPromises()

    expect(apiGetMock).toHaveBeenLastCalledWith('/api/audit-logs', { params: { page: 0, size: 50, action: 'GRANT,REVOKE' } })
    expect(wrapper.find('.permission-log-stub').text()).toBe('1')
  })
})
