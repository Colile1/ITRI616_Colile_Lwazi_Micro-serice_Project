// UserRepository.java : data access for user entities
package za.co.leavesystem.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.leavesystem.auth.model.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
