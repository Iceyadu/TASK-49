import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ContentReviewView from '@/views/curator/ContentReviewView.vue'

const getContentMock = vi.fn()
const getContentByIdMock = vi.fn()
const publishContentMock = vi.fn()

vi.mock('@/api/content', () => ({
  getContent: (...args: unknown[]) => getContentMock(...args),
  getContentById: (...args: unknown[]) => getContentByIdMock(...args),
  publishContent: (...args: unknown[]) => publishContentMock(...args),
}))

describe('ContentReviewView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getContentMock.mockResolvedValue({
      content: [
        { id: 10, title: 'Draft Lesson', isPublished: false },
        { id: 11, title: 'Published Lesson', isPublished: true },
      ],
    })
    getContentByIdMock.mockImplementation(async (id: number) => ({
      id,
      title: id === 10 ? 'Draft Lesson' : 'Published Lesson',
      isPublished: id === 11,
    }))
    publishContentMock.mockResolvedValue({ id: 10, isPublished: true })
  })

  it('loads list and selects first content record on mount', async () => {
    const wrapper = mount(ContentReviewView, {
      global: {
        stubs: {
          ContentPreview: {
            template: '<div class="preview-stub">{{ content?.title }}</div>',
            props: ['content'],
          },
        },
      },
    })

    await flushPromises()
    expect(getContentMock).toHaveBeenCalledWith(0, 50)
    expect(getContentByIdMock).toHaveBeenCalledWith(10)
    expect(wrapper.find('.preview-stub').text()).toContain('Draft Lesson')
  })

  it('publishes selected content and refreshes list', async () => {
    const wrapper = mount(ContentReviewView, {
      global: {
        stubs: {
          ContentPreview: {
            emits: ['publish'],
            template: '<button class="publish-btn" @click="$emit(\'publish\', 10)">Publish</button>',
          },
        },
      },
    })

    await flushPromises()
    await wrapper.get('.publish-btn').trigger('click')
    await flushPromises()

    expect(publishContentMock).toHaveBeenCalledWith(10)
    expect(getContentMock).toHaveBeenCalledTimes(2)
  })
})
