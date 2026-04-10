package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.TimetableEntryRequest;
import com.scholarops.model.entity.Schedule;
import com.scholarops.model.entity.ScheduleChangeJournal;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.TimetableService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
public class TimetableController {

    private final TimetableService timetableService;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    @PostMapping("/{id}/move")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<Schedule>> moveSchedule(@PathVariable Long id,
            @Valid @RequestBody TimetableEntryRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Schedule updated = timetableService.moveSchedule(id, request.getNewStartTime(),
                request.getNewEndTime(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PostMapping("/merge")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<Schedule>> mergeSchedules(@RequestBody Map<String, List<Long>> body,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Schedule merged = timetableService.mergeSchedules(body.get("scheduleIds"), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(merged));
    }

    @PostMapping("/{id}/split")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<List<Schedule>>> splitSchedule(@PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        LocalDateTime splitTime = LocalDateTime.parse(body.get("splitTime"));
        List<Schedule> result = timetableService.splitSchedule(id, splitTime, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/change-journal")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<List<ScheduleChangeJournal>>> getChangeJournal(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableService.getChangeJournal(currentUser.getId())));
    }

    @PostMapping("/undo")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<Schedule>> undo(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.undo(currentUser.getId())));
    }

    @PostMapping("/redo")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SCHEDULE_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<Schedule>> redo(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.redo(currentUser.getId())));
    }
}
