package za.co.leavesystem.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.leavesystem.auth.dto.RegisterRequest;
import za.co.leavesystem.auth.dto.UpdateUserRequest;
import za.co.leavesystem.auth.dto.UserResponse;
import za.co.leavesystem.auth.service.UserManagementService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userManagementService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userManagementService.getUserById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterRequest request,
                                        HttpServletRequest httpRequest) {
        try {
            String adminUsername = (String) httpRequest.getAttribute("adminUsername");
            String ip = resolveIp(httpRequest);
            UserResponse created = userManagementService.createUser(request, adminUsername, ip);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @Valid @RequestBody UpdateUserRequest request,
                                        HttpServletRequest httpRequest) {
        try {
            String adminUsername = (String) httpRequest.getAttribute("adminUsername");
            String ip = resolveIp(httpRequest);
            UserResponse updated = userManagementService.updateUser(id, request, adminUsername, ip);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String adminUsername = (String) httpRequest.getAttribute("adminUsername");
            String ip = resolveIp(httpRequest);
            userManagementService.deleteUser(id, adminUsername, ip);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
