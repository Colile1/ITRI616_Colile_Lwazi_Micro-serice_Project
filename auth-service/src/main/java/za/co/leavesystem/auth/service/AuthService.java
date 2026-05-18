// AuthService.java : core authentication business logic
package za.co.leavesystem.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.leavesystem.auth.dto.AuthResponse;
import za.co.leavesystem.auth.dto.LoginRequest;
import za.co.leavesystem.auth.dto.RegisterRequest;
import za.co.leavesystem.auth.model.Role;
import za.co.leavesystem.auth.model.User;
import za.co.leavesystem.auth.repository.UserRepository;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    // Pure function: authenticates user credentials and returns JWT
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String sanitisedUsername = sanitiseInput(request.getUsername());

        User user = userRepository.findByUsername(sanitisedUsername)
                .orElseThrow(() -> {
                    auditService.logEvent("LOGIN_FAILED", sanitisedUsername,
                            "User not found", ipAddress, false);
                    return new BadCredentialsException("Invalid credentials");
                });

        if (!user.isEnabled()) {
            auditService.logEvent("LOGIN_FAILED", sanitisedUsername,
                    "Account disabled", ipAddress, false);
            throw new BadCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditService.logEvent("LOGIN_FAILED", sanitisedUsername,
                    "Invalid password", ipAddress, false);
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        auditService.logEvent("LOGIN_SUCCESS", user.getUsername(),
                "Successful login with role " + user.getRole(), ipAddress, true);

        log.info("User {} logged in successfully from {}", user.getUsername(), ipAddress);
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), jwtService.getExpiration());
    }

    // Pure function: registers a new user account
    public void register(RegisterRequest request, String ipAddress) {
        String sanitisedUsername = sanitiseInput(request.getUsername());
        String sanitisedEmail = sanitiseInput(request.getEmail());

        if (userRepository.existsByUsername(sanitisedUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(sanitisedEmail)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(
                sanitisedUsername,
                sanitisedEmail,
                passwordEncoder.encode(request.getPassword()),
                Role.valueOf(request.getRole())
        );
        userRepository.save(user);

        auditService.logEvent("REGISTER", sanitisedUsername,
                "New account registered with role " + request.getRole(), ipAddress, true);
        log.info("New user registered: {} with role {}", sanitisedUsername, request.getRole());
    }

    // Pure function: strips HTML and script characters to prevent XSS
    private String sanitiseInput(String input) {
        if (input == null) return null;
        return input.replaceAll("[<>\"'%;()&+]", "").trim();
    }
}
