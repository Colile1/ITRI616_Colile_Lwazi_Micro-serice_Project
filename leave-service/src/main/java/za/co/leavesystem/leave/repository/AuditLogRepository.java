// AuditLogRepository.java : data access for leave audit log entries
package za.co.leavesystem.leave.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.leavesystem.leave.model.AuditLog;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTimestampDesc();
}
