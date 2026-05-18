// AuditService.java : records security-relevant events to the audit log
package za.co.leavesystem.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import za.co.leavesystem.auth.model.AuditLog;
import za.co.leavesystem.auth.repository.AuditLogRepository;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Output logic: persists an audit entry and logs it
    public void logEvent(String action, String username, String details, String ipAddress, boolean success) {
        AuditLog entry = new AuditLog(action, username, details, ipAddress, success);
        auditLogRepository.save(entry);

        if (success) {
            log.info("AUDIT [{}] user={} details={} ip={}", action, username, details, ipAddress);
        } else {
            log.warn("AUDIT FAILED [{}] user={} details={} ip={}", action, username, details, ipAddress);
        }
    }
}
