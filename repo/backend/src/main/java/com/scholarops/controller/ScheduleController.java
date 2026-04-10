package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.ScheduleRequest;
import com.scholarops.model.entity.LockedPeriod;
import com.scholarops.model.entity.Schedule;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.ScheduleService;
import com.scholarops.service.TimetableService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final TimetableService timetableService;

    public ScheduleController(ScheduleService scheduleService, TimetableService timetableService) {
        this.scheduleService = scheduleService;
        this.timetableService = timetableService;
    }

    @GetMapping("/schedules")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<List<Schedule>>> getSchedules(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.getSchedules(currentUser.getId(), start, end)));
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<Schedule>> createSchedule(@Valid @RequestBody ScheduleRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Schedule schedule = scheduleService.createSchedule(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(schedule));
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<Schedule>> updateSchedule(@PathVariable Long id,
            @Valid @RequestBody ScheduleRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.updateSchedule(id, request, currentUser.getId())));
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        scheduleService.deleteSchedule(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/locked-periods")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<List<LockedPeriod>>> getLockedPeriods(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableService.getLockedPeriods(currentUser.getId())));
    }

    @PostMapping("/locked-periods")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<LockedPeriod>> createLockedPeriod(@RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        LockedPeriod period = timetableService.createLockedPeriod(
                currentUser.getId(),
                (String) body.get("title"),
                LocalDateTime.parse((String) body.get("startTime")),
                LocalDateTime.parse((String) body.get("endTime")),
                (String) body.get("reason"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(period));
    }

    @DeleteMapping("/locked-periods/{id}")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<Void>> deleteLockedPeriod(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        timetableService.deleteLockedPeriod(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
