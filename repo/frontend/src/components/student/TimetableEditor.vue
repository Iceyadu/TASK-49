<template>
  <div class="timetable-editor">
    <div class="timetable-editor__toolbar">
      <h3 class="timetable-editor__title">Weekly Timetable</h3>
      <div class="timetable-editor__actions">
        <button type="button" class="timetable-editor__btn" @click="undo" :disabled="!canUndo">Undo</button>
        <button type="button" class="timetable-editor__btn" @click="redo" :disabled="!canRedo">Redo</button>
        <button type="button" class="timetable-editor__btn timetable-editor__btn--merge" @click="emit('merge')">Merge</button>
        <button type="button" class="timetable-editor__btn timetable-editor__btn--split" @click="emit('split')">Split</button>
      </div>
    </div>

    <div v-if="conflictError" class="timetable-editor__conflict-alert" role="alert">
      <strong>Schedule Conflict:</strong> {{ conflictError }}
      <button type="button" class="timetable-editor__conflict-dismiss" @click="conflictError = ''">&times;</button>
    </div>

    <div class="timetable-editor__grid-wrapper">
      <div class="timetable-editor__grid">
        <div class="timetable-editor__time-column">
          <div class="timetable-editor__corner-cell"></div>
          <div
            v-for="hour in timeSlots"
            :key="hour"
            class="timetable-editor__time-label"
          >
            {{ formatHour(hour) }}
          </div>
        </div>

        <div
          v-for="day in days"
          :key="day.index"
          class="timetable-editor__day-column"
        >
          <div class="timetable-editor__day-header">{{ day.name }}</div>
          <div class="timetable-editor__day-slots">
            <div
              v-for="hour in timeSlots"
              :key="hour"
              class="timetable-editor__slot"
              :class="{ 'timetable-editor__slot--locked': isLocked(day.index, hour) }"
              @click="handleSlotClick(day.index, hour)"
            >
              <template v-if="isLocked(day.index, hour)">
                <div class="timetable-editor__locked-indicator" :title="getLockedReason(day.index, hour)">
                  Locked
                </div>
              </template>
            </div>

            <SessionBlock
              v-for="session in getSessionsForDay(day.index)"
              :key="session.id"
              :session="session"
              :style="sessionStyle(session)"
              class="timetable-editor__session-positioned"
              @click="emit('selectSession', session)"
              @dragstart="onDragStart(session, $event)"
              @dragend="onDragEnd"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Schedule, LockedPeriod } from '@/types/schedule'
import SessionBlock from './SessionBlock.vue'

const props = defineProps<{
  sessions: Schedule[]
  lockedPeriods: LockedPeriod[]
  undoStack: any[]
  redoStack: any[]
}>()

const emit = defineEmits<{
  merge: []
  split: []
  undo: []
  redo: []
  selectSession: [session: Schedule]
  slotClick: [dayOfWeek: number, hour: number]
  moveSession: [sessionId: number, newDay: number, newHour: number]
}>()

const conflictError = ref('')

const canUndo = computed(() => props.undoStack.length > 0)
const canRedo = computed(() => props.redoStack.length > 0)

const days = [
  { index: 1, name: 'Monday' },
  { index: 2, name: 'Tuesday' },
  { index: 3, name: 'Wednesday' },
  { index: 4, name: 'Thursday' },
  { index: 5, name: 'Friday' },
  { index: 6, name: 'Saturday' },
  { index: 0, name: 'Sunday' },
]

const timeSlots = Array.from({ length: 14 }, (_, i) => i + 7) // 7:00 to 20:00

function formatHour(hour: number): string {
  const period = hour >= 12 ? 'PM' : 'AM'
  const displayHour = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour
  return `${displayHour}:00 ${period}`
}

function getSessionsForDay(dayOfWeek: number): Schedule[] {
  return props.sessions.filter(s => s.dayOfWeek === dayOfWeek)
}

function parseHour(time: string): number {
  if (time.includes('T')) {
    const d = new Date(time)
    if (!Number.isNaN(d.getTime())) return d.getHours()
  }
  return parseInt(time.split(':')[0] || '0', 10)
}

function parseDayOfWeek(time: string): number | null {
  if (!time.includes('T')) return null
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return null
  return d.getDay()
}

function matchingLockedPeriod(dayOfWeek: number, hour: number): LockedPeriod | undefined {
  return props.lockedPeriods.find(lp => {
    const explicitDay = lp.dayOfWeek
    const derivedDay = parseDayOfWeek(lp.startTime)
    const dayMatches = explicitDay != null
      ? explicitDay === dayOfWeek
      : derivedDay != null
        ? derivedDay === dayOfWeek
        : false

    if (!dayMatches) return false

    const lpStart = parseHour(lp.startTime)
    const lpEnd = parseHour(lp.endTime)
    return hour >= lpStart && hour < lpEnd
  })
}

function isLocked(dayOfWeek: number, hour: number): boolean {
  return matchingLockedPeriod(dayOfWeek, hour) != null
}

function getLockedReason(dayOfWeek: number, hour: number): string {
  const lp = matchingLockedPeriod(dayOfWeek, hour)
  return lp?.reason || 'Locked period'
}

function sessionStyle(session: Schedule) {
  let startHour: number, startMin: number, endHour: number, endMin: number
  if (session.startTime.includes('T')) {
    const sd = new Date(session.startTime)
    const ed = new Date(session.endTime)
    startHour = sd.getHours()
    startMin = sd.getMinutes()
    endHour = ed.getHours()
    endMin = ed.getMinutes()
  } else {
    const sp = session.startTime.split(':')
    const ep = session.endTime.split(':')
    startHour = parseInt(sp[0])
    startMin = parseInt(sp[1] || '0')
    endHour = parseInt(ep[0])
    endMin = parseInt(ep[1] || '0')
  }
  const top = (startHour - 7 + startMin / 60) * 48
  const height = (endHour - startHour + (endMin - startMin) / 60) * 48
  return {
    top: `${top}px`,
    height: `${Math.max(height, 24)}px`,
    backgroundColor: session.color || '#3b82f6',
  }
}

function handleSlotClick(dayOfWeek: number, hour: number) {
  if (isLocked(dayOfWeek, hour)) return
  emit('slotClick', dayOfWeek, hour)
}

function undo() { emit('undo') }
function redo() { emit('redo') }

let draggedSession: Schedule | null = null

function onDragStart(session: Schedule, event: DragEvent) {
  draggedSession = session
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function onDragEnd() {
  draggedSession = null
}

defineExpose({ setConflictError: (msg: string) => { conflictError.value = msg } })
</script>

<style scoped>
.timetable-editor__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.timetable-editor__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
}

.timetable-editor__actions {
  display: flex;
  gap: 6px;
}

.timetable-editor__btn {
  padding: 6px 14px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  font-size: 0.82rem;
  cursor: pointer;
  color: #374151;
  transition: background 0.15s;
}

.timetable-editor__btn:hover:not(:disabled) {
  background: #f1f5f9;
}

.timetable-editor__btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.timetable-editor__btn--merge {
  color: #2563eb;
  border-color: #93c5fd;
}

.timetable-editor__btn--split {
  color: #9333ea;
  border-color: #c084fc;
}

.timetable-editor__conflict-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 0.85rem;
  margin-bottom: 16px;
}

.timetable-editor__conflict-dismiss {
  margin-left: auto;
  background: none;
  border: none;
  color: #dc2626;
  font-size: 1.2rem;
  cursor: pointer;
}

.timetable-editor__grid-wrapper {
  overflow-x: auto;
}

.timetable-editor__grid {
  display: flex;
  min-width: 900px;
}

.timetable-editor__time-column {
  flex-shrink: 0;
  width: 80px;
}

.timetable-editor__corner-cell {
  height: 40px;
  border-bottom: 2px solid #e2e8f0;
}

.timetable-editor__time-label {
  height: 48px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding-right: 8px;
  font-size: 0.72rem;
  color: #64748b;
  border-bottom: 1px solid #f1f5f9;
}

.timetable-editor__day-column {
  flex: 1;
  min-width: 100px;
  border-left: 1px solid #e2e8f0;
}

.timetable-editor__day-header {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.82rem;
  font-weight: 600;
  color: #475569;
  background: #f8fafc;
  border-bottom: 2px solid #e2e8f0;
}

.timetable-editor__day-slots {
  position: relative;
}

.timetable-editor__slot {
  height: 48px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.1s;
}

.timetable-editor__slot:hover {
  background: #eff6ff;
}

.timetable-editor__slot--locked {
  background: repeating-linear-gradient(
    45deg,
    #fef2f2,
    #fef2f2 4px,
    #fee2e2 4px,
    #fee2e2 8px
  );
  cursor: not-allowed;
}

.timetable-editor__slot--locked:hover {
  background: repeating-linear-gradient(
    45deg,
    #fef2f2,
    #fef2f2 4px,
    #fee2e2 4px,
    #fee2e2 8px
  );
}

.timetable-editor__locked-indicator {
  font-size: 0.65rem;
  color: #dc2626;
  text-align: center;
  padding-top: 14px;
  font-weight: 600;
}

.timetable-editor__session-positioned {
  position: absolute;
  left: 4px;
  right: 4px;
  z-index: 10;
}
</style>
