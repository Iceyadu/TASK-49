package com.scholarops.controller;

import com.scholarops.model.dto.*;
import com.scholarops.model.entity.User;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') and hasPermission(null, 'USER_MANAGE')")
    public ResponseEntity<ApiResponse<Page<User>>> listUsers(Pageable pageable,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(userService.listUsers(pageable, keyword)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') and hasPermission(null, 'USER_MANAGE')")
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody UserCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        User user = userService.createUser(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR') and hasPermission(null, 'USER_MANAGE')")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR') and hasPermission(null, 'USER_MANAGE')")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(id, request, currentUser.getId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR') and hasPermission(null, 'USER_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        userService.deleteUser(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMINISTRATOR') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id,
            @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/admin-reset-password")
    @PreAuthorize("hasRole('ADMINISTRATOR') and hasPermission(null, 'PASSWORD_ADMIN_RESET')")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(@PathVariable Long id,
            @Valid @RequestBody AdminPasswordResetRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            HttpServletRequest httpRequest) {
        String workstationId = httpRequest.getHeader("X-Workstation-Id");
        if (workstationId == null || workstationId.isBlank()) {
            workstationId = request.getWorkstationId();
        }
        userService.adminResetPassword(id, request, currentUser.getId(), workstationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
