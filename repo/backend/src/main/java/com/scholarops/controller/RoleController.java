package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.RoleAssignRequest;
import com.scholarops.model.entity.Role;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMINISTRATOR') and hasPermission(null, 'ROLE_ASSIGN')")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllRoles()));
    }

    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMINISTRATOR') and hasPermission(null, 'ROLE_ASSIGN')")
    public ResponseEntity<ApiResponse<Void>> assignRole(@PathVariable Long userId,
            @Valid @RequestBody RoleAssignRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        roleService.assignRole(userId, request.getRoleId(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMINISTRATOR') and hasPermission(null, 'ROLE_ASSIGN')")
    public ResponseEntity<ApiResponse<Void>> revokeRole(@PathVariable Long userId, @PathVariable Long roleId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        roleService.revokeRole(userId, roleId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
