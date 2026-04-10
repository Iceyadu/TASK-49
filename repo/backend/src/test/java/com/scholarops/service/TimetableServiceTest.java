package com.scholarops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scholarops.exception.LockedPeriodConflictException;
import com.scholarops.model.entity.*;
import com.scholarops.repository.LockedPeriodRepository;
import com.scholarops.repository.ScheduleChangeJournalRepository;
import com.scholarops.repository.ScheduleRepository;
import com.scholarops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ScheduleChangeJournalRepository scheduleChangeJournalRepository;
    @Mock private LockedPeriodRepository lockedPeriodRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TimetableService timetableService;

    @BeforeEach
    void registerObjectMapperModules() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    private Schedule createSchedule(Long id, Long userId) {
        return Schedule.builder()
                .id(id).user(User.builder().id(userId).build())
                .title("Test Schedule")
                .startTime(LocalDateTime.of(2024, 6, 1, 9, 0))
                .endTime(LocalDateTime.of(2024, 6, 1, 10, 0))
                .isRecurring(false).build();
    }

    @Test
    void testMoveSchedule() {
        Schedule schedule = createSchedule(1L, 10L);
        LocalDateTime newStart = LocalDateTime.of(2024, 6, 1, 14, 0);
        LocalDateTime newEnd = LocalDateTime.of(2024, 6, 1, 15, 0);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(lockedPeriodRepository.findConflicting(eq(10L), any(), any()))
                .thenReturn(Collections.emptyList());
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleChangeJournalRepository.findTopByUserIdOrderBySequenceNumberDesc(10L)).thenReturn(Optional.empty());
        when(scheduleChangeJournalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.builder().id(10L).build()));

        Schedule result = timetableService.moveSchedule(1L, newStart, newEnd, 10L);

        assertNotNull(result);
        assertEquals(newStart, result.getStartTime());
        assertEquals(newEnd, result.getEndTime());
    }

    @Test
    void testMoveScheduleLockedConflict() {
        Schedule schedule = createSchedule(1L, 10L);
        LocalDateTime newStart = LocalDateTime.of(2024, 6, 1, 14, 0);
        LocalDateTime newEnd = LocalDateTime.of(2024, 6, 1, 15, 0);

        LockedPeriod locked = LockedPeriod.builder()
                .id(5L).title("Exam Period")
                .startTime(LocalDateTime.of(2024, 6, 1, 13, 0))
                .endTime(LocalDateTime.of(2024, 6, 1, 16, 0)).build();

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(lockedPeriodRepository.findConflicting(eq(10L), any(), any()))
                .thenReturn(List.of(locked));

        assertThrows(LockedPeriodConflictException.class,
                () -> timetableService.moveSchedule(1L, newStart, newEnd, 10L));
    }

    @Test
    void testMergeSchedules() {
        Schedule s1 = Schedule.builder().id(1L)
                .user(User.builder().id(10L).build())
                .title("Part A")
                .startTime(LocalDateTime.of(2024, 6, 1, 9, 0))
                .endTime(LocalDateTime.of(2024, 6, 1, 10, 0))
                .isRecurring(false).color("#FF0000").build();
        Schedule s2 = Schedule.builder().id(2L)
                .user(User.builder().id(10L).build())
                .title("Part B")
                .startTime(LocalDateTime.of(2024, 6, 1, 10, 0))
                .endTime(LocalDateTime.of(2024, 6, 1, 11, 0))
                .isRecurring(false).build();

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(s1));
        when(scheduleRepository.findById(2L)).thenReturn(Optional.of(s2));
        when(lockedPeriodRepository.findConflicting(eq(10L), any(), any()))
                .thenReturn(Collections.emptyList());
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            if (s.getId() == null) s.setId(3L);
            return s;
        });
        when(scheduleChangeJournalRepository.findTopByUserIdOrderBySequenceNumberDesc(10L)).thenReturn(Optional.empty());
        when(scheduleChangeJournalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.builder().id(10L).build()));

        Schedule result = timetableService.mergeSchedules(List.of(1L, 2L), 10L);

        assertNotNull(result);
        assertTrue(result.getTitle().contains("Part A"));
        assertEquals(LocalDateTime.of(2024, 6, 1, 9, 0), result.getStartTime());
        assertEquals(LocalDateTime.of(2024, 6, 1, 11, 0), result.getEndTime());
    }

    @Test
    void testSplitSchedule() {
        Schedule schedule = createSchedule(1L, 10L);
        LocalDateTime splitTime = LocalDateTime.of(2024, 6, 1, 9, 30);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            if (s.getId() == null) s.setId((long)(Math.random() * 1000));
            return s;
        });
        when(scheduleChangeJournalRepository.findTopByUserIdOrderBySequenceNumberDesc(10L)).thenReturn(Optional.empty());
        when(scheduleChangeJournalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.builder().id(10L).build()));

        List<Schedule> result = timetableService.splitSchedule(1L, splitTime, 10L);

        assertEquals(2, result.size());
        assertEquals(splitTime, result.get(0).getEndTime());
        assertEquals(splitTime, result.get(1).getStartTime());
    }

    @Test
    void testUndo() {
        Schedule oldState = Schedule.builder()
                .id(5L)
                .title("Restored")
                .startTime(LocalDateTime.of(2024, 6, 1, 8, 0))
                .endTime(LocalDateTime.of(2024, 6, 1, 9, 0))
                .build();
        ScheduleChangeJournal journal = ScheduleChangeJournal.builder()
                .id(1L).user(User.builder().id(10L).build())
                .schedule(Schedule.builder().id(5L).build())
                .changeType("MOVE").isUndone(false).sequenceNumber(1)
                .previousState(writeJson(oldState))
                .build();

        when(scheduleChangeJournalRepository
                .findByUserIdAndIsUndone(10L, false))
                .thenReturn(List.of(journal));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleChangeJournalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Schedule restored = timetableService.undo(10L);

        assertNotNull(restored);
        assertEquals("Restored", restored.getTitle());
        assertTrue(journal.getIsUndone());
    }

    @Test
    void testRedo() {
        String newState = "{\"id\":5,\"title\":\"Restored\",\"startTime\":\"2024-06-01T09:00\",\"endTime\":\"2024-06-01T10:00\",\"isRecurring\":false}";
        ScheduleChangeJournal journal = ScheduleChangeJournal.builder()
                .id(1L).user(User.builder().id(10L).build())
                .schedule(Schedule.builder().id(5L).build())
                .changeType("CREATE").isUndone(true).sequenceNumber(1)
                .newState(newState).build();

        when(scheduleChangeJournalRepository
                .findByUserIdAndIsUndone(10L, true))
                .thenReturn(List.of(journal));
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleChangeJournalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Schedule restored = timetableService.redo(10L);

        assertNotNull(restored);
        assertEquals("Restored", restored.getTitle());
        assertFalse(journal.getIsUndone());
    }

    @Test
    void testLockedPeriodConflictRejection() {
        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 1, 10, 0);

        LockedPeriod locked = LockedPeriod.builder()
                .id(1L).title("Finals Week")
                .startTime(LocalDateTime.of(2024, 6, 1, 8, 0))
                .endTime(LocalDateTime.of(2024, 6, 1, 17, 0)).build();

        when(lockedPeriodRepository.findConflicting(10L, start, end))
                .thenReturn(List.of(locked));

        LockedPeriodConflictException ex = assertThrows(
                LockedPeriodConflictException.class,
                () -> timetableService.checkLockedPeriodConflict(10L, start, end));

        assertEquals(1L, ex.getConflictingPeriodId());
        assertEquals("Finals Week", ex.getConflictingPeriodTitle());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
