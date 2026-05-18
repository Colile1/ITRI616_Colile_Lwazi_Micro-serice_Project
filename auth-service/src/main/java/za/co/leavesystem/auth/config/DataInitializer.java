// DataInitializer.java : seeds default users on startup using the application PasswordEncoder
package za.co.leavesystem.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import za.co.leavesystem.auth.model.Role;
import za.co.leavesystem.auth.model.User;
import za.co.leavesystem.auth.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUser("admin", "admin@leavesystem.co.za", "Admin@1234", Role.ADMIN);
        seedUser("lwazi.manager", "lwazi@leavesystem.co.za", "Manager@1234", Role.MANAGER);
        seedUser("colile.employee", "colile@leavesystem.co.za", "Employee@1234", Role.EMPLOYEE);
        seedUser("thabo.employee", "thabo@leavesystem.co.za", "Employee@1234", Role.EMPLOYEE);
        log.info("Default users seeded successfully.");
    }

    // Output logic: creates a user only if the username does not already exist
    private void seedUser(String username, String email, String rawPassword, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User(username, email, passwordEncoder.encode(rawPassword), role);
            userRepository.save(user);
            log.info("Seeded user: {} with role: {}", username, role);
        }
    }
}
