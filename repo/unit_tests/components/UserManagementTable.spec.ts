import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import UserManagementTable from '@/components/admin/UserManagementTable.vue'

interface Role { id: number; name: string }
interface User {
  id: number
  username: string
  email: string
  fullName: string
  roles: Role[]
  enabled: boolean
  accountLocked: boolean
}

const sampleUsers: User[] = [
  {
    id: 1, username: 'alice', email: 'alice@example.com', fullName: 'Alice Admin',
    roles: [{ id: 1, name: 'ADMINISTRATOR' }], enabled: true, accountLocked: false,
  },
  {
    id: 2, username: 'bob', email: 'bob@example.com', fullName: 'Bob Student',
    roles: [{ id: 5, name: 'STUDENT' }], enabled: false, accountLocked: false,
  },
  {
    id: 3, username: 'charlie', email: 'charlie@example.com', fullName: 'Charlie Locked',
    roles: [{ id: 5, name: 'STUDENT' }], enabled: true, accountLocked: true,
  },
]

describe('UserManagementTable Component', () => {
  it('shows loading spinner when loading is true', () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: [], loading: true, totalItems: 0 }
    })
    expect(wrapper.findComponent({ name: 'LoadingSpinner' }).exists()).toBe(true)
    expect(wrapper.find('table').exists()).toBe(false)
  })

  it('shows empty state when no users match filters', () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: [], loading: false, totalItems: 0 }
    })
    expect(wrapper.findComponent({ name: 'EmptyState' }).exists()).toBe(true)
    expect(wrapper.find('table').exists()).toBe(false)
  })

  it('renders a table row for each user', () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const rows = wrapper.findAll('tbody tr')
    expect(rows.length).toBe(3)
  })

  it('renders username in dedicated cell', () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const usernames = wrapper.findAll('.user-mgmt-table__username')
    expect(usernames[0].text()).toBe('alice')
    expect(usernames[1].text()).toBe('bob')
  })

  it('renders email in table cell', () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const rows = wrapper.findAll('tbody tr')
    expect(rows[0].text()).toContain('alice@example.com')
  })

  it('renders role chip with underscores replaced by spaces', () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const roleChips = wrapper.findAll('.user-mgmt-table__role-chip')
    expect(roleChips[0].text()).toBe('ADMINISTRATOR')
    expect(roleChips[1].text()).toBe('STUDENT')
  })

  it('shows "Active" status for enabled, non-locked users', () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const statusCells = wrapper.findAll('.user-mgmt-table__status')
    expect(statusCells[0].text()).toBe('Active')
    expect(statusCells[0].classes()).toContain('user-mgmt-table__status--active')
  })

  it('shows "Disabled" status for disabled users', () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const statusCells = wrapper.findAll('.user-mgmt-table__status')
    expect(statusCells[1].text()).toBe('Disabled')
    expect(statusCells[1].classes()).toContain('user-mgmt-table__status--disabled')
  })

  it('shows "Locked" status for locked users', () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const statusCells = wrapper.findAll('.user-mgmt-table__status')
    expect(statusCells[2].text()).toBe('Locked')
    expect(statusCells[2].classes()).toContain('user-mgmt-table__status--locked')
  })

  it('emits "editRoles" with user when Roles button is clicked', async () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const rolesButtons = wrapper.findAll('button').filter(b => b.text() === 'Roles')
    await rolesButtons[0].trigger('click')
    expect(wrapper.emitted('editRoles')).toBeTruthy()
    expect(wrapper.emitted('editRoles')![0][0]).toMatchObject({ id: 1, username: 'alice' })
  })

  it('emits "resetPassword" with user when Reset Pwd button is clicked', async () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const resetButtons = wrapper.findAll('button').filter(b => b.text() === 'Reset Pwd')
    await resetButtons[0].trigger('click')
    expect(wrapper.emitted('resetPassword')).toBeTruthy()
    expect(wrapper.emitted('resetPassword')![0][0]).toMatchObject({ id: 1 })
  })

  it('emits "toggleStatus" with user when Disable/Enable button is clicked', async () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const disableButtons = wrapper.findAll('button').filter(b => b.text() === 'Disable')
    await disableButtons[0].trigger('click')
    expect(wrapper.emitted('toggleStatus')).toBeTruthy()
    expect(wrapper.emitted('toggleStatus')![0][0]).toMatchObject({ id: 1, enabled: true })
  })

  it('shows "Enable" button text for disabled users', () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const enableButtons = wrapper.findAll('button').filter(b => b.text() === 'Enable')
    expect(enableButtons.length).toBeGreaterThan(0)
  })

  it('emits "create" when Add User button is clicked', async () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const addButton = wrapper.find('.user-mgmt-table__add-btn')
    await addButton.trigger('click')
    expect(wrapper.emitted('create')).toBeTruthy()
  })

  it('filters users by search query (case-insensitive)', async () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const searchInput = wrapper.find('.search-filter-bar__input')
    await searchInput.setValue('alice')
    await wrapper.vm.$nextTick()
    const rows = wrapper.findAll('tbody tr')
    expect(rows.length).toBe(1)
    expect(rows[0].text()).toContain('alice')
  })

  it('shows all users when search query is cleared', async () => {
    const wrapper = mount(UserManagementTable, {
      props: { users: sampleUsers, loading: false, totalItems: 3 }
    })
    const searchInput = wrapper.find('.search-filter-bar__input')
    await searchInput.setValue('alice')
    await wrapper.vm.$nextTick()
    await searchInput.setValue('')
    await wrapper.vm.$nextTick()
    const rows = wrapper.findAll('tbody tr')
    expect(rows.length).toBe(3)
  })
})
