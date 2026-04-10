package com.scholarops.service;

import com.scholarops.exception.*;
import com.scholarops.model.entity.*;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TimetableService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleChangeJournalRepository journalRepository;
    private final LockedPeriodRepository lockedPeriodRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public TimetableService(ScheduleRepository scheduleRepository, ScheduleChangeJournalRepository journalRepository,
            LockedPeriodRepository lockedPeriodRepository, UserRepository userRepository,
            AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.scheduleRepository = scheduleRepository;
        this.journalRepository = journalRepository;
        this.lockedPeriodRepository = lockedPeriodRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Schedule moveSchedule(Long scheduleId, LocalDateTime newStart, LocalDateTime newEnd, Long userId) {
        Schedule schedule = getOwnSchedule(scheduleId, userId);
        checkLockedPeriodConflict(userId, newStart, newEnd);
        Schedule previous = snapshot(schedule);
        schedule.setStartTime(newStart);
        schedule.setEndTime(newEnd);
        schedule = scheduleRepository.save(schedule);
        recordJournal(userId, scheduleId, "MOVE", previous, schedule);
        auditLogService.log(userId, AuditAction.SCHEDULE_MOVE, "Schedule", scheduleId, "Moved schedule", null, null);
        return schedule;
    }

    @Transactional
    public Schedule mergeSchedules(List<Long> scheduleIds, Long userId) {
        if (scheduleIds == null || scheduleIds.size() < 2)
            throw new IllegalArgumentException("At least 2 schedules required for merge");
        List<Schedule> schedules = new ArrayList<>();
        for (Long id : scheduleIds) {
            schedules.add(getOwnSchedule(id, userId));
        }
        schedules.sort(Comparator.comparing(Schedule::getStartTime));
        LocalDateTime mergedStart = schedules.get(0).getStartTime();
        LocalDateTime mergedEnd = schedules.get(schedules.size() - 1).getEndTime();
        checkLockedPeriodConflict(userId, mergedStart, mergedEnd);

        Schedule merged = Schedule.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .title(schedules.get(0).getTitle() + " (merged)")
                .startTime(mergedStart).endTime(mergedEnd)
                .color(schedules.get(0).getColor()).build();
        merged = scheduleRepository.save(merged);

        for (Schedule s : schedules) {
            scheduleRepository.delete(s);
        }
        recordJournal(userId, merged.getId(), "MERGE", null, merged);
        auditLogService.log(userId, AuditAction.SCHEDULE_MERGE, "Schedule", merged.getId(), "Merged schedules", null, null);
        return merged;
    }

    @Transactional
    public List<Schedule> splitSchedule(Long scheduleId, LocalDateTime splitTime, Long userId) {
        Schedule schedule = getOwnSchedule(scheduleId, userId);
        if (!splitTime.isAfter(schedule.getStartTime()) || !splitTime.isBefore(schedule.getEndTime()))
            throw new IllegalArgumentException("Split time must be within the schedule's time range");

        User user = userRepository.findById(userId).orElseThrow();
        Schedule first = Schedule.builder().user(user).title(schedule.getTitle() + " (1)")
                .startTime(schedule.getStartTime()).endTime(splitTime).color(schedule.getColor()).build();
        Schedule second = Schedule.builder().user(user).title(schedule.getTitle() + " (2)")
                .startTime(splitTime).endTime(schedule.getEndTime()).color(schedule.getColor()).build();
        first = scheduleRepository.save(first);
        second = scheduleRepository.save(second);
        scheduleRepository.delete(schedule);
        recordJournal(userId, scheduleId, "SPLIT", schedule, null);
        auditLogService.log(userId, AuditAction.SCHEDULE_SPLIT, "Schedule", scheduleId, "Split schedule", null, null);
        return List.of(first, second);
    }

    @Transactional
    public Schedule undo(Long userId) {
        List<ScheduleChangeJournal> entries = journalRepository.findByUserIdAndIsUndone(userId, false);
        if (entries.isEmpty()) throw new ResourceNotFoundException("Nothing to undo");
        ScheduleChangeJournal last = entries.get(0);
        last.setIsUndone(true);
        journalRepository.save(last);
        // Restore previous state if available
        if (last.getPreviousState() != null && last.getSchedule() != null) {
            try {
                Schedule restored = objectMapper.readValue(last.getPreviousState(), Schedule.class);
                return scheduleRepository.save(restored);
            } catch (Exception e) { /* fall through */ }
        }
        return null;
    }

    @Transactional
    public Schedule redo(Long userId) {
        List<ScheduleChangeJournal> undoneEntries = journalRepository.findByUserIdAndIsUndone(userId, true);
        if (undoneEntries.isEmpty()) throw new ResourceNotFoundException("Nothing to redo");
        ScheduleChangeJournal last = undoneEntries.get(undoneEntries.size() - 1);
        last.setIsUndone(false);
        journalRepository.save(last);
        if (last.getNewState() != null && last.getSchedule() != null) {
            try {
                Schedule restored = objectMapper.readValue(last.getNewState(), Schedule.class);
                return scheduleRepository.save(restored);
            } catch (Exception e) { /* fall through */ }
        }
        return null;
    }

    public List<ScheduleChangeJournal> getChangeJournal(Long userId) {
        return journalRepository.findByUserIdOrderBySequenceNumberDesc(userId);
    }

    public void checkLockedPeriodConflict(Long userId, LocalDateTime start, LocalDateTime end) {
        List<LockedPeriod> conflicts = lockedPeriodRepository.findConflicting(userId, start, end);
        if (!conflicts.isEmpty()) {
            LockedPeriod conflict = conflicts.get(0);
            throw new LockedPeriodConflictException(
                    "Schedule conflicts with locked period: " + conflict.getTitle() +
                    " (" + conflict.getStartTime() + " - " + conflict.getEndTime() + ")",
                    conflict.getId(), conflict.getTitle(), conflict.getStartTime(), conflict.getEndTime());
        }
    }

    public LockedPeriod createLockedPeriod(Long userId, String title, LocalDateTime start, LocalDateTime end, String reason) {
        User user = userRepository.findById(userId).orElseThrow();
        LockedPeriod period = LockedPeriod.builder().user(user).title(title)
                .startTime(start).endTime(end).reason(reason).build();
        return lockedPeriodRepository.save(period);
    }

    public void deleteLockedPeriod(Long id, Long userId) {
        LockedPeriod period = lockedPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Locked period not found"));
        if (!period.getUser().getId().equals(userId))
            throw new ForbiddenException("Not authorized");
        lockedPeriodRepository.delete(period);
    }

    public List<LockedPeriod> getLockedPeriods(Long userId) {
        return lockedPeriodRepository.findByUserId(userId);
    }

    void recordJournal(Long userId, Long scheduleId, String changeType, Schedule previous, Schedule current) {
        int nextSeq = journalRepository.findTopByUserIdOrderBySequenceNumberDesc(userId)
                .map(j -> j.getSequenceNumber() + 1).orElse(1);
        try {
            ScheduleChangeJournal entry = ScheduleChangeJournal.builder()
                    .user(userRepository.findById(userId).orElseThrow())
                    .schedule(scheduleId != null ? scheduleRepository.findById(scheduleId).orElse(null) : null)
                    .changeType(changeType)
                    .previousState(previous != null ? objectMapper.writeValueAsString(previous) : null)
                    .newState(current != null ? objectMapper.writeValueAsString(current) : null)
                    .sequenceNumber(nextSeq).build();
            journalRepository.save(entry);
        } catch (Exception e) { /* log and continue */ }
    }

    private Schedule getOwnSchedule(Long id, Long userId) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + id));
        if (!schedule.getUser().getId().equals(userId))
            throw new ForbiddenException("Not authorized");
        return schedule;
    }

    private Schedule snapshot(Schedule s) {
        return Schedule.builder().id(s.getId()).title(s.getTitle()).startTime(s.getStartTime())
                .endTime(s.getEndTime()).dayOfWeek(s.getDayOfWeek()).color(s.getColor()).build();
    }
}
