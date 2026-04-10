import { ref, reactive } from 'vue'

export interface DragItem<T = any> {
  data: T
  sourceIndex: number
  sourceList: string
}

export function useDragDrop<T = any>() {
  const draggedItem = ref<DragItem<T> | null>(null)
  const isDragging = ref(false)
  const dropTargetId = ref<string | null>(null)
  const dragOverIndex = ref<number | null>(null)

  function onDragStart(data: T, sourceIndex: number, sourceList: string, event?: DragEvent) {
    draggedItem.value = { data, sourceIndex, sourceList } as DragItem<T>
    isDragging.value = true

    if (event?.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move'
      event.dataTransfer.setData('text/plain', JSON.stringify({ sourceIndex, sourceList }))
    }
  }

  function onDragOver(targetId: string, overIndex: number, event?: DragEvent) {
    dropTargetId.value = targetId
    dragOverIndex.value = overIndex

    if (event) {
      event.preventDefault()
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move'
      }
    }
  }

  function onDragLeave() {
    dropTargetId.value = null
    dragOverIndex.value = null
  }

  function onDrop(
    targetList: string,
    targetIndex: number,
    callback: (item: DragItem<T>, targetList: string, targetIndex: number) => void,
    event?: DragEvent
  ) {
    if (event) event.preventDefault()

    if (draggedItem.value) {
      callback(draggedItem.value as DragItem<T>, targetList, targetIndex)
    }

    reset()
  }

  function onDragEnd() {
    reset()
  }

  function reset() {
    draggedItem.value = null
    isDragging.value = false
    dropTargetId.value = null
    dragOverIndex.value = null
  }

  function reorderList(list: T[], fromIndex: number, toIndex: number): T[] {
    const result = [...list]
    const [removed] = result.splice(fromIndex, 1)
    result.splice(toIndex, 0, removed)
    return result
  }

  function moveItemBetweenLists(
    sourceList: T[],
    targetList: T[],
    fromIndex: number,
    toIndex: number
  ): { source: T[]; target: T[] } {
    const source = [...sourceList]
    const target = [...targetList]
    const [removed] = source.splice(fromIndex, 1)
    target.splice(toIndex, 0, removed)
    return { source, target }
  }

  return {
    draggedItem,
    isDragging,
    dropTargetId,
    dragOverIndex,
    onDragStart,
    onDragOver,
    onDragLeave,
    onDrop,
    onDragEnd,
    reset,
    reorderList,
    moveItemBetweenLists
  }
}
