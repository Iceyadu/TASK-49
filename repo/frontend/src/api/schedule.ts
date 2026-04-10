import apiClient from '@/api/client'
import type { Schedule, LockedPeriod, ScheduleChangeJournal } from '@/types/schedule'

export async function getSchedules(userId?: number): Promise<Schedule[]> {
  const params: any = {}
  if (userId) params.userId = userId
  const { data } = await apiClient.get('/api/schedules', { params })
  return data.data
}

export async function createSchedule(schedule: Partial<Schedule>): Promise<Schedule> {
  const { data } = await apiClient.post('/api/schedules', schedule)
  return data.data
}

export async function updateSchedule(id: number, updates: Partial<Schedule>): Promise<Schedule> {
  const { data } = await apiClient.put(`/api/schedules/${id}`, updates)
  return data.data
}

export async function deleteSchedule(id: number): Promise<void> {
  await apiClient.delete(`/api/schedules/${id}`)
}

export async function moveSchedule(id: number, newStartTime: string, newEndTime: string): Promise<Schedule> {
  const { data } = await apiClient.post(`/api/schedules/${id}/move`, { newStartTime, newEndTime })
  return data.data
}

export async function mergeSchedules(scheduleIds: number[]): Promise<Schedule> {
  const { data } = await apiClient.post('/api/schedules/merge', { scheduleIds })
  return data.data
}

export async function splitSchedule(id: number, splitTime: string): Promise<Schedule[]> {
  const { data } = await apiClient.post(`/api/schedules/${id}/split`, { splitTime })
  return data.data
}

export async function getChangeJournal(): Promise<ScheduleChangeJournal[]> {
  const { data } = await apiClient.get('/api/schedules/change-journal')
  return data.data
}

export async function undo(): Promise<Schedule> {
  const { data } = await apiClient.post('/api/schedules/undo')
  return data.data
}

export async function redo(): Promise<Schedule> {
  const { data } = await apiClient.post('/api/schedules/redo')
  return data.data
}

export async function getLockedPeriods(): Promise<LockedPeriod[]> {
  const { data } = await apiClient.get('/api/locked-periods')
  return data.data
}

export async function createLockedPeriod(period: Partial<LockedPeriod>): Promise<LockedPeriod> {
  const { data } = await apiClient.post('/api/locked-periods', period)
  return data.data
}

export async function deleteLockedPeriod(id: number): Promise<void> {
  await apiClient.delete(`/api/locked-periods/${id}`)
}
