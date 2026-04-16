package com.scholarops.service;

import com.scholarops.exception.ForbiddenException;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.dto.ScheduleRequest;
import com.scholarops.model.entity.Schedule;
import com.scholarops.model.entity.User;
import com.scholarops.repository.LockedPeriodRepository;
import com.scholarops.repository.ScheduleChangeJournalRepository;
import com.scholarops.repository.ScheduleRepository;
import com.scholarops.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private LockedPeriodRepository lockedPeriodRepository;
    @Mock private ScheduleChangeJournalRepository journalRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private TimetableService timetableService;

    @InjectMocks
    private ScheduleService scheduleService;

    private User buildUser(Long id) {
        return User.builder().id(id).username("user" + id)
                .email("user" + id + "@test.com").enabled(true).accountLocked(false).build();
    }

    private ScheduleRequest buildRequest(String title) {
        ScheduleRequest req = new ScheduleRequest();
        req.setTitle(title);
        req.setStartTime(LocalDateTime.of(2025, 1, 10, 9, 0));
        req.setEndTime(LocalDateTime.of(2025, 1, 10, 10, 0));
        req.setIsRecurring(false);
        return req;
    }

    @Test
    void createScheduleSuccessfully() {
        User user = buildUser(1L);
        ScheduleRequest request = buildRequest("Morning Study");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });
        doNothing().when(timetableService).checkLockedPeriodConflict(any(), any(), any());
        doNothing().when(timetableService).recordJournal(any(), any(), any(), any(), any());

        Schedule result = scheduleService.createSchedule(request, 1L);

        assertNotNull(result);
        assertEquals("Morning Study", result.getTitle());
        verify(scheduleRepository).save(any(Schedule.class));
        verify(auditLogService).log(eq(1L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateScheduleSuccessfully() {
        User user = buildUser(1L);
        Schedule existing = Schedule.builder().id(50L).user(user)
                .title("Old Title")
                .startTime(LocalDateTime.of(2025, 1, 10, 9, 0))
                .endTime(LocalDateTime.of(2025, 1, 10, 10, 0))
                .isRecurring(false)
                .build();

        ScheduleRequest request = buildRequest("New Title");

        when(scheduleRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(timetableService).checkLockedPeriodConflict(any(), any(), any());
        doNothing().when(timetableService).recordJournal(any(), any(), any(), any(), any());

        Schedule result = scheduleService.updateSchedule(50L, request, 1L);

        assertEquals("New Title", result.getTitle());
        verify(auditLogService).log(eq(1L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateScheduleThrowsForbiddenForDifferentOwner() {
        User owner = buildUser(1L);
        Schedule existing = Schedule.builder().id(50L).user(owner).title("My Schedule")
                .startTime(LocalDateTime.of(2025, 1, 10, 9, 0))
                .endTime(LocalDateTime.of(2025, 1, 10, 10, 0))
                .build();

        when(scheduleRepository.findById(50L)).thenReturn(Optional.of(existing));

        assertThrows(ForbiddenException.class,
                () -> scheduleService.updateSchedule(50L, buildRequest("Hijack"), 99L));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void deleteScheduleSuccessfully() {
        User user = buildUser(1L);
        Schedule existing = Schedule.builder().id(50L).user(user).title("Delete Me")
                .startTime(LocalDateTime.of(2025, 1, 10, 9, 0))
                .endTime(LocalDateTime.of(2025, 1, 10, 10, 0))
                .build();

        when(scheduleRepository.findById(50L)).thenReturn(Optional.of(existing));
        doNothing().when(scheduleRepository).delete(existing);
        doNothing().when(timetableService).recordJournal(any(), any(), any(), any(), any());

        scheduleService.deleteSchedule(50L, 1L);

        verify(scheduleRepository).delete(existing);
        verify(auditLogService).log(eq(1L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteScheduleThrowsForbiddenForDifferentOwner() {
        User owner = buildUser(1L);
        Schedule existing = Schedule.builder().id(50L).user(owner).title("Not Mine")
                .startTime(LocalDateTime.of(2025, 1, 10, 9, 0))
                .endTime(LocalDateTime.of(2025, 1, 10, 10, 0))
                .build();

        when(scheduleRepository.findById(50L)).thenReturn(Optional.of(existing));

        assertThrows(ForbiddenException.class, () -> scheduleService.deleteSchedule(50L, 99L));
        verify(scheduleRepository, never()).delete(any());
    }

    @Test
    void deleteScheduleThrowsNotFoundForMissingId() {
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> scheduleService.deleteSchedule(999L, 1L));
    }

    @Test
    void getSchedulesWithoutDateRangeReturnsAll() {
        User user = buildUser(1L);
        when(scheduleRepository.findByUserId(1L)).thenReturn(List.of(
                Schedule.builder().id(1L).user(user).title("S1").build(),
                Schedule.builder().id(2L).user(user).title("S2").build()));

        List<Schedule> result = scheduleService.getSchedules(1L, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void getSchedulesWithDateRangeFilters() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 31, 23, 59);
        User user = buildUser(1L);

        when(scheduleRepository.findByUserIdAndStartTimeBetween(1L, start, end))
                .thenReturn(List.of(Schedule.builder().id(1L).user(user).title("Jan Schedule").build()));

        List<Schedule> result = scheduleService.getSchedules(1L, start, end);

        assertEquals(1, result.size());
        verify(scheduleRepository).findByUserIdAndStartTimeBetween(1L, start, end);
    }
}
