<template>
  <div class="timetable-view"><h1>My Timetable</h1><p class="subtitle">Drag and drop to rearrange. Locked periods (red) cannot be modified.</p>
    <TimetableEditor
      :sessions="sessions"
      :locked-periods="lockedPeriods"
      :undo-stack="undoStack"
      :redo-stack="redoStack"
      @slot-click="onSlotClick"
      @select-session="onSelectSession"
      @merge="onMerge"
      @split="onSplit"
      @undo="onUndo"
      @redo="onRedo"
    />
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import TimetableEditor from '@/components/student/TimetableEditor.vue'
import type { LockedPeriod, Schedule } from '@/types/schedule'
import {
  createSchedule,
  getChangeJournal,
  getLockedPeriods,
  getSchedules,
  mergeSchedules,
  redo,
  splitSchedule,
  undo,
} from '@/api/schedule'

const sessions = ref<Schedule[]>([])
const lockedPeriods = ref<LockedPeriod[]>([])
const undoStack = ref<unknown[]>([])
const redoStack = ref<unknown[]>([])
const selectedSession = ref<Schedule | null>(null)

async function loadTimetableData() {
  const [scheduleData, lockedData, journalData] = await Promise.all([
    getSchedules(),
    getLockedPeriods(),
    getChangeJournal(),
  ])
  sessions.value = scheduleData
  lockedPeriods.value = lockedData
  undoStack.value = journalData.filter(j => !j.isUndone)
  redoStack.value = journalData.filter(j => j.isUndone)
}

async function onSlotClick(dayOfWeek: number, hour: number) {
  // Use current week to compute a representative date for the given day of week
  const now = new Date()
  const currentDay = now.getDay()
  const diff = dayOfWeek - currentDay
  const targetDate = new Date(now)
  targetDate.setDate(now.getDate() + diff)
  const dateStr = targetDate.toISOString().split('T')[0]
  const start = `${dateStr}T${String(hour).padStart(2, '0')}:00:00`
  const end = `${dateStr}T${String(hour + 1).padStart(2, '0')}:00:00`
  await createSchedule({
    title: 'New Session',
    description: '',
    startTime: start,
    endTime: end,
    dayOfWeek,
    isRecurring: false,
    color: '#3b82f6',
  })
  await loadTimetableData()
}

function onSelectSession(session: Schedule) {
  selectedSession.value = session
}

async function onMerge() {
  if (sessions.value.length < 2) return
  await mergeSchedules([sessions.value[0].id, sessions.value[1].id])
  await loadTimetableData()
}

async function onSplit() {
  if (!selectedSession.value) return
  // Split at the midpoint of the session
  const start = new Date(selectedSession.value.startTime)
  const end = new Date(selectedSession.value.endTime)
  const midpoint = new Date(start.getTime() + (end.getTime() - start.getTime()) / 2)
  await splitSchedule(selectedSession.value.id, midpoint.toISOString())
  await loadTimetableData()
}

async function onUndo() {
  await undo()
  await loadTimetableData()
}

async function onRedo() {
  await redo()
  await loadTimetableData()
}

onMounted(loadTimetableData)
</script>
<style scoped>
.timetable-view { padding: 24px; }
.timetable-view h1 { color: #1a365d; margin-bottom: 4px; }
.subtitle { color: #718096; margin-bottom: 24px; font-size: 14px; }
</style>
