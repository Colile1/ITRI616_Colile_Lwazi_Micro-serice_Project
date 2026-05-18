// AuditLogRepository.java : data access for audit log entries
package za.co.leavesystem.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.leavesystem.auth.model.AuditLog;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);
    List<AuditLog> findAllByOrderByTimestampDesc();
}
