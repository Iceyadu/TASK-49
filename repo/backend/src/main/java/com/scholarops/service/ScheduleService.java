package com.scholarops.service;

import com.scholarops.exception.*;
import com.scholarops.model.dto.ScheduleRequest;
import com.scholarops.model.entity.*;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final LockedPeriodRepository lockedPeriodRepository;
    private final ScheduleChangeJournalRepository journalRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final TimetableService timetableService;

    public ScheduleService(ScheduleRepository scheduleRepository, LockedPeriodRepository lockedPeriodRepository,
            ScheduleChangeJournalRepository journalRepository, UserRepository userRepository,
            AuditLogService auditLogService, TimetableService timetableService) {
        this.scheduleRepository = scheduleRepository;
        this.lockedPeriodRepository = lockedPeriodRepository;
        this.journalRepository = journalRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.timetableService = timetableService;
    }

    @Transactional
    public Schedule createSchedule(ScheduleRequest request, Long userId) {
        timetableService.checkLockedPeriodConflict(userId, request.getStartTime(), request.getEndTime());
        User user = userRepository.findById(userId).orElseThrow();
        Schedule schedule = Schedule.builder()
                .user(user).title(request.getTitle()).description(request.getDescription())
                .startTime(request.getStartTime()).endTime(request.getEndTime())
                .dayOfWeek(request.getDayOfWeek() != null ? request.getDayOfWeek().getValue() : null)
                .isRecurring(request.getIsRecurring() != null && request.getIsRecurring())
                .color(request.getColor()).contentRecordId(request.getContentRecordId())
                .quizPaperId(request.getQuizPaperId()).build();
        schedule = scheduleRepository.save(schedule);
        timetableService.recordJournal(userId, schedule.getId(), "CREATE", null, schedule);
        auditLogService.log(userId, AuditAction.SCHEDULE_CREATE, "Schedule", schedule.getId(),
                "Created schedule: " + schedule.getTitle(), null, null);
        return schedule;
    }

    @Transactional
    public Schedule updateSchedule(Long id, ScheduleRequest request, Long userId) {
        Schedule schedule = getOwnSchedule(id, userId);
        timetableService.checkLockedPeriodConflict(userId, request.getStartTime(), request.getEndTime());
        Schedule previous = cloneSchedule(schedule);
        schedule.setTitle(request.getTitle());
        schedule.setDescription(request.getDescription());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setDayOfWeek(request.getDayOfWeek() != null ? request.getDayOfWeek().getValue() : null);
        schedule.setColor(request.getColor());
        schedule = scheduleRepository.save(schedule);
        timetableService.recordJournal(userId, schedule.getId(), "UPDATE", previous, schedule);
        auditLogService.log(userId, AuditAction.SCHEDULE_UPDATE, "Schedule", id, "Updated schedule", null, null);
        return schedule;
    }

    @Transactional
    public void deleteSchedule(Long id, Long userId) {
        Schedule schedule = getOwnSchedule(id, userId);
        timetableService.recordJournal(userId, schedule.getId(), "DELETE", schedule, null);
        scheduleRepository.delete(schedule);
        auditLogService.log(userId, AuditAction.SCHEDULE_DELETE, "Schedule", id, "Deleted schedule", null, null);
    }

    public List<Schedule> getSchedules(Long userId, LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            return scheduleRepository.findByUserIdAndStartTimeBetween(userId, start, end);
        }
        return scheduleRepository.findByUserId(userId);
    }

    private Schedule getOwnSchedule(Long id, Long userId) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + id));
        if (!schedule.getUser().getId().equals(userId))
            throw new ForbiddenException("Not authorized to modify this schedule");
        return schedule;
    }

    private Schedule cloneSchedule(Schedule s) {
        return Schedule.builder().id(s.getId()).title(s.getTitle()).description(s.getDescription())
                .startTime(s.getStartTime()).endTime(s.getEndTime()).dayOfWeek(s.getDayOfWeek())
                .color(s.getColor()).build();
    }
}
