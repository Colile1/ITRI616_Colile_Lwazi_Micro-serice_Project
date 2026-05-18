// LeaveController.java : REST endpoints for leave management operations
package za.co.leavesystem.leave.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import za.co.leavesystem.leave.dto.LeaveRequestDto;
import za.co.leavesystem.leave.dto.ReviewRequest;
import za.co.leavesystem.leave.model.LeaveRequest;
import za.co.leavesystem.leave.service.AuditService;
import za.co.leavesystem.leave.service.LeaveService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leave")
public class LeaveController {

    private static final Logger log = LoggerFactory.getLogger(LeaveController.class);

    private final LeaveService leaveService;
    private final AuditService auditService;

    public LeaveController(LeaveService leaveService, AuditService auditService) {
        this.leaveService = leaveService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<LeaveRequest>> getLeaveRequests(Authentication auth) {
        String role = extractRole(auth);
        List<LeaveRequest> requests = leaveService.getLeaveRequests(auth.getName(), role);
        return ResponseEntity.ok(requests);
    }

    @PostMapping
    public ResponseEntity<?> createLeaveRequest(@Valid @RequestBody LeaveRequestDto dto,
                                                Authentication auth) {
        try {
            LeaveRequest created = leaveService.createLeaveRequest(dto, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelLeaveRequest(@PathVariable Long id, Authentication auth) {
        try {
            LeaveRequest cancelled = leaveService.cancelLeaveRequest(id, auth.getName());
            return ResponseEntity.ok(cancelled);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            auditService.logEvent("UNAUTHORISED_ACCESS", auth.getName(),
                    "Attempted to cancel leave request #" + id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> approveLeaveRequest(@PathVariable Long id,
                                                 @Valid @RequestBody(required = false) ReviewRequest reviewRequest,
                                                 Authentication auth) {
        try {
            LeaveRequest approved = leaveService.approveLeaveRequest(id, auth.getName(), reviewRequest);
            return ResponseEntity.ok(approved);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> rejectLeaveRequest(@PathVariable Long id,
                                                @Valid @RequestBody(required = false) ReviewRequest reviewRequest,
                                                Authentication auth) {
        try {
            LeaveRequest rejected = leaveService.rejectLeaveRequest(id, auth.getName(), reviewRequest);
            return ResponseEntity.ok(rejected);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "leave-service"));
    }

    // Pure function: extracts the first granted authority from authentication
    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_EMPLOYEE");
    }
}
