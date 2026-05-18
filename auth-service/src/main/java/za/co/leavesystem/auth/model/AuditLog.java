// AuditLog.java : records security-relevant events for auditing
package za.co.leavesystem.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 500)
    private String details;

    @Column
    private String ipAddress;

    @Column(nullable = false)
    private boolean success;

    public AuditLog() {}

    public AuditLog(String action, String username, String details, String ipAddress, boolean success) {
        this.action = action;
        this.username = username;
        this.timestamp = LocalDateTime.now();
        this.details = details;
        this.ipAddress = ipAddress;
        this.success = success;
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getUsername() { return username; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public boolean isSuccess() { return success; }
}
