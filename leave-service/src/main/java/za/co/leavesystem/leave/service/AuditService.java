// AuditService.java : records leave-related actions to the audit log
package za.co.leavesystem.leave.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import za.co.leavesystem.leave.model.AuditLog;
import za.co.leavesystem.leave.repository.AuditLogRepository;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Output logic: persists and logs an audit entry
    public void logEvent(String action, String username, String details) {
        AuditLog entry = new AuditLog(action, username, details);
        auditLogRepository.save(entry);
        log.info("AUDIT [{}] user={} details={}", action, username, details);
    }
}
