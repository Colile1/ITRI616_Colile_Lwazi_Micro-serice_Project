package za.co.leavesystem.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.leavesystem.auth.dto.RegisterRequest;
import za.co.leavesystem.auth.dto.UpdateUserRequest;
import za.co.leavesystem.auth.dto.UserResponse;
import za.co.leavesystem.auth.model.Role;
import za.co.leavesystem.auth.model.User;
import za.co.leavesystem.auth.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getEmail(),
                        u.getRole().name(), u.isEnabled()))
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getRole().name(), user.isEnabled());
    }

    public UserResponse createUser(RegisterRequest request, String adminUsername, String ipAddress) {
        String username = sanitise(request.getUsername());
        String email = sanitise(request.getEmail());

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(username, email,
                passwordEncoder.encode(request.getPassword()),
                Role.valueOf(request.getRole()));
        userRepository.save(user);

        auditService.logEvent("USER_CREATED", adminUsername,
                "Admin created user: " + username + " with role " + request.getRole(), ipAddress, true);

        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getRole().name(), user.isEnabled());
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request, String adminUsername, String ipAddress) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getEmail() != null) {
            String sanitisedEmail = sanitise(request.getEmail());
            // Only check uniqueness if email is actually changing
            if (!sanitisedEmail.equals(user.getEmail()) && userRepository.existsByEmail(sanitisedEmail)) {
                throw new IllegalArgumentException("Email already registered");
            }
            user.setEmail(sanitisedEmail);
        }
        if (request.getRole() != null) {
            user.setRole(Role.valueOf(request.getRole()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        userRepository.save(user);

        auditService.logEvent("USER_UPDATED", adminUsername,
                "Admin updated user: " + user.getUsername(), ipAddress, true);

        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getRole().name(), user.isEnabled());
    }

    public void deleteUser(Long id, String adminUsername, String ipAddress) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == Role.ADMIN && user.getUsername().equals(adminUsername)) {
            throw new IllegalArgumentException("Cannot delete your own admin account");
        }

        String deletedUsername = user.getUsername();
        userRepository.delete(user);

        auditService.logEvent("USER_DELETED", adminUsername,
                "Admin deleted user: " + deletedUsername, ipAddress, true);
    }

    private String sanitise(String input) {
        if (input == null) return null;
        return input.replaceAll("[<>\"'%;()&+]", "").trim();
    }
}
